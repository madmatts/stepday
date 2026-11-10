package pl.stepday.user;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Jedyny punkt, w którym domena dowiaduje się, kim jest użytkownik. Podmiana na prawdziwy
 * moduł logowania zmienia tylko tę klasę (docs/decisions.md, A2).
 */
@Component
public class CurrentUser {

    private final JdbcTemplate jdbc;
    private final AppUserRepository users;

    public CurrentUser(JdbcTemplate jdbc, AppUserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    public UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Brak uwierzytelnionego użytkownika");
        }
        UUID id = idFor(auth.getName());
        jdbc.update("INSERT INTO app_user (id) VALUES (?) ON CONFLICT DO NOTHING", id);
        return id;
    }

    public AppUser get() {
        return users.findById(id()).orElseThrow();
    }

    public static UUID idFor(String username) {
        return UUID.nameUUIDFromBytes(("stepday:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
