import { shallowReadonly, shallowRef } from 'vue'
import { api } from '@/api/client'
import type { Layout, LayoutBlock } from '@/api/types'

export function useLayout() {
  const layout = shallowRef<Layout | null>(null)
  const error = shallowRef<string | null>(null)

  async function load() {
    try {
      layout.value = await api<Layout>('/layout')
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
    }
  }

  /** Nowa kolejność ścieżki; widok aktualizowany od razu, serwer nadaje klucze. */
  async function reorder(steps: LayoutBlock[]) {
    if (!layout.value) return
    layout.value = { ...layout.value, steps }
    const ids = [...steps, ...layout.value.allDay].map((b) => b.template.id)
    try {
      await api<void>('/templates/order', { method: 'PUT', body: { orderedIds: ids } })
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
      await load()
    }
  }

  void load()
  return { layout: shallowReadonly(layout), error: shallowReadonly(error), reload: load, reorder }
}
