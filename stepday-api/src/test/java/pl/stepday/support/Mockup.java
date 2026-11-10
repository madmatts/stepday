package pl.stepday.support;

/** Bloki z makiet („Układ dnia”, wtorek 10.11.2026) jako ciała POST /templates. */
public final class Mockup {

    private Mockup() {
    }

    public static final String OKNO = """
            {"type":"BOOLEAN","name":"Otworzyć okno, rozciągnąć się",
             "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-01"}}""";

    public static final String TABLETKI = """
            {"type":"LIST","name":"Tabletki rano",
             "items":[{"label":"Witamina D3"},{"label":"Magnez"},{"label":"Omega-3"}],
             "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-01"},"reminderTime":"07:30"}""";

    public static final String FILTR = """
            {"type":"BOOLEAN","name":"Wymiana filtra",
             "schedule":{"rrule":"FREQ=MONTHLY;INTERVAL=3","dtstart":"2026-11-10"}}""";

    public static final String SPACER = """
            {"type":"TIMER","name":"Spacer","config":{"minutes":20},
             "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-01"}}""";

    public static final String ZAKUPY = """
            {"type":"LIST","name":"Zakupy na tydzień","items":[{"label":"Chleb"}],
             "schedule":{"rrule":"FREQ=WEEKLY;BYDAY=SA","dtstart":"2026-11-01"}}""";

    public static final String NOTATKA = """
            {"type":"NOTE","name":"Wieczorna notatka",
             "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-01"}}""";

    public static final String WODA = """
            {"type":"COUNTER","name":"Woda","role":"ALL_DAY",
             "config":{"target":8,"unit":"szklanek","step":1,"threshold_pct":80},
             "schedule":{"rrule":"FREQ=DAILY","dtstart":"2026-11-01"}}""";

    /** Kolejność ścieżki z makiety. */
    public static final String[] ALL = {OKNO, TABLETKI, FILTR, SPACER, ZAKUPY, NOTATKA, WODA};
}
