package pl.stepday.week;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.user.CurrentUser;
import pl.stepday.week.WeekSummaryService.WeekSummary;

@RestController
@RequestMapping("/weeks")
public class WeekController {

    private final WeekSummaryService service;
    private final CurrentUser currentUser;

    public WeekController(WeekSummaryService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    /** Ekrany „Sukces” i „Braki”. */
    @GetMapping("/{weekStart}/summary")
    public WeekSummary summary(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.summary(currentUser.get(), weekStart);
    }
}
