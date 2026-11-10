<script setup lang="ts">
// Koniec ścieżki: przejście na kolejne pole planszy (makieta Main).
import { computed } from 'vue'
import type { BlockView, DayView } from '@/api/types'
import AppIcon from '@/components/icons/AppIcon.vue'

const props = defineProps<{ day: DayView }>()

const completed = computed(() => props.day.status === 'FULL' || props.day.status === 'PARTIAL')

/** „Z niedosytem: woda 7/8” — co dokładnie zostało poniżej celu. */
const partialNote = computed(() => {
  const short = [...props.day.allDay, ...props.day.steps].filter((b: BlockView) => b.completion === 'PARTIAL')
  if (short.length === 0) return ''
  const parts = short.map((b) => b.state === 'SKIPPED'
    ? `${b.name.toLowerCase()} pominięte`
    : `${b.name.toLowerCase()} ${b.value}/${b.target}`)
  return `Z niedosytem: ${parts.join(', ')}. `
})
</script>

<template>
  <section v-if="completed" class="end end--done">
    <div class="badge badge--done"><AppIcon name="arrow-right" color="#fff" /></div>
    <div class="body">
      <div class="title display">Dzień zaliczony</div>
      <div class="note">{{ partialNote }}Ruszasz na pole {{ day.board.position }}</div>
    </div>
  </section>
  <section v-else-if="day.closed" class="end">
    <div class="badge"><AppIcon name="close" color="var(--text-2)" /></div>
    <div class="body">
      <div class="title-plain">Dzień zamknięty</div>
      <div class="hint">Ten dzień nie przesunął pionka. Bez kary — jutro nowe pole.</div>
    </div>
  </section>
  <section v-else class="end">
    <div class="badge"><AppIcon name="arrow-right" color="var(--text-2)" /></div>
    <div class="body">
      <div class="title-plain">Koniec dnia</div>
      <div class="hint">Po ostatnim kroku i celu z „W ciągu dnia” przechodzisz na pole {{ Math.min(day.board.position + 1, day.board.total) }}</div>
    </div>
  </section>
</template>

<style scoped>
.end { border: 2px dashed var(--icon-muted); border-radius: 16px; padding: 16px; display: flex; align-items: center; gap: 12px; }
.end--done { background: var(--ok-soft); border: 2px solid var(--ok); }
.badge { width: 40px; height: 40px; border-radius: 50%; background: var(--muted-bg); display: flex; align-items: center; justify-content: center; flex: none; }
.badge--done { background: var(--ok); }
.body { flex: 1; min-width: 0; }
.title { font-size: 18px; font-weight: 700; color: var(--ok-ink); }
.note { font-size: 13px; color: var(--ok-ink); }
.title-plain { font-size: 15px; font-weight: 600; color: var(--text-2); }
</style>
