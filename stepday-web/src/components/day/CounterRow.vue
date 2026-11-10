<script setup lang="ts">
// Licznik „W ciągu dnia” (makieta Main + podgląd w makiecie Licznik).
import { computed } from 'vue'
import type { BlockView } from '@/api/types'
import AppIcon from '@/components/icons/AppIcon.vue'

const props = defineProps<{ block: BlockView; disabled?: boolean; preview?: boolean }>()
const emit = defineEmits<{ change: [delta: number] }>()

const value = computed(() => Math.max(0, props.block.value ?? 0))
const target = computed(() => props.block.target ?? 1)
const threshold = computed(() => props.block.threshold ?? target.value)
const step = computed(() => props.block.step ?? 1)
const status = computed(() => value.value >= target.value ? 'full' : value.value >= threshold.value ? 'partial' : 'none')

/** Kreski jak w makiecie; przy dużych celach (np. kroki) jeden pasek. */
const pips = computed(() => target.value <= 12
  ? Array.from({ length: target.value }, (_, i) => i < value.value)
  : null)
const pct = computed(() => Math.min(100, Math.round((value.value / target.value) * 100)))

const hint = computed(() => {
  if (status.value === 'full') return 'Cel osiągnięty'
  if (status.value === 'partial') return `Zaliczone, brakuje ${target.value - value.value}`
  return `Dzień zalicza się od ${threshold.value}`
})
const unit = computed(() => props.block.unit ? ` ${props.block.unit}` : '')
</script>

<template>
  <div class="counter">
    <div :class="['badge', `badge--${status}`]">
      <AppIcon v-if="status === 'none'" name="drop" color="var(--accent-ink)" />
      <AppIcon v-else name="check" :color="status === 'full' ? 'var(--ok)' : 'var(--partial-ink)'" />
    </div>
    <div class="body">
      <div class="top">
        <div class="name">{{ block.name }}</div>
        <div class="count" :aria-label="`${value} z ${target}${unit}`">{{ value }}/{{ target }}</div>
      </div>
      <div v-if="pips" class="pips" aria-hidden="true">
        <div v-for="(on, i) in pips" :key="i" :class="['pip', { 'pip--on': on }]" />
      </div>
      <div v-else class="bar" aria-hidden="true"><div class="bar-fill" :style="{ width: pct + '%' }" /></div>
      <div v-if="!preview" :class="['hint', `hint--${status}`]">{{ hint }}</div>
    </div>
    <template v-if="preview">
      <div class="btn btn--minus" aria-hidden="true"><AppIcon name="minus" :size="18" color="var(--text)" /></div>
      <div class="btn btn--plus" aria-hidden="true"><AppIcon name="plus" :size="18" color="#fff" /></div>
    </template>
    <template v-else>
      <button type="button" class="btn btn--minus" :aria-label="`Odejmij: ${block.name}`"
        :disabled="disabled || value <= 0" @click="emit('change', -step)">
        <AppIcon name="minus" :size="18" color="var(--text)" />
      </button>
      <button type="button" class="btn btn--plus" :aria-label="`Dodaj: ${block.name}`"
        :disabled="disabled || value >= target" @click="emit('change', step)">
        <AppIcon name="plus" :size="18" color="#fff" />
      </button>
    </template>
  </div>
</template>

<style scoped>
.counter { display: flex; align-items: center; gap: 12px; }
.badge { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex: none; background: var(--accent-soft); }
.badge--full { background: var(--ok-soft); }
.badge--partial { background: var(--partial-soft); }
.body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.top { display: flex; align-items: baseline; justify-content: space-between; }
.name { font-size: 15px; font-weight: 600; }
.count { font-size: 14px; font-weight: 600; color: var(--text-2); }
.pips { display: flex; gap: 4px; }
.pip { flex: 1; height: 8px; border-radius: 4px; background: var(--border); }
.pip--on { background: var(--accent); }
.bar { height: 8px; border-radius: 4px; background: var(--border); overflow: hidden; }
.bar-fill { height: 8px; background: var(--accent); }
.hint { font-size: 12px; font-weight: 600; color: var(--text-3); }
.hint--full { color: var(--ok); }
.hint--partial { color: var(--partial-ink); }
.btn { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex: none; }
.btn--minus { border: 1px solid var(--border); background: var(--surface-2); }
.btn--plus { border: 0; background: var(--accent); }
.btn:disabled { opacity: 0.4; }
</style>
