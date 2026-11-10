> Lokalna kopia (eksport z 2026-11-10) dokumentu https://claude.ai/artifact/SnrchJn1CzUNj2n9rksqkC.
> Źródłem prawdy jest dokument online; przy rozbieżności wygrywa on. Nie edytuj tej kopii ręcznie.

# Stepday: warstwa danych aplikacji rytuałów dnia

Nov 10, 2026 · @Mateusz Grzędowski

## Cel i jak tego użyć

Ten dokument opisuje warstwę danych aplikacji Stepday (stepday.pl) do planowania i zaliczania dnia, tak aby Claude Code mógł ją zaimplementować razem z ekranami z makiet.

- **Makiety:** [klikalne ekrany w Claude](https://claude.ai/artifact/JQX4MLH2DzerF97cmeMvcc) (widok dnia, wybór typu, konfiguracja listy i licznika, reguła „kiedy”, układ dnia, podsumowanie tygodnia w dwóch wariantach). Link jest prywatny, więc działa z tego samego konta. Pliki ekranów leżą w `project/*.dc.html`, a Claude Code czyta je narzędziem Artifact (akcja `read`).
- **Stos:** Java i Spring Boot, PostgreSQL, Redis (tylko cache), Vue.js z Vite jako PWA, Docker Compose. Mobile first, bo zaliczanie odbywa się głównie z telefonu, a iPhone nie wymaga Maca przy PWA.
- **Zakres:** model danych, reguły zaliczania, API i synchronizacja. Poza zakresem: logowanie, płatności i szczegóły powiadomień push.
- **Jak czytać:** reguły z sekcji 2 i 5 są wiążące, schemat z sekcji 4 jest propozycją gotową do migracji, a sekcja 9 zbiera decyzje, których jeszcze nie podjęto. Wdrożenie najlepiej zacząć od sekcji 10.

## Zasady modelu

Model opiera się na ośmiu decyzjach podjętych podczas projektowania ekranów.

1. **Typ i rola to dwie osobne cechy bloku.** Typ mówi, jak blok się zalicza: `LIST`, `BOOLEAN`, `COUNTER`, `TIMER`, `NOTE`. Rola mówi, gdzie żyje w dniu: `STEP` (krok w ścieżce) albo `ALL_DAY` (w ciągu dnia, obok ścieżki). Woda to `COUNTER` z rolą `ALL_DAY`, bo nie da się jej zaliczyć w jednym momencie.
2. **Szablon i instancja.** Szablon (`block_template`) opisuje blok i to, kiedy wypada. Instancja (`block_instance`) to jego wystąpienie w konkretnym dniu i tylko ona trzyma stan zaliczenia.
3. **Historia jest zamrożona.** Instancja kopiuje nazwę, typ, rolę, konfigurację i pozycje listy w chwili generowania dnia. Edycja szablonu nigdy nie zmienia przeszłości ani podsumowań tygodni.
4. **Próg zaliczenia dla liczników.** Domyślnie 80% celu, zaokrąglone w górę: przy celu 8 próg to 7. Poniżej progu blok jest niezaliczony, od progu zaliczony, a przy pełnym celu zaliczony w pełni.
5. **Trzy stany dnia.** `FULL` (wszystko w pełni), `PARTIAL` (zaliczony z niedosytem: coś pominięte albo licznik między progiem a celem), `MISSED` (niezaliczony). Dni `FULL` i `PARTIAL` liczą się jako zaliczone. W makietach niedosyt oznacza bursztynowa kropka.
6. **Pominięcie i odłożenie nie kosztują punktów ani serii.** „Pomiń” ustawia instancję w stan `SKIPPED` i oznacza dzień jako `PARTIAL`. „Później” ustawia tylko znacznik `deferred_at`, a blok wraca na koniec ścieżki.
7. **Zaległe bloki mają politykę.** Każdy szablon ma `overdue_policy`: `CARRY_OVER` (niezrobiony blok przenosi się na kolejny dzień i nie obniża wyniku dnia, który opuszcza; blok można przenieść najwyżej 2 razy, potem działa jak DROP) albo `DROP` (niezrobiony blok zostaje w swoim dniu jako niezaliczony, przez co dzień jest niezaliczony (MISSED) i widać to w podsumowaniu tygodnia). Przełącznik „Jeśli nie zrobię w terminie” na ekranie „Kiedy” ustawia właśnie to pole.
8. **Podsumowanie tygodnia jest wyliczane, nie przechowywane.** Powstaje z instancji i pozycji list, więc „co dokładnie” zawsze da się odtworzyć.

## Model danych

Szablony opisują, co i kiedy, a dni z instancjami zapisują, co faktycznie się stało.

| Encja | Do czego służy | Kluczowe pola |
| --- | --- | --- |
| `app_user` | Ustawienia użytkownika, tożsamość z istniejącego modułu logowania | `timezone`, `day_start` |
| `block_template` | Definicja bloku: typ, rola, konfiguracja, kolejność | `type`, `role`, `config` (jsonb), `overdue_policy`, `sort_key`, `reminder_time`, `archived_at` |
| `template_item` | Pozycje bloku typu lista (np. trzy tabletki) | `template_id`, `label`, `sort_key` |
| `schedule_rule` | Kiedy blok wypada: reguła RRULE | `dtstart`, `rrule`, `until` |
| `day` | Jeden dzień użytkownika i jego wynik | `local_date`, `status`, `closed_at` |
| `block_instance` | Wystąpienie bloku w dniu: kopia definicji i stan | `state`, `deferred_at`, `value_int`, `note_text`, timer, `carried_from_id` |
| `instance_item` | Stan pozycji listy w danym dniu | `label`, `checked_at` |
| `counter_event` | Pojedyncze przyrosty licznika, dla synchronizacji offline | `delta`, `client_event_id`, `happened_at` |

Relacje: `app_user` 1:N `block_template` 1:1 `schedule_rule`, `block_template` 1:N `template_item`, `app_user` 1:N `day` 1:N `block_instance` 1:N (`instance_item`, `counter_event`). Powiązanie instancji z szablonem jest opcjonalne, żeby usunięcie szablonu nie kasowało historii.

## Schemat PostgreSQL

Propozycja pierwszej migracji (Flyway). Tabela `app_user` zakłada, że tożsamość pochodzi z istniejącego modułu logowania.

```sql
CREATE TYPE block_type     AS ENUM ('LIST','BOOLEAN','COUNTER','TIMER','NOTE');
CREATE TYPE block_role     AS ENUM ('STEP','ALL_DAY');
CREATE TYPE instance_state AS ENUM ('PENDING','DONE','SKIPPED','CARRIED','MISSED');
CREATE TYPE day_status     AS ENUM ('OPEN','FULL','PARTIAL','MISSED');
CREATE TYPE overdue_policy AS ENUM ('CARRY_OVER','DROP');

CREATE TABLE app_user (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  timezone   text NOT NULL DEFAULT 'Europe/Warsaw',
  day_start  time NOT NULL DEFAULT '04:00',   -- granica dnia w strefie użytkownika
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE block_template (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  name           text NOT NULL,
  type           block_type NOT NULL,
  role           block_role NOT NULL DEFAULT 'STEP',
  -- COUNTER: {"target":8,"unit":"szklanek","step":1,"threshold_pct":80}
  -- TIMER:   {"minutes":20}      pozostałe typy: {}
  config         jsonb NOT NULL DEFAULT '{}',
  overdue_policy overdue_policy NOT NULL DEFAULT 'DROP',
  sort_key       text NOT NULL,                -- indeks ułamkowy: kolejność w ścieżce
  reminder_time  time,
  archived_at    timestamptz,
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT counter_has_target
    CHECK (type <> 'COUNTER' OR (config ? 'target')),
  CONSTRAINT timer_has_minutes
    CHECK (type <> 'TIMER' OR (config ? 'minutes'))
);
CREATE INDEX block_template_user_idx
  ON block_template (user_id, role, sort_key) WHERE archived_at IS NULL;

CREATE TABLE template_item (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  template_id uuid NOT NULL REFERENCES block_template(id) ON DELETE CASCADE,
  label       text NOT NULL,
  sort_key    text NOT NULL,
  archived_at timestamptz
);

CREATE TABLE schedule_rule (
  template_id uuid PRIMARY KEY REFERENCES block_template(id) ON DELETE CASCADE,
  dtstart     date NOT NULL,
  rrule       text NOT NULL,   -- np. 'FREQ=DAILY' | 'FREQ=WEEKLY;BYDAY=MO,WE,FR'
                               --     'FREQ=MONTHLY;INTERVAL=3' | 'FREQ=DAILY;COUNT=1' (jednorazowo)
  until       date
);

CREATE TABLE day (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  local_date date NOT NULL,
  status     day_status NOT NULL DEFAULT 'OPEN',
  closed_at  timestamptz,
  UNIQUE (user_id, local_date)
);

CREATE TABLE block_instance (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  day_id            uuid NOT NULL REFERENCES day(id) ON DELETE CASCADE,
  template_id       uuid REFERENCES block_template(id) ON DELETE SET NULL,
  carried_from_id   uuid REFERENCES block_instance(id) ON DELETE SET NULL,
  carry_count       smallint NOT NULL DEFAULT 0 CHECK (carry_count BETWEEN 0 AND 2),  -- ile razy blok już przeniesiono
  -- kopia definicji z chwili generowania dnia (historia jest zamrożona)
  name              text NOT NULL,
  type              block_type NOT NULL,
  role              block_role NOT NULL,
  config            jsonb NOT NULL,
  sort_key          text NOT NULL,
  -- stan
  state             instance_state NOT NULL DEFAULT 'PENDING',
  deferred_at       timestamptz,                 -- „Później”: wraca na koniec ścieżki
  value_int         integer NOT NULL DEFAULT 0,  -- COUNTER: suma counter_event
  note_text         text,
  timer_started_at  timestamptz,
  timer_stopped_at  timestamptz,
  completed_at      timestamptz,
  updated_at        timestamptz NOT NULL DEFAULT now()
);
-- generowanie dnia jest idempotentne: jedna zwykła instancja szablonu na dzień
CREATE UNIQUE INDEX block_instance_once_per_day
  ON block_instance (day_id, template_id) WHERE carried_from_id IS NULL;
CREATE INDEX block_instance_day_idx ON block_instance (day_id, role, sort_key);

CREATE TABLE instance_item (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  instance_id uuid NOT NULL REFERENCES block_instance(id) ON DELETE CASCADE,
  label       text NOT NULL,
  sort_key    text NOT NULL,
  checked_at  timestamptz
);

CREATE TABLE counter_event (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  instance_id     uuid NOT NULL REFERENCES block_instance(id) ON DELETE CASCADE,
  delta           integer NOT NULL,
  client_event_id uuid NOT NULL,                 -- klucz idempotencji z PWA
  happened_at     timestamptz NOT NULL,
  UNIQUE (instance_id, client_event_id)
);

CREATE INDEX day_user_date_idx ON day (user_id, local_date DESC);
```

Uwagi do schematu:

- `value_int` jest sumą `counter_event.delta`, utrzymywaną w tej samej transakcji, co zapis zdarzenia. Zdarzenia (przyrosty zamiast wartości) pozwalają bezpiecznie scalić zapisy z dwóch urządzeń.
- Domyślną wartością `overdue_policy` jest `DROP`. Serwis ustawia `CARRY_OVER` przy tworzeniu szablonu z regułą rzadszą niż codzienna (np. co 3 miesiące), bo zaległa wymiana filtra ma sens, a wczorajsza woda nie.
- Zaległość jest osobną instancją z `carried_from_id`. Instancja w dniu, z którego blok odchodzi, dostaje stan `CARRIED` i nie wlicza się do statusu tego dnia. Limit pilnuje kolumna carry\_count: blok przeniesiony już 2 razy nie wraca na kolejny dzień, dostaje stan MISSED (niezaliczony) i zostaje w historii, więc dzień dostaje MISSED, jak przy DROP. Niezrobiony blok z polityką `DROP` zostaje w swoim dniu i po zamknięciu dnia dostaje stan `MISSED`, więc ten dzień dostaje `MISSED`.

## Reguły zaliczania

Stan dnia wynika wyłącznie ze stanów jego instancji, więc serwer przelicza go po każdym zapisie i nie ufa wynikowi z klienta.

**Stan bloku** (po jednej regule na typ):

| Typ | W pełni | Z niedosytem | Niezaliczony |
| --- | --- | --- | --- |
| `LIST` | wszystkie pozycje odhaczone | `SKIPPED` | część lub żadna pozycja odhaczona |
| `BOOLEAN` | odhaczony | `SKIPPED` | `PENDING` |
| `COUNTER` | `value_int >= target` | `SKIPPED` albo `ceil(target * threshold_pct / 100) <= value_int < target` | `value_int` poniżej progu |
| `TIMER` | zakończony (`timer_stopped_at` ustawione) | `SKIPPED` | `PENDING` |
| `NOTE` | tekst zapisany i zatwierdzony | `SKIPPED` | `PENDING` |

**Stan dnia** (`day.status`):

1. Jeśli którakolwiek instancja jest niezaliczona (instancje ze stanem CARRIED nie liczą się), dzień jest `OPEN`, a po zamknięciu dnia `MISSED`.
2. W przeciwnym razie, jeśli którakolwiek instancja jest „z niedosytem”, dzień jest `PARTIAL`.
3. W przeciwnym razie dzień jest `FULL`.

Do wyniku wchodzą instancje obu ról, `STEP` i `ALL_DAY`. Dzień `FULL` lub `PARTIAL` zalicza się od razu, nie czekając na północ, ale pozostaje edytowalny do zamknięcia: jeśli użytkownik dopije wodę z 7 do 8, status zmieni się z `PARTIAL` na `FULL`.

**Zamknięcie dnia** wykonuje zadanie planowane (`@Scheduled`, osobno dla strefy każdego użytkownika) po przekroczeniu `day_start` następnego dnia:

- dni w stanie `OPEN` dostają `MISSED` i `closed_at`,
- niezaliczone instancje z szablonów `CARRY_OVER` z carry\_count poniżej 2 dostają stan CARRIED w swoim dniu (ten krok idzie pierwszy, więc zaległość nie obniża wyniku tego dnia) i trafiają na nowy dzień z carry\_count o 1 większym, jako instancje z `carried_from_id`,
- pozostałe instancje `PENDING` (polityka `DROP` albo `CARRY_OVER` po 2 przeniesieniach) dostają stan `MISSED`. Wpis nigdy nie jest usuwany: zmienia się tylko jego stan, a w historii i w podsumowaniu tygodnia zostaje widoczny jako niezaliczony,
- brakujące wiersze `day` dla dni, w których aplikacja nie była otwarta, są tworzone jako `MISSED`.

**Podsumowanie tygodnia.** Wynik `SUCCESS` oznacza, że wszystkie 7 dni ma status `FULL`. W przeciwnym razie wynik to `GAPS`, a lista braków pochodzi z poniższego zapytania. Dzień bez wiersza w `day` liczy się jako niezaliczony.

```sql
SELECT d.local_date, d.status, bi.id AS instance_id, bi.name, bi.type, bi.state,
       bi.value_int, (bi.config->>'target')::int AS target,
       array_agg(ii.label ORDER BY ii.sort_key)
         FILTER (WHERE ii.checked_at IS NULL)     AS unchecked,
       array_agg(ii.label ORDER BY ii.sort_key)
         FILTER (WHERE ii.checked_at IS NOT NULL) AS checked
FROM day d
JOIN block_instance bi ON bi.day_id = d.id
LEFT JOIN instance_item ii ON ii.instance_id = bi.id
WHERE d.user_id = :userId
  AND d.local_date BETWEEN :weekStart AND :weekEnd
  AND d.status IN ('PARTIAL','MISSED')
  AND (bi.state NOT IN ('DONE','CARRIED')
       OR (bi.type = 'COUNTER' AND bi.value_int < (bi.config->>'target')::int))
GROUP BY d.local_date, d.status, bi.id
ORDER BY d.local_date, bi.sort_key;
```

Zapytanie zwraca dokładnie to, co pokazuje ekran „Braki”: dla środy tabletki z podziałem na odhaczone i nieodhaczone, spacer jako niezrobiony i wodę 5/8, a dla piątku wodę 7/8 i pominięty spacer.

## Generowanie dnia i reguły powtarzania

Dzień powstaje przy pierwszym odczycie, bez zadania cyklicznego, a jedyną logiką kalendarza jest dopasowanie reguły RRULE do daty.

1. **Generowanie (`ensureDay(user, date)`).** W jednej transakcji: wstaw `day` (`ON CONFLICT DO NOTHING`), przejdź po niezarchiwizowanych szablonach użytkownika, dla każdego, którego `schedule_rule` pasuje do `date`, wstaw `block_instance` z kopią nazwy, typu, roli, konfiguracji i `sort_key`, a następnie skopiuj aktywne `template_item` do `instance_item`. Częściowy indeks unikalny `block_instance_once_per_day` czyni całość idempotentną, więc dwa równoległe żądania nie zdublują bloków.
2. **Dopasowanie reguły.** Backend używa jednej implementacji RRULE (biblioteka ical4j, np. `Recur#getDates` dla jednego dnia; dokładne API zależy od wersji, więc trzeba je sprawdzić przy wdrożeniu). Frontend nie liczy terminów sam: lista „Najbliższe terminy” na ekranie „Kiedy” pochodzi z endpointu podglądu, żeby obie strony nie mogły się rozjechać.
3. **Mapowanie opcji z ekranu „Kiedy” na RRULE:**

| Opcja w UI | `rrule` | `dtstart` |
| --- | --- | --- |
| Codziennie | `FREQ=DAILY` | data utworzenia |
| Wybrane dni tygodnia | `FREQ=WEEKLY;BYDAY=MO,WE,FR` | data utworzenia |
| Co jakiś czas (np. co 3 miesiące od 10 listopada) | `FREQ=MONTHLY;INTERVAL=3` | wybrana data |
| Raz w miesiącu, w wybrany dzień | `FREQ=MONTHLY;BYMONTHDAY=15` | data utworzenia |
| Jednorazowo | `FREQ=DAILY;COUNT=1` | wybrana data |

Przykład z makiet: `FREQ=MONTHLY;INTERVAL=3` z `dtstart` 2026-11-10 daje terminy 10 listopada 2026, 10 lutego 2027 i 10 maja 2027.

4. **Zmiana szablonu.** Edycja obowiązuje od kolejnego dnia, a instancje już wygenerowane zostają bez zmian. Dzisiejsza instancja jest przebudowywana tylko wtedy, gdy użytkownik wybierze „zastosuj także dziś” i nie ma w niej jeszcze żadnego postępu.
5. **Usuwanie.** Szablony są archiwizowane (`archived_at`), nie kasowane. Twardy `DELETE` jest dopuszczalny tylko przy usuwaniu konta, a `ON DELETE SET NULL` na `template_id` chroni historię.
6. **Przypadki brzegowe.** Reguła miesięczna ze dniem 31 pomija miesiące bez 31. dnia, więc dla „ostatniego dnia miesiąca” trzeba użyć `BYMONTHDAY=-1`. Zmiana strefy czasowej użytkownika nie przelicza istniejących dni, bo `local_date` jest zapisana jako data.

## API pod ekrany

Wszystkie zapisy są idempotentne, bo PWA będzie je powtarzać po powrocie sieci. Ścieżki zakładają prefiks `/api` i uwierzytelnionego użytkownika.

| Ekran (plik makiety) | Endpoint | Uwagi |
| --- | --- | --- |
| Widok dnia (`Main`) | `GET /days/{date}` | Wywołuje `ensureDay`. Zwraca `steps[]` (rola `STEP`, z blokami odłożonymi na końcu), `allDay[]`, pasek tygodnia, postęp i `status` dnia. |
| Widok dnia, lista | `PUT /instances/{id}/items/{itemId}` z `{checked}` | Ustawia lub zdejmuje `checked_at`. |
| Widok dnia, tak/nie | `POST /instances/{id}/complete` | Ustawia `DONE` i `completed_at`. |
| Widok dnia, licznik | `POST /instances/{id}/counter-events` z `{delta, clientEventId}` | Zdarzenie plus aktualizacja `value_int` w jednej transakcji. |
| Widok dnia, timer | `POST /instances/{id}/timer/start`, `POST /instances/{id}/timer/stop` | `stop` ustawia `DONE`. |
| Widok dnia, notatka | `PUT /instances/{id}/note` z `{text}`, potem `complete` |  |
| Widok dnia, „Pomiń” i „Później” | `POST /instances/{id}/skip`, `POST /instances/{id}/defer` | `skip` ustawia `SKIPPED`, `defer` ustawia `deferred_at`. |
| Wybór typu, konfiguracja listy i licznika (`Typ`, `Lista`, `Licznik`) | `POST /templates`, `GET /templates/{id}`, `PUT /templates/{id}`, `DELETE /templates/{id}` | Ciało: `type`, `role`, `name`, `config`, `items[]`, `schedule{rrule,dtstart}`, `overdue_policy`, `reminderTime`, `afterTemplateId` (miejsce w ścieżce). `DELETE` archiwizuje. |
| Kiedy (`Kiedy`) | `POST /schedule/preview` z `{rrule, dtstart, count}` | Zwraca najbliższe terminy, w tym „dziś”. |
| Układ dnia (`Uklad`) | `GET /layout?date=`, `PUT /templates/order` z `{orderedIds[]}` | `layout` zwraca szablony z flagą `dueOnDate`, żeby wyszarzyć bloki, które dziś nie wypadają. |
| Podsumowanie tygodnia (`Sukces`, `Braki`) | `GET /weeks/{weekStart}/summary` | Zwraca `outcome` (`SUCCESS` albo `GAPS`), `days[]` ze statusami i `gaps[]` z zapytania z sekcji 5. |

Przykładowa odpowiedź `GET /days/{date}` (skrócona):

```json
{
  "date": "2026-11-10",
  "status": "OPEN",
  "progress": { "done": 1, "total": 5 },
  "steps": [
    { "id": "…", "name": "Tabletki rano", "type": "LIST", "state": "PENDING",
      "items": [ { "id": "…", "label": "Magnez", "checked": false } ] },
    { "id": "…", "name": "Wymiana filtra", "type": "BOOLEAN", "state": "PENDING",
      "fromCalendar": true }
  ],
  "allDay": [
    { "id": "…", "name": "Woda", "type": "COUNTER", "value": 3, "target": 8,
      "threshold": 7, "unit": "szklanek" }
  ]
}
```

Serwer zwraca `threshold` jako wartość już przeliczoną, żeby klient nie powielał reguły zaokrąglania.

## Czas, synchronizacja i offline

Największym ryzykiem technicznym są granice dnia i zapisy z telefonu bez zasięgu, więc obie sprawy trzeba rozstrzygnąć w modelu, nie w UI.

- **Dzień lokalny.** `local_date` jest liczona w strefie użytkownika (`ZoneId`, nie stały offset), z przesunięciem o `day_start` (domyślnie 04:00), żeby wieczorne zaliczenia po północy należały do poprzedniego dnia. Wszystkie znaczniki czasu to `timestamptz`.
- **Zapisy offline.** PWA trzyma kolejkę zmian w IndexedDB i odtwarza ją po powrocie sieci. Każda zmiana niesie klientowy UUID (`clientEventId`), a serwer odrzuca duplikaty bez błędu (`UNIQUE (instance_id, client_event_id)`).
- **Konflikty.** Licznik scala się przez sumowanie przyrostów, więc dwa urządzenia nie nadpiszą się nawzajem. Pozycje listy i notatka działają na zasadzie ostatniego zapisu (`updated_at`). Stan dnia zawsze liczy serwer.
- **Odczyt dnia offline.** Service worker przechowuje ostatnią odpowiedź `GET /days/{date}`, a lokalne zmiany są nakładane na nią do czasu synchronizacji.
- **Redis.** Opcjonalny cache odpowiedzi dnia pod kluczem `user:date`, unieważniany przy każdym zapisie. Nie jest źródłem prawdy, więc aplikacja ma działać bez niego.
- **Przypomnienia.** Pole `reminder_time` zasila Web Push. Na iOS push działa od wersji 16.4, ale tylko dla PWA dodanej do ekranu początkowego, więc ta wersja powinna to jasno komunikować przy pierwszym uruchomieniu.
- **Prywatność.** Nazwy bloków i pozycji listy mogą zawierać dane zdrowotne (np. leki). Jeśli aplikacja trafi do innych użytkowników, wchodzi RODO: szyfrowanie w spoczynku, eksport i trwałe usunięcie konta wraz z historią.

## Otwarte decyzje

Sześć kwestii nie ma jeszcze rozstrzygnięcia i każda wpływa na kod, więc warto je zamknąć przed implementacją danego fragmentu.

- [ ] **Plansza.** Czy numer pola to dzień kalendarza (jak w makietach: „pole 10 z 30”), czy liczba zaliczonych dni? Czy dzień `MISSED` przesuwa pionek? Od tego zależy, czy plansza jest stanem wyliczanym, czy zapisanym.
- [ ] **Punkty.** Czy dzień `PARTIAL` daje mniej punktów niż `FULL`? Obecnie niedosyt jest tylko znacznikiem bez skutku w punktacji.
- [ ] **Seria dni.** `PARTIAL` i `SKIPPED` jej nie przerywają (zgodnie z „bez kary”). Czy przerywa ją dopiero `MISSED`?
- [ ] **Granica dnia.** Czy domyślne 04:00 jest właściwe i czy użytkownik ma ją ustawiać sam?
- [ ] **Wiele reguł na blok.** Obecnie jeden szablon ma jedną regułę (`schedule_rule` 1:1). Tabletki o różnych porach lub w różne dni mogą wymagać kilku bloków albo kilku reguł.
- [ ] **Timer.** Czy częściowo odmierzony czas (np. 12 z 20 minut) ma liczyć się jak licznik z progiem, czy timer zalicza się tylko po zakończeniu?

## Plan wdrożenia dla Claude Code

Najpierw backend od danych do podsumowania tygodnia, potem front według makiet, a PWA na końcu, bo każdy etap da się przetestować osobno.

1. **Migracje i encje.** Skrypt z sekcji 4 jako migracja Flyway, encje JPA i repozytoria Spring Data, testy na PostgreSQL w Testcontainers. Nazwa projektu: stepday (pakiet pl.stepday, moduły stepday-api i stepday-web).
2. **Szablony i reguły.** CRUD `/templates`, serwis harmonogramu z ical4j i endpoint `/schedule/preview`. Test: reguła z sekcji 6 daje dokładnie trzy terminy z przykładu.
3. **Generowanie dnia.** `ensureDay` i `GET /days/{date}`. Testy: idempotencja przy równoległych żądaniach, kopia definicji (zmiana szablonu nie zmienia istniejącej instancji), przełączenie czasu letniego.
4. **Zaliczanie.** Endpointy per typ i przeliczanie `day.status` zgodnie z tabelą z sekcji 5. Testy tabelaryczne: licznik 6, 7 i 8 z 8, blok pominięty, lista częściowo odhaczona.
5. **Zamknięcie dnia.** Zadanie planowane, polityka zaległych, uzupełnianie brakujących dni.
6. **Podsumowanie tygodnia.** Zapytanie z sekcji 5 i endpoint `/weeks/{weekStart}/summary`. Test na danych z makiety „Braki”: środa `MISSED`, piątek `PARTIAL`.
7. **Front (Vue, Vite).** Komponenty według tabeli poniżej.
8. **PWA.** Manifest (name i short\_name: Stepday), service worker, kolejka offline z sekcji 8, na końcu Web Push.

**Mapowanie makiet na komponenty Vue:**

| Plik makiety | Komponenty |
| --- | --- |
| `Main` (widok dnia, klikalny) | `DayView`, `PathStep` z wariantami `ListStep`, `BooleanStep`, `TimerStep`, `NoteStep`, `AllDayStrip`, `BoardWeekStrip` |
| `Typ` | `BlockTypePicker` |
| `Lista`, `Licznik` | `BlockEditor` z sekcjami `ListItemsEditor` i `CounterConfig` |
| `Kiedy` | `ScheduleEditor` |
| `Uklad` | `DayLayoutView` z przeciąganiem |
| `Sukces`, `Braki` | `WeekSummaryView` ze stanami `SuccessState` i `GapsState` |

**Tokeny wyglądu** (z makiet): tło `#F2F5F7`, tekst `#14202B`, akcent `#2D5BFF`, sukces `#1F7A4D` na `#E3F4EA`, niedosyt `#8A4B08` na `#FDF0D5` z kropką `#B7791F`, brak `#B42318` na `#FBE9E7`. Nagłówki Space Grotesk, treść Inter, szerokość projektu 390 px, minimalny cel dotyku 44 px. Stan nie może być przekazywany samym kolorem: każdy ma też ikonę i tekst.
