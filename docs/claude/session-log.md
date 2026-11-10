# Dziennik sesji

Historia prac agentów nad Stepday. Najnowsza sesja na górze. Szczegóły decyzji: `docs/decisions.md` (tu tylko odnośniki).

---

## Sesja 1 — 2026-11-10: od pustego repo do MVP (gałąź `feat/mvp`, wypchnięta)

### Zlecenie
Zbudować Stepday (PWA, Java/Spring Boot, PostgreSQL, Redis tylko cache, Vue/Vite, Docker Compose) z dwóch źródeł prawdy, etapami z sekcji „Plan wdrożenia” dokumentu, z testami na każdym etapie. Pełna treść zlecenia jest w pierwszej wiadomości sesji; najważniejsze reguły przeniesione do `CLAUDE.md` i `docs/claude/rules.md`.

### Źródła i jak je czytano
- **Dokument** (Claude Docs, `https://claude.ai/artifact/SnrchJn1CzUNj2n9rksqkC`): czytany narzędziami Claude Docs (`read` projektu → `read` węzła prose z `payload: {kind: "view"}`). Wynik jest za duży na kontekst — trafia do pliku; bloki kodu siedzą w CDATA wewnątrz `<codeBlock>`. Właściciel wrzucił też własny eksport → kopia robocza `docs/stepday-data-layer.md` (oryginał i PDF są w `.gitignore`).
- **Makiety** (Artifact typu Design, `https://claude.ai/artifact/JQX4MLH2DzerF97cmeMvcc`): `Artifact read` z `paths: ["project/canvas.json", "project/*.dc.html"]`. Źródła w `docs/mockups-src/project/`. Właściciel dał też PDF (8 stron = 8 plansz w kolejności z `canvas.json`, 96 dpi = 390 px) — z niego są wzorce do porównań (`docs/screenshots/mockup/`).
- Pierwszeństwo: dane/zachowanie → dokument, wygląd → makiety. Sprzeczności → pytać właściciela.

### Przebieg (commity na `feat/mvp`)
| Commit | Etap | Wynik |
|---|---|---|
| `acfacab` | 0. Szkielet | Maven (root + `stepday-api`), Vite (`stepday-web`), compose, CLAUDE.md + `docs/claude/*`, decisions.md |
| `ec8b719` | 1. Migracje i encje | V1 = schemat z dokumentu dosłownie, V2 = `instance_item.updated_at` (A3) |
| `9c5b413` | 2. Szablony i reguły | CRUD `/templates`, `ScheduleService` (ical4j 4.4), `/schedule/preview`, sort keys, Basic auth |
| `45b41f0` | 3. Generowanie dnia | `ensureDay` (ON CONFLICT + częściowy indeks), `GET /days/{date}`, plansza/seria, cache Redis |
| `b2768d5` | 4. Zaliczanie | endpointy per typ, `BlockEvaluator`, LWW, idempotentne `counter_event` |
| `d6ad891` | 5. Zamknięcie dnia | `DayCloser` + job co minutę per strefa, CARRY_OVER ≤ 2, uzupełnianie dni |
| `10adcb7` | 6. Tydzień | zapytanie z dokumentu dosłownie, `/weeks/{weekStart}/summary` |
| `72bfbb6` | (dla frontu) | `GET /layout`, `GET/PATCH /me` |
| `fec9fb6` | 7. Front | wszystkie ekrany, Playwright vs makiety, `docs/mockup-diff.md` |
| `bd80d19` | 8a. PWA | manifest, SW (Workbox), kolejka offline w IndexedDB |
| `2b130a2` | 8b. Web Push | RFC 8291/8292 na JDK, V3 (3 tabele — A29), job przypomnień |
| `48a50c7` | po prośbie | Swagger UI (springdoc 3.2) |

Stan testów na koniec: backend 118, front unit 19, e2e 12 — wszystkie zielone.

