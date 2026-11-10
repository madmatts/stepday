package pl.stepday.day;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;
import pl.stepday.user.AppUser;

/**
 * Granica dnia: dzień lokalny zaczyna się o {@code day_start} w strefie użytkownika
 * ({@link ZoneId}, nie stały offset), więc zaliczenie o 1:30 należy do poprzedniego dnia.
 */
@Component
public class DayClock {

    private final Clock clock;

    public DayClock(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today(AppUser user) {
        return localDate(clock.instant(), user.zone(), user.getDayStart());
    }

    public static LocalDate localDate(Instant at, ZoneId zone, LocalTime dayStart) {
        ZonedDateTime local = at.atZone(zone);
        LocalDate date = local.toLocalDate();
        return local.toLocalTime().isBefore(dayStart) ? date.minusDays(1) : date;
    }

    /** Chwila, w której zaczyna się dany dzień lokalny (z poprawką na luki DST). */
    public static Instant startOf(LocalDate date, ZoneId zone, LocalTime dayStart) {
        return ZonedDateTime.of(date, dayStart, zone).toInstant();
    }
}
