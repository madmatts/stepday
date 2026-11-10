package pl.stepday.completion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.stepday.common.NotFoundException;
import pl.stepday.day.BlockInstance;
import pl.stepday.day.BlockInstanceRepository;
import pl.stepday.day.Day;
import pl.stepday.day.DayClock;
import pl.stepday.day.DayRepository;
import pl.stepday.day.DayService;
import pl.stepday.day.InstanceItem;
import pl.stepday.day.InstanceItemRepository;
import pl.stepday.day.InstanceState;
import pl.stepday.template.BlockConfig;
import pl.stepday.template.BlockType;

/**
 * Zaliczanie bloków (dokument, sekcja 7). Każdy zapis jest idempotentny (PWA powtarza go po
 * powrocie sieci) i kończy się przeliczeniem {@code day.status} przez serwer.
 */
@Service
public class CompletionService {

    private final BlockInstanceRepository instances;
    private final InstanceItemRepository items;
    private final DayRepository days;
    private final DayService dayService;
    private final DayClock clock;
    private final JdbcTemplate jdbc;
    private final jakarta.persistence.EntityManager em;

    public CompletionService(BlockInstanceRepository instances, InstanceItemRepository items, DayRepository days,
                             DayService dayService, DayClock clock, JdbcTemplate jdbc,
                             jakarta.persistence.EntityManager em) {
        this.instances = instances;
        this.items = items;
        this.days = days;
        this.dayService = dayService;
        this.clock = clock;
        this.jdbc = jdbc;
        this.em = em;
    }

    /** LIST: ustawia/zdejmuje checked_at; „ostatni zapis wygrywa” po czasie zmiany z klienta. */
    @Transactional
    public Day setItem(UUID userId, UUID instanceId, UUID itemId, boolean checked, Instant changedAt) {
        BlockInstance bi = open(userId, instanceId, BlockType.LIST);
        InstanceItem item = items.findById(itemId)
                .filter(i -> i.getInstanceId().equals(instanceId))
                .orElseThrow(() -> new NotFoundException("pozycja " + itemId));
        if (lastWriteWins(changedAt, item.getUpdatedAt())) {
            item.setChecked(checked, changedAt != null ? changedAt : clock.now());
            items.flush();
        }
        boolean all = items.findByInstanceIdOrderBySortKey(instanceId).stream().allMatch(InstanceItem::isChecked);
        if (all) {
            markDone(bi);
        } else if (bi.getState() == InstanceState.DONE || bi.getState() == InstanceState.SKIPPED) {
            reopen(bi);
        }
        return finish(bi);
    }

    /** BOOLEAN i NOTE: „Zrobione” / „Gotowe”. NOTE wymaga zapisanego tekstu. */
    @Transactional
    public Day complete(UUID userId, UUID instanceId) {
        BlockInstance bi = open(userId, instanceId, null);
        switch (bi.getType()) {
            case BOOLEAN -> { }
            case NOTE -> {
                if (bi.getNoteText() == null || bi.getNoteText().isBlank()) {
                    throw new IllegalArgumentException("Notatka jest pusta");
                }
            }
            default -> throw new IllegalArgumentException("complete nie dotyczy typu " + bi.getType());
        }
        markDone(bi);
        return finish(bi);
    }

    /** NOTE: zapis tekstu (bez zatwierdzenia); „ostatni zapis wygrywa”. */
    @Transactional
    public Day saveNote(UUID userId, UUID instanceId, String text, Instant changedAt) {
        BlockInstance bi = open(userId, instanceId, BlockType.NOTE);
        if (lastWriteWins(changedAt, bi.getUpdatedAt())) {
            bi.setNoteText(text == null ? null : text.strip());
            bi.touch(changedAt != null ? changedAt : clock.now());
        }
        return finish(bi);
    }

    /**
     * COUNTER: zdarzenie z kluczem idempotencji z PWA. Duplikat jest ignorowany bez błędu.
     * {@code value_int} = suma delt, aktualizowana w tej samej transakcji co zapis zdarzenia.
     */
    @Transactional
    public Day counterEvent(UUID userId, UUID instanceId, int delta, UUID clientEventId, Instant happenedAt) {
        BlockInstance bi = open(userId, instanceId, BlockType.COUNTER);
        instances.flush();
        int inserted = jdbc.update("""
                INSERT INTO counter_event (instance_id, delta, client_event_id, happened_at)
                VALUES (?, ?, ?, ?) ON CONFLICT (instance_id, client_event_id) DO NOTHING""",
                instanceId, delta, clientEventId, java.sql.Timestamp.from(happenedAt != null ? happenedAt : clock.now()));
        if (inserted == 1) {
            jdbc.update("UPDATE block_instance SET value_int = value_int + ?, updated_at = now() WHERE id = ?",
                    delta, instanceId);
            bi = reload(bi);
            int threshold = BlockConfig.threshold(bi.getConfig());
            if (bi.getValueInt() >= threshold) {
                markDone(bi);
            } else if (bi.getState() == InstanceState.DONE) {
                reopen(bi);
            }
        }
        return finish(bi);
    }

