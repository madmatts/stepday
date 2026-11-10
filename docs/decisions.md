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
Przycisk „+” zatrzymuje się na celu (jak w makiecie), a „−” na zerze. Serwer przyjmuje dowolną deltę i **nie przycina** wyniku: dokument definiuje `value_int` jako dokładną sumę `counter_event.delta` (przycinanie rozjechałoby sumę przy scalaniu zapisów z dwóch urządzeń).

### A7. „Zastosuj także dziś”
Tylko flaga `applyToday` w `PUT /templates/{id}` (brak ekranu w makietach).

### A8. CARRY_OVER przy regule codziennej
Jeśli zaległa instancja trafia na dzień, w którym szablon i tak wypada, w dniu są obie (częściowy indeks unikalny dopuszcza to, bo zaległa ma `carried_from_id`).

### A9. HTTPS dla iPhone'a
Service worker i Web Push wymagają HTTPS (poza `localhost`). `docker compose up` działa na localhost; do testów na iPhonie potrzebny tunel HTTPS lub reverse proxy z certyfikatem.

### A10. stepday-web poza Mavenem
`stepday-web` to projekt npm budowany przez npm i Docker; root `pom.xml` agreguje tylko `stepday-api`.

### A11. „Rzadsza niż codzienna” (domyślna polityka zaległych)
`DROP` tylko dla `FREQ=DAILY` z `INTERVAL=1` bez `COUNT=1`. Wszystko inne (tygodniowe, co N dni, miesięczne, jednorazowe) dostaje `CARRY_OVER`. Jawne `overduePolicy` z ekranu „Kiedy” zawsze wygrywa. `POST /schedule/preview` zwraca `defaultOverduePolicy`, żeby front nie powielał reguły.
Zmiana: `ScheduleService.defaultOverduePolicy`.

### A12. `sort_key`
Indeks ułamkowy z alfabetem `[0-9a-z]` (porządek identyczny w Javie i w PostgreSQL niezależnie od kolacji). Jedna przestrzeń kluczy dla obu ról. `afterTemplateId` wstawia blok za wskazanym, bez przenumerowania pozostałych.

### A13. Pozycje listy przy edycji szablonu
Pozycje z `id` są aktualizowane, nowe dodawane, usunięte archiwizowane (`archived_at`), nigdy kasowane.

### A14. Dni przyszłe
`GET /days/{date}` dla daty po „dziś” zwraca 400 i niczego nie generuje — wygenerowanie dnia zamroziłoby definicję bloków z wyprzedzeniem. Planowanie przyszłości to ekran „Układ dnia” (`GET /layout?date=`). „Dziś” liczy serwer (`GET /days/today`).

### A15. Cache dnia w Redisie
Klucz `day:<user>:<date>`, TTL 10 min, zbiór-indeks per użytkownik. Każdy zapis unieważnia **wszystkie** dni użytkownika (pasek tygodnia, plansza i seria zależą od innych dni). Timeout 300 ms; po błędzie cache jest omijany przez 30 s (bezpiecznik), żeby awaria Redisa nie spowalniała API. Ryzyko: nieudane unieważnienie w czasie awarii może zostawić nieaktualny wpis do wygaśnięcia TTL. Domyślnie cache wyłączony (`CACHE_ENABLED=false`), w docker compose włączony.

### A16. Plansza: numer pola
Makieta pokazuje 10.11 „pole 10 z 30” i „po ostatnim kroku przechodzisz na pole 11”, więc pionek stoi na polu *(zaliczone dni miesiąca do dziś włącznie) + 1*, maksymalnie liczba dni miesiąca (D1). Nowy miesiąc = nowa plansza.

### A17. „Z kalendarza”
Flaga `fromCalendar` w widoku dnia = reguła szablonu nie jest codzienna (ta sama definicja co A11). Wyliczana przy odczycie z bieżącej reguły szablonu (to informacja do wyświetlenia, nie część zamrożonej historii).

### A18. Postęp „kroków dziś”
`progress.done` = kroki (`STEP`) w stanie `DONE`; `total` = kroki dnia bez `CARRIED`. Pominięte nie liczą się jako zrobione (jak w makiecie). Bloki `ALL_DAY` nie wchodzą do licznika kroków, ale wchodzą do stanu dnia.

