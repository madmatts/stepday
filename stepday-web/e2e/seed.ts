// Dane z makiet przez prawdziwe API (te same bloki co w testach backendu: support/Mockup.java).
import { execSync } from 'node:child_process'

const API = process.env.E2E_API_URL ?? 'http://localhost:8080/api'
export const AUTH = 'Basic ' + Buffer.from('stepday:stepday').toString('base64')

export async function call<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const res = await fetch(API + path, {
    method,
    headers: { Authorization: AUTH, 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${await res.text()}`)
  return (res.status === 204 ? undefined : await res.json()) as T
}

/** Czysta baza lokalnego stosu docker compose (tylko do e2e!). */
export function resetDatabase() {
  execSync('docker compose exec -T db psql -U stepday -d stepday -c "TRUNCATE app_user CASCADE"', { cwd: '..' })
  execSync('docker compose exec -T redis redis-cli FLUSHALL', { cwd: '..' })
}

const daily = (dtstart = '2026-11-01') => ({ rrule: 'FREQ=DAILY', dtstart })

export const MOCKUP_TEMPLATES = [
  { type: 'BOOLEAN', name: 'Otworzyć okno, rozciągnąć się', schedule: daily() },
  { type: 'LIST', name: 'Tabletki rano', items: [{ label: 'Witamina D3' }, { label: 'Magnez' }, { label: 'Omega-3' }],
    schedule: daily(), reminderTime: '07:30' },
  { type: 'BOOLEAN', name: 'Wymiana filtra', schedule: { rrule: 'FREQ=MONTHLY;INTERVAL=3', dtstart: '2026-11-10' } },
  { type: 'TIMER', name: 'Spacer', config: { minutes: 20 }, schedule: daily() },
  { type: 'LIST', name: 'Zakupy na tydzień', items: [{ label: 'Chleb' }], schedule: { rrule: 'FREQ=WEEKLY;BYDAY=SA', dtstart: '2026-11-01' } },
  { type: 'NOTE', name: 'Wieczorna notatka', schedule: daily() },
  { type: 'COUNTER', name: 'Woda', role: 'ALL_DAY', config: { target: 8, unit: 'szklanek', step: 1, threshold_pct: 80 }, schedule: daily(),
    reminderTime: '08:00' },
]

interface Created { id: string; name: string }
interface Block { id: string; name: string; items: { id: string; label: string }[] | null }
interface Day { steps: Block[]; allDay: Block[] }

/** Stan z makiety Main: okno zrobione, Witamina D3 odhaczona, woda 3/8; poniedziałek z niedosytem. */
export async function seedMainMockup(): Promise<Record<string, string>> {
  resetDatabase()
  const ids: Record<string, string> = {}
  for (const t of MOCKUP_TEMPLATES) {
    const c = await call<Created>('/templates', 'POST', t)
    ids[c.name] = c.id
  }
  const find = (d: Day, name: string) => [...d.steps, ...d.allDay].find((b) => b.name === name)!

  // Poniedziałek 9.11: wszystko zrobione, woda 7/8 → PARTIAL (bursztynowa kropka na planszy).
  const mon = await call<Day>('/days/2026-11-09')
  for (const b of mon.steps) {
    if (b.items) for (const i of b.items) await call(`/instances/${b.id}/items/${i.id}`, 'PUT', { checked: true })
    else if (b.name === 'Spacer') {
      await call(`/instances/${b.id}/timer/start`, 'POST')
      await call(`/instances/${b.id}/timer/stop`, 'POST')
    } else if (b.name === 'Wieczorna notatka') {
      await call(`/instances/${b.id}/note`, 'PUT', { text: 'Ok.' })
      await call(`/instances/${b.id}/complete`, 'POST')
    } else await call(`/instances/${b.id}/complete`, 'POST')
  }
  for (let i = 0; i < 7; i++) {
    await call(`/instances/${find(mon, 'Woda').id}/counter-events`, 'POST', { delta: 1, clientEventId: crypto.randomUUID() })
  }

  const today = await call<Day>('/days/2026-11-10')
  await call(`/instances/${find(today, 'Otworzyć okno, rozciągnąć się').id}/complete`, 'POST')
  const pills = find(today, 'Tabletki rano')
  await call(`/instances/${pills.id}/items/${pills.items![0].id}`, 'PUT', { checked: true })
  for (let i = 0; i < 3; i++) {
    await call(`/instances/${find(today, 'Woda').id}/counter-events`, 'POST', { delta: 1, clientEventId: crypto.randomUUID() })
  }
  return ids
}
