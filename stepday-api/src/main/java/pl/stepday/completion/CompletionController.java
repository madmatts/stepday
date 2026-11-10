package pl.stepday.completion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.day.Day;
import pl.stepday.day.DayDtos.DayView;
import pl.stepday.day.DayService;
import pl.stepday.user.AppUser;
import pl.stepday.user.CurrentUser;

/** Wszystkie zapisy zwracają świeży widok dnia, którego dotyczyły. */
@RestController
@RequestMapping("/instances/{id}")
public class CompletionController {

    private final CompletionService completion;
    private final DayService days;
    private final CurrentUser currentUser;

    public CompletionController(CompletionService completion, DayService days, CurrentUser currentUser) {
        this.completion = completion;
        this.days = days;
        this.currentUser = currentUser;
    }

    public record ItemRequest(@NotNull Boolean checked, Instant changedAt) {
    }

    public record NoteRequest(String text, Instant changedAt) {
    }

    public record CounterEventRequest(int delta, @NotNull UUID clientEventId, Instant happenedAt) {
    }

    @PutMapping("/items/{itemId}")
    public DayView item(@PathVariable UUID id, @PathVariable UUID itemId, @Valid @RequestBody ItemRequest req) {
        AppUser u = currentUser.get();
        return view(u, completion.setItem(u.getId(), id, itemId, req.checked(), req.changedAt()));
    }

    @PostMapping("/complete")
    public DayView complete(@PathVariable UUID id) {
        AppUser u = currentUser.get();
        return view(u, completion.complete(u.getId(), id));
    }

    @PutMapping("/note")
    public DayView note(@PathVariable UUID id, @RequestBody NoteRequest req) {
        AppUser u = currentUser.get();
        return view(u, completion.saveNote(u.getId(), id, req.text(), req.changedAt()));
    }

    @PostMapping("/counter-events")
    public DayView counter(@PathVariable UUID id, @Valid @RequestBody CounterEventRequest req) {
        AppUser u = currentUser.get();
        return view(u, completion.counterEvent(u.getId(), id, req.delta(), req.clientEventId(), req.happenedAt()));
    }

    @PostMapping("/timer/start")
    public DayView start(@PathVariable UUID id) {
        AppUser u = currentUser.get();
        return view(u, completion.startTimer(u.getId(), id));
    }

    @PostMapping("/timer/stop")
    public DayView stop(@PathVariable UUID id) {
        AppUser u = currentUser.get();
        return view(u, completion.stopTimer(u.getId(), id));
    }

    @PostMapping("/skip")
    public DayView skip(@PathVariable UUID id) {
        AppUser u = currentUser.get();
        return view(u, completion.skip(u.getId(), id));
    }

    @PostMapping("/defer")
    public DayView defer(@PathVariable UUID id) {
        AppUser u = currentUser.get();
        return view(u, completion.defer(u.getId(), id));
    }

    private DayView view(AppUser user, Day day) {
        return days.view(user, day.getLocalDate());
    }
}
