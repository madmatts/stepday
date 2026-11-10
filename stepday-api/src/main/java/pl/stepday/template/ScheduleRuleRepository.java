package pl.stepday.template;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, UUID> {
}
