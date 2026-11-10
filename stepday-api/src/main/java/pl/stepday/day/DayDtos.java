package pl.stepday.day;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pl.stepday.template.BlockRole;
import pl.stepday.template.BlockType;

public final class DayDtos {

    private DayDtos() {
    }

    public record ItemView(UUID id, String label, boolean checked) {
    }

    /** Blok na ekranie dnia. Pola typu-specyficzne są null dla pozostałych typów. */
    public record BlockView(
            UUID id,
            String name,
            BlockType type,
            BlockRole role,
            InstanceState state,
            Completion completion,
            boolean deferred,
            int carryCount,
            boolean fromCalendar,
            List<ItemView> items,
            Integer value,
            Integer target,
            Integer threshold,
            String unit,
            Integer step,
            Integer minutes,
            Instant timerStartedAt,
            Instant timerStoppedAt,
            String note) {
    }

    public record Progress(int done, int total) {
    }

    public record WeekDay(LocalDate date, DayStatus status, boolean today) {
    }

    /** Plansza (decyzja D1): pole = liczba zaliczonych dni w miesiącu + 1. */
    public record Board(int position, int total, int year, int month) {
    }

    public record DayView(
            LocalDate date,
            boolean today,
            DayStatus status,
            boolean closed,
            Progress progress,
            List<BlockView> steps,
            List<BlockView> allDay,
            List<WeekDay> week,
            Board board,
            int streak) {
    }
}
