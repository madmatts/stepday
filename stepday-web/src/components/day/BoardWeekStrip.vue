<script setup lang="ts">
// Plansza z makiety Main: tydzień (pole = dzień), kropka niedosytu, pasek postępu dnia.
import { computed } from 'vue'
import type { DayView, WeekDay } from '@/api/types'
import { dayOfMonth, monthName, weekdayAbbr, dayMonth } from '@/utils/dates'
import AppIcon from '@/components/icons/AppIcon.vue'

const props = defineProps<{
  week: readonly WeekDay[]
  board: DayView['board']
  progress: DayView['progress']
  selectedDate: string
}>()

type Tile = WeekDay & { abbr: string; num: number; kind: 'full' | 'partial' | 'missed' | 'current' | 'plain'; label: string; link: boolean }

const STATUS_TEXT = { FULL: 'zaliczony', PARTIAL: 'zaliczony, ale czegoś brakowało', MISSED: 'niezaliczony', OPEN: 'w toku' }

const tiles = computed<Tile[]>(() => props.week.map((d) => {
  const kind: Tile['kind'] = d.date === props.selectedDate ? 'current'
    : d.status === 'FULL' ? 'full'
      : d.status === 'PARTIAL' ? 'partial'
        : d.status === 'MISSED' ? 'missed' : 'plain'
  const label = `${dayMonth(d.date)}${d.status ? ', ' + STATUS_TEXT[d.status] : ''}${d.today ? ', dziś' : ''}`
  return { ...d, abbr: weekdayAbbr(d.date), num: dayOfMonth(d.date), kind, label, link: d.status !== null && d.date !== props.selectedDate }
}))

const hasPartial = computed(() => props.week.some((d) => d.status === 'PARTIAL'))
const progressPct = computed(() => props.progress.total === 0 ? 0
  : Math.round((props.progress.done / props.progress.total) * 100))
</script>

<template>
  <section class="board card" aria-label="Plansza">
    <div class="row">
      <div class="section-label">Plansza · {{ monthName(board.month) }}</div>
      <div class="meta">pole {{ board.position }} z {{ board.total }}</div>
    </div>
    <ol class="tiles">
      <li v-for="t in tiles" :key="t.date" class="tile-wrap">
        <component
          :is="t.link ? 'RouterLink' : 'div'"
          :to="t.link ? (t.today ? '/' : `/day/${t.date}`) : undefined"
          :class="['tile', `tile--${t.kind}`]"
          :aria-label="t.label"
          :aria-current="t.kind === 'current' ? 'date' : undefined"
        >
          <span class="abbr">{{ t.abbr }}</span>
          <span v-if="t.kind === 'partial'" class="dot" aria-hidden="true" />
          <AppIcon v-if="t.kind === 'full' || t.kind === 'partial'" name="check" :size="18" color="var(--ok-ink)" />
          <AppIcon v-else-if="t.kind === 'missed'" name="close" :size="18" :stroke-width="2.5" color="var(--miss)" />
          <span v-else :class="['num', { 'num--current': t.kind === 'current' }]">{{ t.num }}</span>
        </component>
      </li>
    </ol>
    <div v-if="hasPartial" class="legend">
      <span class="dot dot--inline" aria-hidden="true" />
      Dzień zaliczony, ale czegoś brakowało
    </div>
    <div class="track" role="progressbar" :aria-valuenow="progressPct" aria-valuemin="0" aria-valuemax="100"
      :aria-label="`Postęp dnia: ${progress.done} z ${progress.total} kroków`">
      <div class="fill" :style="{ width: progressPct + '%' }" />
    </div>
  </section>
</template>

<style scoped>
.board { padding: 14px 14px 16px; display: flex; flex-direction: column; gap: 12px; }
.row { display: flex; justify-content: space-between; align-items: baseline; }
.meta { font-size: 12px; color: var(--text-3); }
.tiles { display: flex; gap: 6px; list-style: none; margin: 0; padding: 0; }
.tile-wrap { flex: 1; min-width: 0; }
.tile {
  height: 56px; border-radius: 12px; position: relative; text-decoration: none;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px;
  background: var(--muted-bg); color: var(--text-2);
}
.tile--full, .tile--partial { background: var(--ok-soft); color: var(--ok-ink); }
.tile--missed { background: var(--miss-soft); color: var(--miss); }
.tile--current { background: var(--accent); color: #fff; box-shadow: 0 4px 12px rgba(45, 91, 255, 0.35); }
.abbr { font-size: 11px; font-weight: 600; }
.num { font-family: var(--font-display); font-size: 16px; font-weight: 600; }
.num--current { font-size: 18px; font-weight: 700; }
.dot { position: absolute; top: 6px; right: 6px; width: 8px; height: 8px; border-radius: 50%; background: var(--partial-dot); }
.dot--inline { position: static; flex: none; }
.legend { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-3); }
.track { height: 6px; border-radius: 3px; background: var(--divider); overflow: hidden; }
.fill { height: 6px; border-radius: 3px; background: var(--accent); transition: width 0.3s ease; }
</style>
