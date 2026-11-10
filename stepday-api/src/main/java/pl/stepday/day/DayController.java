package pl.stepday.day;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.day.DayDtos.DayView;
import pl.stepday.user.AppUser;
import pl.stepday.user.CurrentUser;

@RestController
@RequestMapping("/days")
public class DayController {

    private final DayService days;
    private final DayClock dayClock;
    private final CurrentUser currentUser;

    public DayController(DayService days, DayClock dayClock, CurrentUser currentUser) {
        this.days = days;
        this.dayClock = dayClock;
        this.currentUser = currentUser;
    }

    /** Dzisiejszy dzień lokalny użytkownika (strefa + day_start liczone na serwerze). */
    @GetMapping("/today")
    public DayView today() {
        AppUser user = currentUser.get();
        return days.view(user, dayClock.today(user));
    }

    @GetMapping("/{date}")
    public DayView day(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return days.view(currentUser.get(), date);
    }
}
