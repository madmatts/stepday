package pl.stepday.push;

/** Wysyłka jednej wiadomości do usługi push (Apple, Google, Mozilla). */
public interface PushGateway {

    enum Result { SENT, GONE, FAILED }

    record Subscription(String endpoint, String p256dh, String auth) {
    }

    Result send(Subscription subscription, String jsonPayload);
}
