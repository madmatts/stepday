<script setup lang="ts">
// Nagłówek ekranów konfiguracji: „Wróć” + tytuł (+ nadtytuł / podtytuł) — makiety Typ, Lista, Kiedy, Układ.
import AppIcon from '@/components/icons/AppIcon.vue'

defineProps<{ title: string; overline?: string; subtitle?: string; closeLabel?: string }>()
const emit = defineEmits<{ back: [] }>()
</script>

<template>
  <header class="header">
    <button v-if="!closeLabel" type="button" class="icon-btn icon-btn--left" aria-label="Wróć" @click="emit('back')">
      <AppIcon name="back" :size="22" color="var(--text)" />
    </button>
    <div class="titles">
      <div v-if="overline" class="sub">{{ overline }}</div>
      <h1 class="title display">{{ title }}</h1>
      <div v-if="subtitle" class="sub">{{ subtitle }}</div>
    </div>
    <button v-if="closeLabel" type="button" class="icon-btn icon-btn--right" :aria-label="closeLabel" @click="emit('back')">
      <AppIcon name="close" :size="20" color="var(--text)" />
    </button>
  </header>
</template>

<style scoped>
.header { display: flex; align-items: center; gap: 8px; min-height: 48px; }
.icon-btn { width: 44px; height: 44px; border-radius: 12px; border: 0; background: transparent; display: flex; align-items: center; justify-content: center; flex: none; }
.icon-btn--left { margin-left: -8px; }
.icon-btn--right { margin-right: -8px; }
.titles { flex: 1; min-width: 0; }
.title { margin: 0; font-size: 24px; font-weight: 700; line-height: 1.1; }
.sub { font-size: 12px; color: var(--text-3); font-weight: 500; }
</style>
