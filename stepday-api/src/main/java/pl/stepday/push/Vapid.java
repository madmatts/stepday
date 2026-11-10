package pl.stepday.push;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;

/** Nagłówek Authorization dla Web Push (VAPID, RFC 8292): JWT ES256 podpisany kluczem serwera. */
public final class Vapid {

    private Vapid() {
    }

    public static String authorization(KeyPair keys, String endpoint, String subject, Instant now) {
        URI uri = URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getAuthority();
        String header = WebPushCrypto.b64("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = WebPushCrypto.b64(("{\"aud\":\"" + audience + "\",\"exp\":" + now.plusSeconds(12 * 3600).getEpochSecond()
                + ",\"sub\":\"" + subject + "\"}").getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + claims;
        try {
            Signature ecdsa = Signature.getInstance("SHA256withECDSAinP1363Format"); // surowe R||S (64 B), jak w JWS
            ecdsa.initSign(keys.getPrivate());
            ecdsa.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            String jwt = signingInput + "." + WebPushCrypto.b64(ecdsa.sign());
            return "vapid t=" + jwt + ", k=" + WebPushCrypto.b64(WebPushCrypto.encode((ECPublicKey) keys.getPublic()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("VAPID signing failed", e);
        }
    }
}
