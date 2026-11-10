package pl.stepday.user;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.cache.DayCache;
import pl.stepday.day.DayClock;

/** Ustawienia użytkownika: strefa i granica dnia (decyzja D4). */
@RestController
@RequestMapping("/me")
public class MeController {

    private final CurrentUser currentUser;
    private final DayClock dayClock;
    private final DayCache cache;

    public MeController(CurrentUser currentUser, DayClock dayClock, DayCache cache) {
        this.currentUser = currentUser;
        this.dayClock = dayClock;
        this.cache = cache;
    }

    public record Me(String timezone, LocalTime dayStart, LocalDate today) {
    }

    public record MePatch(String timezone, @Pattern(regexp = "\\d{2}:\\d{2}(:\\d{2})?") String dayStart) {
    }

    @GetMapping
    public Me me() {
        AppUser u = currentUser.get();
        return new Me(u.getTimezone(), u.getDayStart(), dayClock.today(u));
    }

    /**
     * Zmiana strefy nie przelicza istniejących dni ({@code local_date} jest datą — dokument, sekcja 6).
     */
    @PatchMapping
    @Transactional
    public Me patch(@RequestBody MePatch patch) {
        AppUser u = currentUser.get();
        if (patch.timezone() != null) {
            try {
                u.setTimezone(ZoneId.of(patch.timezone()).getId());
            } catch (java.time.DateTimeException e) {
                throw new IllegalArgumentException("Nieznana strefa czasowa: " + patch.timezone());
            }
        }
        if (patch.dayStart() != null) {
            u.setDayStart(LocalTime.parse(patch.dayStart()));
        }
        cache.evictUser(u.getId());
        return new Me(u.getTimezone(), u.getDayStart(), dayClock.today(u));
    }
}
