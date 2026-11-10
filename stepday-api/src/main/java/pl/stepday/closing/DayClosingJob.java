package pl.stepday.closing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Zadanie planowane: co minutę sprawdza, którym użytkownikom w ich strefie minął {@code day_start}
 * następnego dnia, i zamyka ich dni. Granica liczona per użytkownik, więc każda strefa zamyka się
 * we własnej chwili.
 */
@Component
@ConditionalOnProperty(name = "stepday.closing.enabled", havingValue = "true", matchIfMissing = true)
public class DayClosingJob {

    private final DayCloser closer;

    public DayClosingJob(DayCloser closer) {
        this.closer = closer;
    }

    @Scheduled(fixedDelayString = "${stepday.closing.interval:PT1M}", initialDelayString = "PT10S")
    public void run() {
        closer.closeDue();
    }
}
