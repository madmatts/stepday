package pl.stepday.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String timezone = "Europe/Warsaw";

    @Column(name = "day_start", nullable = false)
    private LocalTime dayStart = LocalTime.of(4, 0);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
    }

    public AppUser(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public String getTimezone() { return timezone; }
    public ZoneId zone() { return ZoneId.of(timezone); }
    public void setTimezone(String timezone) { this.timezone = ZoneId.of(timezone).getId(); }
    public LocalTime getDayStart() { return dayStart; }
    public void setDayStart(LocalTime dayStart) { this.dayStart = dayStart; }
    public Instant getCreatedAt() { return createdAt; }
}
