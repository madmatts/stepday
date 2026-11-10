package pl.stepday.push;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpPushGateway.class);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final VapidKeys keys;
    private final Clock clock;
    private final String subject;

    public HttpPushGateway(VapidKeys keys, Clock clock, @Value("${stepday.push.subject:https://stepday.pl}") String subject) {
        this.keys = keys;
        this.clock = clock;
        this.subject = subject;
    }

    @Override
    public Result send(Subscription s, String jsonPayload) {
        try {
            byte[] body = WebPushCrypto.encrypt(jsonPayload.getBytes(StandardCharsets.UTF_8),
                    WebPushCrypto.unb64(s.p256dh()), WebPushCrypto.unb64(s.auth()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(s.endpoint()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", Vapid.authorization(keys.get(), s.endpoint(), subject, clock.instant()))
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", "3600")
                    .header("Urgency", "normal")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 404 || status == 410) {
                return Result.GONE; // subskrypcja wygasła albo użytkownik ją cofnął
            }
            if (status >= 200 && status < 300) {
                return Result.SENT;
            }
            log.warn("Push rejected with status {} by {}", status, URI.create(s.endpoint()).getHost());
            return Result.FAILED;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Push failed: {}", e.toString());
            return Result.FAILED;
        }
    }
}
