// Główny scenariusz przez UI na prawdziwym stosie: rytuał → zaliczenie dnia → podsumowanie tygodnia.
// Zamknięcie dnia wymaga przesunięcia zegara, więc ten krok pokrywa MainScenarioTest (backend).
import { test, expect } from '@playwright/test'
import { AUTH, resetDatabase } from './seed'

test.beforeEach(async ({ page }) => {
  resetDatabase()
  await page.addInitScript((auth) => {
    localStorage.setItem('stepday.installHintDismissed', '1')
    if (!sessionStorage.getItem('skipAuth')) localStorage.setItem('stepday.auth', auth.replace('Basic ', ''))
  }, AUTH)
})

test('login screen guards the app', async ({ browser }) => {
  const page = await browser.newPage()
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Zaloguj się' })).toBeVisible()
  await page.getByLabel('Login').fill('stepday')
  await page.getByLabel('Hasło').fill('zle')
  await page.getByRole('button', { name: 'Zaloguj' }).click()
  await expect(page.getByRole('alert')).toHaveText('Nieprawidłowy login lub hasło')
  await page.getByLabel('Hasło').fill('stepday')
  await page.getByRole('button', { name: 'Zaloguj' }).click()
  await expect(page.getByText('kroków dziś')).toBeVisible()
  await page.close()
})

test('create a ritual, complete the day, see the week', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('link', { name: 'Zaprojektuj pierwszy rytuał' }).click()

  // Lista z dwiema pozycjami
  await page.getByRole('button', { name: /^Lista/ }).click()
  await page.getByLabel('Nazwa').fill('Tabletki rano')
  await page.getByRole('textbox', { name: 'Pozycja 1', exact: true }).fill('Witamina D3')
  await page.getByRole('button', { name: 'Dodaj pozycję' }).click()
  await page.getByRole('textbox', { name: 'Pozycja 2', exact: true }).fill('Magnez')
  await page.getByRole('button', { name: 'Zapisz blok' }).click()
  await expect(page.getByRole('heading', { name: 'Tabletki rano' })).toBeVisible()

  // Licznik „w ciągu dnia”: cel 5, próg 80% → 4
  await page.getByRole('link', { name: 'Układ dnia' }).click()
  await page.getByRole('link', { name: 'Dodaj blok' }).click()
  await page.getByRole('button', { name: /^Licznik/ }).click()
  await page.getByLabel('Nazwa').fill('Woda')
  for (let i = 0; i < 3; i++) await page.getByRole('button', { name: 'Zmniejsz cel' }).click()
  await page.getByLabel('Jednostka').fill('szklanek')
  await expect(page.getByText('Przy celu 5 to 4 szklanek.')).toBeVisible()
  await page.getByRole('button', { name: 'Zapisz blok' }).click()

  // Zaliczanie
  await expect(page.getByText('0/2', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Witamina D3' }).click()
  await expect(page.getByText('1/2', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Magnez' }).click()
  await expect(page.getByText('Zrobione')).toBeVisible()
  await expect(page.getByText('Koniec dnia')).toBeVisible() // woda 0/5

  const plus = page.getByRole('button', { name: 'Dodaj: Woda' })
  for (let i = 0; i < 4; i++) {
    await plus.click()
    await expect(page.getByText(`${i + 1}/5`, { exact: true })).toBeVisible()
  }
  await expect(page.getByText('Dzień zaliczony', { exact: true })).toBeVisible()
  await expect(page.getByText('Z niedosytem: woda 4/5. Ruszasz na pole 2')).toBeVisible()

  await plus.click()
  await expect(page.getByText('Cel osiągnięty')).toBeVisible()
  await expect(page.getByText('Ruszasz na pole 2')).toBeVisible()
  await expect(page.getByText(/Z niedosytem/)).toHaveCount(0)
  await expect(page.getByText('kroków dziś')).toBeVisible()
  await expect(page.getByText('1/1', { exact: true })).toBeVisible()

  // Podsumowanie bieżącego tygodnia (trwa): dziś zaliczony w pełni
  const monday = await page.evaluate(() => {
    const d = new Date(); const day = d.getDay() || 7; d.setDate(d.getDate() - day + 1)
    return d.toISOString().slice(0, 10)
  })
  await page.goto(`/week/${monday}`)
  await expect(page.getByText('Tydzień jeszcze trwa')).toBeVisible()
  await expect(page.getByText('1 w pełni')).toBeVisible()
})

test('skip and later cost nothing and reorder the path', async ({ page }) => {
  await page.goto('/blocks/new')
  for (const name of ['Okno', 'Spacer']) {
    await page.goto('/blocks/new')
    await page.getByRole('button', { name: /^Tak \/ nie/ }).click()
    await page.getByLabel('Nazwa').fill(name)
    await page.getByRole('button', { name: 'Zapisz blok' }).click()
    await expect(page.getByText('kroków dziś')).toBeVisible()
  }
  await expect(page.getByRole('heading', { name: 'Okno' })).toBeVisible()
  await page.getByRole('button', { name: 'Później' }).click()
  await expect(page.getByRole('heading', { name: 'Spacer' })).toBeVisible()
  await expect(page.getByText('Na później')).toBeVisible()
  await page.getByRole('button', { name: 'Pomiń' }).click()
  await expect(page.getByText('Pominięte')).toBeVisible()
  await page.getByRole('button', { name: 'Zrobione' }).click()
  await expect(page.getByText('Dzień zaliczony', { exact: true })).toBeVisible()
  await expect(page.getByText('Z niedosytem: spacer pominięte.', { exact: false })).toBeVisible()
})
