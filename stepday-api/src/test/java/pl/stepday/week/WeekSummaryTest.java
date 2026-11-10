package pl.stepday.week;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.stepday.closing.DayCloser;
import pl.stepday.support.IntegrationTest;

/** Tydzień 2–8 listopada 2026 z makiet „Sukces” i „Braki”. */
class WeekSummaryTest extends IntegrationTest {

    static final LocalDate MON = LocalDate.of(2026, 11, 2);

    @Autowired DayCloser closer;

    /** Jak dzień ma wyglądać na koniec: ile wody, co pominięte, co niezrobione, które tabletki. */
    record Plan(int water, Set<String> skip, Set<String> leave, Set<String> pills) {
        static Plan full() {
            return new Plan(8, Set.of(), Set.of(), Set.of("Witamina D3", "Magnez", "Omega-3"));
        }
    }

    void live(LocalDate date, Plan plan) throws Exception {
        clock.set(ZonedDateTime.of(date.atTime(12, 0), WARSAW));
        closer.closeDue();
        String day = json(get("/days/{d}", date));
        for (Object o : JsonPath.<List<Object>>read(day, "$.steps[*]")) {
            @SuppressWarnings("unchecked") Map<String, Object> b = (Map<String, Object>) o;
            String id = (String) b.get("id");
            String name = (String) b.get("name");
            if (plan.skip().contains(name)) {
                mvc.perform(post("/instances/{id}/skip", id)).andExpect(status().isOk());
                continue;
            }
            if (plan.leave().contains(name)) {
                continue;
            }
            switch ((String) b.get("type")) {
                case "BOOLEAN" -> mvc.perform(post("/instances/{id}/complete", id));
                case "TIMER" -> {
                    mvc.perform(post("/instances/{id}/timer/start", id));
                    mvc.perform(post("/instances/{id}/timer/stop", id));
                }
                case "NOTE" -> {
                    mvc.perform(put("/instances/{id}/note", id).content("{\"text\":\"Ok.\"}"));
                    mvc.perform(post("/instances/{id}/complete", id));
                }
                case "LIST" -> {
                    @SuppressWarnings("unchecked") List<Map<String, Object>> items = (List<Map<String, Object>>) b.get("items");
                    for (Map<String, Object> item : items) {
                        String label = (String) item.get("label");
                        if (!name.equals("Tabletki rano") || plan.pills().contains(label)) {
                            mvc.perform(put("/instances/{id}/items/{item}", id, item.get("id")).content("{\"checked\":true}"));
                        }
                    }
                }
                default -> throw new IllegalStateException();
            }
        }
        String water = JsonPath.<List<String>>read(day, "$.allDay[?(@.name=='Woda')].id").getFirst();
        for (int i = 0; i < plan.water(); i++) {
            mvc.perform(post("/instances/{id}/counter-events", water)
                    .content("{\"delta\":1,\"clientEventId\":\"%s\"}".formatted(UUID.randomUUID())));
        }
    }

    void closeWeek() {
        clock.set(ZonedDateTime.of(MON.plusDays(7).atTime(12, 0), WARSAW));
        closer.closeDue();
    }

