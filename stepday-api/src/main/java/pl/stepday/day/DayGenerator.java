package pl.stepday.day;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.stepday.schedule.ScheduleService;
import pl.stepday.template.BlockTemplate;
import pl.stepday.template.BlockTemplateRepository;
import pl.stepday.template.ScheduleRuleRepository;

/**
 * {@code ensureDay(user, date)}: leniwie i idempotentnie (dokument, sekcja 6). Wiersz {@code day}
 * przez {@code ON CONFLICT DO NOTHING}, instancje przez częściowy indeks
 * {@code block_instance_once_per_day}, więc równoległe żądania nie dublują bloków.
 */
@Service
public class DayGenerator {

    private final JdbcTemplate jdbc;
    private final BlockTemplateRepository templates;
    private final ScheduleRuleRepository rules;
    private final ScheduleService schedule;

    public DayGenerator(JdbcTemplate jdbc, BlockTemplateRepository templates, ScheduleRuleRepository rules,
                        ScheduleService schedule) {
        this.jdbc = jdbc;
        this.templates = templates;
        this.rules = rules;
        this.schedule = schedule;
    }

    /** Zwraca id dnia. Zamkniętego dnia nie uzupełnia o nowe bloki (historia jest zamrożona). */
    @Transactional
    public UUID ensureDay(UUID userId, LocalDate date) {
        jdbc.update("INSERT INTO day (user_id, local_date) VALUES (?, ?) ON CONFLICT DO NOTHING", userId, date);
        record Row(UUID id, boolean closed) { }
        Row day = jdbc.queryForObject("SELECT id, closed_at IS NOT NULL FROM day WHERE user_id = ? AND local_date = ?",
                (rs, i) -> new Row(rs.getObject(1, UUID.class), rs.getBoolean(2)), userId, date);
        if (day.closed()) {
            return day.id();
        }
        for (BlockTemplate t : templates.findByUserIdAndArchivedAtIsNullOrderBySortKey(userId)) {
            boolean due = rules.findById(t.getId())
                    .map(r -> schedule.occursOn(List.of(r), date))
                    .orElse(false);
            if (due) {
                insertInstance(day.id(), t.getId());
            }
        }
        return day.id();
    }

    /** Kopia definicji z szablonu (nazwa, typ, rola, config, sort_key, aktywne pozycje listy). */
    private void insertInstance(UUID dayId, UUID templateId) {
        List<UUID> inserted = jdbc.queryForList("""
                INSERT INTO block_instance (day_id, template_id, name, type, role, config, sort_key)
                SELECT ?, t.id, t.name, t.type, t.role, t.config, t.sort_key
                FROM block_template t WHERE t.id = ?
                ON CONFLICT (day_id, template_id) WHERE carried_from_id IS NULL DO NOTHING
                RETURNING id""", UUID.class, dayId, templateId);
        for (UUID instanceId : inserted) {
            jdbc.update("""
                    INSERT INTO instance_item (instance_id, label, sort_key)
                    SELECT ?, label, sort_key FROM template_item
                    WHERE template_id = ? AND archived_at IS NULL""", instanceId, templateId);
        }
    }
}
