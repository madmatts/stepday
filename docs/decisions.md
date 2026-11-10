# Decyzje i założenia

Każda decyzja ma identyfikator, uzasadnienie i miejsce w kodzie, w którym ją zmienić. Status „przyjęte” oznacza akceptację planu przez właściciela (2026-11-10); „założenie” to wartość domyślna przyjęta bez osobnego pytania.

## Otwarte decyzje z dokumentu (sekcja 9)

### D1. Plansza — przyjęte
Pozycja pionka to **liczba dni zaliczonych (FULL + PARTIAL) w bieżącym miesiącu**; „z N” to liczba dni miesiąca. `MISSED` nie przesuwa pionka. Plansza jest **wyliczana**, nie zapisywana.
Uzasadnienie: brief („zaliczony dzień przesuwa pionek o jedno pole”), spójność z wyliczanym podsumowaniem tygodnia, brak stanu do migrowania.
Zmiana: jedna metoda `BoardService.position(...)`.

### D2. Punkty — przyjęte
W MVP **brak punktacji**. `PARTIAL` to tylko znacznik (bursztynowa kropka) bez skutku.
Uzasadnienie: dokument nie definiuje punktów; „bez kary” dla ADHD.
Zmiana: punkty można wyliczyć z `day.status` bez zmian schematu.

### D3. Seria dni — przyjęte
Seria jest wyliczana i **przerywa ją tylko `MISSED`** (także dzień bez wiersza w `day`). Dzisiejszy dzień `OPEN` jej nie przerywa. `PARTIAL` i `SKIPPED` nie przerywają.
Zmiana: `StreakService`.

### D4. Granica dnia — przyjęte
Domyślnie **04:00**, ustawiana przez użytkownika (`PATCH /me` z `dayStart`, `timezone`). Kolumny są już w `app_user`.

### D5. Wiele reguł na blok — przyjęte
Zostaje **1:1** (`schedule_rule.template_id` jako PK). Różne pory = osobne bloki. `ScheduleService` przyjmuje kolekcję reguł, więc przejście na 1:N to migracja + zmiana repozytorium.

### D6. Timer — przyjęte
Timer zalicza się **po zakończeniu** (`stop` → `DONE`), także przy zakończeniu przed czasem, bez kary. `timer_started_at`/`timer_stopped_at` są zapisywane, więc próg w stylu licznika można dodać później w `config` (`threshold_pct`).

## Sprzeczności rozstrzygnięte przez właściciela

### D7. Przypomnienia licznika — przyjęte
Makieta „Licznik” pokazuje „Co 2 godziny, 8:00–20:00”, model ma jedno `reminder_time`. Wygrywa dokument: **jedna godzina przypomnienia**. Przypomnienia cykliczne wracają przy etapie Web Push. Front pokazuje przełącznik i jedną godzinę.

### D8. Gotowe szablony „Poranek / Wieczór / Dzień roboczy” — przyjęte
Dokument nie definiuje ich treści. Przyciski są w UI, ale **nieaktywne (bez treści) do czasu, aż właściciel ją poda**. Bez nowych tabel: szablon gotowy to po prostu seria `POST /templates`.

## Założenia implementacyjne

### A1. Stan instancji LIST i COUNTER
Licznik dostaje `DONE`, gdy `value_int >= threshold`; FULL vs PARTIAL wynika z `value_int` vs `target`. Spadek poniżej progu przywraca `PENDING`. Lista: `DONE`, gdy wszystkie pozycje odhaczone; odznaczenie przywraca `PENDING`.
Uzasadnienie: enum `instance_state` nie ma stanu „częściowo”, a zapytanie tygodniowe (`state NOT IN ('DONE','CARRIED') OR (COUNTER AND value_int < target)`) zakłada licznik 7/8 w stanie `DONE`. Gdyby został `PENDING`, zamknięcie dnia zmieniłoby go w `MISSED`.

### A2. Uwierzytelnianie (D-AUTH)
Dokument zakłada istniejący moduł logowania; nie ma go. Tryb **jednego użytkownika**: HTTP Basic z `STEPDAY_USER`/`STEPDAY_PASSWORD`, wiersz `app_user` tworzony przy starcie. Kod domeny pobiera użytkownika wyłącznie przez `CurrentUser`, więc podmiana na prawdziwe logowanie nie dotyka domeny.

### A3. `instance_item.updated_at`
Dokument przewiduje „ostatni zapis wygrywa” po `updated_at` dla pozycji listy, ale tabela nie ma tej kolumny. Dodana migracją `V2` (V1 = schemat z dokumentu bez zmian).

### A4. Uzupełnianie brakujących dni
Brakujący dzień jest generowany jak zwykle (zamrożone instancje, żeby „co dokładnie” miało szczegóły) i od razu zamykany. Dzień bez żadnego zaplanowanego bloku dostaje `MISSED`, zgodnie z dokumentem („brakujące wiersze `day` … są tworzone jako `MISSED`”).

### A5. Tydzień
Zaczyna się w poniedziałek (makieta: „2–8 listopada”).

### A6. Licznik w UI
Przycisk „+” zatrzymuje się na celu (jak w makiecie), serwer przyjmuje dowolną deltę, a `value_int` nie spada poniżej 0.

### A7. „Zastosuj także dziś”
Tylko flaga `applyToday` w `PUT /templates/{id}` (brak ekranu w makietach).

### A8. CARRY_OVER przy regule codziennej
Jeśli zaległa instancja trafia na dzień, w którym szablon i tak wypada, w dniu są obie (częściowy indeks unikalny dopuszcza to, bo zaległa ma `carried_from_id`).

### A9. HTTPS dla iPhone'a
Service worker i Web Push wymagają HTTPS (poza `localhost`). `docker compose up` działa na localhost; do testów na iPhonie potrzebny tunel HTTPS lub reverse proxy z certyfikatem.

### A10. stepday-web poza Mavenem
`stepday-web` to projekt npm budowany przez npm i Docker; root `pom.xml` agreguje tylko `stepday-api`.