    @Test
    void gapsMockup_wednesdayMissed_fridayPartial() throws Exception {
        createMockupTemplates();
        for (int i = 0; i < 7; i++) {
            LocalDate d = MON.plusDays(i);
            Plan plan = switch (d.getDayOfWeek()) {
                case WEDNESDAY -> new Plan(5, Set.of(), Set.of("Spacer"), Set.of("Witamina D3"));
                case FRIDAY -> new Plan(7, Set.of("Spacer"), Set.of(), Plan.full().pills());
                default -> Plan.full();
            };
            live(d, plan);
        }
        closeWeek();

        mvc.perform(get("/weeks/2026-11-02/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("GAPS"))
                .andExpect(jsonPath("$.finished").value(true))
                .andExpect(jsonPath("$.weekEnd").value("2026-11-08"))
                // „6 z 7 dni zaliczone”, „5 w pełni”, „1 z niedosytem”, „1 niezaliczony”
                .andExpect(jsonPath("$.counts.completed").value(6))
                .andExpect(jsonPath("$.counts.full").value(5))
                .andExpect(jsonPath("$.counts.partial").value(1))
                .andExpect(jsonPath("$.counts.missed").value(1))
                .andExpect(jsonPath("$.days[*].status").value(contains(
                        "FULL", "FULL", "MISSED", "FULL", "PARTIAL", "FULL", "FULL")))
                .andExpect(jsonPath("$.gaps", hasSize(2)))
                // Środa, 4 listopada: Niezaliczony
                .andExpect(jsonPath("$.gaps[0].date").value("2026-11-04"))
                .andExpect(jsonPath("$.gaps[0].status").value("MISSED"))
                .andExpect(jsonPath("$.gaps[0].blocks[*].name").value(contains("Tabletki rano", "Spacer", "Woda")))
                .andExpect(jsonPath("$.gaps[0].blocks[0].unchecked").value(contains("Magnez", "Omega-3")))
                .andExpect(jsonPath("$.gaps[0].blocks[0].checked").value(contains("Witamina D3")))
                .andExpect(jsonPath("$.gaps[0].blocks[1].state").value("MISSED"))
                .andExpect(jsonPath("$.gaps[0].blocks[2].value").value(5))
                .andExpect(jsonPath("$.gaps[0].blocks[2].target").value(8))
                .andExpect(jsonPath("$.gaps[0].blocks[2].threshold").value(7))
                .andExpect(jsonPath("$.gaps[0].blocks[2].unit").value("szklanek"))
                // Piątek, 6 listopada: Z niedosytem — woda 7/8, spacer pominięty
                .andExpect(jsonPath("$.gaps[1].date").value("2026-11-06"))
                .andExpect(jsonPath("$.gaps[1].status").value("PARTIAL"))
                .andExpect(jsonPath("$.gaps[1].blocks[*].name").value(contains("Spacer", "Woda")))
                .andExpect(jsonPath("$.gaps[1].blocks[0].state").value("SKIPPED"))
                .andExpect(jsonPath("$.gaps[1].blocks[1].value").value(7));
    }

    @Test
    void successOnlyWhenAllSevenDaysFull() throws Exception {
        createMockupTemplates();
        for (int i = 0; i < 7; i++) {
            live(MON.plusDays(i), Plan.full());
        }
        closeWeek();
        mvc.perform(get("/weeks/2026-11-02/summary"))
                .andExpect(jsonPath("$.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.counts.full").value(7))
                .andExpect(jsonPath("$.gaps", empty()));
    }

    @Test
    void onePartialDayIsEnoughForGaps() throws Exception {
        createMockupTemplates();
        for (int i = 0; i < 7; i++) {
            live(MON.plusDays(i), i == 6 ? new Plan(7, Set.of(), Set.of(), Plan.full().pills()) : Plan.full());
        }
        closeWeek();
        mvc.perform(get("/weeks/2026-11-02/summary"))
                .andExpect(jsonPath("$.outcome").value("GAPS"))
                .andExpect(jsonPath("$.counts.completed").value(7))
                .andExpect(jsonPath("$.gaps[0].date").value("2026-11-08"));
    }

    @Test
    void dayWithoutRowCountsAsMissed() throws Exception {
        createMockupTemplates();
        live(MON.plusDays(5), Plan.full()); // pierwszy raz w aplikacji w sobotę
        live(MON.plusDays(6), Plan.full());
        closeWeek();
        mvc.perform(get("/weeks/2026-11-02/summary"))
                .andExpect(jsonPath("$.outcome").value("GAPS"))
                .andExpect(jsonPath("$.counts.missed").value(5))
                .andExpect(jsonPath("$.days[0].status").value("MISSED"))
                .andExpect(jsonPath("$.gaps", hasSize(5)))
                .andExpect(jsonPath("$.gaps[0].blocks", empty()));
    }

    @Test
    void currentWeekIsNotFinishedAndMondayIsRequired() throws Exception {
        createMockupTemplates();
        live(LocalDate.of(2026, 11, 9), Plan.full());
        clock.set(NOV_10_NOON);
        mvc.perform(get("/weeks/2026-11-09/summary"))
                .andExpect(jsonPath("$.finished").value(false))
                .andExpect(jsonPath("$.days[0].status").value("FULL"))
                .andExpect(jsonPath("$.days[1].status").doesNotExist());
        mvc.perform(get("/weeks/2026-11-10/summary")).andExpect(status().isBadRequest());
    }
}
