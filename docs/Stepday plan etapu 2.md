# Stepday: plan etapu 2

Nov 10, 2026 · @Mateusz Grzędowski

Etap 2 dodaje konta (e-mail, Google), przeglądanie dni bieżącego tygodnia, miejsce „na początku” w ścieżce, dwa szablony startowe oraz ustawienia konta i powiadomień. Plan czeka na akceptację, implementacja rusza dopiero po niej.

## Ocena potrzeb

Wszystkie pięć braków jest zasadnych. Konta są warunkiem pozostałych punktów, więc idą pierwsze.

| Potrzeba | Ocena | Co proponuję |
| --- | --- | --- |
| Rejestracja: e-mail, Google | Konieczna. Etap 1 zakłada „uwierzytelnionego użytkownika”, ale logowanie było poza zakresem. Bez kont nie ma synchronizacji między telefonem a przeglądarką. | Jeden ekran startowy z dwiema drogami: Google albo e-mail. Logowanie przez Apple wypada z zakresu (korekta wymagań z 10 listopada). |
| Przeskakiwanie między dniami tygodnia | Zasadne i tanie: `GET /days/{date}` już istnieje. Trzeba dodać regułę „tylko dziś da się zmieniać” po stronie serwera, a nie tylko w interfejsie. | Pola planszy stają się klikalne. Dzień miniony pokazuje stan bloków bez przycisków. Dzień przyszły pokazuje plan wyliczony z reguł, bez zapisu do bazy. |
| Miejsce „Na początku” | Luka w etapie 1: selektor miał tylko „Po: …”. Indeks ułamkowy (`sort_key`) obsługuje to bez zmiany schematu. | Osobny arkusz wyboru: „Na początku”, „Po: …”, „Na końcu”. |
| Szablony: Dzień roboczy, Weekend | Zasadne. Pusta aplikacja to największa bariera startu, zwłaszcza przy ADHD. | Szablon to pakiet startowy: tworzy zwykłe bloki z regułą dni (Pn–Pt albo So–Nd). Bez nowej tabeli. Wybór zaraz po rejestracji i później z ekranu typu bloku. |
| Ustawienia konta, połączone konta, powiadomienia | Konieczne razem z kontami. Część pól już istnieje (`timezone`, `day_start`), ale nie ma ich gdzie zmienić. | Dwa ekrany: Ustawienia i Powiadomienia. Wejście przez ikonę w nagłówku widoku dnia. |

„Sterowanie synchronizacją z kontami” rozumiem jako łączenie i rozłączanie logowania przez Google. Logowanie przez Apple oraz synchronizacja z kalendarzem są poza zakresem tego planu.

## Moje propozycje

Pierwsze cztery proponuję włączyć do etapu 2, bo wynikają wprost z kont i powiadomień. Reszta może poczekać.

| Propozycja | Po co | Etap |
| --- | --- | --- |
| Przejęcie istniejących danych | Bloki i historia z etapu 1 należą do użytkownika bez konta. Pierwsze zarejestrowane konto przejmuje ten wiersz `app_user`, więc nic nie ginie. | 2 |
| Reset hasła i potwierdzenie e-maila | Konto na hasło bez resetu to pewna utrata dostępu. Wymaga wysyłki poczty (SMTP). | 2 |
| Eksport danych i usunięcie konta | Wymóg RODO. Usunięcie kasuje wszystko kaskadowo, eksport to jeden plik JSON. | 2 |
| Podpowiedź instalacji i wieczorne przypomnienie | Na iPhonie powiadomienia działają tylko w aplikacji dodanej do ekranu początkowego, więc ekran Powiadomienia musi to powiedzieć. Jedno przypomnienie wieczorem („zostały 2 kroki”) daje więcej niż przypomnienia per blok. | 2 |
| Cisza nocna | Powiadomienia nie przychodzą w wybranych godzinach. Mały koszt, duży komfort. | 2 |
| Historia starszych tygodni | Strzałka „poprzedni tydzień” nad planszą. Dane już są, to tylko nawigacja. | 3 |
| Logowanie bez hasła (passkey lub link z e-maila) | Wygodniejsze niż hasło, ale to osobny temat wdrożeniowy. | 3 |
| Synchronizacja z kalendarzem | Bloki z wydarzeń kalendarza Google lub Apple. Duży zakres (zgody, odświeżanie tokenów). | 3 |
| Punkty, serie i nagrody | Otwarte decyzje z etapu 1 nadal czekają. Warto je zamknąć po kilku tygodniach używania. | 3 |

