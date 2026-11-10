package pl.stepday.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.stepday.template.OverduePolicy;

class ScheduleServiceTest {

    private final ScheduleService schedule = new ScheduleService();
    private static final LocalDate NOV_10 = LocalDate.of(2026, 11, 10);

    @Test
    void controlExampleFromDocument() {
        // Dokument, sekcja 6: FREQ=MONTHLY;INTERVAL=3 od 2026-11-10.
        assertThat(schedule.upcoming("FREQ=MONTHLY;INTERVAL=3", NOV_10, null, NOV_10, 3))
                .containsExactly(NOV_10, LocalDate.of(2027, 2, 10), LocalDate.of(2027, 5, 10));
    }

    @ParameterizedTest(name = "{0} od {1}: {2} → {3}")
    @CsvSource({
            "FREQ=DAILY,                 2026-11-10, 2026-11-09, false",
            "FREQ=DAILY,                 2026-11-10, 2026-11-10, true",
            "FREQ=DAILY,                 2026-11-10, 2027-03-29, true",
            "FREQ=MONTHLY;INTERVAL=3,    2026-11-10, 2027-02-10, true",
            "FREQ=MONTHLY;INTERVAL=3,    2026-11-10, 2026-12-10, false",
            "'FREQ=WEEKLY;BYDAY=MO,WE,FR', 2026-11-10, 2026-11-11, true",
            "'FREQ=WEEKLY;BYDAY=MO,WE,FR', 2026-11-10, 2026-11-12, false",
            "FREQ=DAILY;COUNT=1,         2026-11-10, 2026-11-10, true",
            "FREQ=DAILY;COUNT=1,         2026-11-10, 2026-11-11, false",
            "FREQ=MONTHLY;BYMONTHDAY=31, 2026-11-10, 2027-02-28, false",
            "FREQ=MONTHLY;BYMONTHDAY=-1, 2026-11-10, 2027-02-28, true",
            "FREQ=MONTHLY;BYMONTHDAY=-1, 2026-11-10, 2026-11-30, true",
    })
    void occursOn(String rrule, LocalDate dtstart, LocalDate date, boolean expected) {
        assertThat(schedule.occursOn(rrule, dtstart, null, date)).isEqualTo(expected);
    }

    @Test
    void untilIsInclusive() {
        LocalDate until = LocalDate.of(2026, 11, 12);
        assertThat(schedule.occursOn("FREQ=DAILY", NOV_10, until, until)).isTrue();
        assertThat(schedule.occursOn("FREQ=DAILY", NOV_10, until, until.plusDays(1))).isFalse();
    }

    @Test
    void previewStartsFromTodayWhenRuleStartedEarlier() {
        assertThat(schedule.upcoming("FREQ=WEEKLY;BYDAY=SA", LocalDate.of(2026, 1, 1), null, NOV_10, 2))
                .containsExactly(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 21));
    }

    @ParameterizedTest
    @CsvSource({
            "FREQ=DAILY,                 DROP",
            "FREQ=DAILY;INTERVAL=2,      CARRY_OVER",
            "FREQ=WEEKLY;BYDAY=SA,       CARRY_OVER",
            "FREQ=MONTHLY;INTERVAL=3,    CARRY_OVER",
            "FREQ=DAILY;COUNT=1,         CARRY_OVER",
    })
    void defaultOverduePolicy(String rrule, OverduePolicy expected) {
        assertThat(schedule.defaultOverduePolicy(rrule)).isEqualTo(expected);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> schedule.parse("FREQ=SOMETIMES")).isInstanceOf(InvalidRuleException.class);
    }
}
