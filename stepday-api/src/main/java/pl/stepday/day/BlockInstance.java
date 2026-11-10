package pl.stepday.day;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.stepday.template.BlockRole;
import pl.stepday.template.BlockType;

/** Wystąpienie bloku w dniu: zamrożona kopia definicji + stan zaliczenia. */
@Entity
@Table(name = "block_instance")
public class BlockInstance {

    @Id
    private UUID id;

    @Column(name = "day_id", nullable = false, updatable = false)
    private UUID dayId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "carried_from_id", updatable = false)
    private UUID carriedFromId;

    @Column(name = "carry_count", nullable = false, updatable = false)
    private short carryCount;

    @Column(nullable = false, updatable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, updatable = false, columnDefinition = "block_type")
    private BlockType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, updatable = false, columnDefinition = "block_role")
    private BlockRole role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> config = new HashMap<>();

    @Column(name = "sort_key", nullable = false, updatable = false)
    private String sortKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "instance_state")
    private InstanceState state = InstanceState.PENDING;

    @Column(name = "deferred_at")
    private Instant deferredAt;

    @Column(name = "value_int", nullable = false)
    private int valueInt;

    @Column(name = "note_text")
    private String noteText;

    @Column(name = "timer_started_at")
    private Instant timerStartedAt;

    @Column(name = "timer_stopped_at")
    private Instant timerStoppedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected BlockInstance() {
    }

    public UUID getId() { return id; }
    public UUID getDayId() { return dayId; }
    public UUID getTemplateId() { return templateId; }
    public UUID getCarriedFromId() { return carriedFromId; }
    public int getCarryCount() { return carryCount; }
    public String getName() { return name; }
    public BlockType getType() { return type; }
    public BlockRole getRole() { return role; }
    public Map<String, Object> getConfig() { return config; }
    public String getSortKey() { return sortKey; }
    public InstanceState getState() { return state; }
    public void setState(InstanceState state) { this.state = state; }
    public Instant getDeferredAt() { return deferredAt; }
    public void setDeferredAt(Instant deferredAt) { this.deferredAt = deferredAt; }
    public int getValueInt() { return valueInt; }
    public void setValueInt(int valueInt) { this.valueInt = valueInt; }
    public String getNoteText() { return noteText; }
    public void setNoteText(String noteText) { this.noteText = noteText; }
    public Instant getTimerStartedAt() { return timerStartedAt; }
    public void setTimerStartedAt(Instant at) { this.timerStartedAt = at; }
    public Instant getTimerStoppedAt() { return timerStoppedAt; }
    public void setTimerStoppedAt(Instant at) { this.timerStoppedAt = at; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch(Instant at) { this.updatedAt = at; }
}
