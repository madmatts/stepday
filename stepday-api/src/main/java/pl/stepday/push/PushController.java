package pl.stepday.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.user.CurrentUser;

@RestController
@RequestMapping("/push")
public class PushController {

    private final VapidKeys keys;
    private final JdbcTemplate jdbc;
    private final CurrentUser currentUser;

    public PushController(VapidKeys keys, JdbcTemplate jdbc, CurrentUser currentUser) {
        this.keys = keys;
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    public record Config(String publicKey) {
    }

    public record Keys(@NotBlank String p256dh, @NotBlank String auth) {
    }

    /** Kształt PushSubscription.toJSON() z przeglądarki. */
    public record SubscriptionRequest(@NotBlank @Pattern(regexp = "https://.+") String endpoint, @Valid Keys keys) {
    }

    public record Unsubscribe(@NotBlank String endpoint) {
    }

    @GetMapping("/config")
    public Config config() {
        return new Config(keys.publicKey());
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@Valid @RequestBody SubscriptionRequest req) {
        if (req.keys() == null) {
            throw new IllegalArgumentException("Brak kluczy subskrypcji");
        }
        jdbc.update("""
                INSERT INTO push_subscription (user_id, endpoint, p256dh, auth) VALUES (?, ?, ?, ?)
                ON CONFLICT (endpoint) DO UPDATE SET user_id = EXCLUDED.user_id, p256dh = EXCLUDED.p256dh, auth = EXCLUDED.auth""",
                currentUser.id(), req.endpoint(), req.keys().p256dh(), req.keys().auth());
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@Valid @RequestBody Unsubscribe req) {
        jdbc.update("DELETE FROM push_subscription WHERE endpoint = ? AND user_id = ?", req.endpoint(), currentUser.id());
    }
}
