<script setup lang="ts">
defineProps<{ disabled?: boolean; showHint?: boolean }>()
const emit = defineEmits<{ skip: []; defer: [] }>()
</script>

<template>
  <div class="actions">
    <button type="button" class="btn-secondary" :disabled="disabled" @click="emit('defer')">Później</button>
    <button type="button" class="btn-secondary" :disabled="disabled" @click="emit('skip')">Pomiń</button>
    <div v-if="showHint" class="hint">Bez kary, seria zostaje</div>
  </div>
</template>

<style scoped>
.actions { display: flex; align-items: center; gap: 8px; }
.hint { flex: 1; text-align: right; }
</style>
