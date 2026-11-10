package pl.stepday.common;

/**
 * Indeks ułamkowy dla {@code sort_key}: klucz między dwoma sąsiadami bez przenumerowania reszty.
 * Alfabet tylko [0-9a-z], żeby porządek w PostgreSQL (dowolna kolacja) i w Javie był ten sam.
 * Klucz nigdy nie kończy się na '0', więc zawsze da się wstawić coś przed nim.
 */
public final class SortKeys {

    private static final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int BASE = DIGITS.length();

    private SortKeys() {
    }

    /** Klucz ściśle między {@code a} i {@code b}; {@code null} oznacza początek/koniec. */
    public static String between(String a, String b) {
        String lo = a == null ? "" : a;
        if (b != null && lo.compareTo(b) >= 0) {
            throw new IllegalArgumentException("Wymagane a < b: " + a + " / " + b);
        }
        return midpoint(lo, b);
    }

    public static String after(String a) {
        return between(a, null);
    }

    private static String midpoint(String a, String b) {
        if (b != null) {
            int n = 0;
            while (n < b.length() && (n < a.length() ? a.charAt(n) : '0') == b.charAt(n)) {
                n++;
            }
            if (n > 0) {
                return b.substring(0, n) + midpoint(n < a.length() ? a.substring(n) : "", b.substring(n));
            }
        }
        int da = a.isEmpty() ? 0 : DIGITS.indexOf(a.charAt(0));
        int db = b == null ? BASE : DIGITS.indexOf(b.charAt(0));
        if (db - da > 1) {
            return String.valueOf(DIGITS.charAt((da + db + 1) / 2));
        }
        if (b != null && b.length() > 1) {
            return b.substring(0, 1);
        }
        return DIGITS.charAt(da) + midpoint(a.isEmpty() ? "" : a.substring(1), null);
    }
}
