package pl.stepday.day;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayRepository extends JpaRepository<Day, UUID> {

    Optional<Day> findByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    List<Day> findByUserIdAndLocalDateBetweenOrderByLocalDate(UUID userId, LocalDate from, LocalDate to);

    List<Day> findByUserIdAndClosedAtIsNullAndLocalDateBeforeOrderByLocalDate(UUID userId, LocalDate before);

    Optional<Day> findFirstByUserIdOrderByLocalDateDesc(UUID userId);
}
