package pl.stepday.push;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Klucze VAPID: generowane przy pierwszym użyciu i trzymane w {@code app_setting}, żeby subskrypcje
 * przetrwały restart bez ręcznej konfiguracji (A29).
 */
@Component
public class VapidKeys {

    private final JdbcTemplate jdbc;
    private volatile KeyPair cached;

    public VapidKeys(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public KeyPair get() {
        if (cached == null) {
            synchronized (this) {
                if (cached == null) {
                    cached = loadOrCreate();
                }
            }
        }
        return cached;
    }

    public String publicKey() {
        return WebPushCrypto.b64(WebPushCrypto.encode((ECPublicKey) get().getPublic()));
    }

    private KeyPair loadOrCreate() {
        KeyPair fresh = WebPushCrypto.generateKeyPair();
        jdbc.update("INSERT INTO app_setting (key, value) VALUES ('vapid.public', ?), ('vapid.private', ?) ON CONFLICT DO NOTHING",
                WebPushCrypto.b64(WebPushCrypto.encode((ECPublicKey) fresh.getPublic())),
                WebPushCrypto.b64(WebPushCrypto.encodePrivate((ECPrivateKey) fresh.getPrivate())));
        List<String> pub = jdbc.queryForList("SELECT value FROM app_setting WHERE key = 'vapid.public'", String.class);
        List<String> priv = jdbc.queryForList("SELECT value FROM app_setting WHERE key = 'vapid.private'", String.class);
        try {
            return WebPushCrypto.keyPair(WebPushCrypto.unb64(pub.getFirst()), WebPushCrypto.unb64(priv.getFirst()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Uszkodzone klucze VAPID w app_setting", e);
        }
    }
}
