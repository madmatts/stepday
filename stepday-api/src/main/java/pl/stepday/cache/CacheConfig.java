package pl.stepday.cache;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.stepday.day.DayDtos.DayView;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @ConditionalOnProperty(name = "stepday.cache.enabled", havingValue = "true")
    DayCache redisDayCache(StringRedisTemplate redis, JsonMapper json) {
        return new RedisDayCache(redis, json);
    }

    @Bean
    @ConditionalOnProperty(name = "stepday.cache.enabled", havingValue = "false", matchIfMissing = true)
    DayCache noopDayCache() {
        return new DayCache() {
            public Optional<DayView> get(UUID u, LocalDate d) { return Optional.empty(); }
            public void put(UUID u, LocalDate d, DayView v) { }
            public void evict(UUID u, LocalDate d) { }
            public void evictUser(UUID u) { }
        };
    }

    /**
     * Każda awaria Redisa jest tylko logowana: aplikacja działa dalej na bazie.
     * Unieważnienie per użytkownik czyści wszystkie jego klucze (zbiór indeksu {@code day:<user>}).
     */
    static final class RedisDayCache implements DayCache {
        private static final Duration TTL = Duration.ofMinutes(10);
        /** Po błędzie cache jest omijany przez ten czas, żeby awaria Redisa nie spowalniała API. */
        private static final Duration BREAK = Duration.ofSeconds(30);
        private final StringRedisTemplate redis;
        private final JsonMapper json;
        private volatile long downUntilNanos = 0;

        private boolean down() {
            return System.nanoTime() - downUntilNanos < 0;
        }

        private void trip(String op, RuntimeException e) {
            downUntilNanos = System.nanoTime() + BREAK.toNanos();
            log.warn("Redis {} failed, bypassing cache for {}s: {}", op, BREAK.toSeconds(), e.getMessage());
        }

        RedisDayCache(StringRedisTemplate redis, JsonMapper json) {
            this.redis = redis;
            this.json = json;
        }

        private static String key(UUID u, LocalDate d) { return "day:" + u + ":" + d; }
        private static String index(UUID u) { return "day:" + u; }

        public Optional<DayView> get(UUID u, LocalDate d) {
            if (down()) {
                return Optional.empty();
            }
            try {
                String v = redis.opsForValue().get(key(u, d));
                return v == null ? Optional.empty() : Optional.of(json.readValue(v, DayView.class));
            } catch (RuntimeException e) {
                trip("get", e);
                return Optional.empty();
            }
        }

        public void put(UUID u, LocalDate d, DayView view) {
            if (down()) {
                return;
            }
            try {
                redis.opsForValue().set(key(u, d), json.writeValueAsString(view), TTL);
                redis.opsForSet().add(index(u), key(u, d));
                redis.expire(index(u), TTL);
            } catch (RuntimeException e) {
                trip("put", e);
            }
        }

        public void evict(UUID u, LocalDate d) {
            // Pasek tygodnia i plansza innych dni zależą od tego dnia, więc czyścimy całego użytkownika.
            evictUser(u);
        }

        public void evictUser(UUID u) {
            // Unieważnienie próbujemy zawsze (nieudane = TTL i tak wygasi wpis), ale z krótkim timeoutem.
            try {
                Set<String> keys = redis.opsForSet().members(index(u));
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                }
                redis.delete(index(u));
            } catch (RuntimeException e) {
                trip("evict", e);
            }
        }
    }
}
