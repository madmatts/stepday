// Każdy ekran obok makiety (390 px). Wynik: docs/screenshots/{mockup,app,compare}/<ekran>.png
import { test, expect, type Page } from '@playwright/test'
import { execSync } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync } from 'node:fs'
import { AUTH, seedMainMockup } from './seed'
import { GAPS_WEEK, SUCCESS_WEEK } from './fixtures'

const OUT = '../docs/screenshots'
const PDF = '../docs/Stepday · widok dnia.pdf'
/** Kolejność stron PDF = kolejność plansz w canvas.json. */
const PAGES = ['main', 'typ', 'lista', 'licznik', 'kiedy', 'uklad', 'sukces', 'braki']

test.beforeAll(() => {
  for (const d of ['mockup', 'app', 'compare']) mkdirSync(`${OUT}/${d}`, { recursive: true })
  if (existsSync(PDF) && !existsSync(`${OUT}/mockup/braki.png`)) {
    execSync(`pdftoppm -r 96 -png "${PDF}" ${OUT}/mockup/p`)
    PAGES.forEach((name, i) => execSync(`mv ${OUT}/mockup/p-${i + 1}.png ${OUT}/mockup/${name}.png`))
  }
})

test.beforeEach(async ({ page }) => {
  await page.addInitScript((auth) => {
    localStorage.setItem('stepday.auth', auth.replace('Basic ', ''))
    localStorage.setItem('stepday.installHintDismissed', '1')
  }, AUTH)
})

async function shoot(page: Page, name: string) {
  await page.evaluate(() => document.fonts.ready)
  await page.waitForTimeout(200)
  await page.screenshot({ path: `${OUT}/app/${name}.png`, fullPage: true })
  const mock = `${OUT}/mockup/${name}.png`
  if (!existsSync(mock)) return
  // Zestawienie obok siebie: makieta | aplikacja.
  const b64 = (p: string) => readFileSync(p).toString('base64')
  const sheet = await page.context().newPage()
  await sheet.setViewportSize({ width: 820, height: 600 })
  await sheet.setContent(`<body style="margin:0;background:#888;display:flex;gap:40px;align-items:flex-start;padding:0">
    <figure style="margin:0"><figcaption style="font:12px sans-serif;color:#fff;padding:4px">makieta</figcaption>
      <img src="data:image/png;base64,${b64(mock)}" width="390"></figure>
    <figure style="margin:0"><figcaption style="font:12px sans-serif;color:#fff;padding:4px">aplikacja</figcaption>
      <img src="data:image/png;base64,${b64(`${OUT}/app/${name}.png`)}" width="390"></figure></body>`)
  await sheet.screenshot({ path: `${OUT}/compare/${name}.png`, fullPage: true })
  await sheet.close()
}

test.describe.serial('ekrany vs makiety', () => {
  test.beforeAll(async () => { await seedMainMockup() })

  test('main — widok dnia', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Tabletki rano' })).toBeVisible()
    await expect(page.getByText('TERAZ · LISTA')).toBeVisible()
    await expect(page.getByText('pole 2 z 30')).toBeVisible()
    await expect(page.getByText('3/8')).toBeVisible()
    await shoot(page, 'main')
  })

  test('typ — wybór typu bloku', async ({ page }) => {
    await page.goto('/blocks/new')
    await expect(page.getByRole('button', { name: /Licznik/ })).toBeVisible()
    await shoot(page, 'typ')
  })

  test('lista — konfiguracja listy', async ({ page }) => {
    await page.goto('/blocks/new')
    await page.getByRole('button', { name: /^Lista/ }).click()
    await page.getByLabel('Nazwa').fill('Tabletki rano')
    await page.getByRole('textbox', { name: 'Pozycja 1', exact: true }).fill('Witamina D3')
    for (const [i, label] of [[2, 'Magnez'], [3, 'Omega-3']] as const) {
      await page.getByRole('button', { name: 'Dodaj pozycję' }).click()
      await page.getByRole('textbox', { name: `Pozycja ${i}`, exact: true }).fill(label)
    }
    await page.getByLabel('Miejsce w ścieżce').selectOption({ label: 'Po: Otworzyć okno, rozciągnąć się' })
    await page.getByRole('switch', { name: 'Przypomnienie' }).click()
    await shoot(page, 'lista')
  })

  test('licznik — konfiguracja licznika', async ({ page }) => {
    await page.goto('/blocks/new')
    await page.getByRole('button', { name: /^Licznik/ }).click()
    await page.getByLabel('Nazwa').fill('Woda')
    await page.getByLabel('Jednostka').fill('szklanek')
    await page.getByRole('switch', { name: 'Przypomnienie' }).click()
    await expect(page.getByText('Przy celu 8 to 7 szklanek.')).toBeVisible()
    await shoot(page, 'licznik')
  })

  test('kiedy — reguła powtarzania', async ({ page }) => {
    await page.goto('/blocks/new')
    await page.getByRole('button', { name: /^Tak \/ nie/ }).click()
    await page.getByLabel('Nazwa').fill('Wymiana filtra')
    await page.getByRole('button', { name: /Kiedy/ }).click()
    await page.getByLabel('Co jakiś czas').check()
    await page.getByLabel('Powtarzaj co').fill('3')
    await page.getByLabel('Jednostka okresu').selectOption('MONTHLY')
    await page.getByLabel('Od dnia').fill('2026-11-10')
    await expect(page.getByText('pn, 10 maja 2027')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Zostaje do zrobienia' })).toHaveAttribute('aria-pressed', 'true')
    await shoot(page, 'kiedy')
  })

  test('uklad — układ dnia', async ({ page }) => {
    await page.goto('/layout')
    await expect(page.getByText('Nie dziś')).toBeVisible()
    await expect(page.getByText('Wypada dziś')).toBeVisible()
    await shoot(page, 'uklad')
  })

  test('sukces — podsumowanie tygodnia', async ({ page }) => {
    await page.route('**/api/weeks/2026-11-02/summary', (r) => r.fulfill({ json: SUCCESS_WEEK }))
    await page.goto('/week/2026-11-02')
    await expect(page.getByRole('heading', { name: 'Sukces' })).toBeVisible()
    await shoot(page, 'sukces')
  })

  test('braki — podsumowanie tygodnia', async ({ page }) => {
    await page.route('**/api/weeks/2026-11-02/summary', (r) => r.fulfill({ json: GAPS_WEEK }))
    await page.goto('/week/2026-11-02')
    await expect(page.getByText('6 z 7')).toBeVisible()
    await expect(page.getByText('Nie odhaczono: Magnez, Omega-3. Zrobione: Witamina D3.')).toBeVisible()
    await expect(page.getByText('7/8 szklanek, brakowało 1.')).toBeVisible()
    await shoot(page, 'braki')
  })
})
