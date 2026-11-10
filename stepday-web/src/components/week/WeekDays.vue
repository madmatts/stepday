<script setup lang="ts">
// Rząd 7 dni ze statusem (makiety Sukces i Braki). Stan nigdy samym kolorem: ikona + opis.
import { computed } from 'vue'
import type { DayStatus } from '@/api/types'
import { weekdayAbbr, weekday } from '@/utils/dates'
import AppIcon from '@/components/icons/AppIcon.vue'

const props = defineProps<{ days: readonly { date: string; status: DayStatus | null }[] }>()

const TEXT: Record<DayStatus, string> = { FULL: 'zaliczony', PARTIAL: 'zaliczony z niedosytem', MISSED: 'niezaliczony', OPEN: 'w toku' }
const label = computed(() => props.days.map((d) => `${weekday(d.date)} ${d.status ? TEXT[d.status] : 'jeszcze przed nami'}`).join(', '))
</script>

<template>
  <div role="img" :aria-label="label" class="days">
    <div v-for="d in days" :key="d.date" :class="['tile', `tile--${(d.status ?? 'none').toLowerCase()}`]">
      <span class="abbr">{{ weekdayAbbr(d.date) }}</span>
      <span v-if="d.status === 'PARTIAL'" class="dot" />
      <AppIcon v-if="d.status === 'FULL' || d.status === 'PARTIAL'" name="check" :size="18" color="var(--ok-ink)" />
      <AppIcon v-else-if="d.status === 'MISSED'" name="close" :size="18" :stroke-width="2.5" color="var(--miss)" />
      <AppIcon v-else name="dash" :size="18" color="var(--text-3)" />
    </div>
  </div>
</template>

<style scoped>
.days { display: flex; gap: 6px; position: relative; }
.tile { flex: 1; min-width: 0; height: 56px; border-radius: 12px; position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; background: var(--muted-bg); color: var(--text-2); }
.tile--full, .tile--partial { background: var(--ok-soft); color: var(--ok-ink); }
.tile--missed { background: var(--miss-soft); color: var(--miss); }
.abbr { font-size: 11px; font-weight: 600; }
.dot { position: absolute; top: 6px; right: 6px; width: 8px; height: 8px; border-radius: 50%; background: var(--partial-dot); }
</style>
