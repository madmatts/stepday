package pl.stepday.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SortKeysTest {

    @Test
    void appendingProducesIncreasingKeys() {
        String prev = null;
        for (int i = 0; i < 200; i++) {
            String next = SortKeys.after(prev);
            if (prev != null) {
                assertThat(next).isGreaterThan(prev);
            }
            prev = next;
        }
    }

    @Test
    void randomInsertionsKeepOrderAndNeverEndWithZero() {
        Random rnd = new Random(42);
        List<String> keys = new ArrayList<>();
        keys.add(SortKeys.after(null));
        for (int i = 0; i < 2000; i++) {
            int pos = rnd.nextInt(keys.size() + 1);
            String lo = pos == 0 ? null : keys.get(pos - 1);
            String hi = pos == keys.size() ? null : keys.get(pos);
            String k = SortKeys.between(lo, hi);
            if (lo != null) assertThat(k).isGreaterThan(lo);
            if (hi != null) assertThat(k).isLessThan(hi);
            assertThat(k).doesNotEndWith("0").matches("[0-9a-z]+");
            keys.add(pos, k);
        }
        assertThat(keys).isSorted();
    }
}
