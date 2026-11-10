package pl.stepday.template;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import pl.stepday.support.IntegrationTest;

class LayoutAndMeTest extends IntegrationTest {

    @Test
    void layoutMarksBlocksDueOnTuesdayLikeMockup() throws Exception {
        createMockupTemplates();
        mvc.perform(get("/layout?date=2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[*].template.name").value(contains(
                        "Otworzyć okno, rozciągnąć się", "Tabletki rano", "Wymiana filtra", "Spacer",
                        "Zakupy na tydzień", "Wieczorna notatka")))
                .andExpect(jsonPath("$.steps[*].dueOnDate").value(contains(true, true, true, true, false, true)))
                .andExpect(jsonPath("$.steps[2].everyDay").value(false)) // „Wypada dziś”
                .andExpect(jsonPath("$.steps[4].everyDay").value(false)) // „Nie dziś”
                .andExpect(jsonPath("$.allDay[0].template.name").value("Woda"));
    }

    @Test
    void dayStartIsConfigurable() throws Exception {
        clock.set(ZonedDateTime.of(2026, 11, 11, 5, 0, 0, 0, WARSAW));
        mvc.perform(get("/me")).andExpect(jsonPath("$.today").value("2026-11-11"))
                .andExpect(jsonPath("$.dayStart").value("04:00:00"));
        mvc.perform(patch("/me").content("{\"dayStart\":\"06:00\"}"))
                .andExpect(jsonPath("$.today").value("2026-11-10"));
        mvc.perform(patch("/me").content("{\"timezone\":\"Mars/Olympus\"}")).andExpect(status().isBadRequest());
    }
}
