package pl.stepday.day;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** Granica dnia (day_start w strefie użytkownika) i przełączenie czasu letniego. */
class DayClockTest {

    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    static final LocalTime FOUR = LocalTime.of(4, 0);

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            // zwykły dzień: 01:30 lokalnie należy do poprzedniego dnia
            "2026-11-11T00:30:00Z, 2026-11-10",
            "2026-11-11T02:59:59Z, 2026-11-10",
            "2026-11-11T03:00:00Z, 2026-11-11",
            // wiosna 2027-03-28: 02:00 CET → 03:00 CEST (UTC+2), 04:00 CEST = 02:00Z
            "2027-03-28T00:59:00Z, 2027-03-27",
            "2027-03-28T01:30:00Z, 2027-03-27",
            "2027-03-28T01:59:59Z, 2027-03-27",
            "2027-03-28T02:00:00Z, 2027-03-28",
            // jesień 2027-10-31: 03:00 CEST → 02:00 CET (UTC+1), 04:00 CET = 03:00Z
            "2027-10-31T00:30:00Z, 2027-10-30",
            "2027-10-31T01:30:00Z, 2027-10-30",
            "2027-10-31T02:59:59Z, 2027-10-30",
            "2027-10-31T03:00:00Z, 2027-10-31",
    })
    void localDateAcrossDst(Instant at, LocalDate expected) {
        assertThat(DayClock.localDate(at, WARSAW, FOUR)).isEqualTo(expected);
    }

    @Test
    void dstDaysAre23And25HoursLong() {
        // Przy day_start 04:00 zmiana o 02:00/03:00 wpada w dzień lokalny POPRZEDZAJĄCY datę zmiany.
        Duration spring = Duration.between(
                DayClock.startOf(LocalDate.of(2027, 3, 27), WARSAW, FOUR),
                DayClock.startOf(LocalDate.of(2027, 3, 28), WARSAW, FOUR));
        Duration autumn = Duration.between(
                DayClock.startOf(LocalDate.of(2027, 10, 30), WARSAW, FOUR),
                DayClock.startOf(LocalDate.of(2027, 10, 31), WARSAW, FOUR));
        assertThat(spring).isEqualTo(Duration.ofHours(23));
        assertThat(autumn).isEqualTo(Duration.ofHours(25));
    }

    @Test
    void dayStartInsideSpringGapIsShiftedForward() {
        // 02:30 nie istnieje 28.03.2027 w Warszawie: dzień zaczyna się o 03:30 CEST.
        LocalTime gap = LocalTime.of(2, 30);
        assertThat(DayClock.startOf(LocalDate.of(2027, 3, 28), WARSAW, gap))
                .isEqualTo(Instant.parse("2027-03-28T01:30:00Z"));
        assertThat(DayClock.localDate(Instant.parse("2027-03-28T01:00:00Z"), WARSAW, gap))
                .isEqualTo(LocalDate.of(2027, 3, 28)); // 03:00 CEST ≥ 02:30
    }
}
