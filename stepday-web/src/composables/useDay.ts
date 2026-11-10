import { computed, onScopeDispose, shallowReadonly, shallowRef, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { api, ApiError } from '@/api/client'
import { cachedDay, isOffline, onSyncState, rememberDay } from '@/sync/outbox'
import { dayMutations } from '@/api/mutations'
import type { BlockView, DayView } from '@/api/types'

/**
 * Stan ekranu dnia. Źródłem prawdy jest odpowiedź serwera; akcje podmieniają cały widok
 * (serwer przelicza stan dnia po każdym zapisie i nie ufa klientowi).
 */
export function useDay(date: MaybeRefOrGetter<string | null>) {
  const day = shallowRef<DayView | null>(null)
  const error = shallowRef<string | null>(null)
  const busy = shallowRef(false)
  /** Widok pochodzi z kopii lokalnej (brak sieci). */
  const offline = shallowRef(false)

  async function load() {
    const d = toValue(date)
    error.value = null
    try {
      const fresh = await api<DayView>(d ? `/days/${d}` : '/days/today')
      await rememberDay(fresh)
      // Zmiany czekające w kolejce nakładamy także na świeży widok, dopóki nie dojdą do serwera.
      day.value = (await cachedDay(fresh.date)) ?? fresh
      offline.value = false
    } catch (e) {
      if (isOffline(e)) {
        offline.value = true
        day.value = await cachedDay(d)
        if (!day.value) error.value = 'Brak sieci i brak zapisanej kopii tego dnia'
      } else {
        error.value = e instanceof Error ? e.message : String(e)
      }
    }
  }

  watch(() => toValue(date), load, { immediate: true })

  // Po wysłaniu kolejki odświeżamy widok z serwera (stan dnia liczy serwer).
  let lastPending = 0
  const stop = onSyncState(({ pending }) => {
    if (lastPending > 0 && pending === 0) void load()
    lastPending = pending
  })
  onScopeDispose(stop)

  /** Bieżący krok („TERAZ”): pierwszy oczekujący, potem odłożone na „Później”. */
  const currentStepId = computed(() => {
    const steps = day.value?.steps ?? []
    const open = (s: BlockView) => s.state === 'PENDING'
    return (steps.find((s) => open(s) && !s.deferred) ?? steps.find(open))?.id ?? null
  })

  async function run(action: (date: string) => Promise<DayView | null>) {
    if (!day.value || busy.value) return
    busy.value = true
    error.value = null
    try {
      const next = await action(day.value.date)
      if (next) day.value = next
    } catch (e) {
      error.value = e instanceof ApiError ? e.message : 'Nie udało się zapisać'
      if (e instanceof ApiError && e.status === 409) void load()
    } finally {
      busy.value = false
    }
  }

  const actions = {
    toggleItem: (b: BlockView, itemId: string, checked: boolean) =>
      run((d) => dayMutations.setItem(d, b.id, itemId, checked)),
    complete: (b: BlockView) => run((d) => dayMutations.complete(d, b.id)),
    finishNote: (b: BlockView, text: string) => run(async (d) => {
      await dayMutations.saveNote(d, b.id, text)
      return dayMutations.complete(d, b.id)
    }),
    counter: (b: BlockView, delta: number) => run((d) => dayMutations.counter(d, b.id, delta)),
    startTimer: (b: BlockView) => run((d) => dayMutations.startTimer(d, b.id)),
    stopTimer: (b: BlockView) => run((d) => dayMutations.stopTimer(d, b.id)),
    skip: (b: BlockView) => run((d) => dayMutations.skip(d, b.id)),
    defer: (b: BlockView) => run((d) => dayMutations.defer(d, b.id)),
  }

  return {
    day: shallowReadonly(day),
    error: shallowReadonly(error),
    busy: shallowReadonly(busy),
    offline: shallowReadonly(offline),
    currentStepId,
    reload: load,
    /** Podmiana widoku z zewnątrz (kolejka offline nakłada lokalne zmiany). */
    replace: (next: DayView) => { day.value = next },
    ...actions,
  }
}

export type DayActions = Omit<ReturnType<typeof useDay>, 'day' | 'error' | 'busy' | 'offline' | 'currentStepId' | 'reload' | 'replace'>
