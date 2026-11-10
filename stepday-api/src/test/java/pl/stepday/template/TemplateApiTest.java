package pl.stepday.template;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import pl.stepday.support.IntegrationTest;

class TemplateApiTest extends IntegrationTest {

    static final String PILLS = """
            {"type":"LIST","name":"Tabletki rano","role":"STEP",
             "items":[{"label":"Witamina D3"},{"label":"Magnez"},{"label":"Omega-3"}],
             "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-10"},
             "reminderTime":"07:30"}""";

    static final String WATER = """
            {"type":"COUNTER","name":"Woda","role":"ALL_DAY",
             "config":{"target":8,"unit":"szklanek"},
             "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-10"}}""";

    static final String FILTER = """
            {"type":"BOOLEAN","name":"Wymiana filtra",
             "schedule":{"rrule":"FREQ=MONTHLY;INTERVAL=3","dtstart":"2026-11-10"}}""";

    String create(String body) throws Exception {
        String json = mvc.perform(post("/templates").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void createsListTemplateWithItemsAndDefaults() throws Exception {
        String id = create(PILLS);
        mvc.perform(get("/templates/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].label").value(org.hamcrest.Matchers.contains("Witamina D3", "Magnez", "Omega-3")))
                .andExpect(jsonPath("$.overduePolicy").value("DROP"))
                .andExpect(jsonPath("$.reminderTime").value("07:30:00"))
                .andExpect(jsonPath("$.schedule.rrule").value("FREQ=DAILY"));
    }

    @Test
    void counterGetsDefaultStepAndThreshold() throws Exception {
        String id = create(WATER);
        mvc.perform(get("/templates/{id}", id))
                .andExpect(jsonPath("$.config.target").value(8))
                .andExpect(jsonPath("$.config.step").value(1))
                .andExpect(jsonPath("$.config.threshold_pct").value(80))
                .andExpect(jsonPath("$.role").value("ALL_DAY"));
    }

    @Test
    void rarerThanDailyRuleDefaultsToCarryOver() throws Exception {
        String id = create(FILTER);
        mvc.perform(get("/templates/{id}", id)).andExpect(jsonPath("$.overduePolicy").value("CARRY_OVER"));
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        mvc.perform(post("/templates").content(WATER.replace("\"target\":8,", "")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/templates").content(FILTER.replace("FREQ=MONTHLY;INTERVAL=3", "FREQ=SOMETIMES")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/templates").content("{\"type\":\"BOOLEAN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateKeepsItemIdsAndArchivesRemovedOnes() throws Exception {
        String id = create(PILLS);
        String json = mvc.perform(get("/templates/{id}", id)).andReturn().getResponse().getContentAsString();
        String d3 = JsonPath.read(json, "$.items[0].id");
        mvc.perform(put("/templates/{id}", id).content("""
                        {"type":"LIST","name":"Tabletki","items":[{"id":"%s","label":"Witamina D3"},{"label":"Cynk"}],
                         "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-10"}}""".formatted(d3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(d3))
                .andExpect(jsonPath("$.items[1].label").value("Cynk"));
        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("SELECT count(*) FROM template_item WHERE template_id = ?::uuid", Integer.class, id))
                .isEqualTo(4);
    }

    @Test
    void deleteArchivesInsteadOfRemoving() throws Exception {
        String id = create(FILTER);
        mvc.perform(delete("/templates/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/templates/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/templates")).andExpect(jsonPath("$", hasSize(0)));
        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("SELECT archived_at IS NOT NULL FROM block_template WHERE id = ?::uuid", Boolean.class, id))
                .isTrue();
    }

    @Test
    void afterTemplateIdPlacesBlockInPathAndOrderEndpointReorders() throws Exception {
        String pills = create(PILLS);
        String walk = create("""
                {"type":"TIMER","name":"Spacer","config":{"minutes":20},
                 "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-10"}}""");
        String filter = create(FILTER.replace("{\"type\"", "{\"afterTemplateId\":\"" + pills + "\",\"type\""));
        mvc.perform(get("/templates"))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("Tabletki rano", "Wymiana filtra", "Spacer")));

        mvc.perform(put("/templates/order").content("{\"orderedIds\":[\"%s\",\"%s\",\"%s\"]}".formatted(walk, filter, pills)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/templates"))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("Spacer", "Wymiana filtra", "Tabletki rano")));
    }

    @Test
    void schedulePreviewMatchesKiedyMockup() throws Exception {
        mvc.perform(post("/schedule/preview").content("""
                        {"rrule":"FREQ=MONTHLY;INTERVAL=3","dtstart":"2026-11-10","count":3}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates[*].date").value(org.hamcrest.Matchers.contains("2026-11-10", "2027-02-10", "2027-05-10")))
                .andExpect(jsonPath("$.dates[0].today").value(true))
                .andExpect(jsonPath("$.dates[1].today").value(false))
                .andExpect(jsonPath("$.defaultOverduePolicy").value("CARRY_OVER"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/templates")).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/templates").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("stepday", "zle")))
                .andExpect(status().isUnauthorized());
    }

}
