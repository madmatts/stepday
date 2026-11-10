<script setup lang="ts">
// Podsumowanie tygodnia: SUCCESS (makieta „Sukces”) albo GAPS (makieta „Braki”). Wynik liczy serwer.
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWeekSummary } from '@/composables/useWeekSummary'
import SuccessState from '@/components/week/SuccessState.vue'
import GapsState from '@/components/week/GapsState.vue'

const route = useRoute()
const router = useRouter()
const start = computed(() => route.params.start as string)
const { summary, error } = useWeekSummary(start)
const close = () => router.push('/')
</script>

<template>
  <main :class="['screen', 'week', { 'week--success': summary?.outcome === 'SUCCESS' }]">
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <SuccessState v-if="summary?.outcome === 'SUCCESS'" :summary="summary" @close="close" />
    <GapsState v-else-if="summary" :summary="summary" @close="close" />
  </main>
</template>

<style scoped>
.week--success { height: 100dvh; min-height: 0; }
.error { color: var(--miss); }
</style>
