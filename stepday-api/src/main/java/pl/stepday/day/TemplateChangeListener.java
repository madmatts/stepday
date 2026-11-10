package pl.stepday.day;

import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import pl.stepday.cache.DayCache;
import pl.stepday.template.TemplateService.TemplateChanged;
import pl.stepday.user.AppUserRepository;

/**
 * Edycja szablonu obowiązuje od kolejnego dnia. Dzisiejsza instancja jest przebudowywana tylko przy
 * „zastosuj także dziś” i tylko wtedy, gdy nie ma w niej żadnego postępu (dokument, sekcja 6).
 */
@Component
public class TemplateChangeListener {

    private final JdbcTemplate jdbc;
    private final DayGenerator generator;
    private final DayClock dayClock;
    private final AppUserRepository users;
    private final DayCache cache;

    public TemplateChangeListener(JdbcTemplate jdbc, DayGenerator generator, DayClock dayClock,
                                  AppUserRepository users, DayCache cache) {
        this.jdbc = jdbc;
        this.generator = generator;
        this.dayClock = dayClock;
        this.users = users;
        this.cache = cache;
    }

    @EventListener
    public void onChange(TemplateChanged event) {
        users.findById(event.userId()).ifPresent(user -> {
            var today = dayClock.today(user);
            if (event.applyToday()) {
                List<java.util.UUID> untouched = jdbc.queryForList("""
                        SELECT bi.id FROM block_instance bi JOIN day d ON d.id = bi.day_id
                        WHERE d.user_id = ? AND d.local_date = ? AND d.closed_at IS NULL
                          AND bi.template_id = ? AND bi.carried_from_id IS NULL
                          AND bi.state = 'PENDING' AND bi.value_int = 0 AND bi.note_text IS NULL
                          AND bi.timer_started_at IS NULL AND bi.deferred_at IS NULL
                          AND NOT EXISTS (SELECT 1 FROM instance_item ii
                                          WHERE ii.instance_id = bi.id AND ii.checked_at IS NOT NULL)
                          AND NOT EXISTS (SELECT 1 FROM counter_event ce WHERE ce.instance_id = bi.id)""",
                        java.util.UUID.class, user.getId(), today, event.templateId());
                untouched.forEach(id -> jdbc.update("DELETE FROM block_instance WHERE id = ?", id));
                if (!untouched.isEmpty()) {
                    generator.ensureDay(user.getId(), today);
                }
            }
            cache.evictUser(user.getId());
        });
    }
}
