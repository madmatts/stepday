package pl.stepday.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.day.DayClock;
import pl.stepday.template.OverduePolicy;
import pl.stepday.user.CurrentUser;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService schedule;
    private final DayClock dayClock;
    private final CurrentUser user;

    public ScheduleController(ScheduleService schedule, DayClock dayClock, CurrentUser user) {
        this.schedule = schedule;
        this.dayClock = dayClock;
        this.user = user;
    }

    public record PreviewRequest(@NotBlank String rrule, @NotNull LocalDate dtstart, LocalDate until,
                                 @Min(1) @Max(20) Integer count) {
    }

    public record Occurrence(LocalDate date, boolean today) {
    }

    public record PreviewResponse(List<Occurrence> dates, OverduePolicy defaultOverduePolicy) {
    }

    /** Ekran „Kiedy”: najbliższe terminy (w tym „dziś”) z tej samej implementacji RRULE co backend. */
    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest req) {
        LocalDate today = dayClock.today(user.get());
        int count = req.count() == null ? 3 : req.count();
        List<Occurrence> dates = schedule.upcoming(req.rrule(), req.dtstart(), req.until(), today, count).stream()
                .map(d -> new Occurrence(d, d.equals(today)))
                .toList();
        return new PreviewResponse(dates, schedule.defaultOverduePolicy(req.rrule()));
    }
}
