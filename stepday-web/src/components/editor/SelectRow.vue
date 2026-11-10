<script setup lang="ts" generic="T extends string | null">
// Wiersz w stylu NavRow z natywnym wyborem pod spodem (na iPhonie otwiera systemowy picker).
import { computed, useId } from 'vue'
import AppIcon from '@/components/icons/AppIcon.vue'

const model = defineModel<T>({ required: true })
const props = defineProps<{ label: string; options: { value: T; label: string }[] }>()
const id = useId()
const current = computed(() => props.options.find((o) => o.value === model.value)?.label ?? '')
</script>

<template>
  <div class="select-row">
    <span class="texts" aria-hidden="true">
      <span class="label">{{ label }}</span>
      <span class="value">{{ current }}</span>
    </span>
    <AppIcon name="chevron" :size="18" color="var(--icon-muted)" />
    <label :for="id" class="sr-only">{{ label }}</label>
    <select :id="id" v-model="model" class="native">
      <option v-for="o in options" :key="String(o.value)" :value="o.value">{{ o.label }}</option>
    </select>
  </div>
</template>

<style scoped>
.select-row {
  position: relative; box-sizing: border-box; display: flex; align-items: center; gap: 12px; min-height: 64px; padding: 10px 14px;
  border-radius: 16px; border: 1px solid var(--border); background: var(--surface);
}
.select-row:focus-within { outline: 3px solid var(--accent); outline-offset: 2px; }
.texts { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.label { font-size: 12px; font-weight: 500; color: var(--text-3); }
.value { font-size: 16px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.native { position: absolute; inset: 0; opacity: 0; width: 100%; height: 100%; font-size: 16px; }
</style>
