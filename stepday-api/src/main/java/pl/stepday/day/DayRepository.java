package pl.stepday.day;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayRepository extends JpaRepository<Day, UUID> {

    Optional<Day> findByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    /** Blokada wiersza dnia: zapis z telefonu i zamknięcie dnia nie mogą się przeplatać. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from Day d where d.id = :id")
    Optional<Day> lockById(@org.springframework.data.repository.query.Param("id") UUID id);

    List<Day> findByUserIdAndLocalDateBetweenOrderByLocalDate(UUID userId, LocalDate from, LocalDate to);

    List<Day> findByUserIdAndClosedAtIsNullAndLocalDateBeforeOrderByLocalDate(UUID userId, LocalDate before);

    Optional<Day> findFirstByUserIdOrderByLocalDateDesc(UUID userId);

    Optional<Day> findFirstByUserIdOrderByLocalDateAsc(UUID userId);

    Optional<Day> findFirstByUserIdAndClosedAtIsNotNullOrderByLocalDateDesc(UUID userId);
}
