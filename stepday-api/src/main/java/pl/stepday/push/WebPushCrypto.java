package pl.stepday.push;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Szyfrowanie wiadomości Web Push (RFC 8291, kodowanie aes128gcm z RFC 8188) i klucze P-256.
 * Tylko prymitywy z JDK.
 */
public final class WebPushCrypto {

    public static final int RECORD_SIZE = 4096;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ECParameterSpec P256 = p256();

    private WebPushCrypto() {
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Wiadomość dla subskrybenta: nagłówek (salt, rs, keyid) + rekord AES-128-GCM. */
    public static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret) {
        KeyPair ephemeral = generateKeyPair();
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encrypt(plaintext, uaPublic, authSecret, ephemeral, salt);
    }

    /** Wariant deterministyczny (testy z wektorem z RFC 8291). */
    static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair as, byte[] salt) {
        try {
            byte[] asPublic = encode((ECPublicKey) as.getPublic());
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(as.getPrivate());
            ka.doPhase(decodePublic(uaPublic), true);
            byte[] ecdhSecret = ka.generateSecret();

            byte[] prkKey = hmac(authSecret, ecdhSecret);
            byte[] keyInfo = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] {0}, uaPublic, asPublic);
            byte[] ikm = hmac(prkKey, concat(keyInfo, new byte[] {1}));
            byte[] prk = hmac(salt, ikm);
            byte[] cek = Arrays.copyOf(hmac(prk, concat("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[] {0, 1})), 16);
            byte[] nonce = Arrays.copyOf(hmac(prk, concat("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[] {0, 1})), 12);

            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
            byte[] ciphertext = aes.doFinal(concat(plaintext, new byte[] {2})); // 0x02 = ostatni rekord

            ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length);
            header.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic);
            return concat(header.array(), ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Web Push encryption failed", e);
        }
    }

    /** Odszyfrowanie po stronie odbiorcy — używane w testach do sprawdzenia całej ścieżki. */
    static byte[] decrypt(byte[] body, KeyPair ua, byte[] authSecret) throws GeneralSecurityException {
        ByteBuffer b = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        b.get(salt);
        b.getInt();
        byte[] asPublic = new byte[b.get()];
        b.get(asPublic);
        byte[] ciphertext = new byte[b.remaining()];
        b.get(ciphertext);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ua.getPrivate());
        ka.doPhase(decodePublic(asPublic), true);
        byte[] uaPublic = encode((ECPublicKey) ua.getPublic());
        byte[] prkKey = hmac(authSecret, ka.generateSecret());
        byte[] ikm = hmac(prkKey, concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] {0}, uaPublic, asPublic, new byte[] {1}));
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, concat("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[] {0, 1})), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, concat("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[] {0, 1})), 12);
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = aes.doFinal(ciphertext);
        int end = padded.length - 1;
        while (end >= 0 && padded[end] == 0) {
            end--;
        }
        return Arrays.copyOf(padded, end); // bez ogranicznika 0x02
    }

    /** Punkt nieskompresowany: 0x04 || X || Y (65 bajtów). */
    public static byte[] encode(ECPublicKey key) {
        return concat(new byte[] {4}, unsigned32(key.getW().getAffineX()), unsigned32(key.getW().getAffineY()));
    }

    public static ECPublicKey decodePublic(byte[] raw) throws GeneralSecurityException {
        if (raw.length != 65 || raw[0] != 4) {
            throw new GeneralSecurityException("Oczekiwano nieskompresowanego punktu P-256");
        }
        ECPoint w = new ECPoint(new BigInteger(1, Arrays.copyOfRange(raw, 1, 33)), new BigInteger(1, Arrays.copyOfRange(raw, 33, 65)));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, P256));
    }

    public static ECPrivateKey decodePrivate(byte[] d) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), P256));
    }

    public static byte[] encodePrivate(ECPrivateKey key) {
        return unsigned32(key.getS());
    }

    public static KeyPair keyPair(byte[] publicRaw, byte[] privateRaw) throws GeneralSecurityException {
        return new KeyPair(decodePublic(publicRaw), decodePrivate(privateRaw));
    }

    public static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] unb64(String s) {
        return Base64.getUrlDecoder().decode(s.replace('+', '-').replace('/', '_').replace("=", ""));
    }

    static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    static byte[] unsigned32(BigInteger v) {
        byte[] b = v.toByteArray();
        if (b.length == 32) {
            return b;
        }
        byte[] out = new byte[32];
        int n = Math.min(b.length, 32);
        System.arraycopy(b, b.length - n, out, 32 - n, n);
        return out;
    }

    static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) {
            len += p.length;
        }
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    private static ECParameterSpec p256() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"));
            return ((ECPublicKey) g.generateKeyPair().getPublic()).getParams();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
