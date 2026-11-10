<script setup lang="ts">
// Prośba o zgodę na powiadomienia — tylko po kliknięciu (iOS wymaga gestu) i tylko tam, gdzie push dotrze.
import { computed, shallowRef } from 'vue'
import { pushPossible, usePush } from '@/composables/usePush'

const KEY = 'stepday.reminderSetupDismissed'
const { permission, error, enable } = usePush()
const dismissed = shallowRef((() => { try { return localStorage.getItem(KEY) === '1' } catch { return false } })())
const visible = computed(() => pushPossible && permission.value === 'default' && !dismissed.value)

function later() {
  dismissed.value = true
  try { localStorage.setItem(KEY, '1') } catch { /* tryb prywatny */ }
}
</script>

<template>
  <aside v-if="visible" class="setup" aria-label="Przypomnienia">
    <div class="text">
      <strong>Przypomnienia</strong>
      Stepday może przypomnieć o kroku o ustawionej godzinie.
      <span v-if="error" class="err">{{ error }}</span>
    </div>
    <div class="actions">
      <button type="button" class="btn-secondary on" @click="enable">Włącz</button>
      <button type="button" class="btn-secondary" @click="later">Nie teraz</button>
    </div>
  </aside>
</template>

<style scoped>
.setup { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; padding: 12px 14px; border-radius: 16px; background: var(--accent-soft); color: var(--accent-ink); font-size: 13px; line-height: 1.4; }
.text strong { display: block; font-size: 14px; }
.err { display: block; color: var(--miss); }
.actions { display: flex; gap: 8px; }
.on { background: var(--accent); color: #fff; border-color: var(--accent); }
</style>
