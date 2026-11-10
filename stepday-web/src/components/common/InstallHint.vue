<script setup lang="ts">
// Web Push działa na iPhonie tylko w PWA dodanej do ekranu początkowego (iOS 16.4+).
// Pokazujemy to raz, dopóki aplikacja nie jest zainstalowana (dokument, sekcja 8).
import { computed, shallowRef } from 'vue'

const KEY = 'stepday.installHintDismissed'
const standalone = window.matchMedia?.('(display-mode: standalone)').matches
  || (navigator as Navigator & { standalone?: boolean }).standalone === true
const ios = /iPhone|iPad|iPod/.test(navigator.userAgent)

function dismissedBefore(): boolean {
  try { return localStorage.getItem(KEY) === '1' } catch { return false }
}

const dismissed = shallowRef(dismissedBefore())
const visible = computed(() => ios && !standalone && !dismissed.value)

function dismiss() {
  dismissed.value = true
  try { localStorage.setItem(KEY, '1') } catch { /* tryb prywatny */ }
}
</script>

<template>
  <aside v-if="visible" class="install" aria-label="Instalacja aplikacji">
    <div class="text">
      <strong>Dodaj Stepday do ekranu początkowego</strong>
      W Safari: Udostępnij → „Do ekranu początkowego”. Tylko wtedy działają przypomnienia.
    </div>
    <button type="button" class="btn-secondary" @click="dismiss">OK</button>
  </aside>
</template>

<style scoped>
.install {
  display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 12px 14px;
  border-radius: 16px; background: var(--accent-soft); color: var(--accent-ink); font-size: 13px; line-height: 1.4;
}
.text { flex: 1; }
.text strong { display: block; font-size: 14px; }
</style>
