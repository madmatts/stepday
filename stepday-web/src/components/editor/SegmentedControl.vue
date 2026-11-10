<script setup lang="ts" generic="T extends string">
// Przełącznik dwóch opcji (makiety: „Rola w dniu”, „Jeśli nie zrobię w terminie”).
const model = defineModel<T>({ required: true })
defineProps<{ label: string; options: { value: T; label: string }[] }>()
</script>

<template>
  <div class="segmented" role="group" :aria-label="label">
    <button v-for="o in options" :key="o.value" type="button" :aria-pressed="model === o.value"
      :class="['seg', { 'seg--on': model === o.value }]" @click="model = o.value">{{ o.label }}</button>
  </div>
</template>

<style scoped>
.segmented { display: flex; gap: 4px; background: var(--muted-bg); border-radius: 14px; padding: 4px; }
.seg { flex: 1; min-height: 48px; border-radius: 11px; border: 0; background: transparent; font-size: 14px; font-weight: 600; color: var(--text-2); }
.seg--on { background: var(--surface); box-shadow: var(--shadow-segment); color: var(--text); }
</style>
