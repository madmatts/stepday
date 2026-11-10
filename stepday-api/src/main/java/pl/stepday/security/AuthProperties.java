package pl.stepday.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tryb jednego użytkownika (docs/decisions.md, A2). */
@ConfigurationProperties("stepday.auth")
public record AuthProperties(String username, String password) {
}
