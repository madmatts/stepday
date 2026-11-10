package pl.stepday.template;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockTemplateRepository extends JpaRepository<BlockTemplate, UUID> {

    List<BlockTemplate> findByUserIdAndArchivedAtIsNullOrderBySortKey(UUID userId);

    Optional<BlockTemplate> findByIdAndUserId(UUID id, UUID userId);
}
