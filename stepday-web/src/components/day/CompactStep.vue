<script setup lang="ts">
// Zwinięty blok ścieżki (makieta Main: zrobione, nadchodzące, pominięte, na później).
import { computed } from 'vue'
import type { BlockView } from '@/api/types'
import { blockIcon } from '@/utils/blocks'
import AppIcon from '@/components/icons/AppIcon.vue'
import { statusText, subtitle } from './stepText'

const props = defineProps<{ block: BlockView; flat?: boolean }>()
const status = computed(() => statusText(props.block))
const done = computed(() => props.block.state === 'DONE')
</script>

<template>
  <section :class="['compact', { 'compact--flat': flat }]" :aria-label="block.name">
    <div :class="['badge', { 'badge--done': done }]">
      <AppIcon v-if="done" name="check" color="var(--ok)" />
      <AppIcon v-else :name="blockIcon(block.type, block.fromCalendar)" color="var(--text-2)" />
    </div>
    <div class="body">
      <div class="name">{{ block.name }}</div>
      <div class="sub">{{ subtitle(block) }}</div>
    </div>
    <div v-if="status" :class="['status', `status--${status.tone}`]">{{ status.text }}</div>
  </section>
</template>

<style scoped>
.compact {
  background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 14px;
  display: flex; align-items: center; gap: 12px;
}
.compact--flat { border: 0; padding: 0; }
.badge { width: 40px; height: 40px; border-radius: 50%; background: var(--muted-bg); display: flex; align-items: center; justify-content: center; flex: none; }
.badge--done { background: var(--ok-soft); }
.body { flex: 1; min-width: 0; }
.name { font-size: 15px; font-weight: 600; }
.sub { font-size: 12px; color: var(--text-3); }
.status { font-size: 12px; font-weight: 600; color: var(--text-2); }
.status--ok { color: var(--ok); }
.status--miss { color: var(--miss); }
</style>
