# Stepday

Gamifikowany przewodnik dnia dla osoby z ADHD — PWA (mobile first). Źródła prawdy, reguły i decyzje: `CLAUDE.md`, `docs/`.

## Start

```bash
docker compose up --build
```

- aplikacja: http://localhost:8000 (login `stepday` / `stepday`, zmiana: `STEPDAY_USER`, `STEPDAY_PASSWORD`)
- API: http://localhost:8080/api

## Testy

```bash
mvn -B test                          # backend, PostgreSQL i Redis w Testcontainers
cd stepday-web && npm test           # front, jednostkowe
cd stepday-web && npm run e2e        # Playwright 390 px na działającym stosie (czyści bazę compose!)
```

## iPhone

Service worker i powiadomienia wymagają HTTPS (poza `localhost`). Wystaw port 8000 przez reverse proxy z certyfikatem albo tunel HTTPS, otwórz w Safari, potem Udostępnij → „Do ekranu początkowego”. Przypomnienia działają tylko w tak zainstalowanej aplikacji (iOS 16.4+).
