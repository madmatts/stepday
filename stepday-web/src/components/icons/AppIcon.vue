<script setup lang="ts">
// Ikony stroke z makiet (ścieżki SVG skopiowane 1:1 z docs/mockups-src).
import { computed } from 'vue'
import type { IconName } from '@/utils/blocks'

const props = withDefaults(defineProps<{
  name: IconName
  size?: number
  color?: string
  strokeWidth?: number
}>(), { size: 20, color: 'currentColor', strokeWidth: undefined })

const PATHS: Record<IconName, { d: string[]; circles?: [number, number, number][]; rects?: string; viewBox?: string; width?: number; fill?: boolean }> = {
  back: { d: ['M15 5l-7 7 7 7'], width: 2.2 },
  close: { d: ['M6 6l12 12M18 6L6 18'], width: 2.2 },
  check: { d: ['M5 12l4.5 4.5L19 7'], width: 2.5 },
  chevron: { d: ['M9 5l7 7-7 7'], width: 2.2 },
  plus: { d: ['M12 5v14M5 12h14'], width: 2.5 },
  minus: { d: ['M5 12h14'], width: 2.5 },
  dash: { d: ['M6 12h12'], width: 2.5 },
  warn: { d: ['M12 6v8M12 18v.5'], width: 2.5 },
  list: { d: ['M9 6h11M9 12h11M9 18h11', 'M3.5 6l1.2 1.2L7 5M3.5 12l1.2 1.2L7 11M3.5 18l1.2 1.2L7 17'], width: 2 },
  drop: { d: ['M12 3c3 4 6 7 6 11a6 6 0 0 1-12 0c0-4 3-7 6-11z'], width: 2 },
  timer: { d: ['M12 9v4l3 2M9 3h6'], circles: [[12, 13, 7]], width: 2 },
  pencil: { d: ['M4 20l4-1 11-11-3-3L5 16l-1 4z'], width: 2 },
  calendar: { d: ['M4 10h16M9 3v4M15 3v4'], rects: 'x=4 y=5 w=16 h=15 r=2', width: 2 },
  grip: { d: [], circles: [[4, 4, 1.6], [10, 4, 1.6], [4, 10, 1.6], [10, 10, 1.6], [4, 16, 1.6], [10, 16, 1.6]], viewBox: '0 0 14 20', fill: true },
  'arrow-right': { d: ['M5 12h14M13 6l6 6-6 6'], width: 2.5 },
  'arrow-down': { d: ['M12 1v22M6 17l6 7 6-7'], viewBox: '0 0 24 28', width: 2 },
  'arrow-down-short': { d: ['M12 1v16M6 12l6 7 6-7'], viewBox: '0 0 24 22', width: 2 },
}

const icon = computed(() => PATHS[props.name])
const viewBox = computed(() => icon.value.viewBox ?? '0 0 24 24')
const ratio = computed(() => {
  const [, , w, h] = viewBox.value.split(' ').map(Number)
  return h / w
})
const rect = computed(() => {
  if (!icon.value.rects) return null
  const m = Object.fromEntries(icon.value.rects.split(' ').map((p) => p.split('=')))
  return { x: m.x, y: m.y, width: m.w, height: m.h, rx: m.r }
})
</script>

<template>
  <svg
    :width="size"
    :height="size * ratio"
    :viewBox="viewBox"
    :fill="icon.fill ? color : 'none'"
    :stroke="icon.fill ? 'none' : color"
    :stroke-width="strokeWidth ?? icon.width"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path v-for="(d, i) in icon.d" :key="i" :d="d" />
    <circle v-for="(c, i) in icon.circles ?? []" :key="'c' + i" :cx="c[0]" :cy="c[1]" :r="c[2]" />
    <rect v-if="rect" v-bind="rect" />
  </svg>
</template>
