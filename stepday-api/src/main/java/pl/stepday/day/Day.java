package pl.stepday.day;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "day")
public class Day {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "local_date", nullable = false, updatable = false)
    private LocalDate localDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "day_status")
    private DayStatus status = DayStatus.OPEN;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Day() {
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getLocalDate() { return localDate; }
    public DayStatus getStatus() { return status; }
    public void setStatus(DayStatus status) { this.status = status; }
    public Instant getClosedAt() { return closedAt; }
    public boolean isClosed() { return closedAt != null; }
    public void close(Instant at) { this.closedAt = at; }
}
