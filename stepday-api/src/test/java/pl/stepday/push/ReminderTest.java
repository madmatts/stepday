package pl.stepday.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import pl.stepday.support.IntegrationTest;
import pl.stepday.support.Mockup;

@Import(ReminderTest.FakeGatewayConfig.class)
class ReminderTest extends IntegrationTest {

    record Sent(String endpoint, String payload) {
    }

    static final List<Sent> SENT = new CopyOnWriteArrayList<>();
    static volatile PushGateway.Result next = PushGateway.Result.SENT;

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeGatewayConfig {
        @Bean
        @Primary
        PushGateway fakeGateway() {
            return (s, payload) -> {
                SENT.add(new Sent(s.endpoint(), payload));
                return next;
            };
        }
    }

    @Autowired ReminderJob job;

    @BeforeEach
    void subscribe() throws Exception {
        SENT.clear();
        next = PushGateway.Result.SENT;
        mvc.perform(post("/push/subscriptions").content("""
                {"endpoint":"https://web.push.apple.com/abc","keys":{"p256dh":"BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4","auth":"BTBZMqHH6r4Tts7J_aSIgg"}}"""))
                .andExpect(status().isNoContent());
    }

    void at(int hour, int minute) {
        clock.set(ZonedDateTime.of(2026, 11, 10, hour, minute, 0, 0, WARSAW));
    }

    @Test
    void configExposesStableVapidKey() throws Exception {
        String a = json(get("/push/config"));
        String b = json(get("/push/config"));
        assertThat((String) JsonPath.read(a, "$.publicKey")).hasSize(87).isEqualTo(JsonPath.read(b, "$.publicKey"));
    }

    @Test
    void sendsOncePerDayAfterReminderTime() throws Exception {
        createTemplate(Mockup.TABLETKI); // przypomnienie 07:30
        at(7, 29);
        assertThat(job.run()).isZero();
        at(7, 30);
        assertThat(job.run()).isEqualTo(1);
        assertThat(SENT).singleElement().satisfies(s -> {
            assertThat(s.endpoint()).isEqualTo("https://web.push.apple.com/abc");
            assertThat(s.payload()).contains("\"title\":\"Tabletki rano\"").contains("Do odhaczenia: 3");
        });
        at(7, 31);
        assertThat(job.run()).isZero(); // bez duplikatu
    }

    @Test
    void skipsDoneBlocksAndStaleWindow() throws Exception {
        createTemplate(Mockup.TABLETKI);
        at(9, 0); // 90 minut po godzinie — poza oknem 30 min
        assertThat(job.run()).isZero();

        at(7, 0);
        String d = json(get("/days/2026-11-10"));
        String id = JsonPath.read(d, "$.steps[0].id");
        mvc.perform(post("/instances/{id}/skip", id)).andExpect(status().isOk());
        at(7, 35);
        assertThat(job.run()).isZero();
    }

    @Test
    void counterReminderShowsProgressAndGoneSubscriptionIsRemoved() throws Exception {
        createTemplate(Mockup.WODA.replace("\"role\":\"ALL_DAY\",", "\"role\":\"ALL_DAY\",\"reminderTime\":\"10:00\","));
        next = PushGateway.Result.GONE;
        at(10, 5);
        assertThat(job.run()).isEqualTo(1);
        assertThat(SENT.getFirst().payload()).contains("Masz 0/8 szklanek. Dzień zalicza się od 7.");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM push_subscription", Integer.class)).isZero();
    }

    @Test
    void noSubscriptionNoWork() throws Exception {
        mvc.perform(delete("/push/subscriptions").content("{\"endpoint\":\"https://web.push.apple.com/abc\"}"))
                .andExpect(status().isNoContent());
        createTemplate(Mockup.TABLETKI);
        at(7, 30);
        assertThat(job.run()).isZero();
        mvc.perform(post("/push/subscriptions").content("{\"endpoint\":\"http://insecure\",\"keys\":{\"p256dh\":\"x\",\"auth\":\"y\"}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/push/config")).andExpect(jsonPath("$.publicKey").isNotEmpty());
    }
}
