package pl.stepday.day;

import java.util.Collection;
import java.util.List;
import pl.stepday.template.BlockConfig;

/**
 * Reguły zaliczania (dokument, sekcja 5). Stan dnia wynika wyłącznie ze stanów instancji;
 * serwer liczy go po każdym zapisie i nie ufa klientowi.
 */
public final class BlockEvaluator {

    private BlockEvaluator() {
    }

    public static Completion completion(BlockInstance bi, List<InstanceItem> items) {
        return switch (bi.getState()) {
            case CARRIED -> Completion.EXCLUDED;
            case SKIPPED -> Completion.PARTIAL;
            case MISSED -> Completion.NONE;
            case PENDING, DONE -> switch (bi.getType()) {
                case LIST -> items.stream().allMatch(InstanceItem::isChecked) ? Completion.FULL : Completion.NONE;
                case COUNTER -> counter(bi.getValueInt(), BlockConfig.intValue(bi.getConfig(), "target"),
                        BlockConfig.threshold(bi.getConfig()));
                case BOOLEAN, TIMER, NOTE -> bi.getState() == InstanceState.DONE ? Completion.FULL : Completion.NONE;
            };
        };
    }

    /** Licznik: {@code >= target} w pełni, {@code [threshold, target)} z niedosytem, poniżej progu niezaliczony. */
    public static Completion counter(int value, int target, int threshold) {
        if (value >= target) {
            return Completion.FULL;
        }
        return value >= threshold ? Completion.PARTIAL : Completion.NONE;
    }

    /**
     * Stan dnia: jakikolwiek niezaliczony → OPEN (po zamknięciu MISSED); inaczej jakikolwiek
     * z niedosytem → PARTIAL; inaczej FULL. Obie role (STEP i ALL_DAY) wchodzą do wyniku.
     */
    public static DayStatus dayStatus(Collection<Completion> completions, boolean closed) {
        if (completions.contains(Completion.NONE)) {
            return closed ? DayStatus.MISSED : DayStatus.OPEN;
        }
        return completions.contains(Completion.PARTIAL) ? DayStatus.PARTIAL : DayStatus.FULL;
    }
}
