package pl.stepday.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.stepday.support.IntegrationTest;
import pl.stepday.support.Mockup;

class DayClosingTest extends IntegrationTest {

    @Autowired DayCloser closer;

    /** 04:00 następnego dnia w Warszawie — chwila zamknięcia dnia {@code date}. */
    void closeAfter(LocalDate date) {
        clock.set(ZonedDateTime.of(date.plusDays(1).atTime(4, 0, 30), WARSAW));
        closer.closeDue();
    }

    String state(LocalDate date, String name) {
        return jdbc.queryForObject("""
                SELECT bi.state::text FROM block_instance bi JOIN day d ON d.id = bi.day_id
                WHERE d.user_id = ? AND d.local_date = ? AND bi.name = ? ORDER BY bi.carry_count DESC LIMIT 1""",
                String.class, USER_ID, date, name);
    }

    String dayStatus(LocalDate date) {
        return jdbc.queryForObject("SELECT status::text FROM day WHERE user_id = ? AND local_date = ?",
                String.class, USER_ID, date);
    }

    String id(String dayJson, String name) {
        List<String> ids = JsonPath.read(dayJson, "$..[?(@.name=='" + name + "')].id");
        return ids.getFirst();
    }

    static final LocalDate NOV_10 = LocalDate.of(2026, 11, 10);

    @Test
    void doesNotCloseBeforeDayStart() throws Exception {
        createTemplate(Mockup.OKNO);
        mvc.perform(get("/days/2026-11-10"));
        clock.set(ZonedDateTime.of(2026, 11, 11, 3, 59, 0, 0, WARSAW));
        assertThat(closer.closeDue()).isZero();
        assertThat(dayStatus(NOV_10)).isEqualTo("OPEN");
    }

    @Test
    void dropBlockBecomesMissedAndSoDoesTheDay() throws Exception {
        createTemplate(Mockup.OKNO);
        createTemplate(Mockup.SPACER);
        String d = json(get("/days/2026-11-10"));
        mvc.perform(post("/instances/{id}/complete", id(d, "Otworzyć okno, rozciągnąć się")));

        closeAfter(NOV_10);

        assertThat(state(NOV_10, "Spacer")).isEqualTo("MISSED");
        assertThat(state(NOV_10, "Otworzyć okno, rozciągnąć się")).isEqualTo("DONE");
        assertThat(dayStatus(NOV_10)).isEqualTo("MISSED");
        assertThat(jdbc.queryForObject("SELECT closed_at IS NOT NULL FROM day WHERE local_date = ?", Boolean.class, NOV_10)).isTrue();
        // wpis nie znika
        assertThat(jdbc.queryForObject("SELECT count(*) FROM block_instance WHERE name = 'Spacer'", Integer.class)).isEqualTo(1);
    }

    @Test
    void carryOverMovesBlockWithoutLoweringSourceDay() throws Exception {
        createTemplate(Mockup.OKNO);
        createTemplate(Mockup.FILTR); // co 3 miesiące → CARRY_OVER
        String d = json(get("/days/2026-11-10"));
        mvc.perform(post("/instances/{id}/complete", id(d, "Otworzyć okno, rozciągnąć się")));

        closeAfter(NOV_10);

        assertThat(state(NOV_10, "Wymiana filtra")).isEqualTo("CARRIED");
        assertThat(dayStatus(NOV_10)).isEqualTo("FULL");
        mvc.perform(get("/days/2026-11-11"))
                .andExpect(jsonPath("$.steps[?(@.name=='Wymiana filtra')].carryCount").value(org.hamcrest.Matchers.contains(1)))
                .andExpect(jsonPath("$.steps[?(@.name=='Wymiana filtra')].state").value(org.hamcrest.Matchers.contains("PENDING")));
        Map<String, Object> copy = jdbc.queryForMap("""
                SELECT carried_from_id, carry_count FROM block_instance bi JOIN day d ON d.id = bi.day_id
                WHERE d.local_date = '2026-11-11' AND bi.name = 'Wymiana filtra'""");
        assertThat(copy.get("carried_from_id")).isNotNull();
        assertThat(((Number) copy.get("carry_count")).intValue()).isEqualTo(1);
    }

