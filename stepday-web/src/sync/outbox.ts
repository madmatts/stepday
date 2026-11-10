// Kolejka zapisów offline w IndexedDB + kopia ostatniego widoku każdego dnia.
// Każda zmiana ma UUID nadany po stronie klienta; serwer odrzuca duplikaty bez błędu.
import { openDB, type DBSchema, type IDBPDatabase } from 'idb'
import { api, ApiError } from '@/api/client'
import type { Mutation, Sender } from '@/api/mutations'
import type { DayView } from '@/api/types'
import { applyLocal } from './localApply'

interface StepdayDB extends DBSchema {
  outbox: { key: string; value: Mutation & { seq: number } ; indexes: { seq: number } }
  days: { key: string; value: DayView }
}

let dbPromise: Promise<IDBPDatabase<StepdayDB>> | null = null
function db() {
  dbPromise ??= openDB<StepdayDB>('stepday', 1, {
    upgrade(d) {
      d.createObjectStore('outbox', { keyPath: 'id' }).createIndex('seq', 'seq')
      d.createObjectStore('days')
    },
  })
  return dbPromise
}

/** Błąd sieci (brak zasięgu) — w odróżnieniu od odpowiedzi serwera z kodem błędu. */
export function isOffline(e: unknown): boolean {
  return e instanceof TypeError || (typeof navigator !== 'undefined' && navigator.onLine === false && !(e instanceof ApiError))
}

export async function rememberDay(day: DayView): Promise<void> {
  await (await db()).put('days', day, day.date)
}

export async function pending(): Promise<Mutation[]> {
  return (await (await db()).getAllFromIndex('outbox', 'seq')).map(({ seq: _seq, ...m }) => m)
}

/** Ostatni znany widok dnia z nałożonymi zmianami, które czekają na wysłanie. */
export async function cachedDay(date: string | null): Promise<DayView | null> {
  const d = await db()
  let day: DayView | undefined
  if (date) {
    day = await d.get('days', date)
  } else {
    const all = await d.getAll('days')
    day = all.filter((x) => x.today).sort((a, b) => b.date.localeCompare(a.date))[0]
  }
  if (!day) return null
  for (const m of await pending()) day = applyLocal(day, m)
  return day
}

type Listener = (state: { pending: number; rejected: string[] }) => void
const listeners = new Set<Listener>()
const rejected: string[] = []
export function onSyncState(l: Listener): () => void {
  listeners.add(l)
  void notify()
  return () => listeners.delete(l)
}
async function notify() {
  const n = (await pending()).length
  listeners.forEach((l) => l({ pending: n, rejected: [...rejected] }))
}

let flushing: Promise<DayView | null> | null = null

/**
 * Wysyła kolejkę po kolei. Błąd sieci przerywa (spróbujemy później); odpowiedź 4xx (np. 409 — dzień już
 * zamknięty) usuwa zmianę z kolejki i zgłasza ją użytkownikowi (A20). Zwraca ostatni widok z serwera.
 */
export function flush(): Promise<DayView | null> {
  flushing ??= (async () => {
    const d = await db()
    let last: DayView | null = null
    try {
      for (const m of await pending()) {
        try {
          last = await api<DayView>(m.path, { method: m.method, body: m.body ?? {} })
          await rememberDay(last)
        } catch (e) {
          if (isOffline(e)) break
          if (e instanceof ApiError && e.status >= 400 && e.status < 500 && e.status !== 401) {
            rejected.push(e.message)
          } else {
            break
          }
        }
        await d.delete('outbox', m.id)
      }
    } finally {
      flushing = null
      await notify()
    }
    return last
  })()
  return flushing
}

let seq = Date.now()

/**
 * Nadawca zapisów dla `dayMutations`: online — od razu do serwera (najpierw dosyła starszą kolejkę, żeby
 * zachować kolejność); offline — zapis w kolejce i lokalny podgląd zmiany.
 */
export const offlineSender: Sender = async (m) => {
  const d = await db()
  if ((await d.count('outbox')) === 0) {
    try {
      const view = await api<DayView>(m.path, { method: m.method, body: m.body ?? {} })
      await rememberDay(view)
      return view
    } catch (e) {
      if (!isOffline(e)) throw e
    }
  }
  await d.put('outbox', { ...m, seq: seq++ })
  await notify()
  void flush()
  return cachedDay(m.date)
}

export function startSync(): void {
  window.addEventListener('online', () => void flush())
  document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') void flush() })
  window.setInterval(() => { if (navigator.onLine) void flush() }, 30_000)
  void flush()
}

/** Tylko do testów. */
export async function _reset(): Promise<void> {
  const d = await db()
  await d.clear('outbox')
  await d.clear('days')
  rejected.length = 0
}
