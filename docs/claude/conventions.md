# Konwencje

## Repozytorium
- Moduły: `stepday-api` (Spring Boot, Maven), `stepday-web` (Vue 3 + Vite + TS, npm). Pakiet `pl.stepday`.
- Gałąź robocza, nigdy push na `main` bez zgody. Małe, opisowe commity (po angielsku, tryb rozkazujący).
- Każdy etap kończy się zielonymi testami, zanim ruszy następny.

## Backend
- Pakiety według domeny: `pl.stepday.template`, `.schedule`, `.day`, `.completion`, `.closing`, `.week`, `.user`, `.common`.
- Schemat tylko przez Flyway (`src/main/resources/db/migration`), `ddl-auto=validate`. `V1` to dosłownie schemat z dokumentu; zmiany jako kolejne migracje i wpis w `docs/decisions.md`.
- DTO na granicy API (rekordy), nigdy encje. Wstrzykiwanie przez konstruktor. Czas tylko przez wstrzykiwany `Clock`.
- Testy integracyjne: `@SpringBootTest` + `@Import(PostgresTestConfig.class)` (PostgreSQL w Testcontainers, nie H2). Reguły progowe: testy tabelaryczne `@ParameterizedTest`.

## Front
- Composition API, `<script setup lang="ts">`, Pinia, vue-router. Teksty UI po polsku.
- Tokeny z makiet jako zmienne CSS w `src/styles/tokens.css`:

| Token | Wartość | Użycie |
|---|---|---|
| `--bg` | `#F2F5F7` | tło |
| `--surface` | `#FFFFFF` | karty |
| `--surface-2` | `#F7F9FB` | pola, przyciski drugorzędne |
| `--muted-bg` | `#E7ECF0` | nieaktywne pola, segmenty |
| `--border` | `#D5DDE4` | obramowania |
| `--divider` | `#E1E7EC` | separatory, tor paska postępu |
| `--text` | `#14202B` | tekst |
| `--text-2` | `#3F4F5E` | tekst drugorzędny |
| `--text-3` | `#556575` | podpisy |
| `--icon-muted` | `#7A8B9B` | ikony, przerywane ramki |
| `--accent` | `#2D5BFF` | akcent, aktywny blok |
| `--accent-ink` | `#1F44C8` | tekst akcentu |
| `--accent-soft` | `#E6ECFF` | tło akcentu |
| `--ok` | `#1F7A4D` | sukces |
| `--ok-ink` | `#14602F` | tekst na tle sukcesu |
| `--ok-soft` | `#E3F4EA` | tło sukcesu |
| `--partial-ink` | `#8A4B08` | niedosyt |
| `--partial-soft` | `#FDF0D5` | tło niedosytu |
| `--partial-dot` | `#B7791F` | bursztynowa kropka |
| `--miss` | `#B42318` | brak |
| `--miss-soft` | `#FBE9E7` | tło braku |

- Fonty: Space Grotesk (500/600/700) w nagłówkach i liczbach, Inter (400/500/600) w treści.
- Szerokość projektu 390 px, marginesy 16 px, cele dotykowe ≥ 44 px, promienie 12/14/16/18/20 px jak w makietach.
- Ikony: inline SVG w stylu stroke z makiet (`src/components/icons`).
