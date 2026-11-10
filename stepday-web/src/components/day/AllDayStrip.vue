<script setup lang="ts">
// Sekcja „W ciągu dnia” — bloki ALL_DAY obok ścieżki (makieta Main).
import type { BlockView } from '@/api/types'
import CounterRow from './CounterRow.vue'
import CompactStep from './CompactStep.vue'

defineProps<{ blocks: readonly BlockView[]; disabled?: boolean }>()
const emit = defineEmits<{ counter: [block: BlockView, delta: number]; open: [block: BlockView] }>()
</script>

<template>
  <section class="all-day card" aria-label="W ciągu dnia">
    <div class="row">
      <div class="section-label">W ciągu dnia</div>
      <div class="meta">poza ścieżką</div>
    </div>
    <template v-for="b in blocks" :key="b.id">
      <CounterRow v-if="b.type === 'COUNTER'" :block="b" :disabled="disabled" @change="emit('counter', b, $event)" />
      <CompactStep v-else :block="b" flat />
    </template>
  </section>
</template>

<style scoped>
.all-day { padding: 14px; display: flex; flex-direction: column; gap: 12px; }
.row { display: flex; justify-content: space-between; align-items: baseline; }
.meta { font-size: 12px; color: var(--text-3); }
</style>