### A19. „Ostatni zapis wygrywa” a zegary
Zapisy pozycji listy i notatki mogą nieść `changedAt` (czas zmiany na telefonie, z kolejki offline). Zapis wygrywa, jeśli nie jest starszy niż ostatni zapisany; zapis bez `changedAt` dostaje czas serwera. Wiersze generowane przez `ensureDay` mają `updated_at = epoch` („nie zmieniane przez klienta”), więc pierwszy zapis z telefonu wygrywa nawet przy spóźnionym zegarze.

### A20. Zapis do zamkniętego dnia
Zwraca 409. Dzień jest edytowalny do zamknięcia (dokument, sekcja 5). Kolejka offline, która dotrze po zamknięciu (po `day_start` następnego dnia), dostaje 409 i front pokazuje, że zmiana nie weszła.

### A21. Postęp po „Pomiń”
Postęp na pominiętym bloku (odhaczenie pozycji, przyrost licznika) zdejmuje `SKIPPED` i stan liczy się od nowa. „Pomiń” na bloku `DONE` zwraca 409. Każdy zapis zwraca świeży widok dnia.

### A22. Polityka zaległych przy zamykaniu
Polityka (`overdue_policy`) jest czytana z **bieżącego** szablonu w chwili zamknięcia (instancja jej nie kopiuje — schemat z dokumentu nie ma takiej kolumny). Szablon usunięty → `DROP`.

### A23. Co przechodzi z zaległym blokiem
Nowa instancja kopiuje zamrożoną definicję instancji źródłowej (nie bieżący szablon) oraz pozycje listy **ze stanem odhaczenia** (zaczęte zakupy kontynuuje się, a nie zaczyna od nowa). Licznik startuje od 0, bo `value_int` musi być sumą zdarzeń tej instancji.

### A24. Zamykanie: chwila, kolejność, uzupełnianie
Zadanie co minutę (`stepday.closing.interval`, domyślnie `PT1M`); dzień D zamyka się, gdy w strefie użytkownika minie `day_start` dnia D+1. Dni zamykane po kolei od dnia po ostatnim zamkniętym, każdy w osobnej transakcji z blokadą wiersza `day` (zapisy z telefonu blokują ten sam wiersz). Brakujące dni są generowane i zamykane jako `MISSED` (A4). Dzień utworzony przez przeniesienie zaległości nie jest „brakujący” — jego stan wynika z reguł (np. jedyny blok przeniesiony dalej → `FULL`). Użytkownik bez żadnego dnia nie jest uzupełniany.

### A25. Podsumowanie tygodnia: kształt odpowiedzi
`GET /weeks/{weekStart}/summary` (weekStart = poniedziałek, inaczej 400). Pola: `outcome` (`SUCCESS`|`GAPS`), `finished` (false, gdy tydzień trwa — dni przyszłe/otwarte mają `status: null`), `counts` (full/partial/missed/completed), `days[]`, `gaps[]`. `gaps` pochodzi z zapytania z dokumentu (dosłownie) + `config` dla jednostki i progu; dni bez wiersza `day` dochodzą jako `MISSED` z pustą listą bloków. Teksty („Nie odhaczono: …”, „brakowało 1”) składa front z danych.

### A26. Pusty dzień
Dzień bez żadnego bloku ma według reguł z dokumentu status `FULL` (nic nie jest niezaliczone). UI nie pokazuje wtedy „Dzień zaliczony”, tylko zachętę „Zaprojektuj pierwszy rytuał”. Dane zostają zgodne z regułą.

### A27. Front: nawigacja, logowanie, przechowywanie
Nawigacja do „Układu dnia” i „Podsumowania tygodnia” to dwie ikony w wierszu „Stepday” (makiety nie pokazują nawigacji). Dane logowania (Basic, A2) leżą w `localStorage`. Przy prawdziwym logowaniu zastąpi je sesja albo token. Kafelki dni na planszy prowadzą do widoku danego dnia (dni zamknięte tylko do odczytu).
