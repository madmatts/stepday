import { shallowReadonly, shallowRef, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { api } from '@/api/client'
import type { WeekSummary } from '@/api/types'

export function useWeekSummary(weekStart: MaybeRefOrGetter<string>) {
  const summary = shallowRef<WeekSummary | null>(null)
  const error = shallowRef<string | null>(null)
  watch(() => toValue(weekStart), async (start) => {
    error.value = null
    try {
      summary.value = await api<WeekSummary>(`/weeks/${start}/summary`)
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
    }
  }, { immediate: true })
  return { summary: shallowReadonly(summary), error: shallowReadonly(error) }
}
