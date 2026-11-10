<script setup lang="ts">
// Makieta „Sukces”: zielony ptaszek, konfetti, napis „Sukces”.
import type { WeekSummary } from '@/api/types'
import { range } from '@/utils/dates'
import AppIcon from '@/components/icons/AppIcon.vue'
import WeekDays from './WeekDays.vue'

defineProps<{ summary: WeekSummary }>()
const emit = defineEmits<{ close: [] }>()

// Konfetti 1:1 z makiety (pozycje, kolory, obroty).
const RECTS: [number, number, string, number][] = [
  [40, 30, '#2D5BFF', 30], [170, 40, '#7FCB9E', -20], [250, 22, '#B7791F', 50], [360, 20, '#1F7A4D', 15],
  [70, 120, '#F0B95A', 70], [120, 80, '#8FB3FF', -40], [290, 95, '#7FCB9E', 25], [370, 85, '#2D5BFF', -60],
  [30, 170, '#1F7A4D', 10], [355, 190, '#B7791F', -30], [20, 250, '#F0B95A', 40], [65, 285, '#7FCB9E', -15],
  [260, 260, '#2D5BFF', 65], [330, 280, '#1F7A4D', -45], [150, 320, '#8FB3FF', 20], [295, 320, '#F0B95A', -10],
  [190, 12, '#2D5BFF', 80], [215, 300, '#7FCB9E', 35],
]
const DOTS: [number, number, string][] = [
  [110, 18, '#F0B95A'], [320, 48, '#2D5BFF'], [22, 90, '#7FCB9E'], [340, 130, '#F0B95A'], [80, 200, '#2D5BFF'],
  [310, 215, '#8FB3FF'], [120, 250, '#B7791F'], [370, 250, '#F0B95A'], [230, 330, '#1F7A4D'], [100, 330, '#2D5BFF'],
]
</script>

<template>
  <div class="success">
    <svg class="confetti" width="390" height="360" viewBox="0 0 390 360" aria-hidden="true">
      <rect v-for="(r, i) in RECTS" :key="'r' + i" :x="r[0]" :y="r[1]" width="12" height="6" rx="1" :fill="r[2]"
        :transform="`rotate(${r[3]} ${r[0] + 6} ${r[1] + 3})`" />
      <circle v-for="(c, i) in DOTS" :key="'c' + i" :cx="c[0]" :cy="c[1]" r="4.5" :fill="c[2]" />
    </svg>

    <header class="head">
      <div class="overline">Podsumowanie tygodnia</div>
      <button type="button" class="close" aria-label="Zamknij" @click="emit('close')">
        <AppIcon name="close" :size="20" color="var(--text)" />
      </button>
    </header>

    <div class="hero">
      <div class="medal" role="img" aria-label="Sukces"><AppIcon name="check" :size="64" color="#fff" :stroke-width="3" /></div>
      <h1 class="title display">Sukces</h1>
      <div class="range">Tydzień {{ range(summary.weekStart, summary.weekEnd) }}</div>
      <div class="lead">Wszystko zaliczone wedle planu</div>
    </div>

    <div class="week-row"><WeekDays :days="summary.days" /></div>

    <button type="button" class="btn-primary closing" @click="emit('close')">Zamknij</button>
  </div>
</template>

<style scoped>
.success { position: relative; flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.confetti { position: absolute; top: 64px; left: 50%; transform: translateX(-50%); }
.head { position: relative; display: flex; align-items: center; justify-content: space-between; min-height: 48px; }
.overline { font-size: 13px; font-weight: 600; color: var(--text-3); }
.close { width: 44px; height: 44px; border-radius: 12px; border: 0; background: transparent; display: flex; align-items: center; justify-content: center; margin-right: -8px; }
.hero { position: relative; display: flex; flex-direction: column; align-items: center; margin-top: 108px; gap: 6px; text-align: center; }
.medal { width: 132px; height: 132px; border-radius: 50%; background: var(--ok); display: flex; align-items: center; justify-content: center; box-shadow: 0 12px 32px rgba(31, 122, 77, 0.35); margin-bottom: 14px; }
.title { margin: 0; font-size: 44px; font-weight: 700; line-height: 1.1; }
.range { font-size: 14px; color: var(--text-3); font-weight: 500; }
.lead { font-size: 18px; font-weight: 600; margin-top: 8px; }
.week-row { position: relative; margin-top: 36px; }
.closing { position: relative; margin-top: auto; }
</style>
