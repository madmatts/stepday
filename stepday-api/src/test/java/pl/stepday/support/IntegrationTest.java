package pl.stepday.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import pl.stepday.user.CurrentUser;

/**
 * Baza testów integracyjnych: pełny kontekst, PostgreSQL w Testcontainers, zegar testowy
 * ustawiony na wtorek 10.11.2026 12:00 Europe/Warsaw (dzień z makiet), czysta baza przed testem.
 */
@SpringBootTest(properties = {"stepday.closing.enabled=false", "stepday.push.enabled=false"})
@AutoConfigureMockMvc
@Import({PostgresTestConfig.class, IntegrationTest.ClockConfig.class})
public abstract class IntegrationTest {

    public static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    public static final ZonedDateTime NOV_10_NOON = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, WARSAW);
    public static final UUID USER_ID = CurrentUser.idFor("stepday");

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected MutableClock clock;

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NOV_10_NOON.toInstant());
        }
    }

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbc.execute("TRUNCATE app_user CASCADE");
        jdbc.update("INSERT INTO app_user (id) VALUES (?)", USER_ID);
        clock.set(NOV_10_NOON);
    }

    /** Tworzy szablon przez API i zwraca jego id. */
    protected UUID createTemplate(String body) throws Exception {
        String json = mvc.perform(post("/templates").content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    protected java.util.Map<String, UUID> createMockupTemplates() throws Exception {
        java.util.Map<String, UUID> ids = new java.util.LinkedHashMap<>();
        for (String body : Mockup.ALL) {
            ids.put(com.jayway.jsonpath.JsonPath.read(body, "$.name"), createTemplate(body));
        }
        return ids;
    }

    protected String json(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
    }

    protected static MockHttpServletRequestBuilder get(String url, Object... vars) {
        return auth(MockMvcRequestBuilders.get(url, vars));
    }

    protected static MockHttpServletRequestBuilder post(String url, Object... vars) {
        return auth(MockMvcRequestBuilders.post(url, vars));
    }

    protected static MockHttpServletRequestBuilder put(String url, Object... vars) {
        return auth(MockMvcRequestBuilders.put(url, vars));
    }

    protected static MockHttpServletRequestBuilder patch(String url, Object... vars) {
        return auth(MockMvcRequestBuilders.patch(url, vars));
    }

    protected static MockHttpServletRequestBuilder delete(String url, Object... vars) {
        return auth(MockMvcRequestBuilders.delete(url, vars));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.with(httpBasic("stepday", "stepday")).contentType(MediaType.APPLICATION_JSON);
    }

}
