import { defineConfig } from '@playwright/test'

// Porównanie ekranów z makietami na 390 px. Wymaga działającego `docker compose up` (front :8000, API :8080).
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8000',
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 1,
    locale: 'pl-PL',
    timezoneId: 'Europe/Warsaw',
  },
})
