package pl.stepday.day;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterEventRepository extends JpaRepository<CounterEvent, UUID> {

    List<CounterEvent> findByInstanceId(UUID instanceId);
}
