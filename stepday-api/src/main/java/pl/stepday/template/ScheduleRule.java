package pl.stepday.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Reguła RRULE szablonu (1:1, decyzja D5). */
@Entity
@Table(name = "schedule_rule")
public class ScheduleRule {

    @Id
    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false)
    private LocalDate dtstart;

    @Column(nullable = false)
    private String rrule;

    private LocalDate until;

    protected ScheduleRule() {
    }

    public ScheduleRule(UUID templateId, LocalDate dtstart, String rrule, LocalDate until) {
        this.templateId = templateId;
        this.dtstart = dtstart;
        this.rrule = rrule;
        this.until = until;
    }

    public UUID getTemplateId() { return templateId; }
    public LocalDate getDtstart() { return dtstart; }
    public String getRrule() { return rrule; }
    public LocalDate getUntil() { return until; }

    public void update(LocalDate dtstart, String rrule, LocalDate until) {
        this.dtstart = dtstart;
        this.rrule = rrule;
        this.until = until;
    }
}
