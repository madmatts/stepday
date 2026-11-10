<script setup lang="ts">
import type { GapDay } from '@/api/types'
import { dayMonth, weekdayCap } from '@/utils/dates'
import AppIcon from '@/components/icons/AppIcon.vue'
import { gapLine } from './gapText'

defineProps<{ gap: GapDay }>()
const ICON = { miss: 'close', partial: 'warn', skip: 'dash' } as const
const COLOR = { miss: 'var(--miss)', partial: 'var(--partial-ink)', skip: 'var(--partial-ink)' } as const
</script>

<template>
  <section class="card gap" :aria-label="`${weekdayCap(gap.date)}, ${dayMonth(gap.date)}`">
    <div class="head">
      <div>
        <div class="weekday">{{ weekdayCap(gap.date) }}</div>
        <h2 class="date display">{{ dayMonth(gap.date) }}</h2>
      </div>
      <span v-if="gap.status === 'MISSED'" class="chip chip--miss badge">Niezaliczony</span>
      <span v-else class="chip chip--partial badge">Z niedosytem</span>
    </div>
    <ul class="lines">
      <li v-for="b in gap.blocks" :key="b.instanceId" class="line">
        <AppIcon :name="ICON[gapLine(b).tone]" :size="20" :stroke-width="2.5" :color="COLOR[gapLine(b).tone]" />
        <div>
          <div class="name">{{ b.name }}</div>
          <div class="text">{{ gapLine(b).text }}</div>
        </div>
      </li>
      <li v-if="!gap.blocks.length" class="line">
        <AppIcon name="close" :size="20" :stroke-width="2.5" color="var(--miss)" />
        <div class="text">Aplikacja nie była otwierana tego dnia.</div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.gap { padding: 16px; display: flex; flex-direction: column; gap: 14px; }
.head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.weekday { font-size: 12px; color: var(--text-3); font-weight: 500; }
.date { margin: 0; font-size: 20px; font-weight: 700; line-height: 1.15; }
.badge { font-size: 12px; padding: 5px 10px; flex: none; }
.lines { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.line { display: flex; align-items: flex-start; gap: 10px; }
.line > :first-child { flex: none; margin-top: 1px; }
.name { font-size: 15px; font-weight: 600; }
.text { font-size: 13px; line-height: 1.4; color: var(--text-3); }
</style>
