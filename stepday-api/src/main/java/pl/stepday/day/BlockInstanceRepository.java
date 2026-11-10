package pl.stepday.day;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockInstanceRepository extends JpaRepository<BlockInstance, UUID> {

    List<BlockInstance> findByDayIdOrderBySortKey(UUID dayId);

    List<BlockInstance> findByDayIdIn(Collection<UUID> dayIds);
}
