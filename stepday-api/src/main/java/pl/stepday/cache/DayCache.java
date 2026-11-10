package pl.stepday.cache;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import pl.stepday.day.DayDtos.DayView;

/** Cache odpowiedzi dnia pod kluczem {@code user:date}. Nigdy nie jest źródłem prawdy. */
public interface DayCache {

    Optional<DayView> get(UUID userId, LocalDate date);

    void put(UUID userId, LocalDate date, DayView view);

    void evict(UUID userId, LocalDate date);

    /** Unieważnia wszystkie dni użytkownika (zmiana szablonu, pasek tygodnia, plansza). */
    void evictUser(UUID userId);
}
