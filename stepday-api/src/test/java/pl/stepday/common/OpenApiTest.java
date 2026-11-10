package pl.stepday.common;

import static org.hamcrest.Matchers.hasKey;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import pl.stepday.support.IntegrationTest;

class OpenApiTest extends IntegrationTest {

    @Test
    void specIsPublicAndDescribesEndpoints() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Stepday API"))
                .andExpect(jsonPath("$.paths", hasKey("/days/{date}")))
                .andExpect(jsonPath("$.paths", hasKey("/instances/{id}/counter-events")))
                .andExpect(jsonPath("$.paths", hasKey("/weeks/{weekStart}/summary")))
                .andExpect(jsonPath("$.components.securitySchemes.basic.scheme").value("basic"));
        mvc.perform(MockMvcRequestBuilders.get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders.get("/days/today")).andExpect(status().isUnauthorized());
    }
}
