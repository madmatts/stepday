package pl.stepday.day;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceItemRepository extends JpaRepository<InstanceItem, UUID> {

    List<InstanceItem> findByInstanceIdOrderBySortKey(UUID instanceId);

    List<InstanceItem> findByInstanceIdInOrderBySortKey(Collection<UUID> instanceIds);
}
