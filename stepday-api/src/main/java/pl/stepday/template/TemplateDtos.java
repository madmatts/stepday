package pl.stepday.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TemplateDtos {

    private TemplateDtos() {
    }

    public record ItemDto(UUID id, @NotBlank String label) {
    }

    public record ScheduleDto(@NotBlank String rrule, @NotNull LocalDate dtstart, LocalDate until) {
    }

    public record TemplateRequest(
            @NotNull BlockType type,
            BlockRole role,
            @NotBlank String name,
            Map<String, Object> config,
            List<@Valid ItemDto> items,
            @NotNull @Valid ScheduleDto schedule,
            OverduePolicy overduePolicy,
            LocalTime reminderTime,
            UUID afterTemplateId,
            Boolean applyToday) {

        public boolean applyTodayOrFalse() {
            return Boolean.TRUE.equals(applyToday);
        }
    }

    public record TemplateResponse(
            UUID id,
            BlockType type,
            BlockRole role,
            String name,
            Map<String, Object> config,
            List<ItemDto> items,
            ScheduleDto schedule,
            OverduePolicy overduePolicy,
            LocalTime reminderTime,
            String sortKey) {
    }

    public record OrderRequest(@NotNull List<UUID> orderedIds) {
    }
}
