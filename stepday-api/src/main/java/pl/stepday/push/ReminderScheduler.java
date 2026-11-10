package pl.stepday.push;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "stepday.push.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private final ReminderJob job;

    public ReminderScheduler(ReminderJob job) {
        this.job = job;
    }

    @Scheduled(fixedDelayString = "${stepday.push.interval:PT1M}", initialDelayString = "PT20S")
    public void tick() {
        job.run();
    }
}
