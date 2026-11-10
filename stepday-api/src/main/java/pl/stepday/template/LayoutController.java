package pl.stepday.template;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.day.DayClock;
import pl.stepday.schedule.ScheduleService;
import pl.stepday.template.TemplateDtos.TemplateResponse;
import pl.stepday.user.AppUser;
import pl.stepday.user.CurrentUser;

/** Ekran „Układ dnia”: szablony w kolejności ścieżki z flagą, czy wypadają w danym dniu. */
@RestController
public class LayoutController {

    private final TemplateService templates;
    private final ScheduleService schedule;
    private final DayClock dayClock;
    private final CurrentUser currentUser;

    public LayoutController(TemplateService templates, ScheduleService schedule, DayClock dayClock,
                            CurrentUser currentUser) {
        this.templates = templates;
        this.schedule = schedule;
        this.dayClock = dayClock;
        this.currentUser = currentUser;
    }

    public record LayoutBlock(TemplateResponse template, boolean dueOnDate, boolean everyDay) {
    }

    public record Layout(LocalDate date, List<LayoutBlock> steps, List<LayoutBlock> allDay) {
    }

    @GetMapping("/layout")
    public Layout layout(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AppUser user = currentUser.get();
        LocalDate d = date != null ? date : dayClock.today(user);
        List<LayoutBlock> all = templates.list(user.getId()).stream()
                .map(t -> new LayoutBlock(t,
                        schedule.occursOn(t.schedule().rrule(), t.schedule().dtstart(), t.schedule().until(), d),
                        schedule.isEveryDay(t.schedule().rrule())))
                .toList();
        return new Layout(d,
                all.stream().filter(b -> b.template().role() == BlockRole.STEP).toList(),
                all.stream().filter(b -> b.template().role() == BlockRole.ALL_DAY).toList());
    }
}
