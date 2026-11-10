package pl.stepday.schedule;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import org.springframework.stereotype.Service;
import pl.stepday.template.OverduePolicy;
import pl.stepday.template.ScheduleRule;

/**
 * Jedyna implementacja RRULE w systemie (ical4j). Front nie liczy terminów sam,
 * tylko pyta {@code POST /schedule/preview}.
 */
@Service
public class ScheduleService {

    /** Jak daleko w przód szukamy terminów do podglądu. */
    private static final int PREVIEW_HORIZON_YEARS = 10;

    /** Czy któraś z reguł szablonu wypada w danym dniu (kolekcja: decyzja D5). */
    public boolean occursOn(Collection<ScheduleRule> rules, LocalDate date) {
        return rules.stream().anyMatch(r -> occursOn(r.getRrule(), r.getDtstart(), r.getUntil(), date));
    }

    public boolean occursOn(String rrule, LocalDate dtstart, LocalDate until, LocalDate date) {
        if (date.isBefore(dtstart) || (until != null && date.isAfter(until))) {
            return false;
        }
        // Okres [date, date] — COUNT liczony od dtstart, bo dtstart jest „seedem”.
        return !parse(rrule).getDates(dtstart, date, date.plusDays(1)).stream()
                .filter(d -> d.equals(date))
                .toList().isEmpty();
    }

    /** Najbliższe {@code count} terminów od {@code from} włącznie. */
    public List<LocalDate> upcoming(String rrule, LocalDate dtstart, LocalDate until, LocalDate from, int count) {
        LocalDate start = from.isAfter(dtstart) ? from : dtstart;
        LocalDate end = start.plusYears(PREVIEW_HORIZON_YEARS);
        if (until != null && until.isBefore(end)) {
            end = until.plusDays(1);
        }
        if (end.isBefore(start)) {
            return List.of();
        }
        return parse(rrule).getDates(dtstart, start, end).stream()
                .filter(d -> !d.isBefore(start) && (until == null || !d.isAfter(until)))
                .sorted()
                .limit(count)
                .toList();
    }

    /**
     * Domyślna polityka zaległych: CARRY_OVER dla reguł rzadszych niż codzienna
     * (dokument, sekcja 4), DROP dla codziennych. Jednorazowe (COUNT=1) też CARRY_OVER.
     */
    public OverduePolicy defaultOverduePolicy(String rrule) {
        return isEveryDay(rrule) ? OverduePolicy.DROP : OverduePolicy.CARRY_OVER;
    }

    /** Reguła codzienna (FREQ=DAILY, INTERVAL=1, nie jednorazowa). Inne bloki są „z kalendarza”. */
    public boolean isEveryDay(String rrule) {
        Recur<LocalDate> recur = parse(rrule);
        return recur.getFrequency() == Frequency.DAILY
                && Math.max(recur.getInterval(), 1) == 1
                && recur.getCount() != 1;
    }

    /** Waliduje regułę; rzuca {@link InvalidRuleException} dla błędnej składni. */
    public Recur<LocalDate> parse(String rrule) {
        if (rrule == null || rrule.isBlank()) {
            throw new InvalidRuleException("Pusta reguła RRULE");
        }
        String body = rrule.startsWith("RRULE:") ? rrule.substring(6) : rrule;
        try {
            return new Recur<>(body);
        } catch (RuntimeException e) {
            throw new InvalidRuleException("Niepoprawna reguła RRULE: " + rrule);
        }
    }
}
