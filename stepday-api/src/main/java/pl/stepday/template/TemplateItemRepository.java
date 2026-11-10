package pl.stepday.template;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateItemRepository extends JpaRepository<TemplateItem, UUID> {

    List<TemplateItem> findByTemplateIdAndArchivedAtIsNullOrderBySortKey(UUID templateId);
}
