# Komendy

Wymagania: Java 25, Maven 3.9, Node 20, Docker (Testcontainers potrzebuje działającego demona).

| Cel | Komenda (z katalogu repo) |
|---|---|
| Testy backendu (Testcontainers) | `mvn -B test` |
| Jeden test | `mvn -B -pl stepday-api test -Dtest=CounterThresholdTest` |
| Build jar | `mvn -B -pl stepday-api package` |
| Front: dev (proxy `/api` → `:8080`) | `cd stepday-web && npm run dev` |
| Front: testy jednostkowe | `cd stepday-web && npm test` |
| Front: build | `cd stepday-web && npm run build` |
| E2E + porównanie z makietami (390 px), wymaga `docker compose up` | `cd stepday-web && npm run e2e` (czyści lokalną bazę compose!) |
| Całość | `docker compose up --build` → front http://localhost:8000, API http://localhost:8080/api |

Login w trybie jednego użytkownika: `STEPDAY_USER` / `STEPDAY_PASSWORD` (domyślnie `stepday` / `stepday`).
