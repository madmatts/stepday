// Zapisy w dniu. Każdy zwraca świeży widok dnia z serwera (serwer liczy stan dnia).
// Etap PWA podmienia `send` na kolejkę offline (src/sync), interfejs zostaje ten sam.
import { api } from './client'
import type { DayView } from './types'
import { uuid } from '@/utils/uuid'

export interface Mutation {
  /** UUID nadany po stronie klienta — klucz idempotencji i kolejki offline. */
  id: string
  date: string
  instanceId: string
  path: string
  method: 'POST' | 'PUT'
  body?: Record<string, unknown>
}

export type Sender = (m: Mutation) => Promise<DayView | null>

let sender: Sender = (m) => api<DayView>(m.path, { method: m.method, body: m.body ?? {} })

export function setSender(s: Sender): void {
  sender = s
}

function mutation(date: string, instanceId: string, suffix: string, method: 'POST' | 'PUT',
  body?: Record<string, unknown>): Mutation {
  return { id: uuid(), date, instanceId, path: `/instances/${instanceId}${suffix}`, method, body }
}

const now = () => new Date().toISOString()

export const dayMutations = {
  setItem: (date: string, instanceId: string, itemId: string, checked: boolean) =>
    sender(mutation(date, instanceId, `/items/${itemId}`, 'PUT', { checked, changedAt: now() })),
  complete: (date: string, instanceId: string) => sender(mutation(date, instanceId, '/complete', 'POST')),
  saveNote: (date: string, instanceId: string, text: string) =>
    sender(mutation(date, instanceId, '/note', 'PUT', { text, changedAt: now() })),
  counter: (date: string, instanceId: string, delta: number) => {
    const m = mutation(date, instanceId, '/counter-events', 'POST')
    m.body = { delta, clientEventId: m.id, happenedAt: now() }
    return sender(m)
  },
  startTimer: (date: string, instanceId: string) => sender(mutation(date, instanceId, '/timer/start', 'POST')),
  stopTimer: (date: string, instanceId: string) => sender(mutation(date, instanceId, '/timer/stop', 'POST')),
  skip: (date: string, instanceId: string) => sender(mutation(date, instanceId, '/skip', 'POST')),
  defer: (date: string, instanceId: string) => sender(mutation(date, instanceId, '/defer', 'POST')),
}
