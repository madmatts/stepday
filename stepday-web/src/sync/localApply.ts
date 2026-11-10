// Lokalne nałożenie zmiany z kolejki na ostatni widok dnia (dokument, sekcja 8: „lokalne zmiany są nakładane
// na nią do czasu synchronizacji”). To tylko podgląd: stan dnia zawsze przelicza serwer po synchronizacji.
import type { BlockView, DayView } from '@/api/types'
import type { Mutation } from '@/api/mutations'

function updateBlock(day: DayView, id: string, fn: (b: BlockView) => BlockView): DayView {
  const map = (list: BlockView[]) => list.map((b) => (b.id === id ? fn(b) : b))
  const steps = map(day.steps)
  return {
    ...day,
    steps,
    allDay: map(day.allDay),
    progress: { done: steps.filter((s) => s.state === 'DONE').length, total: day.progress.total },
  }
}

export function applyLocal(day: DayView, m: Mutation): DayView {
  if (m.date !== day.date) return day
  const action = m.path.slice(`/instances/${m.instanceId}`.length)
  const now = new Date().toISOString()
  return updateBlock(day, m.instanceId, (b) => {
    if (action.startsWith('/items/')) {
      const itemId = action.slice('/items/'.length)
      const items = (b.items ?? []).map((i) => (i.id === itemId ? { ...i, checked: Boolean(m.body?.checked) } : i))
      const all = items.every((i) => i.checked)
      return { ...b, items, state: all ? 'DONE' : 'PENDING', completion: all ? 'FULL' : 'NONE' }
    }
    switch (action) {
      case '/counter-events': {
        const value = (b.value ?? 0) + Number(m.body?.delta ?? 0)
        const full = value >= (b.target ?? Infinity)
        const done = value >= (b.threshold ?? Infinity)
        return { ...b, value, state: done ? 'DONE' : 'PENDING', completion: full ? 'FULL' : done ? 'PARTIAL' : 'NONE' }
      }
      case '/complete': return { ...b, state: 'DONE', completion: 'FULL' }
      case '/note': return { ...b, note: String(m.body?.text ?? '') }
      case '/skip': return { ...b, state: 'SKIPPED', completion: 'PARTIAL' }
      case '/defer': return { ...b, deferred: true }
      case '/timer/start': return { ...b, timerStartedAt: b.timerStartedAt ?? now }
      case '/timer/stop': return { ...b, timerStoppedAt: b.timerStoppedAt ?? now, state: 'DONE', completion: 'FULL' }
      default: return b
    }
  })
}
