import 'fake-indexeddb/auto'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { DayView } from '@/api/types'
import { dayMutations, setSender } from '@/api/mutations'
import { _reset, cachedDay, flush, offlineSender, pending, rememberDay } from './outbox'

const day: DayView = {
  date: '2026-11-10', today: true, status: 'OPEN', closed: false, progress: { done: 0, total: 1 },
  steps: [{ id: 'pills', name: 'Tabletki', type: 'LIST', role: 'STEP', state: 'PENDING', completion: 'NONE',
    deferred: false, carryCount: 0, fromCalendar: false, items: [{ id: 'd3', label: 'D3', checked: false }],
    value: null, target: null, threshold: null, unit: null, step: null, minutes: null, timerStartedAt: null,
    timerStoppedAt: null, note: null }],
  allDay: [{ id: 'water', name: 'Woda', type: 'COUNTER', role: 'ALL_DAY', state: 'PENDING', completion: 'NONE',
    deferred: false, carryCount: 0, fromCalendar: false, items: null, value: 6, target: 8, threshold: 7, unit: 'szklanek',
    step: 1, minutes: null, timerStartedAt: null, timerStoppedAt: null, note: null }],
  week: [], board: { position: 1, total: 30, year: 2026, month: 11 }, streak: 0,
}

const fetchMock = vi.fn()
beforeEach(async () => {
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockReset()
  setSender(offlineSender)
  await _reset()
  await rememberDay(day)
})
afterEach(() => vi.unstubAllGlobals())

const ok = (body: unknown) => Promise.resolve(new Response(JSON.stringify(body), { status: 200 }))
const offline = () => Promise.reject(new TypeError('Failed to fetch'))

describe('kolejka offline', () => {
  it('bez sieci zapisuje zmianę w kolejce i nakłada ją lokalnie', async () => {
    fetchMock.mockImplementation(offline)
    const view = await dayMutations.counter('2026-11-10', 'water', 1)
    expect(view?.allDay[0].value).toBe(7)
    expect(view?.allDay[0].completion).toBe('PARTIAL')
    expect(await pending()).toHaveLength(1)
    expect((await cachedDay('2026-11-10'))?.allDay[0].value).toBe(7)
  })

  it('po powrocie sieci wysyła kolejkę w kolejności, z tym samym clientEventId', async () => {
    fetchMock.mockImplementation(offline)
    await dayMutations.counter('2026-11-10', 'water', 1)
    await dayMutations.setItem('2026-11-10', 'pills', 'd3', true)
    const [first] = await pending()

    const sent: string[] = []
    fetchMock.mockImplementation((url: string, init: RequestInit) => {
      sent.push(url)
      if (url.endsWith('/counter-events')) expect(JSON.parse(String(init.body)).clientEventId).toBe(first.id)
      return ok(day)
    })
    await flush()
    expect(sent).toEqual(['/api/instances/water/counter-events', '/api/instances/pills/items/d3'])
    expect(await pending()).toHaveLength(0)
  })

  it('zmiana odrzucona przez serwer (409, dzień zamknięty) znika z kolejki, nie blokuje reszty', async () => {
    fetchMock.mockImplementation(offline)
    await dayMutations.complete('2026-11-10', 'pills')
    await dayMutations.counter('2026-11-10', 'water', 1)
    fetchMock
      .mockImplementationOnce(() => Promise.resolve(new Response(JSON.stringify({ detail: 'Dzień jest już zamknięty' }), { status: 409 })))
      .mockImplementationOnce(() => ok(day))
    await flush()
    expect(await pending()).toHaveLength(0)
  })

  it('online: zapis idzie od razu do serwera', async () => {
    fetchMock.mockImplementation(() => ok({ ...day, status: 'PARTIAL' }))
    const view = await dayMutations.skip('2026-11-10', 'pills')
    expect(view?.status).toBe('PARTIAL')
    expect(await pending()).toHaveLength(0)
  })
})
