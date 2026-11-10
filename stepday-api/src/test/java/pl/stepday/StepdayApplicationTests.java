package pl.stepday;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pl.stepday.support.PostgresTestConfig;

@SpringBootTest
@Import(PostgresTestConfig.class)
class StepdayApplicationTests {

    @Test
    void contextLoads() {
    }
}
