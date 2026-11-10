package pl.stepday.closing;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pl.stepday.cache.DayCache;
import pl.stepday.day.BlockEvaluator;
import pl.stepday.day.BlockInstance;
import pl.stepday.day.BlockInstanceRepository;
import pl.stepday.day.Completion;
import pl.stepday.day.Day;
import pl.stepday.day.DayClock;
import pl.stepday.day.DayGenerator;
import pl.stepday.day.DayRepository;
import pl.stepday.day.DayStatus;
import pl.stepday.day.InstanceItem;
import pl.stepday.day.InstanceItemRepository;
import pl.stepday.day.InstanceState;
import pl.stepday.template.BlockTemplateRepository;
import pl.stepday.template.OverduePolicy;
import pl.stepday.user.AppUser;
import pl.stepday.user.AppUserRepository;

/**
 * Zamknięcie dnia (dokument, sekcja 5), osobno dla każdego użytkownika w jego strefie: dzień
 * zamyka się po przekroczeniu {@code day_start} następnego dnia. Kolejność kroków jest wiążąca:
 * najpierw CARRY_OVER (zaległość nie obniża wyniku dnia, który opuszcza), potem MISSED.
 * Wpis nigdy nie jest usuwany — zmienia się tylko jego stan.
 */
@Service
public class DayCloser {

    public static final int MAX_CARRIES = 2;
    private static final Logger log = LoggerFactory.getLogger(DayCloser.class);