    @Transactional
    public Day startTimer(UUID userId, UUID instanceId) {
        BlockInstance bi = open(userId, instanceId, BlockType.TIMER);
        if (bi.getTimerStartedAt() == null) {
            bi.setTimerStartedAt(clock.now());
        }
        return finish(bi);
    }

    /** TIMER: stop ustawia DONE (decyzja D6: także przed czasem, bez kary). */
    @Transactional
    public Day stopTimer(UUID userId, UUID instanceId) {
        BlockInstance bi = open(userId, instanceId, BlockType.TIMER);
        if (bi.getTimerStartedAt() == null) {
            throw new IllegalStateException("Timer nie został uruchomiony");
        }
        if (bi.getTimerStoppedAt() == null) {
            bi.setTimerStoppedAt(clock.now());
        }
        markDone(bi);
        return finish(bi);
    }

    /** „Pomiń”: SKIPPED, dzień najwyżej PARTIAL, bez kary. */
    @Transactional
    public Day skip(UUID userId, UUID instanceId) {
        BlockInstance bi = open(userId, instanceId, null);
        if (bi.getState() == InstanceState.DONE) {
            throw new IllegalStateException("Blok jest już zrobiony");
        }
        bi.setState(InstanceState.SKIPPED);
        return finish(bi);
    }

    /** „Później”: tylko znacznik deferred_at, blok wraca na koniec ścieżki. */
    @Transactional
    public Day defer(UUID userId, UUID instanceId) {
        BlockInstance bi = open(userId, instanceId, null);
        bi.setDeferredAt(clock.now());
        return finish(bi);
    }

    private void markDone(BlockInstance bi) {
        if (bi.getState() != InstanceState.DONE) {
            bi.setState(InstanceState.DONE);
            bi.setCompletedAt(clock.now());
        }
    }

    private void reopen(BlockInstance bi) {
        bi.setState(InstanceState.PENDING);
        bi.setCompletedAt(null);
    }

    /**
     * „Ostatni zapis wygrywa” (dokument, sekcja 8): zapis z czasem zmiany z klienta przegrywa tylko
     * z zapisem nowszym; zapis bez czasu (online) przyjmuje czas serwera i zawsze wygrywa.
     */
    static boolean lastWriteWins(Instant changedAt, Instant storedUpdatedAt) {
        return changedAt == null || !changedAt.isBefore(storedUpdatedAt);
    }

    private Day finish(BlockInstance bi) {
        instances.flush();
        dayService.recomputeStatus(bi.getDayId());
        return days.findById(bi.getDayId()).orElseThrow();
    }

    /** Instancja użytkownika w dniu, który nie jest jeszcze zamknięty (dzień edytowalny do zamknięcia). */
    private BlockInstance open(UUID userId, UUID instanceId, BlockType expected) {
        BlockInstance bi = instances.findById(instanceId).orElseThrow(() -> new NotFoundException("blok " + instanceId));
        Day day = days.findById(bi.getDayId()).orElseThrow();
        if (!day.getUserId().equals(userId)) {
            throw new NotFoundException("blok " + instanceId);
        }
        if (day.isClosed()) {
            throw new IllegalStateException("Dzień " + day.getLocalDate() + " jest już zamknięty");
        }
        if (List.of(InstanceState.CARRIED, InstanceState.MISSED).contains(bi.getState())) {
            throw new IllegalStateException("Blok jest w stanie " + bi.getState());
        }
        if (expected != null && bi.getType() != expected) {
            throw new IllegalArgumentException("Operacja dotyczy typu " + expected + ", a blok jest typu " + bi.getType());
        }
        return bi;
    }

    /** Atomowy UPDATE w SQL (bez utraconych przyrostów), potem odświeżenie encji. */
    private BlockInstance reload(BlockInstance bi) {
        em.refresh(bi);
        return bi;
    }
}
