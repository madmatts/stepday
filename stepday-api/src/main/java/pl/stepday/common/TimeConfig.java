package pl.stepday.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    /** Jedyne źródło „teraz” w aplikacji; testy podmieniają je na zegar stały. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
