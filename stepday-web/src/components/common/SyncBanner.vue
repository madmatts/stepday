<script setup lang="ts">
// Stan synchronizacji: zmiany czekające w kolejce offline i zmiany odrzucone przez serwer (np. dzień zamknięty).
import { onBeforeUnmount, shallowRef } from 'vue'
import { onSyncState } from '@/sync/outbox'

const pending = shallowRef(0)
const rejected = shallowRef<string[]>([])
const stop = onSyncState((s) => { pending.value = s.pending; rejected.value = s.rejected })
onBeforeUnmount(stop)
const dismissed = shallowRef(0)
</script>

<template>
  <div v-if="pending > 0" class="sync" role="status">
    Bez sieci. {{ pending }} {{ pending === 1 ? 'zmiana czeka' : 'zmiany czekają' }} na wysłanie — zapiszemy je po powrocie zasięgu.
  </div>
  <div v-if="rejected.length > dismissed" class="sync sync--error" role="alert">
    Nie wszystko się zapisało: {{ rejected[rejected.length - 1] }}
    <button type="button" class="btn-secondary" @click="dismissed = rejected.length">OK</button>
  </div>
</template>

<style scoped>
.sync { margin-bottom: 16px; padding: 10px 14px; border-radius: 14px; background: var(--partial-soft); color: var(--partial-ink); font-size: 13px; line-height: 1.4; display: flex; align-items: center; gap: 10px; }
.sync--error { background: var(--miss-soft); color: var(--miss); }
</style>