### Decyzje właściciela w tej sesji
- Plan zaakceptowany („OK”) bez odpowiedzi na dwa pytania → przyjęto domyślne: **D7** (jedna godzina przypomnienia, wygrywa dokument), **D8** (gotowe szablony nieaktywne do czasu podania treści).
- Otwarte decyzje z dokumentu D1–D6: przyjęte rekomendacje z planu (plansza wyliczana, brak punktów, serię przerywa tylko MISSED, 04:00 konfigurowalne, 1 reguła/blok, timer zalicza stop).
- Poprosił o Swaggera → dodany. Poprosił o push i PR → sesja nie miała poświadczeń GitHuba; właściciel wypchnął sam. **PR — sprawdź, czy został otwarty** (treść przygotowana w sesji; link: `https://github.com/madmatts/stepday/compare/main...feat/mvp`).

### Otwarte sprawy (dla następnej sesji)
1. **A29 czeka na akceptację**: Web Push dodał 3 tabele spoza dokumentu (`push_subscription`, `reminder_log`, `app_setting`).
2. Push nietestowany na prawdziwym iPhonie (wymaga HTTPS + urządzenia). Brak konfiguracji HTTPS w compose (A9).
3. D8: treść szablonów „Poranek / Wieczór / Dzień roboczy” od właściciela.
4. Brak prawdziwego logowania (A2) — `CurrentUser` jest jedynym punktem do podmiany.
5. Zamknięcie dnia na żywym stosie nie było pokazane (wymaga czekania do 04:00); pokrywają je testy z przestawianym zegarem.

### Pułapki znalezione w tej sesji (oszczędzą czas)
- **Spring Boot 4 / Jackson 3**: brak pola prymitywnego (`boolean`) w rekordzie żądania = 400. Używaj `Boolean`. JSON mapper to `tools.jackson.databind.json.JsonMapper`.
- **Hibernate 7 + enumy PG**: `@Enumerated(STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)` + `columnDefinition = "<typ_pg>"` przechodzi `ddl-auto=validate`.
- **Testcontainers 2.x**: artefakty `testcontainers-postgresql`, klasa `org.testcontainers.postgresql.PostgreSQLContainer`. Dwa kontenery w kontekście → `@Qualifier` przy wstrzykiwaniu.
- **Zegar testowy vs `now()` w bazie**: `MutableClock` w testach jest na 10.11.2026 12:00 Warszawa; wartości domyślne `now()` z bazy mają czas rzeczywisty. Dlatego `ensureDay` ustawia `updated_at = 'epoch'` (A19) — to też poprawka realnego błędu z zegarem telefonu.
- **Redis bez timeoutu**: po padzie Redisa każde żądanie wisiało ~60 s. Teraz timeout 300 ms + bezpiecznik 30 s (A15).
- **W testach `IntegrationTest`** metody `get/post/...` dodają Basic auth i przesłaniają statyczny import `MockMvcRequestBuilders.get` — do żądań bez logowania pisz pełną nazwę.
- **CSS z makiet**: makiety nie mają globalnego `box-sizing: border-box`. Front też nie (tylko pola i `.screen`) — inaczej wiersze wychodzą niższe. Klasy w rodzicu o tej samej nazwie co korzeń dziecka sumują style (scoped CSS).
- **Vue**: `readonly()` na `shallowRef` daje typy głęboko readonly → `shallowReadonly`. `erasableSyntaxOnly` zabrania właściwości w parametrach konstruktora.
- **vite-plugin-pwa `autoUpdate`** przeładowywał stronę w losowej chwili → `registerType: 'prompt'` + `skipWaiting()` + `clientsClaim()` w SW.
- **Playwright testuje kontener `web`** — po zmianach frontu: `docker compose up -d --build web` i poczekaj na `/api/actuator/health` (api też się restartuje). `npm run e2e` czyści bazę compose (`TRUNCATE app_user CASCADE`).
- W selektorach Playwright używaj `exact: true` dla krótkich tekstów („4/5”, „Dzień zaliczony” występują w kilku miejscach).
- Sesja nie ma `gh` ani poświadczeń GitHuba; `!` w promptcie nie jest interaktywne. Klucz `~/.ssh/id_ed25519` nie był dodany do GitHuba.
