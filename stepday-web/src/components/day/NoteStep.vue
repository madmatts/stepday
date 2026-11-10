<script setup lang="ts">
import { computed, shallowRef, useId } from 'vue'
import type { BlockView } from '@/api/types'

const props = defineProps<{ block: BlockView; disabled?: boolean }>()
const emit = defineEmits<{ finish: [text: string] }>()

const text = shallowRef(props.block.note ?? '')
const id = useId()
const canFinish = computed(() => text.value.trim().length > 0)
</script>

<template>
  <label :for="id" class="label">Jedno zdanie o dzisiejszym dniu</label>
  <textarea :id="id" v-model="text" rows="3" class="area" maxlength="500" />
  <button type="button" class="btn-primary done" :disabled="disabled || !canFinish" @click="emit('finish', text.trim())">Gotowe</button>
</template>

<style scoped>
.label { font-size: 14px; font-weight: 500; }
.area {
  width: 100%; border-radius: 14px; border: 1px solid var(--border); background: var(--surface-2);
  padding: 12px 14px; font-size: 16px; resize: none; box-sizing: border-box;
}
.done { min-height: 52px; border-radius: 14px; }
</style>
