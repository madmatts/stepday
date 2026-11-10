package pl.stepday.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_item")
public class TemplateItem {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @Column(nullable = false)
    private String label;

    @Column(name = "sort_key", nullable = false)
    private String sortKey;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected TemplateItem() {
    }

    public TemplateItem(UUID templateId, String label, String sortKey) {
        this.id = UUID.randomUUID();
        this.templateId = templateId;
        this.label = label;
        this.sortKey = sortKey;
    }

    public UUID getId() { return id; }
    public UUID getTemplateId() { return templateId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getSortKey() { return sortKey; }
    public void setSortKey(String sortKey) { this.sortKey = sortKey; }
    public Instant getArchivedAt() { return archivedAt; }
    public void archive(Instant at) { this.archivedAt = at; }
}
