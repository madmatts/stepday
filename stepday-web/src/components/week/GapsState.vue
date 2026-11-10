<script setup lang="ts">
// Makieta „Braki”: wynik tygodnia, legenda, „Co dokładnie”.
import type { WeekSummary } from '@/api/types'
import { range } from '@/utils/dates'
import { plural } from '@/utils/polish'
import ScreenHeader from '@/components/common/ScreenHeader.vue'
import WeekDays from './WeekDays.vue'
import GapDayCard from './GapDayCard.vue'

defineProps<{ summary: WeekSummary }>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <div class="gaps">
    <ScreenHeader title="Podsumowanie tygodnia" :subtitle="range(summary.weekStart, summary.weekEnd)"
      close-label="Zamknij" @back="emit('close')" />

    <section aria-label="Wynik tygodnia" class="card score">
      <div class="big">
        <div class="num display">{{ summary.counts.completed }} z 7</div>
        <div class="num-label">{{ summary.counts.completed === 1 ? 'dzień zaliczony' : 'dni zaliczone' }}</div>
      </div>
      <WeekDays :days="summary.days" />
      <div class="legend">
        <div v-if="summary.counts.full" class="leg"><span class="sq sq--ok" />{{ summary.counts.full }} w pełni</div>
        <div v-if="summary.counts.partial" class="leg"><span class="sq sq--dot" />{{ summary.counts.partial }} {{ plural(summary.counts.partial, 'zaliczony', 'zaliczone', 'zaliczonych') }} z niedosytem</div>
        <div v-if="summary.counts.missed" class="leg"><span class="sq sq--miss" />{{ summary.counts.missed }} {{ plural(summary.counts.missed, 'niezaliczony', 'niezaliczone', 'niezaliczonych') }}</div>
        <div v-if="!summary.finished" class="leg">Tydzień jeszcze trwa</div>
      </div>
    </section>

    <template v-if="summary.gaps.length">
      <div class="section-label more">Co dokładnie</div>
      <GapDayCard v-for="g in summary.gaps" :key="g.date" :gap="g" />
    </template>

    <button type="button" class="btn-primary closing" @click="emit('close')">Zamknij</button>
  </div>
</template>

<style scoped>
.gaps { display: flex; flex-direction: column; gap: 16px; }
.score { padding: 16px; display: flex; flex-direction: column; gap: 14px; }
.big { display: flex; align-items: baseline; gap: 10px; }
.num { font-size: 40px; font-weight: 700; line-height: 1; }
.num-label { font-size: 15px; font-weight: 600; color: var(--text-2); }
.legend { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--text-2); }
.leg { display: flex; align-items: center; gap: 8px; }
.sq { width: 10px; height: 10px; border-radius: 3px; flex: none; }
.sq--ok { background: var(--ok); }
.sq--dot { background: var(--partial-dot); border-radius: 50%; }
.sq--miss { background: var(--miss); }
.more { margin-top: 4px; }
.closing { margin-top: 4px; }
</style>
