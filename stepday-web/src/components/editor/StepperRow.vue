<script setup lang="ts">
// Wiersz „− wartość +” (makieta Licznik: cel, krok, próg).
import AppIcon from '@/components/icons/AppIcon.vue'

const model = defineModel<number>({ required: true })
const props = withDefaults(defineProps<{ label: string; noun: string; min?: number; max?: number; step?: number; suffix?: string; wide?: boolean }>(),
  { min: 1, max: 999, step: 1, suffix: '' })

function change(d: number) {
  model.value = Math.min(props.max, Math.max(props.min, model.value + d))
}
</script>

<template>
  <div class="row">
    <div class="label">{{ label }}</div>
    <div class="ctrl">
      <button type="button" class="btn" :aria-label="`Zmniejsz ${noun}`" :disabled="model <= min" @click="change(-step)">
        <AppIcon name="minus" :size="18" color="var(--text)" />
      </button>
      <div :class="['value display', { 'value--wide': wide }]" aria-live="polite">{{ model }}{{ suffix }}</div>
      <button type="button" class="btn" :aria-label="`Zwiększ ${noun}`" :disabled="model >= max" @click="change(step)">
        <AppIcon name="plus" :size="18" color="var(--text)" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.row { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 68px; padding: 8px 14px; }
.label { font-size: 15px; font-weight: 600; }
.ctrl { display: flex; align-items: center; gap: 10px; }
.btn { width: 44px; height: 44px; border-radius: 12px; border: 1px solid var(--border); background: var(--surface-2); display: flex; align-items: center; justify-content: center; }
.btn:disabled { opacity: 0.4; }
.value { min-width: 28px; text-align: center; font-size: 24px; font-weight: 700; }
.value--wide { min-width: 56px; }
</style>
