import { computed, shallowRef } from 'vue'

/**
 * Zmiana kolejności przeciąganiem za uchwyt (dotyk i mysz przez Pointer Events — działa w Safari na iOS)
 * oraz strzałkami z klawiatury. Kontener ma `data-reorder`, wiersze `data-reorder-row`.
 */
export function useDragReorder<T>(source: () => readonly T[], commit: (next: T[]) => void) {
  const working = shallowRef<T[] | null>(null)
  const draggingIndex = shallowRef<number | null>(null)
  const items = computed<readonly T[]>(() => working.value ?? source())

  function move(list: T[], from: number, to: number): T[] {
    const next = [...list]
    const [x] = next.splice(from, 1)
    next.splice(to, 0, x)
    return next
  }

  function onPointerDown(index: number, e: PointerEvent) {
    const handle = e.currentTarget as HTMLElement
    const container = handle.closest('[data-reorder]')
    if (!container) return
    e.preventDefault()
    handle.setPointerCapture(e.pointerId)
    working.value = [...source()]
    draggingIndex.value = index
    let changed = false

    const onMove = (ev: PointerEvent) => {
      const rows = [...container.querySelectorAll<HTMLElement>('[data-reorder-row]')]
      let target = rows.findIndex((r) => {
        const box = r.getBoundingClientRect()
        return ev.clientY < box.top + box.height / 2
      })
      if (target === -1) target = rows.length - 1
      const from = draggingIndex.value!
      if (target !== from && working.value) {
        working.value = move(working.value, from, target)
        draggingIndex.value = target
        changed = true
      }
    }
    const onUp = () => {
      handle.removeEventListener('pointermove', onMove)
      handle.removeEventListener('pointerup', onUp)
      handle.removeEventListener('pointercancel', onUp)
      const result = working.value
      working.value = null
      draggingIndex.value = null
      if (changed && result) commit(result)
    }
    handle.addEventListener('pointermove', onMove)
    handle.addEventListener('pointerup', onUp)
    handle.addEventListener('pointercancel', onUp)
  }

  function onKeyDown(index: number, e: KeyboardEvent) {
    const delta = e.key === 'ArrowUp' ? -1 : e.key === 'ArrowDown' ? 1 : 0
    const list = source()
    const to = index + delta
    if (delta === 0 || to < 0 || to >= list.length) return
    e.preventDefault()
    commit(move([...list], index, to))
  }

  return { items, draggingIndex, onPointerDown, onKeyDown }
}
