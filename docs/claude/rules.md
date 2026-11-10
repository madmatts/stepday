# Reguły domeny (lista kontrolna, nie wolno ich zgubić)

Pełna wersja: `docs/stepday-data-layer.md`, sekcje 2 i 5 są wiążące.

- **Blok = typ + rola.** Typ: `LIST`, `BOOLEAN`, `COUNTER`, `TIMER`, `NOTE`. Rola: `STEP` (na ścieżce) albo `ALL_DAY` (obok ścieżki, np. woda).
- **Szablon a instancja.** `block_instance` trzyma zamrożoną kopię definicji (nazwa, typ, rola, config, sort_key, pozycje listy). Edycja szablonu nie zmienia historii; obowiązuje od następnego dnia.
- **Próg licznika:** `ceil(target * threshold_pct / 100)`, domyślnie 80% (7 z 8). Poniżej progu: niezaliczony; od progu: z niedosytem; `>= target`: w pełni. Serwer zwraca `threshold` już policzony.
- **Stany dnia:** `OPEN`, `FULL`, `PARTIAL`, `MISSED`. FULL i PARTIAL są zaliczone. Stan dnia liczy wyłącznie serwer, po każdym zapisie, z instancji obu ról (instancje `CARRIED` się nie liczą).
- **„Pomiń”** = `SKIPPED` (dzień najwyżej PARTIAL). **„Później”** = tylko `deferred_at`, blok na koniec ścieżki. Żadne z nich nie kosztuje punktów ani serii.
- **Zaległe:** `CARRY_OVER` → w dniu źródłowym `CARRIED`, nowa instancja z `carried_from_id` i `carry_count + 1`, najwyżej 2 razy. `DROP` albo wyczerpany limit → instancja `MISSED`, dzień `MISSED`. Wpis nigdy nie jest usuwany.
- **Domyślna polityka:** `DROP`; serwis ustawia `CARRY_OVER` dla reguł rzadszych niż codzienne.
- **Podsumowanie tygodnia jest liczone, nie zapisywane.** `SUCCESS` tylko gdy 7/7 dni `FULL`. Inaczej `GAPS` z listą: dzień + co dokładnie. Dzień bez wiersza `day` = niezaliczony. Tydzień od poniedziałku.
- **Harmonogram:** RRULE przez ical4j, jedna implementacja na backendzie, front bierze terminy z `POST /schedule/preview`. Kontrola: `FREQ=MONTHLY;INTERVAL=3` od 2026-11-10 → 10.11.2026, 10.02.2027, 10.05.2027.
- **`ensureDay`** leniwie i idempotentnie (`ON CONFLICT DO NOTHING` + częściowy indeks `block_instance_once_per_day`).
- **Granica dnia:** `day_start` (domyślnie 04:00) w `ZoneId` użytkownika. Zamykanie dni osobno dla każdej strefy. Znaczniki czasu to `timestamptz`.
- **Offline:** kolejka zdarzeń w IndexedDB z UUID od klienta; licznik = suma `counter_event.delta`, duplikat `client_event_id` ignorowany bez błędu.
- **Redis** wyłącznie jako cache (klucz `user:date`, unieważniany przy zapisie). Aplikacja działa bez niego.
- **Web Push** działa tylko w zainstalowanej PWA (iOS 16.4+, „Dodaj do ekranu początkowego”). UX musi to komunikować.
- **UI:** po polsku, cele dotykowe ≥ 44 px, stan nigdy samym kolorem (ikona + tekst), tokeny i fonty z makiet (`docs/claude/conventions.md`).
