package pl.stepday.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "block_template")
public class BlockTemplate {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "block_type")
    private BlockType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "block_role")
    private BlockRole role = BlockRole.STEP;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "overdue_policy", nullable = false, columnDefinition = "overdue_policy")
    private OverduePolicy overduePolicy = OverduePolicy.DROP;

    @Column(name = "sort_key", nullable = false)
    private String sortKey;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected BlockTemplate() {
    }

    public BlockTemplate(UUID userId, String name, BlockType type) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.type = type;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BlockType getType() { return type; }
    public void setType(BlockType type) { this.type = type; }
    public BlockRole getRole() { return role; }
    public void setRole(BlockRole role) { this.role = role; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = new HashMap<>(config); }
    public OverduePolicy getOverduePolicy() { return overduePolicy; }
    public void setOverduePolicy(OverduePolicy overduePolicy) { this.overduePolicy = overduePolicy; }
    public String getSortKey() { return sortKey; }
    public void setSortKey(String sortKey) { this.sortKey = sortKey; }
    public LocalTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalTime reminderTime) { this.reminderTime = reminderTime; }
    public Instant getArchivedAt() { return archivedAt; }
    public void archive(Instant at) { this.archivedAt = at; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch(Instant at) { this.updatedAt = at; }
}
