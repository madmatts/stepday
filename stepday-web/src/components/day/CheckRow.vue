<script setup lang="ts">
// Wiersz do odhaczenia (pozycja listy, „Zrobione” w tak/nie) — makieta Main.
import AppIcon from '@/components/icons/AppIcon.vue'

defineProps<{ label: string; checked: boolean; disabled?: boolean; strong?: boolean }>()
const emit = defineEmits<{ toggle: [] }>()
</script>

<template>
  <button type="button" :class="['check-row', { 'check-row--strong': strong }]" :aria-pressed="checked"
    :disabled="disabled" @click="emit('toggle')">
    <span v-if="checked" class="box box--on"><AppIcon name="check" :size="16" color="#fff" :stroke-width="3" /></span>
    <span v-else class="box" />
    <span class="label">{{ label }}</span>
  </button>
</template>

<style scoped>
.check-row {
  display: flex; align-items: center; gap: 12px; min-height: 52px; padding: 0 14px; border-radius: 14px;
  border: 1px solid var(--border); background: var(--surface-2); text-align: left; font-size: 16px; font-weight: 500;
  color: var(--text);
}
.check-row--strong { min-height: 56px; font-weight: 600; }
.box { width: 24px; height: 24px; border-radius: 7px; border: 2px solid var(--icon-muted); background: var(--surface); flex: none; display: flex; align-items: center; justify-content: center; }
.box--on { background: var(--ok); border: 0; }
.label { flex: 1; }
</style>
