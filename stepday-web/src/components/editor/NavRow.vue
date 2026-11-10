<script setup lang="ts">
// Wiersz „etykieta / wartość ›” (makiety: Kiedy, Miejsce w ścieżce).
import AppIcon from '@/components/icons/AppIcon.vue'

defineProps<{ label: string; value: string }>()
const emit = defineEmits<{ open: [] }>()
</script>

<template>
  <button type="button" class="nav-row" @click="emit('open')">
    <span class="texts">
      <span class="label">{{ label }}</span>
      <span class="value">{{ value }}</span>
    </span>
    <AppIcon name="chevron" :size="18" color="var(--icon-muted)" />
  </button>
</template>

<style scoped>
.nav-row {
  display: flex; align-items: center; gap: 12px; min-height: 64px; padding: 10px 14px; border-radius: 16px;
  border: 1px solid var(--border); background: var(--surface); text-align: left; color: var(--text); width: 100%; box-sizing: border-box;
}
.texts { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.label { font-size: 12px; font-weight: 500; color: var(--text-3); }
.value { font-size: 16px; font-weight: 600; }
</style>