## Makiety etapu 2

Siedem nowych plansz leży w drugim rzędzie [canvasu z makietami](https://claude.ai/artifact/JQX4MLH2DzerF97cmeMvcc), pod ekranami etapu 1. Pliki mają nazwy z tabeli i leżą w `project/*.dc.html`.

| Plansza | Plik | Co pokazuje | Komponent Vue |
| --- | --- | --- | --- |
| 8 · Start | `Logowanie` | Google albo e-mail. Jeden ekran dla logowania i rejestracji: po wpisaniu adresu aplikacja sama rozpoznaje, czy konto istnieje. | `AuthStartView` |
| 9 · Nowe konto | `Rejestracja` | E-mail, imię, hasło, zgoda. Informacja o linku potwierdzającym. | `RegisterView` |
| 10 · Szablony dnia | `Szablony` | Dwa pakiety po trzy bloki, każdy blok można odznaczyć. Można też zacząć od pustego dnia. | `StarterPacksView` |
| 11 · Podgląd innego dnia | `Podglad` | Poniedziałek wybrany na planszy, pasek „Podgląd dnia”, bloki ze stanem i bez przycisków, „Wróć do dziś”. | `DayView` w trybie `readonly` |
| 12 · Miejsce w ścieżce | `Miejsce` | „Na początku”, „Po: …”, „Na końcu” oraz podgląd kolejności. | `PathPlaceSheet` |
| 13 · Ustawienia | `Ustawienia` | Konto, logowanie przez Google, powiadomienia, początek dnia i strefa, eksport, usunięcie konta, wylogowanie. | `SettingsView` |
| 14 · Powiadomienia | `Powiadomienia` | Stan na tym urządzeniu, trzy rodzaje powiadomień, cisza nocna, lista urządzeń. | `NotificationSettingsView` |

Na planszy „Widok dnia” z etapu 1 doszła ikona konta w prawym górnym rogu. Prowadzi do Ustawień.

Przycisk Google jest na makiecie tekstowy. W aplikacji musi to być oficjalny przycisk zgodny z wytycznymi Google.

## Warstwa danych

Etap 2 rozszerza `app_user` i dodaje pięć tabel. Tabele dni i bloków z etapu 1 zostają bez zmian, poza jedną kolumną w `block_template`. Całość to jedna migracja Flyway (`V2__accounts_and_notifications.sql`).

| Tabela | Rola |
| --- | --- |
| `app_user` (zmiana) | Dochodzą: e-mail, hasło, imię, znacznik ukończenia startu |
| `user_identity` | Logowanie przez Google przypięte do konta |
| `email_token` | Jednorazowe linki: potwierdzenie adresu i reset hasła |
| `notification_settings` | Co i kiedy wysyłać, cisza nocna |
| `push_subscription` | Urządzenia z włączonym Web Push |
| `notification_log` | Co już wysłano, żeby nie wysłać dwa razy |
| `block_template` (zmiana) | `starter_key`: z którego szablonu startowego powstał blok |

```sql
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TYPE auth_provider       AS ENUM ('GOOGLE');
CREATE TYPE email_token_purpose AS ENUM ('VERIFY_EMAIL','RESET_PASSWORD');
CREATE TYPE notification_kind   AS ENUM ('BLOCK_REMINDER','EVENING_REMINDER','WEEKLY_SUMMARY');

ALTER TABLE app_user
  ADD COLUMN email             citext UNIQUE,   -- NULL tylko dla wiersza sprzed etapu 2
  ADD COLUMN email_verified_at timestamptz,
  ADD COLUMN password_hash     text,            -- Argon2id; NULL = konto bez hasła (tylko Google)
  ADD COLUMN display_name      text,
  ADD COLUMN onboarded_at      timestamptz;     -- ustawiane po ekranie szablonów

CREATE TABLE user_identity (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  provider   auth_provider NOT NULL,
  subject    text NOT NULL,                     -- pole "sub" z tokenu dostawcy
  email      citext,                            -- adres od dostawcy
  linked_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE (provider, subject),
  UNIQUE (user_id, provider)
);

CREATE TABLE email_token (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  purpose    email_token_purpose NOT NULL,
  token_hash text NOT NULL UNIQUE,              -- SHA-256 tokenu; sam token tylko w e-mailu
  expires_at timestamptz NOT NULL,
  used_at    timestamptz
);

CREATE TABLE notification_settings (
  user_id         uuid PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
  block_reminders boolean NOT NULL DEFAULT true,
  evening_enabled boolean NOT NULL DEFAULT true,
  evening_time    time    NOT NULL DEFAULT '20:00',
  weekly_summary  boolean NOT NULL DEFAULT true,
  quiet_enabled   boolean NOT NULL DEFAULT true,
  quiet_from      time    NOT NULL DEFAULT '22:00',
  quiet_to        time    NOT NULL DEFAULT '07:00'
);

CREATE TABLE push_subscription (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  endpoint        text NOT NULL UNIQUE,
  p256dh          text NOT NULL,
  auth            text NOT NULL,
  device_label    text NOT NULL,                -- np. "iPhone", "Chrome, Windows"
  created_at      timestamptz NOT NULL DEFAULT now(),
  last_success_at timestamptz
);
CREATE INDEX push_subscription_user_idx ON push_subscription (user_id);

CREATE TABLE notification_log (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  kind        notification_kind NOT NULL,
  local_date  date NOT NULL,
  instance_id uuid REFERENCES block_instance(id) ON DELETE CASCADE,
  sent_at     timestamptz NOT NULL DEFAULT now()
);
-- jedno powiadomienie danego rodzaju na dzień (i na blok, jeśli dotyczy bloku)
CREATE UNIQUE INDEX notification_log_once
  ON notification_log (user_id, kind, local_date,
                       COALESCE(instance_id, '00000000-0000-0000-0000-000000000000'::uuid));

ALTER TABLE block_template ADD COLUMN starter_key text;  -- np. 'workday.morning'
CREATE UNIQUE INDEX block_template_starter_once
  ON block_template (user_id, starter_key)
  WHERE starter_key IS NOT NULL AND archived_at IS NULL;
```

Uwagi do schematu:

- Sesje trzyma Spring Session w PostgreSQL (tabele tworzy biblioteka). Redis zostaje wyłącznie pamięcią podręczną, zgodnie z etapem 1.
- Reguły „konto ma co najmniej jeden sposób logowania” nie da się wygodnie zapisać w bazie, więc pilnuje jej serwis: hasło albo przynajmniej jeden wiersz `user_identity`.
- Ustawienia powiadomień powstają razem z kontem, z wartościami domyślnymi.
- Usunięcie konta to `DELETE FROM app_user`. Kaskady usuwają resztę, łącznie z dniami i blokami z etapu 1.

## Reguły i API

Nowe ścieżki mają ten sam prefiks `/api` co w etapie 1. Wszystko poza `/auth/**` wymaga zalogowanej sesji.

**Konta i logowanie**

1. Sesja to ciasteczko `HttpOnly`, `Secure`, `SameSite=Lax`. Front nie przechowuje żadnych tokenów.
2. Logowanie przez Google szuka pary (`provider`, `subject`). Jeśli jej nie ma, a dostawca potwierdza adres e-mail zgodny z istniejącym, potwierdzonym kontem, tożsamość zostaje do niego dołączona. W każdym innym przypadku powstaje nowe konto.
3. Imię z konta Google zapisujemy przy pierwszym logowaniu jako nazwę wyświetlaną. Użytkownik może ją zmienić w Ustawieniach.
4. Rozłączenie ostatniego sposobu logowania jest odrzucane (`409 LAST_LOGIN_METHOD`). Konto bez hasła może je ustawić w Ustawieniach.
5. Pierwsze konto założone po wdrożeniu przejmuje wiersz `app_user` z etapu 1 (ten bez adresu e-mail), więc dotychczasowe bloki i historia zostają.
6. Hasła: Argon2id, co najmniej 10 znaków. Logowanie, reset i sprawdzanie adresu mają limit prób.

**Dni tygodnia**

1. Zmieniać można tylko dzień bieżący: `local_date` równe dzisiejszej dacie użytkownika i `status = OPEN`. Każdy zapis do innego dnia serwer odrzuca (`409 DAY_NOT_CURRENT`). Interfejs dodatkowo ukrywa przyciski, ale regułę egzekwuje serwer.
2. Dzień miniony zwraca zapisane instancje ze stanami, bez zmian.
3. Dzień przyszły w bieżącym tygodniu zwraca plan wyliczony z reguł i nie zapisuje niczego w bazie. `ensureDay` nadal działa tylko dla dnia bieżącego.
4. Odpowiedź `GET /days/{date}` dostaje dwa pola: `editable` i `preview`.
5. Zmiana z kolejki offline, która dotrze po zamknięciu dnia, też jest odrzucana. Aplikacja pokazuje wtedy krótką informację i usuwa ją z kolejki.

**Endpointy**

| Ekran | Endpoint | Uwagi |
| --- | --- | --- |
| Start | `POST /auth/email/check`, `POST /auth/login`, `POST /auth/register`, `POST /auth/logout` | `check` mówi, czy pokazać pole hasła, czy formularz nowego konta. |
| Start | `GET /auth/google/start`, `GET /auth/google/callback` | Spring Security OAuth2 Client. Jedyny zewnętrzny dostawca w etapie 2. |
| Nowe konto | `GET /auth/verify?token=`, `POST /auth/password/forgot`, `POST /auth/password/reset` | Tokeny jednorazowe: potwierdzenie 48 godzin, reset 1 godzina. |
| Szablony dnia | `GET /starter-packs`, `POST /starter-packs/apply` z `{blocks: [starterKey]}` | Tworzy zwykłe szablony bloków. Powtórzenie niczego nie dubluje (`starter_key`). Ustawia `onboarded_at`. |
| Podgląd innego dnia | `GET /days/{date}` | Jak w etapie 1, plus `editable` i `preview`. Zakres: od poniedziałku do niedzieli bieżącego tygodnia. |
| Miejsce w ścieżce | `POST /templates`, `PUT /templates/{id}` | Zamiast samego `afterTemplateId`: `position: {placement: START / AFTER / END, afterTemplateId}`. `START` liczy `sort_key` przed pierwszym blokiem. |
| Ustawienia | `GET /me`, `PATCH /me`, `PUT /me/password` | `PATCH`: `displayName`, `timezone`, `dayStart`. Nowy początek dnia obowiązuje od następnego dnia. |
| Ustawienia | `GET /me/identities`, `DELETE /me/identities/{provider}` | Łączenie idzie przez `/auth/{provider}/start?link=true`. |
| Ustawienia | `GET /me/export`, `DELETE /me` | Eksport to jeden plik JSON. Usunięcie wymaga ponownego logowania tuż przed operacją. |
| Powiadomienia | `GET /me/notifications`, `PUT /me/notifications` | Pola jak w tabeli `notification_settings`. |
| Powiadomienia | `GET /push/vapid-key`, `POST /push/subscriptions`, `GET /push/subscriptions`, `DELETE /push/subscriptions/{id}` | Subskrypcja, na którą serwer push odpowiada 404 lub 410, jest usuwana. |

**Wysyłka powiadomień.** Zadanie `@Scheduled` co minutę wybiera użytkowników, u których w ich strefie czasowej przypada termin:

- przypomnienie bloku o `reminder_time`, jeśli blok jest jeszcze `PENDING`,
- wieczorne przypomnienie o `evening_time`, jeśli dzień jest `OPEN` i zostały niezrobione kroki,
- podsumowanie tygodnia w poniedziałek o 8:00.

Powiadomienie, które wypada w ciszy nocnej, wychodzi o `quiet_to`. Przed wysłaniem serwer wstawia wiersz do `notification_log`; jeśli wiersz już jest, niczego nie wysyła.

## Szablony startowe

Szablon dnia to pakiet trzech gotowych bloków z regułą dni tygodnia. Po dodaniu są to zwykłe bloki użytkownika. Definicje pakietów leżą w kodzie (plik JSON w zasobach), nie w bazie.

| Pakiet | Blok | `starter_key` | Typ i rola | Konfiguracja | Reguła | Zaległy |
| --- | --- | --- | --- | --- | --- | --- |
| Dzień roboczy | Poranek | `workday.morning` | `LIST`, `STEP` | Śniadanie, Witaminy lub leki, Zęby | `FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR` | `DROP` |
| Dzień roboczy | Woda | `workday.water` | `COUNTER`, `ALL_DAY` | cel 8 szklanek, próg 80% | jak wyżej | `DROP` |
| Dzień roboczy | Ruch | `workday.move` | `TIMER`, `STEP` | 20 minut | jak wyżej | `DROP` |
| Weekend | Porządki | `weekend.chores` | `LIST`, `STEP` | Pranie, Odkurzanie, Łazienka | `FREQ=WEEKLY;BYDAY=SA` | `CARRY_OVER` |
| Weekend | Zakupy na tydzień | `weekend.groceries` | `BOOLEAN`, `STEP` | brak | `FREQ=WEEKLY;BYDAY=SA` | `CARRY_OVER` |
| Weekend | Spacer | `weekend.walk` | `TIMER`, `STEP` | 30 minut | `FREQ=WEEKLY;BYDAY=SA,SU` | `DROP` |

Porządki i zakupy wypadają w sobotę i przechodzą na niedzielę, jeśli nie zostały zrobione. Dzięki temu nie trzeba ich robić dwa razy, a niezrobione w sobotę nie psują od razu dnia.

Pakiety można dodać na ekranie startowym po rejestracji oraz później z ekranu wyboru typu bloku (sekcja „Albo zacznij od gotowego szablonu” z etapu 1).

## Decyzje do akceptacji

Siedem kwestii, przy każdej moja rekomendacja. Zaznacz te, z którymi się zgadzasz, a przy pozostałych zostaw komentarz. Po akceptacji całości plan idzie do implementacji.

- [ ] **Sesja w ciasteczku, dane sesji w PostgreSQL.** Alternatywą jest JWT trzymany w przeglądarce, ale jest trudniejszy do bezpiecznego wylogowania. Rekomendacja: ciasteczko.
- [ ] **Poczta wychodząca.** Potwierdzenie adresu i reset hasła wymagają dostawcy SMTP oraz rekordów SPF i DKIM dla stepday.pl. Potrzebuję od Ciebie wyboru dostawcy. Do testów wystarczy lokalna skrzynka w Docker Compose.
- [ ] **Łączenie kont po adresie e-mail.** Rekomendacja: automatycznie tylko wtedy, gdy adres jest potwierdzony po obu stronach. Inaczej ktoś mógłby przejąć cudze konto, zakładając je wcześniej na ten sam adres.
- [ ] **Dni przyszłe pokazują plan.** Rekomendacja: tak, jako podgląd bez przycisków. Alternatywa: pola przyszłych dni są nieaktywne.
- [ ] **Zakres przeglądania to bieżący tydzień.** Tak jak w Twoim opisie. Starsze tygodnie przesuwam do etapu 3.
- [ ] **„Synchronizacja z kontami” oznacza łączenie logowania przez Google.** Jeśli chodziło Ci o kalendarz, to osobny, duży temat i proponuję etap 3.
- [ ] **Skład szablonów startowych.** Sześć bloków z tabeli wyżej. Do rozstrzygnięcia: czy „Woda” ma być codziennie, a nie tylko w dni robocze.

Rozstrzygnięte 10 listopada: logowanie przez Apple jest poza zakresem etapu 2.

Przyjęte bez pytania, do zmiany w każdej chwili: hasło od 10 znaków, wieczorne przypomnienie o 20:00, cisza nocna 22:00–07:00, usunięcie konta natychmiastowe po ponownym logowaniu.

## Kroki wykonania i prompt dla Opus 5.5

Dziewięć kroków, każdy kończy się zielonymi testami. Kolejność wynika z zależności: najpierw konta, bo reszta potrzebuje zalogowanego użytkownika.

1. **Rozpoznanie.** Przeczytać kod etapu 1 i opisać, jak dziś ustalany jest użytkownik oraz co już istnieje z Web Push. Rozbieżności z tym planem zgłosić przed kodowaniem.
2. **Migracja i encje.** `V2__accounts_and_notifications.sql`, encje JPA, repozytoria. Test migracji na bazie z danymi etapu 1.
3. **Konta e-mail.** Rejestracja, logowanie, sesja, potwierdzenie adresu, reset hasła, przejęcie danych etapu 1. Testy: podwójna rejestracja, zły i wygasły token, limit prób, obce dane niedostępne dla innego konta.
4. **Google.** Logowanie, łączenie i rozłączanie. Testy: dołączenie po potwierdzonym adresie, odmowa rozłączenia ostatniego sposobu logowania.
5. **Dni tygodnia.** Pola `editable` i `preview`, blokada zapisu, plan dnia przyszłego. Testy: zapis do wczoraj i do jutra daje 409, podgląd jutra nie tworzy wierszy, granica dnia o 04:00.
6. **Miejsce w ścieżce.** `position` ze `START`, `AFTER`, `END`. Testy: blok dodany na początku jest pierwszy także po kolejnych wstawieniach.
7. **Szablony startowe.** Katalog pakietów, `apply`, ekran po rejestracji. Testy: powtórne `apply` nie dubluje bloków, reguły dni zgodne z tabelą.
8. **Ustawienia.** Profil, początek dnia i strefa, eksport, usunięcie konta. Testy: zmiana początku dnia działa od następnego dnia, po usunięciu nie zostaje żaden wiersz użytkownika.
9. **Powiadomienia.** Ustawienia, subskrypcje, zadanie wysyłające, cisza nocna. Testy z zegarem testowym: brak duplikatów, przesunięcie na koniec ciszy, usunięcie martwej subskrypcji.

Front powstaje razem z każdym krokiem, według tabeli makiet. Każdy ekran jest porównywany z makietą przy szerokości 390 px.

**Prompt startowy**

```text
Kontynuujesz pracę nad aplikacją Stepday (stepday.pl). Etap 1 jest wdrożony w tym repozytorium. Teraz realizujesz etap 2 według zaakceptowanego planu. Nie wymyślaj własnego designu ani własnego modelu danych.

## Źródła prawdy

1. Plan etapu 2 (ocena, warstwa danych, reguły, API, kroki): https://claude.ai/artifact/DL4mNfHT23EuQJWFPEXuyx
2. Makiety (canvas, plansze E2 · 8 do E2 · 14 w drugim rzędzie oraz zmieniony widok dnia): https://claude.ai/artifact/JQX4MLH2DzerF97cmeMvcc
3. Dokument etapu 1 (obowiązuje nadal, etap 2 go rozszerza): https://claude.ai/artifact/SnrchJn1CzUNj2n9rksqkC

Przeczytaj wszystkie trzy w całości, zanim zmienisz kod. Jeśli któregoś nie możesz otworzyć, przerwij i poproś o eksport do docs/. Nie zgaduj treści.
Pierwszeństwo: dane i zachowanie według planu, wygląd według makiet. Sprzeczności wypisz i zapytaj.

## Zakres etapu 2

Konta (e-mail z hasłem, Google), podgląd dni bieżącego tygodnia z edycją tylko dzisiaj, miejsce "Na początku" w ścieżce, dwa szablony startowe po trzy bloki, ustawienia konta, powiadomienia Web Push.

## Reguły, których nie wolno zgubić

- Sesja w ciasteczku HttpOnly, Secure, SameSite=Lax. Dane sesji w PostgreSQL (Spring Session JDBC). Redis tylko jako cache. Żadnych tokenów w localStorage.
- Każde zapytanie o dane jest zawężone do zalogowanego użytkownika. Dodaj test, że konto A nie widzi i nie zmienia danych konta B.
- Pierwsze konto po wdrożeniu przejmuje wiersz app_user z etapu 1. Historia nie może zginąć.
- Konto zawsze ma co najmniej jeden sposób logowania (409 LAST_LOGIN_METHOD).
- Łączenie tożsamości po adresie e-mail tylko przy adresie potwierdzonym po obu stronach.
- Zapis wyłącznie do dnia bieżącego i otwartego. Inaczej 409 DAY_NOT_CURRENT, egzekwowane na serwerze.
- Podgląd dnia przyszłego niczego nie zapisuje. ensureDay działa tylko dla dzisiaj.
- Szablony startowe tworzą zwykłe bloki. Powtórne zastosowanie nie dubluje (starter_key).
- Powiadomienia: bez duplikatów (notification_log), cisza nocna przesuwa wysyłkę na jej koniec, martwe subskrypcje są usuwane.
- Reguły etapu 1 zostają bez zmian: próg 80%, stany FULL / PARTIAL / MISSED, CARRY_OVER najwyżej 2 razy, wpis nigdy nie znika.
- Hasła Argon2id. Sekrety (Google, VAPID, SMTP) tylko w zmiennych środowiskowych, nigdy w repozytorium.
- Interfejs po polsku, cele dotykowe co najmniej 44 px, kolory i fonty z makiet. Przycisk Google zgodny z wytycznymi Google.

## Decyzje

Sekcja "Decyzje do akceptacji" w planie pokazuje, co właściciel zaakceptował (zaznaczone pola) i co skomentował. Pozycje niezaznaczone traktuj jako nierozstrzygnięte: zapytaj, zanim je zaimplementujesz. Własne założenia dopisuj do docs/decisions.md.

## Sposób pracy

1. Krok 1 planu to rozpoznanie: opisz obecną autoryzację, właściciela danych i stan Web Push w kodzie. Przedstaw plan zmian i zaczekaj na moje "OK".
2. Potem kroki 2 do 9 z planu, po kolei. Każdy kończy się zielonymi testami (PostgreSQL w Testcontainers, zegar testowy dla czasu), zanim zaczniesz następny.
3. Front budujesz razem z krokiem, którego dotyczy. Każdy ekran porównaj z makietą w Playwright przy szerokości 390 px i opisz odchylenia.
4. Zaktualizuj CLAUDE.md o reguły etapu 2.
5. Małe, opisowe commity na gałęzi etap-2. Nie pushuj na main bez mojej zgody.
6. Logowanie przez Apple jest poza zakresem etapu 2. Nie dodawaj go.

## Gotowe, gdy

Nowy użytkownik zakłada konto e-mailem albo przez Google, wybiera szablony, zalicza dzień, ogląda wczoraj i jutro bez możliwości zmiany, dodaje blok na początek ścieżki, zmienia ustawienia, włącza powiadomienia na telefonie i dostaje wieczorne przypomnienie. Dane z etapu 1 są nadal dostępne na pierwszym koncie. Testy są zielone, ekrany zgadzają się z makietami.
```

## Źródła

- [Stepday: warstwa danych etapu 1](https://claude.ai/artifact/SnrchJn1CzUNj2n9rksqkC)
- [Makiety Stepday](https://claude.ai/artifact/JQX4MLH2DzerF97cmeMvcc)
