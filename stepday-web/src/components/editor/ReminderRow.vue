<script setup lang="ts">
// „Przypomnienie” z przełącznikiem (makiety Lista i Licznik). Jedna godzina — decyzja D7.
import { useId } from 'vue'
import { isIos, isStandalone } from '@/composables/usePush'

const needsInstall = isIos && !isStandalone

const on = defineModel<boolean>('on', { required: true })
const time = defineModel<string>('time', { required: true })
const id = useId()
</script>

<template>
  <div class="reminder">
    <span class="texts">
      <label :for="id" class="label">Przypomnienie</label>
      <input v-if="on" :id="id" v-model="time" type="time" class="time" />
      <span v-else class="value">Wyłączone</span>
    </span>
    <button type="button" role="switch" :aria-checked="on" aria-label="Przypomnienie" class="switch"
      :class="{ 'switch--on': on }" @click="on = !on">
      <span class="knob" />
    </button>
  </div>
  <div v-if="on && needsInstall" class="hint">Przypomnienia zadziałają po dodaniu Stepday do ekranu początkowego (Safari → Udostępnij).</div>
</template>

<style scoped>
.reminder { box-sizing: border-box; display: flex; align-items: center; gap: 12px; min-height: 64px; padding: 10px 14px; border-radius: 16px; border: 1px solid var(--border); background: var(--surface); }
.texts { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.label { font-size: 12px; font-weight: 500; color: var(--text-3); }
.value, .time { font-size: 16px; font-weight: 600; }
.time { border: 0; background: transparent; padding: 0; min-height: 24px; }
.switch { width: 52px; height: 32px; border-radius: 16px; border: 0; background: var(--icon-muted); position: relative; padding: 0; flex: none; }
.switch--on { background: var(--accent); }
.knob { position: absolute; top: 4px; left: 4px; width: 24px; height: 24px; border-radius: 50%; background: #fff; transition: left 0.15s ease; }
.switch--on .knob { left: 24px; }
/* cel dotykowy 44 px wokół przełącznika 52×32 */
.switch::after { content: ''; position: absolute; inset: -6px; }
</style>
