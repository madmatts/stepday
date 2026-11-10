package pl.stepday.day;

/** Wynik bloku według tabeli z sekcji 5 dokumentu. */
public enum Completion {
    /** W pełni. */
    FULL,
    /** Z niedosytem: SKIPPED albo licznik między progiem a celem. */
    PARTIAL,
    /** Niezaliczony (jeszcze albo ostatecznie). */
    NONE,
    /** CARRIED: nie wlicza się do wyniku dnia, z którego blok odszedł. */
    EXCLUDED
}
