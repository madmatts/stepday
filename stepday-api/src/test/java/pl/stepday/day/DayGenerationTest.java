package pl.stepday.day;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.stepday.support.IntegrationTest;

class DayGenerationTest extends IntegrationTest {

    static final LocalDate NOV_10 = LocalDate.of(2026, 11, 10);

    @Autowired DayGenerator generator;

    @Test
    void generatesTuesdayFromMockup() throws Exception {
        createMockupTemplates();
        mvc.perform(get("/days/2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-10"))
                .andExpect(jsonPath("$.today").value(true))
                .andExpect(jsonPath("$.status").value("OPEN"))
                // Zakupy (co sobotę) dziś nie wypadają
                .andExpect(jsonPath("$.steps[*].name").value(contains(
                        "Otworzyć okno, rozciągnąć się", "Tabletki rano", "Wymiana filtra", "Spacer", "Wieczorna notatka")))
                .andExpect(jsonPath("$.progress.done").value(0))
                .andExpect(jsonPath("$.progress.total").value(5))
                .andExpect(jsonPath("$.steps[1].items[*].label").value(contains("Witamina D3", "Magnez", "Omega-3")))
                .andExpect(jsonPath("$.steps[2].fromCalendar").value(true))
                .andExpect(jsonPath("$.steps[0].fromCalendar").value(false))
                .andExpect(jsonPath("$.steps[3].minutes").value(20))
                .andExpect(jsonPath("$.allDay", hasSize(1)))
                .andExpect(jsonPath("$.allDay[0].name").value("Woda"))
                .andExpect(jsonPath("$.allDay[0].target").value(8))
                .andExpect(jsonPath("$.allDay[0].threshold").value(7))
                .andExpect(jsonPath("$.allDay[0].unit").value("szklanek"))
                .andExpect(jsonPath("$.week", hasSize(7)))
                .andExpect(jsonPath("$.week[0].date").value("2026-11-09"))
                .andExpect(jsonPath("$.week[1].today").value(true))
                .andExpect(jsonPath("$.board.total").value(30));
    }

