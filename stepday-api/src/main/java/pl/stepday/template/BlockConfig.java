package pl.stepday.template;

import java.util.LinkedHashMap;
import java.util.Map;

/** Walidacja i uzupełnienie {@code config} (jsonb) według typu bloku. */
public final class BlockConfig {

    public static final int DEFAULT_THRESHOLD_PCT = 80;

    private BlockConfig() {
    }

    public static Map<String, Object> normalize(BlockType type, Map<String, Object> raw) {
        Map<String, Object> in = raw == null ? Map.of() : raw;
        Map<String, Object> out = new LinkedHashMap<>();
        switch (type) {
            case COUNTER -> {
                out.put("target", positiveInt(in, "target", null));
                out.put("unit", in.getOrDefault("unit", "").toString());
                out.put("step", positiveInt(in, "step", 1));
                int pct = positiveInt(in, "threshold_pct", DEFAULT_THRESHOLD_PCT);
                if (pct > 100) {
                    throw new IllegalArgumentException("threshold_pct musi być w zakresie 1–100");
                }
                out.put("threshold_pct", pct);
            }
            case TIMER -> out.put("minutes", positiveInt(in, "minutes", null));
            default -> { }
        }
        return out;
    }

    /** Próg zaliczenia licznika: ceil(target * pct / 100), liczony w liczbach całkowitych. */
    public static int threshold(int target, int thresholdPct) {
        return (target * thresholdPct + 99) / 100;
    }

    public static int threshold(Map<String, Object> config) {
        return threshold(intValue(config, "target"), intValue(config, "threshold_pct", DEFAULT_THRESHOLD_PCT));
    }

    public static int intValue(Map<String, Object> config, String key) {
        Object v = config.get(key);
        if (!(v instanceof Number n)) {
            throw new IllegalStateException("Brak liczby w config." + key);
        }
        return n.intValue();
    }

    public static int intValue(Map<String, Object> config, String key, int fallback) {
        return config.get(key) instanceof Number n ? n.intValue() : fallback;
    }

    private static int positiveInt(Map<String, Object> in, String key, Integer fallback) {
        Object v = in.get(key);
        if (v == null) {
            if (fallback == null) {
                throw new IllegalArgumentException("Wymagane pole config." + key);
            }
            return fallback;
        }
        if (!(v instanceof Number n) || n.intValue() < 1 || n.doubleValue() != n.intValue()) {
            throw new IllegalArgumentException("config." + key + " musi być liczbą całkowitą ≥ 1");
        }
        return n.intValue();
    }
}
