<script setup lang="ts">
import { computed } from 'vue'
import type { LayoutBlock } from '@/api/types'
import { blockIcon, TYPE_LABEL } from '@/utils/blocks'
import { describe } from '@/utils/rrule'
import { plural } from '@/utils/polish'
import AppIcon from '@/components/icons/AppIcon.vue'

const props = defineProps<{ block: LayoutBlock; dragging?: boolean }>()
const emit = defineEmits<{ gripDown: [e: PointerEvent]; gripKey: [e: KeyboardEvent]; open: [] }>()

const t = computed(() => props.block.template)
const sub = computed(() => {
  const parts = [TYPE_LABEL[t.value.type]]
  const c = t.value.config as Record<string, number>
  if (t.value.type === 'TIMER') parts.push(`${c.minutes} ${plural(c.minutes, 'minuta', 'minuty', 'minut')}`)
  if (t.value.type === 'COUNTER') parts.push(`cel ${c.target}, zalicza od ${c.threshold_pct}%`)
  else parts.push(describe(t.value.schedule.rrule))
  return parts.join(' · ')
})
const chip = computed(() => !props.block.dueOnDate ? 'off' : !props.block.everyDay ? 'due' : null)
const accent = computed(() => t.value.role === 'ALL_DAY')
</script>

<template>
  <div :class="['row', { 'row--off': chip === 'off', 'row--drag': dragging }]">
    <button type="button" class="grip" :aria-label="`Zmień kolejność: ${t.name}`"
      @pointerdown="emit('gripDown', $event)" @keydown="emit('gripKey', $event)">
      <AppIcon name="grip" :size="14" color="var(--icon-muted)" />
    </button>
    <div :class="['badge', { 'badge--accent': accent }]">
      <AppIcon :name="blockIcon(t.type, !block.everyDay)" :color="accent ? 'var(--accent-ink)' : 'var(--text-2)'" />
    </div>
    <button type="button" class="open" :aria-label="`Edytuj: ${t.name}`" @click="emit('open')">
      <span :class="['name', { 'name--off': chip === 'off' }]">{{ t.name }}</span>
      <span class="sub">{{ sub }}</span>
    </button>
    <span v-if="chip === 'due'" class="chip chip--partial">Wypada dziś</span>
    <span v-else-if="chip === 'off'" class="chip chip--muted">Nie dziś</span>
  </div>
</template>

<style scoped>
.row { display: flex; align-items: center; gap: 8px; min-height: 68px; padding: 10px 12px 10px 4px; border-radius: 16px; border: 1px solid var(--border); background: var(--surface); }
.row--off { border: 2px dashed var(--icon-muted); background: var(--bg); }
.row--drag { border-color: var(--accent); box-shadow: var(--shadow-active); }
.grip { width: 28px; height: 44px; border: 0; background: transparent; display: flex; align-items: center; justify-content: center; flex: none; touch-action: none; cursor: grab; }
.badge { width: 40px; height: 40px; border-radius: 50%; background: var(--muted-bg); display: flex; align-items: center; justify-content: center; flex: none; }
.badge--accent { background: var(--accent-soft); }
.open { flex: 1; min-width: 0; min-height: 44px; border: 0; background: transparent; padding: 0; text-align: left; display: flex; flex-direction: column; justify-content: center; color: var(--text); }
.name { font-size: 15px; font-weight: 600; }
.name--off { color: var(--text-2); }
.sub { font-size: 12px; color: var(--text-3); }
.chip { flex: none; }
</style>
