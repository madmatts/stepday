<script setup lang="ts">
// Widok dnia (makieta Main): nagłówek, plansza, „W ciągu dnia”, ścieżka kroków, koniec dnia.
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useDay } from '@/composables/useDay'
import { dayMonth, mondayOf, addDays, weekday } from '@/utils/dates'
import AppIcon from '@/components/icons/AppIcon.vue'
import BoardWeekStrip from '@/components/day/BoardWeekStrip.vue'
import AllDayStrip from '@/components/day/AllDayStrip.vue'
import ActiveStep from '@/components/day/ActiveStep.vue'
import CompactStep from '@/components/day/CompactStep.vue'
import PathArrow from '@/components/day/PathArrow.vue'
import EndOfDay from '@/components/day/EndOfDay.vue'
import InstallHint from '@/components/common/InstallHint.vue'
import SyncBanner from '@/components/common/SyncBanner.vue'

const route = useRoute()
const dateParam = computed(() => (route.params.date as string | undefined) ?? null)
const { day, error, busy, currentStepId, ...act } = useDay(dateParam)

const readonlyDay = computed(() => day.value?.closed ?? false)
const previousWeek = computed(() => day.value ? addDays(mondayOf(day.value.date), -7) : null)
</script>

<template>
  <main class="screen day">
    <div class="brand-row">
      <div class="brand display">Stepday</div>
      <nav class="nav" aria-label="Nawigacja">
        <RouterLink class="nav-btn" to="/layout" aria-label="Układ dnia">
          <AppIcon name="list" color="var(--accent-ink)" />
        </RouterLink>
        <RouterLink v-if="previousWeek" class="nav-btn" :to="`/week/${previousWeek}`" aria-label="Podsumowanie tygodnia">
          <AppIcon name="calendar" color="var(--accent-ink)" />
        </RouterLink>
      </nav>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <template v-if="day">
      <header class="head">
        <div>
          <div class="weekday">{{ weekday(day.date) }}</div>
          <h1 class="date display">{{ dayMonth(day.date) }}</h1>
        </div>
        <div class="progress">
          <div class="progress-num display">{{ day.progress.done }}/{{ day.progress.total }}</div>
          <div class="progress-label">kroków dziś</div>
        </div>
      </header>

      <InstallHint />
      <SyncBanner />

      <BoardWeekStrip :week="day.week" :board="day.board" :progress="day.progress" :selected-date="day.date" />

      <AllDayStrip v-if="day.allDay.length" :blocks="day.allDay" :disabled="busy || readonlyDay"
        @counter="(b, delta) => act.counter(b, delta)" />

      <div class="path">
        <template v-for="s in day.steps" :key="s.id">
          <ActiveStep v-if="s.id === currentStepId && !readonlyDay" :block="s" :disabled="busy"
            @toggle-item="(id, checked) => act.toggleItem(s, id, checked)"
            @complete="act.complete(s)"
            @finish-note="(text) => act.finishNote(s, text)"
            @start-timer="act.startTimer(s)"
            @stop-timer="act.stopTimer(s)"
            @skip="act.skip(s)"
            @defer="act.defer(s)" />
          <CompactStep v-else :block="s" />
          <PathArrow />
        </template>
        <EndOfDay v-if="day.steps.length || day.allDay.length" :day="day" />
      </div>

      <RouterLink v-if="!day.steps.length && !day.allDay.length" to="/blocks/new" class="btn-primary empty-cta">
        <AppIcon name="plus" color="#fff" /> Zaprojektuj pierwszy rytuał
      </RouterLink>
    </template>
  </main>
</template>

<style scoped>
.day { padding-top: 24px; padding-bottom: 40px; gap: 0; }
.brand-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; height: 20px; }
.brand { font-size: 15px; font-weight: 700; color: var(--accent-ink); letter-spacing: 0.01em; }
.nav { display: flex; gap: 4px; margin-right: -10px; }
.nav-btn { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; border-radius: 12px; }
.head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.weekday { font-size: 13px; font-weight: 500; color: var(--text-3); }
.date { margin: 0; font-size: 32px; line-height: 1.1; font-weight: 700; }
.progress { text-align: right; }
.progress-num { font-size: 24px; font-weight: 700; line-height: 1.1; }
.progress-label { font-size: 12px; color: var(--text-3); }
.day > :deep(.board), .day > :deep(.all-day) { margin-bottom: 20px; }
.path { display: flex; flex-direction: column; }
.error { background: var(--miss-soft); color: var(--miss); border-radius: 12px; padding: 10px 12px; font-size: 14px; }
.empty-cta { margin-top: 20px; text-decoration: none; }
</style>
