<script setup lang="ts">
import type { BlockView } from '@/api/types'
import AppIcon from '@/components/icons/AppIcon.vue'
import CheckRow from './CheckRow.vue'

defineProps<{ block: BlockView; disabled?: boolean }>()
const emit = defineEmits<{ complete: [] }>()
</script>

<template>
  <div v-if="block.carryCount > 0" class="chips">
    <span class="chip chip--partial chip--lg">
      <AppIcon name="calendar" :size="14" color="var(--partial-ink)" />
      Zaległe · przeniesione {{ block.carryCount }}× z poprzedniego dnia
    </span>
  </div>
  <div v-else-if="block.fromCalendar" class="chips">
    <span class="chip chip--partial chip--lg">
      <AppIcon name="calendar" :size="14" color="var(--partial-ink)" />
      Z kalendarza · przypada dziś
    </span>
  </div>
  <CheckRow label="Zrobione" :checked="false" :disabled="disabled" strong @toggle="emit('complete')" />
</template>

<style scoped>
.chips { display: flex; }
.chip--lg { font-size: 12px; padding: 5px 10px; }
</style>