    @Test
    void repeatedRequestsDoNotDuplicate() throws Exception {
        createMockupTemplates();
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/days/2026-11-10")).andExpect(status().isOk());
        }
        assertThat(count("block_instance")).isEqualTo(6);
        assertThat(count("instance_item")).isEqualTo(3);
        assertThat(count("day")).isEqualTo(1);
    }

    @Test
    void ensureDayIsIdempotentUnderConcurrency() throws Exception {
        createMockupTemplates();
        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<UUID>> calls = java.util.stream.IntStream.range(0, threads)
                    .<Callable<UUID>>mapToObj(i -> () -> {
                        start.await();
                        return generator.ensureDay(USER_ID, NOV_10);
                    }).toList();
            List<Future<UUID>> futures = calls.stream().map(pool::submit).toList();
            start.countDown();
            java.util.Set<UUID> dayIds = new java.util.HashSet<>();
            for (Future<UUID> f : futures) {
                dayIds.add(f.get());
            }
            assertThat(dayIds).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(count("day")).isEqualTo(1);
        assertThat(count("block_instance")).isEqualTo(6);
        assertThat(count("instance_item")).isEqualTo(3);
    }

    @Test
    void instanceIsFrozenCopyOfDefinition() throws Exception {
        Map<String, UUID> ids = createMockupTemplates();
        mvc.perform(get("/days/2026-11-10")).andExpect(status().isOk());

        mvc.perform(put("/templates/{id}", ids.get("Tabletki rano")).content("""
                {"type":"LIST","name":"Suplementy","items":[{"label":"Cynk"}],
                 "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-01"}}""")).andExpect(status().isOk());

        mvc.perform(get("/days/2026-11-10"))
                .andExpect(jsonPath("$.steps[1].name").value("Tabletki rano"))
                .andExpect(jsonPath("$.steps[1].items[*].label").value(contains("Witamina D3", "Magnez", "Omega-3")));

        clock.set(NOV_10_NOON.plusDays(1));
        mvc.perform(get("/days/2026-11-11"))
                .andExpect(jsonPath("$.steps[1].name").value("Suplementy"))
                .andExpect(jsonPath("$.steps[1].items[*].label").value(contains("Cynk")));
    }

    @Test
    void applyTodayRebuildsOnlyUntouchedInstance() throws Exception {
        Map<String, UUID> ids = createMockupTemplates();
        mvc.perform(get("/days/2026-11-10")).andExpect(status().isOk());
        String edit = """
                {"type":"%s","name":"%s","applyToday":true,%s
                 "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-01"}}""";

        mvc.perform(put("/templates/{id}", ids.get("Spacer"))
                .content(edit.formatted("TIMER", "Długi spacer", "\"config\":{\"minutes\":40},")))
                .andExpect(status().isOk());

        // Tabletki mają postęp (odhaczona pozycja), więc zostają jak były.
        jdbc.update("UPDATE instance_item SET checked_at = now() WHERE label = 'Witamina D3'");
        mvc.perform(put("/templates/{id}", ids.get("Tabletki rano"))
                .content(edit.formatted("LIST", "Suplementy", "\"items\":[{\"label\":\"Cynk\"}],")))
                .andExpect(status().isOk());

        mvc.perform(get("/days/2026-11-10"))
                .andExpect(jsonPath("$.steps[?(@.name=='Długi spacer')].minutes").value(contains(40)))
                .andExpect(jsonPath("$.steps[?(@.name=='Tabletki rano')]", hasSize(1)))
                .andExpect(jsonPath("$.steps[?(@.name=='Suplementy')]", hasSize(0)));
    }

    @Test
    void todayRespectsDayStartAndFutureIsRejected() throws Exception {
        createMockupTemplates();
        clock.set(ZonedDateTime.of(2026, 11, 11, 1, 30, 0, 0, WARSAW)); // po północy, przed 04:00
        mvc.perform(get("/days/today")).andExpect(jsonPath("$.date").value("2026-11-10"));
        mvc.perform(get("/days/2026-11-11")).andExpect(status().isBadRequest());

        clock.set(ZonedDateTime.of(2026, 11, 11, 4, 0, 0, 0, WARSAW));
        mvc.perform(get("/days/today")).andExpect(jsonPath("$.date").value("2026-11-11"));
    }

    @Test
    void todayAcrossSpringDstChange() throws Exception {
        createMockupTemplates();
        clock.set(java.time.Instant.parse("2027-03-28T01:30:00Z")); // 03:30 CEST, przed 04:00
        mvc.perform(get("/days/today")).andExpect(jsonPath("$.date").value("2027-03-27"));
        clock.set(java.time.Instant.parse("2027-03-28T02:00:00Z")); // 04:00 CEST
        String json = json(get("/days/today"));
        assertThat((String) JsonPath.read(json, "$.date")).isEqualTo("2027-03-28");
        assertThat((Integer) JsonPath.read(json, "$.progress.total")).isEqualTo(4); // filtr nie wypada
    }

    @Test
    void archivedTemplateDisappearsFromNewDaysButStaysInHistory() throws Exception {
        Map<String, UUID> ids = createMockupTemplates();
        mvc.perform(get("/days/2026-11-10")).andExpect(status().isOk());
        mvc.perform(delete("/templates/{id}", ids.get("Spacer"))).andExpect(status().isNoContent());
        mvc.perform(get("/days/2026-11-10")).andExpect(jsonPath("$.steps[?(@.name=='Spacer')]", hasSize(1)));
        clock.set(NOV_10_NOON.plusDays(1));
        mvc.perform(get("/days/2026-11-11")).andExpect(jsonPath("$.steps[?(@.name=='Spacer')]", hasSize(0)));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
