<script setup lang="ts">
// „Cel i krok” (makieta Licznik).
import { computed, useId } from 'vue'
import StepperRow from './StepperRow.vue'

const target = defineModel<number>('target', { required: true })
const unit = defineModel<string>('unit', { required: true })
const step = defineModel<number>('step', { required: true })
const thresholdPct = defineModel<number>('thresholdPct', { required: true })
const unitId = useId()

/** Tylko do opisu: wiążący próg liczy serwer (ceil). */
const threshold = computed(() => Math.ceil((target.value * thresholdPct.value) / 100))
</script>

<template>
  <section aria-label="Cel i krok" class="cfg">
    <div class="section-label">Cel i krok</div>
    <div class="card box">
      <StepperRow v-model="target" label="Cel dzienny" noun="cel" :max="100000" />
      <div class="row">
        <label :for="unitId" class="label">Jednostka</label>
        <input :id="unitId" v-model="unit" type="text" class="unit" maxlength="20" />
      </div>
      <StepperRow v-model="step" label="Jedno kliknięcie" noun="krok" :max="target" />
      <StepperRow v-model="thresholdPct" label="Zalicza dzień od" noun="próg" :min="10" :max="100" :step="10" suffix="%" wide />
    </div>
    <div class="hint pad">
      Przy celu {{ target }} to {{ threshold }}{{ unit ? ' ' + unit : '' }}. Poniżej 100% dzień się zalicza, ale z oznaczeniem, że czegoś brakowało.
    </div>
  </section>
</template>

<style scoped>
.cfg { display: flex; flex-direction: column; gap: 8px; }
.box { border-radius: 16px; display: flex; flex-direction: column; }
.box > * + * { border-top: 1px solid var(--divider); }
.row { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 68px; padding: 8px 14px; }
.label { font-size: 15px; font-weight: 600; }
.unit { width: 150px; height: 44px; border-radius: 12px; border: 1px solid var(--border); background: var(--surface-2); padding: 0 12px; font-size: 16px; text-align: right; }
.pad { padding: 0 2px; }
</style>
