<script setup lang="ts">
import type { ItemView } from '@/api/types'
import CheckRow from './CheckRow.vue'

defineProps<{ items: readonly ItemView[]; disabled?: boolean }>()
const emit = defineEmits<{ toggle: [itemId: string, checked: boolean] }>()
</script>

<template>
  <div class="items">
    <CheckRow v-for="i in items" :key="i.id" :label="i.label" :checked="i.checked" :disabled="disabled"
      @toggle="emit('toggle', i.id, !i.checked)" />
  </div>
</template>

<style scoped>
.items { display: flex; flex-direction: column; gap: 8px; }
</style>