    private final AppUserRepository users;
    private final DayRepository days;
    private final BlockInstanceRepository instances;
    private final InstanceItemRepository items;
    private final BlockTemplateRepository templates;
    private final DayGenerator generator;
    private final DayClock dayClock;
    private final DayCache cache;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public DayCloser(AppUserRepository users, DayRepository days, BlockInstanceRepository instances,
                     InstanceItemRepository items, BlockTemplateRepository templates, DayGenerator generator,
                     DayClock dayClock, DayCache cache, JdbcTemplate jdbc, TransactionTemplate tx) {
        this.users = users;
        this.days = days;
        this.instances = instances;
        this.items = items;
        this.templates = templates;
        this.generator = generator;
        this.dayClock = dayClock;
        this.cache = cache;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /** Zamyka wszystkie zaległe dni wszystkich użytkowników. Idempotentne. Zwraca liczbę zamkniętych dni. */
    public int closeDue() {
        int closed = 0;
        for (AppUser user : users.findAll()) {
            try {
                closed += closeDueFor(user);
            } catch (RuntimeException e) {
                log.error("Closing days failed for user {}", user.getId(), e);
            }
        }
        return closed;
    }

    /**
     * Dla jednego użytkownika: zamyka otwarte dni sprzed „dziś” w jego strefie i uzupełnia brakujące
     * dni (aplikacja nie była otwierana) aż do wczoraj. Dzień po dniu, bo zaległości łańcuchowo
     * przechodzą na kolejne dni.
     */
    public int closeDueFor(AppUser user) {
        LocalDate today = dayClock.today(user);
        // Dni zamykamy po kolei, więc wszystko przed ostatnim zamkniętym jest już zamknięte.
        LocalDate from = days.findFirstByUserIdAndClosedAtIsNotNullOrderByLocalDateDesc(user.getId())
                .map(d -> d.getLocalDate().plusDays(1))
                .or(() -> days.findFirstByUserIdOrderByLocalDateAsc(user.getId()).map(Day::getLocalDate))
                .orElse(null);
        if (from == null) {
            return 0; // użytkownik jeszcze nie zaczął — nie ma czego zamykać ani uzupełniać
        }
        int closed = 0;
        for (LocalDate d = from; d.isBefore(today); d = d.plusDays(1)) {
            LocalDate date = d;
            Boolean done = tx.execute(s -> closeDay(user.getId(), date));
            if (Boolean.TRUE.equals(done)) {
                closed++;
            }
        }
        if (closed > 0) {
            cache.evictUser(user.getId());
            log.info("Closed {} day(s) for user {} (zone {}, today {})", closed, user.getId(), user.getTimezone(), today);
        }
        return closed;
    }

    /** Jedna transakcja na dzień; blokada wiersza dnia chroni przed równoległym zapisem z telefonu. */
    boolean closeDay(UUID userId, LocalDate date) {
        boolean existed = days.findByUserIdAndLocalDate(userId, date).isPresent();
        UUID dayId = generator.ensureDay(userId, date);
        Boolean alreadyClosed = jdbc.queryForObject(
                "SELECT closed_at IS NOT NULL FROM day WHERE id = ? FOR UPDATE", Boolean.class, dayId);
        if (Boolean.TRUE.equals(alreadyClosed)) {
            return false;
        }
        Day day = days.findById(dayId).orElseThrow();
        List<BlockInstance> list = instances.findByDayIdOrderBySortKey(dayId);
        Map<UUID, List<InstanceItem>> its = new HashMap<>();
        list.forEach(bi -> its.put(bi.getId(), items.findByInstanceIdOrderBySortKey(bi.getId())));

        // 1. CARRY_OVER idzie pierwsze, więc zaległość nie obniża wyniku tego dnia.
        for (BlockInstance bi : list) {
            if (BlockEvaluator.completion(bi, its.get(bi.getId())) != Completion.NONE) {
                continue;
            }
            if (policyOf(bi) == OverduePolicy.CARRY_OVER && bi.getCarryCount() < MAX_CARRIES) {
                carryOver(userId, bi, date.plusDays(1));
            } else {
                // 2. DROP albo wyczerpany limit: niezaliczony, zostaje w historii.
                bi.setState(InstanceState.MISSED);
            }
        }
        instances.flush();

        // 3. Stan dnia po zamknięciu. Dzień nieotwierany przez użytkownika jest MISSED (dokument, sekcja 5).
        List<Completion> completions = instances.findByDayIdOrderBySortKey(dayId).stream()
                .map(bi -> BlockEvaluator.completion(bi, its.getOrDefault(bi.getId(), List.of())))
                .toList();
        day.setStatus(existed ? BlockEvaluator.dayStatus(completions, true) : DayStatus.MISSED);
        day.close(dayClock.now());
        return true;
    }

    /** Polityka z szablonu; szablon usunięty → DROP (A22). */
    private OverduePolicy policyOf(BlockInstance bi) {
        if (bi.getTemplateId() == null) {
            return OverduePolicy.DROP;
        }
        return templates.findById(bi.getTemplateId()).map(t -> t.getOverduePolicy()).orElse(OverduePolicy.DROP);
    }

    /**
     * Instancja źródłowa → CARRIED; w kolejnym dniu nowa instancja z {@code carried_from_id} i
     * {@code carry_count + 1}, z kopią zamrożonej definicji i pozycji listy (A23).
     */
    private void carryOver(UUID userId, BlockInstance bi, LocalDate nextDate) {
        bi.setState(InstanceState.CARRIED);
        instances.flush();
        UUID nextDay = generator.ensureDay(userId, nextDate);
        UUID copy = jdbc.queryForObject("""
                INSERT INTO block_instance (day_id, template_id, carried_from_id, carry_count,
                                            name, type, role, config, sort_key, updated_at)
                SELECT ?, template_id, id, carry_count + 1, name, type, role, config, sort_key, 'epoch'::timestamptz
                FROM block_instance WHERE id = ?
                RETURNING id""", UUID.class, nextDay, bi.getId());
        jdbc.update("""
                INSERT INTO instance_item (instance_id, label, sort_key, checked_at, updated_at)
                SELECT ?, label, sort_key, checked_at, 'epoch'::timestamptz FROM instance_item WHERE instance_id = ?""",
                copy, bi.getId());
    }
}
