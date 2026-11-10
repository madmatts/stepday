package pl.stepday.day;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.stepday.template.BlockConfig;

class BlockEvaluatorTest {

    @ParameterizedTest(name = "cel {0}, {1}% → próg {2}")
    @CsvSource({
            "8,  80, 7",   // przykład z dokumentu: ceil(6.4) = 7
            "10, 80, 8",
            "5,  80, 4",
            "3,  80, 3",   // ceil(2.4) = 3
            "1,  80, 1",
            "8, 100, 8",
            "8,  50, 4",
            "7,  1,  1",
    })
    void thresholdRoundsUp(int target, int pct, int expected) {
        assertThat(BlockConfig.threshold(target, pct)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} z 8 → {1}")
    @CsvSource({
            "0, NONE",
            "6, NONE",
            "7, PARTIAL",
            "8, FULL",
            "9, FULL",
    })
    void counterAgainstTargetEight(int value, Completion expected) {
        assertThat(BlockEvaluator.counter(value, 8, 7)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}, zamknięty={1} → {2}")
    @CsvSource(delimiter = '|', value = {
            "FULL,FULL           | false | FULL",
            "FULL,PARTIAL        | false | PARTIAL",
            "FULL,NONE           | false | OPEN",
            "FULL,NONE           | true  | MISSED",
            "PARTIAL,NONE        | true  | MISSED",
            "FULL,EXCLUDED       | true  | FULL",
            "PARTIAL,EXCLUDED    | false | PARTIAL",
            "EXCLUDED            | true  | FULL",
    })
    void dayStatus(String completions, boolean closed, DayStatus expected) {
        List<Completion> list = java.util.Arrays.stream(completions.split(",")).map(String::trim).map(Completion::valueOf).toList();
        assertThat(BlockEvaluator.dayStatus(list, closed)).isEqualTo(expected);
    }
}