    @Test
    void carryOverIsLimitedToTwoThenMissed() throws Exception {
        createTemplate(Mockup.FILTR);
        mvc.perform(get("/days/2026-11-10"));
        closeAfter(NOV_10);                     // → 11.11 (carry 1)
        closeAfter(NOV_10.plusDays(1));         // → 12.11 (carry 2)
        closeAfter(NOV_10.plusDays(2));         // limit wyczerpany → MISSED

        assertThat(state(NOV_10, "Wymiana filtra")).isEqualTo("CARRIED");
        assertThat(state(NOV_10.plusDays(1), "Wymiana filtra")).isEqualTo("CARRIED");
        assertThat(state(NOV_10.plusDays(2), "Wymiana filtra")).isEqualTo("MISSED");
        // 10.11 otwarty w aplikacji: jedyny blok przeniesiony → FULL. 12.11: limit wyczerpany → MISSED.
        assertThat(dayStatus(NOV_10)).isEqualTo("FULL");
        assertThat(dayStatus(NOV_10.plusDays(2))).isEqualTo("MISSED");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM block_instance bi JOIN day d ON d.id = bi.day_id
                WHERE d.local_date = '2026-11-13' AND bi.name = 'Wymiana filtra'""", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT max(carry_count) FROM block_instance", Integer.class)).isEqualTo(2);
    }

    @Test
    void completedCarriedBlockCountsOnTheNewDay() throws Exception {
        createTemplate(Mockup.FILTR);
        mvc.perform(get("/days/2026-11-10"));
        closeAfter(NOV_10);
        clock.set(ZonedDateTime.of(2026, 11, 11, 12, 0, 0, 0, WARSAW));
        String d = json(get("/days/2026-11-11"));
        mvc.perform(post("/instances/{id}/complete", id(d, "Wymiana filtra")))
                .andExpect(jsonPath("$.status").value("FULL"));
    }

    @Test
    void eachTimezoneClosesAtItsOwnDayStart() throws Exception {
        createTemplate(Mockup.OKNO);
        mvc.perform(get("/days/2026-11-10"));
        UUID ny = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, timezone) VALUES (?, 'America/New_York')", ny);
        jdbc.update("INSERT INTO day (user_id, local_date) VALUES (?, '2026-11-10')", ny);

        clock.set(Instant.parse("2026-11-11T03:30:00Z")); // Warszawa 04:30, Nowy Jork 22:30 (10.11)
        closer.closeDue();
        assertThat(jdbc.queryForObject("SELECT closed_at IS NOT NULL FROM day WHERE user_id = ?", Boolean.class, USER_ID)).isTrue();
        assertThat(jdbc.queryForObject("SELECT closed_at IS NOT NULL FROM day WHERE user_id = ?", Boolean.class, ny)).isFalse();

        clock.set(Instant.parse("2026-11-11T09:00:30Z")); // Nowy Jork 04:00 (EST, UTC−5)
        closer.closeDue();
        assertThat(jdbc.queryForObject("SELECT closed_at IS NOT NULL FROM day WHERE user_id = ?", Boolean.class, ny)).isTrue();
    }

    @Test
    void missingDaysAreBackfilledAsMissed() throws Exception {
        createTemplate(Mockup.OKNO);
        String d = json(get("/days/2026-11-10"));
        mvc.perform(post("/instances/{id}/complete", id(d, "Otworzyć okno, rozciągnąć się")));

        clock.set(ZonedDateTime.of(2026, 11, 14, 12, 0, 0, 0, WARSAW)); // 4 dni bez aplikacji
        assertThat(closer.closeDue()).isEqualTo(4); // 10, 11, 12, 13

        assertThat(dayStatus(NOV_10)).isEqualTo("FULL");
        for (int i = 1; i <= 3; i++) {
            assertThat(dayStatus(NOV_10.plusDays(i))).isEqualTo("MISSED");
            assertThat(state(NOV_10.plusDays(i), "Otworzyć okno, rozciągnąć się")).isEqualTo("MISSED");
        }
        // „dziś” powstaje dopiero przy otwarciu aplikacji (brak przeniesień, więc zamykanie go nie tworzy)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM day WHERE local_date = '2026-11-14'", Integer.class)).isZero();
    }

    @Test
    void gapAfterClosedDayIsFilledEvenWhenTodayWasOpened() throws Exception {
        createTemplate(Mockup.OKNO);
        mvc.perform(get("/days/2026-11-10"));
        closeAfter(NOV_10);
        clock.set(ZonedDateTime.of(2026, 11, 14, 12, 0, 0, 0, WARSAW));
        mvc.perform(get("/days/2026-11-14")); // użytkownik otwiera aplikację, zanim zadanie ruszy
        closer.closeDue();
        assertThat(jdbc.queryForList("SELECT local_date FROM day WHERE closed_at IS NOT NULL ORDER BY 1", LocalDate.class))
                .containsExactly(NOV_10, NOV_10.plusDays(1), NOV_10.plusDays(2), NOV_10.plusDays(3));
    }

    @Test
    void closingIsIdempotentAndClosedDayRejectsWrites() throws Exception {
        createTemplate(Mockup.OKNO);
        String d = json(get("/days/2026-11-10"));
        closeAfter(NOV_10);
        assertThat(closer.closeDue()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM day", Integer.class)).isEqualTo(1);
        mvc.perform(post("/instances/{id}/complete", id(d, "Otworzyć okno, rozciągnąć się")))
                .andExpect(status().isConflict());
        mvc.perform(get("/days/2026-11-10"))
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.status").value("MISSED"));
    }

    @Test
    void userWhoNeverStartedIsLeftAlone() {
        clock.set(ZonedDateTime.of(2026, 11, 20, 12, 0, 0, 0, WARSAW));
        assertThat(closer.closeDue()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM day", Integer.class)).isZero();
    }
}
