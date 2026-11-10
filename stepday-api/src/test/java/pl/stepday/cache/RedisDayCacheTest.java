package pl.stepday.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import pl.stepday.support.IntegrationTest;
import pl.stepday.support.Mockup;

@TestPropertySource(properties = "stepday.cache.enabled=true")
@Import(RedisDayCacheTest.RedisConfig.class)
class RedisDayCacheTest extends IntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisConfig {
        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redis() {
            return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        }
    }

    @Autowired StringRedisTemplate redis;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("redis") GenericContainer<?> redisContainer;
    @Autowired DayCache cache;

    @Test
    void cachesDayAndEvictsOnTemplateChangeAndSurvivesRedisOutage() throws Exception {
        assertThat(cache).isInstanceOf(CacheConfig.RedisDayCache.class);
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();

        var water = createTemplate(Mockup.WODA);
        mvc.perform(get("/days/2026-11-10")).andExpect(status().isOk());
        String key = "day:" + USER_ID + ":2026-11-10";
        assertThat(redis.hasKey(key)).isTrue();

        // Trafienie w cache: zmiana bezpośrednio w bazie (z pominięciem serwisu) nie jest widoczna.
        jdbc.update("UPDATE block_instance SET value_int = 5");
        mvc.perform(get("/days/2026-11-10")).andExpect(jsonPath("$.allDay[0].value").value(0));

        // Każdy zapis przez API unieważnia klucze użytkownika.
        createTemplate(Mockup.OKNO);
        assertThat(redis.hasKey(key)).isFalse();
        mvc.perform(get("/days/2026-11-10"))
                .andExpect(jsonPath("$.allDay[0].value").value(5))
                .andExpect(jsonPath("$.steps[0].name").value("Otworzyć okno, rozciągnąć się"));

        // Redis jest tylko cache'em: po jego zatrzymaniu API działa dalej na bazie.
        redisContainer.stop();
        mvc.perform(put("/templates/{id}", water).content(Mockup.WODA)).andExpect(status().isOk());
        mvc.perform(get("/days/2026-11-10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.allDay[0].value").value(5));
    }
}
