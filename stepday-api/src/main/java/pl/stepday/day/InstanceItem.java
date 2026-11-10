package pl.stepday.day;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instance_item")
public class InstanceItem {

    @Id
    private UUID id;

    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    @Column(nullable = false, updatable = false)
    private String label;

    @Column(name = "sort_key", nullable = false, updatable = false)
    private String sortKey;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected InstanceItem() {
    }

    public UUID getId() { return id; }
    public UUID getInstanceId() { return instanceId; }
    public String getLabel() { return label; }
    public String getSortKey() { return sortKey; }
    public Instant getCheckedAt() { return checkedAt; }
    public boolean isChecked() { return checkedAt != null; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setChecked(boolean checked, Instant at) {
        this.checkedAt = checked ? (checkedAt != null ? checkedAt : at) : null;
        this.updatedAt = at;
    }
}
