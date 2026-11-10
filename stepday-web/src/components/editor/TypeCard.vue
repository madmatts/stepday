<script setup lang="ts">
import type { BlockType } from '@/api/types'
import { TYPE_ICON } from '@/utils/blocks'
import AppIcon from '@/components/icons/AppIcon.vue'

defineProps<{ type: BlockType; title: string; text: string; tag?: string }>()
const emit = defineEmits<{ pick: [] }>()
</script>

<template>
  <button type="button" class="type-card" @click="emit('pick')">
    <span class="badge"><AppIcon :name="TYPE_ICON[type]" :size="22" color="var(--accent-ink)" /></span>
    <span class="texts">
      <span class="title">{{ title }}</span>
      <span class="text">{{ text }}</span>
      <span v-if="tag" class="chip chip--accent tag">{{ tag }}</span>
    </span>
    <AppIcon name="chevron" :size="18" color="var(--icon-muted)" />
  </button>
</template>

<style scoped>
.type-card { display: flex; align-items: center; gap: 14px; min-height: 88px; padding: 14px; border-radius: 18px; border: 1px solid var(--border); background: var(--surface); text-align: left; color: var(--text); width: 100%; box-sizing: border-box; }
.badge { width: 48px; height: 48px; border-radius: 14px; background: var(--accent-soft); display: flex; align-items: center; justify-content: center; flex: none; }
.texts { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.title { font-size: 16px; font-weight: 600; }
.text { font-size: 13px; line-height: 1.35; color: var(--text-3); }
.tag { align-self: flex-start; margin-top: 4px; padding: 3px 8px; }
</style>
