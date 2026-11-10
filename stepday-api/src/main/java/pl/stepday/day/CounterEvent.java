package pl.stepday.day;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "counter_event")
public class CounterEvent {

    @Id
    private UUID id;

    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    @Column(nullable = false, updatable = false)
    private int delta;

    @Column(name = "client_event_id", nullable = false, updatable = false)
    private UUID clientEventId;

    @Column(name = "happened_at", nullable = false, updatable = false)
    private Instant happenedAt;

    protected CounterEvent() {
    }

    public UUID getId() { return id; }
    public UUID getInstanceId() { return instanceId; }
    public int getDelta() { return delta; }
    public UUID getClientEventId() { return clientEventId; }
    public Instant getHappenedAt() { return happenedAt; }
}
