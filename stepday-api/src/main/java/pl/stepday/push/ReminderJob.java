package pl.stepday.push;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.stepday.day.DayClock;
import pl.stepday.day.DayDtos.BlockView;
import pl.stepday.day.DayDtos.DayView;
import pl.stepday.day.DayService;
import pl.stepday.day.InstanceState;
import pl.stepday.push.PushGateway.Result;
import pl.stepday.push.PushGateway.Subscription;
import pl.stepday.template.BlockType;
import pl.stepday.user.AppUser;
import pl.stepday.user.AppUserRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Przypomnienia z {@code reminder_time} (dokument, sekcja 8). Co minutę: dla każdego bloku, którego godzina
 * minęła w strefie użytkownika (najwyżej 30 min temu), który dziś wypada i nie jest zrobiony — jedno
 * powiadomienie na dzień ({@code reminder_log}).
 */
@Component
public class ReminderJob {

    static final Duration WINDOW = Duration.ofMinutes(30);
    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private final AppUserRepository users;
    private final DayService days;
    private final DayClock dayClock;
    private final JdbcTemplate jdbc;
    private final PushGateway gateway;
    private final JsonMapper json;

    public ReminderJob(AppUserRepository users, DayService days, DayClock dayClock, JdbcTemplate jdbc,
                       PushGateway gateway, JsonMapper json) {
        this.users = users;
        this.days = days;
        this.dayClock = dayClock;
        this.jdbc = jdbc;
        this.gateway = gateway;
        this.json = json;
    }

    /** Zwraca liczbę wysłanych przypomnień. */
    public int run() {
        int sent = 0;
        for (AppUser user : users.findAll()) {
            try {
                sent += runFor(user);
            } catch (RuntimeException e) {
                log.error("Reminders failed for user {}", user.getId(), e);
            }
        }
        return sent;
    }

    int runFor(AppUser user) {
        List<Subscription> subs = jdbc.query("SELECT endpoint, p256dh, auth FROM push_subscription WHERE user_id = ?",
                (rs, i) -> new Subscription(rs.getString(1), rs.getString(2), rs.getString(3)), user.getId());
        if (subs.isEmpty()) {
            return 0;
        }
        ZonedDateTime now = dayClock.now().atZone(user.zone());
        LocalTime wall = now.toLocalTime();
        List<UUID> due = jdbc.queryForList("""
                SELECT id FROM block_template
                WHERE user_id = ? AND archived_at IS NULL AND reminder_time IS NOT NULL
                  AND reminder_time <= ? AND reminder_time > ?""", UUID.class,
                user.getId(), wall, wall.minus(WINDOW).isAfter(wall) ? LocalTime.MIN : wall.minus(WINDOW));
        if (due.isEmpty()) {
            return 0;
        }
        LocalDate today = dayClock.today(user);
        DayView day = days.view(user, today); // leniwie generuje dzień
        int sent = 0;
        for (UUID templateId : due) {
            BlockView block = find(day, templateId);
            if (block == null || block.state() != InstanceState.PENDING) {
                continue; // dziś nie wypada albo już zrobione / pominięte
            }
            int claimed = jdbc.update("INSERT INTO reminder_log (template_id, local_date) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    templateId, today);
            if (claimed == 0) {
                continue;
            }
            String payload = json.writeValueAsString(java.util.Map.of(
                    "title", block.name(), "body", body(block), "url", "/"));
            for (Subscription s : subs) {
                if (gateway.send(s, payload) == Result.GONE) {
                    jdbc.update("DELETE FROM push_subscription WHERE endpoint = ?", s.endpoint());
                }
            }
            sent++;
        }
        return sent;
    }

    private BlockView find(DayView day, UUID templateId) {
        List<UUID> ids = jdbc.queryForList("""
                SELECT bi.id FROM block_instance bi JOIN day d ON d.id = bi.day_id
                WHERE d.user_id = (SELECT user_id FROM block_template WHERE id = ?) AND d.local_date = ?
                  AND bi.template_id = ? AND bi.carried_from_id IS NULL""", UUID.class, templateId, day.date(), templateId);
        if (ids.isEmpty()) {
            return null;
        }
        return java.util.stream.Stream.concat(day.steps().stream(), day.allDay().stream())
                .filter(b -> b.id().equals(ids.getFirst())).findFirst().orElse(null);
    }

    /** Krótki tekst po polsku; licznik pokazuje stan, reszta zaprasza do kroku. */
    static String body(BlockView b) {
        if (b.type() == BlockType.COUNTER) {
            String unit = b.unit() == null || b.unit().isBlank() ? "" : " " + b.unit();
            return "Masz " + b.value() + "/" + b.target() + unit + ". Dzień zalicza się od " + b.threshold() + ".";
        }
        if (b.type() == BlockType.LIST && b.items() != null) {
            long left = b.items().stream().filter(i -> !i.checked()).count();
            return "Do odhaczenia: " + left + ". Bez kary, jeśli trzeba — „Później”.";
        }
        return "Pora na ten krok. Bez kary, jeśli trzeba — „Później”.";
    }
}
