package pl.stepday.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.stepday.support.IntegrationTest;
import pl.stepday.support.Mockup;

class CompletionApiTest extends IntegrationTest {

    String day() throws Exception {
        return json(get("/days/2026-11-10"));
    }

    String id(String dayJson, String name) {
        List<String> ids = JsonPath.read(dayJson, "$..[?(@.name=='" + name + "')].id");
        return ids.getFirst();
    }

    void water(String instanceId, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            mvc.perform(post("/instances/{id}/counter-events", instanceId)
                    .content("{\"delta\":1,\"clientEventId\":\"%s\"}".formatted(UUID.randomUUID())))
                    .andExpect(status().isOk());
        }
    }

    @ParameterizedTest(name = "woda {0} z 8 → blok {1}, stan {2}, dzień {3}")
    @CsvSource({
            "6, NONE,    PENDING, OPEN",
            "7, PARTIAL, DONE,    PARTIAL",
            "8, FULL,    DONE,    FULL",
    })
    void counterThresholds(int glasses, String completion, String state, String dayStatus) throws Exception {
        createTemplate(Mockup.WODA);
        String w = id(day(), "Woda");
        water(w, glasses);
        mvc.perform(get("/days/2026-11-10"))
                .andExpect(jsonPath("$.allDay[0].value").value(glasses))
                .andExpect(jsonPath("$.allDay[0].completion").value(completion))
                .andExpect(jsonPath("$.allDay[0].state").value(state))
                .andExpect(jsonPath("$.status").value(dayStatus));
    }

    @Test
    void duplicateClientEventIdIsIgnoredWithoutError() throws Exception {
        createTemplate(Mockup.WODA);
        String w = id(day(), "Woda");
        String body = "{\"delta\":1,\"clientEventId\":\"%s\"}".formatted(UUID.randomUUID());
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/instances/{id}/counter-events", w).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allDay[0].value").value(1));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM counter_event", Integer.class)).isEqualTo(1);
    }

    @Test
    void valueIsSumOfDeltasAndDropsBackBelowThreshold() throws Exception {
        createTemplate(Mockup.WODA);
        String w = id(day(), "Woda");
        water(w, 7);
        mvc.perform(post("/instances/{id}/counter-events", w)
                        .content("{\"delta\":-1,\"clientEventId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(jsonPath("$.allDay[0].value").value(6))
                .andExpect(jsonPath("$.allDay[0].state").value("PENDING"))
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(jdbc.queryForObject("SELECT sum(delta) FROM counter_event", Integer.class)).isEqualTo(6);
    }

    @Test
    void listPartiallyCheckedIsNotCompleted() throws Exception {
        createTemplate(Mockup.TABLETKI);
        String d = day();
        String pills = id(d, "Tabletki rano");
        List<String> items = JsonPath.read(d, "$.steps[0].items[*].id");

        mvc.perform(put("/instances/{id}/items/{item}", pills, items.get(0)).content("{\"checked\":true}"))
                .andExpect(jsonPath("$.steps[0].completion").value("NONE"))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(put("/instances/{id}/items/{item}", pills, items.get(1)).content("{\"checked\":true}"));
        mvc.perform(put("/instances/{id}/items/{item}", pills, items.get(2)).content("{\"checked\":true}"))
                .andExpect(jsonPath("$.steps[0].state").value("DONE"))
                .andExpect(jsonPath("$.status").value("FULL"))
                .andExpect(jsonPath("$.progress.done").value(1));
        // odznaczenie wraca do PENDING
        mvc.perform(put("/instances/{id}/items/{item}", pills, items.get(2)).content("{\"checked\":false}"))
                .andExpect(jsonPath("$.steps[0].state").value("PENDING"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void olderListWriteLosesToNewer() throws Exception {
        createTemplate(Mockup.TABLETKI);
        String d = day();
        String pills = id(d, "Tabletki rano");
        String item = JsonPath.read(d, "$.steps[0].items[0].id");
        mvc.perform(put("/instances/{id}/items/{item}", pills, item)
                .content("{\"checked\":true,\"changedAt\":\"2026-11-10T11:00:00Z\"}"));
        // spóźniona kolejka offline z wcześniejszą zmianą
        mvc.perform(put("/instances/{id}/items/{item}", pills, item)
                        .content("{\"checked\":false,\"changedAt\":\"2026-11-10T10:00:00Z\"}"))
                .andExpect(jsonPath("$.steps[0].items[0].checked").value(true));
    }

    @Test
    void skipMakesDayPartialWithoutPenalty() throws Exception {
        createTemplate(Mockup.OKNO);
        createTemplate(Mockup.SPACER);
        String d = day();
        mvc.perform(post("/instances/{id}/complete", id(d, "Otworzyć okno, rozciągnąć się")))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(post("/instances/{id}/skip", id(d, "Spacer")))
                .andExpect(jsonPath("$.steps[1].state").value("SKIPPED"))
                .andExpect(jsonPath("$.steps[1].completion").value("PARTIAL"))
                .andExpect(jsonPath("$.status").value("PARTIAL"));
    }

    @Test
    void deferMovesBlockToEndOfPath() throws Exception {
        for (String t : new String[] {Mockup.OKNO, Mockup.TABLETKI, Mockup.SPACER}) {
            createTemplate(t);
        }
        mvc.perform(post("/instances/{id}/defer", id(day(), "Tabletki rano")))
                .andExpect(jsonPath("$.steps[*].name").value(contains("Otworzyć okno, rozciągnąć się", "Spacer", "Tabletki rano")))
                .andExpect(jsonPath("$.steps[2].deferred").value(true))
                .andExpect(jsonPath("$.steps[2].state").value("PENDING"));
    }

    @Test
    void timerStartStopCompletes() throws Exception {
        createTemplate(Mockup.SPACER);
        String walk = id(day(), "Spacer");
        mvc.perform(post("/instances/{id}/timer/stop", walk)).andExpect(status().isConflict());
        mvc.perform(post("/instances/{id}/timer/start", walk))
                .andExpect(jsonPath("$.steps[0].timerStartedAt").isNotEmpty())
                .andExpect(jsonPath("$.status").value("OPEN"));
        clock.advance(java.time.Duration.ofMinutes(12)); // zakończony przed czasem: też zalicza (D6)
        mvc.perform(post("/instances/{id}/timer/stop", walk))
                .andExpect(jsonPath("$.steps[0].state").value("DONE"))
                .andExpect(jsonPath("$.status").value("FULL"));
    }

    @Test
    void noteNeedsTextBeforeCompletion() throws Exception {
        createTemplate(Mockup.NOTATKA);
        String note = id(day(), "Wieczorna notatka");
        mvc.perform(post("/instances/{id}/complete", note)).andExpect(status().isBadRequest());
        mvc.perform(put("/instances/{id}/note", note).content("{\"text\":\"Spokojny dzień.\"}"))
                .andExpect(jsonPath("$.steps[0].note").value("Spokojny dzień."))
                .andExpect(jsonPath("$.steps[0].state").value("PENDING"));
        mvc.perform(post("/instances/{id}/complete", note))
                .andExpect(jsonPath("$.steps[0].state").value("DONE"));
    }

    @Test
    void wrongTypeAndForeignOrClosedInstancesAreRejected() throws Exception {
        createTemplate(Mockup.OKNO);
        String okno = id(day(), "Otworzyć okno, rozciągnąć się");
        mvc.perform(post("/instances/{id}/timer/start", okno)).andExpect(status().isBadRequest());
        mvc.perform(post("/instances/{id}/complete", UUID.randomUUID())).andExpect(status().isNotFound());

        jdbc.update("UPDATE day SET closed_at = now()");
        mvc.perform(post("/instances/{id}/complete", okno)).andExpect(status().isConflict());
    }

    /** Dokument: „jeśli użytkownik dopije wodę z 7 do 8, status zmieni się z PARTIAL na FULL”. */
    @Test
    void wholeMockupDayGoesFromPartialToFull() throws Exception {
        createMockupTemplates();
        String d = day();
        mvc.perform(post("/instances/{id}/complete", id(d, "Otworzyć okno, rozciągnąć się")));
        String pills = id(d, "Tabletki rano");
        for (Object item : JsonPath.<List<Object>>read(d, "$.steps[1].items[*].id")) {
            mvc.perform(put("/instances/{id}/items/{item}", pills, item).content("{\"checked\":true}"));
        }
        mvc.perform(post("/instances/{id}/complete", id(d, "Wymiana filtra")));
        mvc.perform(post("/instances/{id}/timer/start", id(d, "Spacer")));
        mvc.perform(post("/instances/{id}/timer/stop", id(d, "Spacer")));
        mvc.perform(put("/instances/{id}/note", id(d, "Wieczorna notatka")).content("{\"text\":\"Dobrze.\"}"));
        mvc.perform(post("/instances/{id}/complete", id(d, "Wieczorna notatka")))
                .andExpect(jsonPath("$.progress.done").value(5))
                .andExpect(jsonPath("$.status").value("OPEN")); // woda 0/8

        water(id(d, "Woda"), 7);
        mvc.perform(get("/days/2026-11-10")).andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.board.position").value(2));
        water(id(d, "Woda"), 1);
        mvc.perform(get("/days/2026-11-10")).andExpect(jsonPath("$.status").value("FULL"))
                .andExpect(jsonPath("$.streak").value(1));
    }
}
