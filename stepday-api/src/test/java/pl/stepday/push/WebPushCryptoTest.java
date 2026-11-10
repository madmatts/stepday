package pl.stepday.push;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.stepday.push.WebPushCrypto.b64;
import static pl.stepday.push.WebPushCrypto.unb64;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WebPushCryptoTest {

    /** RFC 8291, sekcja 5 („Push Message Encryption Example”). */
    @Test
    void matchesRfc8291Example() throws Exception {
        KeyPair as = WebPushCrypto.keyPair(
                unb64("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8"),
                unb64("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw"));
        byte[] uaPublic = unb64("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] auth = unb64("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = unb64("DGv6ra1nlYgDCS1FRnbzlw");

        byte[] body = WebPushCrypto.encrypt("When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8),
                uaPublic, auth, as, salt);

        assertThat(b64(body)).isEqualTo(
                "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");

        KeyPair ua = WebPushCrypto.keyPair(uaPublic, unb64("q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94"));
        assertThat(new String(WebPushCrypto.decrypt(body, ua, auth), StandardCharsets.UTF_8))
                .isEqualTo("When I grow up, I want to be a watermelon");
    }

    @Test
    void roundTripsWithFreshKeys() throws Exception {
        KeyPair ua = WebPushCrypto.generateKeyPair();
        byte[] auth = new byte[16];
        new java.security.SecureRandom().nextBytes(auth);
        byte[] uaPublic = WebPushCrypto.encode((java.security.interfaces.ECPublicKey) ua.getPublic());
        String msg = "{\"title\":\"Stepday\",\"body\":\"Tabletki rano — pora na ten krok\"}";
        byte[] body = WebPushCrypto.encrypt(msg.getBytes(StandardCharsets.UTF_8), uaPublic, auth);
        assertThat(new String(WebPushCrypto.decrypt(body, ua, auth), StandardCharsets.UTF_8)).isEqualTo(msg);
    }

    @Test
    void vapidJwtIsSignedWithServerKeyForEndpointOrigin() throws Exception {
        KeyPair keys = WebPushCrypto.generateKeyPair();
        String header = Vapid.authorization(keys, "https://web.push.apple.com/QGuQyavXutnMm?x=1", "https://stepday.pl",
                Instant.parse("2026-11-10T12:00:00Z"));
        String jwt = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        assertThat(new String(unb64(parts[1]), StandardCharsets.UTF_8))
                .contains("\"aud\":\"https://web.push.apple.com\"")
                .contains("\"exp\":" + Instant.parse("2026-11-11T00:00:00Z").getEpochSecond());
        Signature verify = Signature.getInstance("SHA256withECDSAinP1363Format");
        verify.initVerify(WebPushCrypto.decodePublic(unb64(header.substring(header.indexOf(", k=") + 4))));
        verify.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verify.verify(unb64(parts[2]))).isTrue();
    }
}
