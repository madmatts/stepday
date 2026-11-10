<script setup lang="ts">
// Timer (makieta Main): Start → odliczanie → „Zakończ”. Po dojściu do zera kończy się sam (D6).
import { computed, onBeforeUnmount, shallowRef, watch } from 'vue'
import type { BlockView } from '@/api/types'

const props = defineProps<{ block: BlockView; disabled?: boolean }>()
const emit = defineEmits<{ start: []; stop: [] }>()

const now = shallowRef(Date.now())
let handle: number | undefined
const running = computed(() => props.block.timerStartedAt !== null && props.block.timerStoppedAt === null)

watch(running, (on) => {
  window.clearInterval(handle)
  if (on) handle = window.setInterval(() => { now.value = Date.now() }, 1000)
}, { immediate: true })
onBeforeUnmount(() => window.clearInterval(handle))

const totalMs = computed(() => (props.block.minutes ?? 0) * 60_000)
const remainingMs = computed(() => {
  if (!props.block.timerStartedAt) return totalMs.value
  return Math.max(0, totalMs.value - (now.value - Date.parse(props.block.timerStartedAt)))
})
const clock = computed(() => {
  const s = Math.ceil(remainingMs.value / 1000)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})

watch(remainingMs, (ms) => {
  if (running.value && ms === 0 && !props.disabled) emit('stop')
})
</script>

<template>
  <div class="timer">
    <div class="clock display" role="timer" :aria-label="`Zostało ${clock}`">{{ clock }}</div>
    <div class="hint">{{ running ? 'W trakcie' : 'Minut do końca' }}</div>
  </div>
  <button v-if="!running" type="button" class="go go--start" :disabled="disabled" @click="emit('start')">Start</button>
  <button v-else type="button" class="go go--stop" :disabled="disabled" @click="emit('stop')">Zakończ {{ block.name.toLowerCase() }}</button>
</template>

<style scoped>
.timer { text-align: center; }
.clock { font-size: 48px; font-weight: 700; line-height: 1; font-variant-numeric: tabular-nums; }
.hint { margin-top: 6px; }
.go { min-height: 52px; border-radius: 14px; border: 0; color: #fff; font-size: 16px; font-weight: 600; }
.go--start { background: var(--accent); }
.go--stop { background: var(--ok); }
</style>
