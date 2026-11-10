<script setup lang="ts">
// Aktywny krok „TERAZ · …” (makieta Main). Treść zależy od typu bloku.
import { computed } from 'vue'
import type { BlockView } from '@/api/types'
import { blockIcon, TYPE_LABEL } from '@/utils/blocks'
import AppIcon from '@/components/icons/AppIcon.vue'
import ListStep from './ListStep.vue'
import BooleanStep from './BooleanStep.vue'
import TimerStep from './TimerStep.vue'
import NoteStep from './NoteStep.vue'
import StepActions from './StepActions.vue'

const props = defineProps<{ block: BlockView; disabled?: boolean }>()
const emit = defineEmits<{
  toggleItem: [itemId: string, checked: boolean]
  complete: []
  finishNote: [text: string]
  startTimer: []
  stopTimer: []
  skip: []
  defer: []
}>()

const label = computed(() => `TERAZ · ${TYPE_LABEL[props.block.type].toUpperCase()}`)
const listCount = computed(() => {
  const items = props.block.items ?? []
  return `${items.filter((i) => i.checked).length}/${items.length}`
})
</script>

<template>
  <section class="active" :aria-label="block.name">
    <div class="head">
      <div class="badge"><AppIcon :name="blockIcon(block.type, block.fromCalendar)" color="#fff" /></div>
      <div class="titles">
        <div class="label">{{ label }}</div>
        <h2 class="title display">{{ block.name }}</h2>
      </div>
      <div v-if="block.type === 'LIST'" class="count">{{ listCount }}</div>
    </div>

    <ListStep v-if="block.type === 'LIST'" :items="block.items ?? []" :disabled="disabled"
      @toggle="(id, checked) => emit('toggleItem', id, checked)" />
    <BooleanStep v-else-if="block.type === 'BOOLEAN'" :block="block" :disabled="disabled" @complete="emit('complete')" />
    <TimerStep v-else-if="block.type === 'TIMER'" :block="block" :disabled="disabled"
      @start="emit('startTimer')" @stop="emit('stopTimer')" />
    <NoteStep v-else-if="block.type === 'NOTE'" :block="block" :disabled="disabled" @finish="emit('finishNote', $event)" />

    <StepActions :disabled="disabled" :show-hint="block.type === 'LIST'" @skip="emit('skip')" @defer="emit('defer')" />
  </section>
</template>

<style scoped>
.active {
  background: var(--surface); border: 2px solid var(--accent); border-radius: 20px; padding: 16px;
  box-shadow: var(--shadow-active); display: flex; flex-direction: column; gap: 14px;
}
.head { display: flex; align-items: center; gap: 12px; }
.badge { width: 40px; height: 40px; border-radius: 50%; background: var(--accent); display: flex; align-items: center; justify-content: center; flex: none; }
.titles { flex: 1; min-width: 0; }
.label { font-size: 11px; font-weight: 600; letter-spacing: 0.06em; color: var(--accent-ink); }
.title { margin: 0; font-size: 22px; font-weight: 700; line-height: 1.15; }
.count { font-size: 14px; font-weight: 600; color: var(--text-3); }
</style>
