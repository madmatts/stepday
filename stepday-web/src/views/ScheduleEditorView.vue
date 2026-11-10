<script setup lang="ts">
// Makieta „Kiedy”: reguła powtarzania + polityka zaległych. Terminy liczy backend (/schedule/preview).
import { computed, shallowRef, useId, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { api } from '@/api/client'
import type { OverduePolicy, PreviewResponse } from '@/api/types'
import { useTemplateDraft } from '@/stores/templateDraft'
import { toRrule, WEEKDAY_CODES, WEEKDAY_SHORT, type When, type WhenKind, type EveryUnit, type WeekdayCode } from '@/utils/rrule'
import { longDate, shortWeekdayLongDate } from '@/utils/dates'
import ScreenHeader from '@/components/common/ScreenHeader.vue'
import SegmentedControl from '@/components/editor/SegmentedControl.vue'

const router = useRouter()
const store = useTemplateDraft()
const { draft } = storeToRefs(store)
if (!draft.value.when) void router.replace('/blocks/new')

const when = computed(() => draft.value.when as When)
const uid = useId()

const KINDS: { kind: WhenKind; label: string }[] = [
  { kind: 'DAILY', label: 'Codziennie' },
  { kind: 'WEEKDAYS', label: 'Wybrane dni tygodnia' },
  { kind: 'EVERY', label: 'Co jakiś czas' },
  { kind: 'MONTH_DAY', label: 'Raz w miesiącu, w wybrany dzień' },
  { kind: 'ONCE', label: 'Jednorazowo' },
]
const UNITS: { value: EveryUnit; label: string }[] = [
  { value: 'DAILY', label: 'dni' },
  { value: 'WEEKLY', label: 'tygodni' },
  { value: 'MONTHLY', label: 'miesięcy' },
]
const POLICIES: { value: OverduePolicy; label: string }[] = [
  { value: 'CARRY_OVER', label: 'Zostaje do zrobienia' },
  { value: 'DROP', label: 'Zostaje niezaliczony' },
]

function toggleWeekday(code: WeekdayCode) {
  const set = new Set(when.value.weekdays)
  if (set.has(code)) {
    if (set.size > 1) set.delete(code)
  } else {
    set.add(code)
  }
  when.value.weekdays = WEEKDAY_CODES.filter((c) => set.has(c))
}

const preview = shallowRef<PreviewResponse | null>(null)
/** Polityka wybrana ręcznie wygrywa; inaczej domyślna z serwera dla danej reguły (A11). */
const policyTouched = shallowRef(draft.value.overduePolicy !== null && store.editingId !== null)
const policy = computed<OverduePolicy>({
  get: () => draft.value.overduePolicy ?? preview.value?.defaultOverduePolicy ?? 'DROP',
  set: (v) => { policyTouched.value = true; draft.value.overduePolicy = v },
})

watch(() => [toRrule(when.value), when.value.dtstart] as const, async ([rrule, dtstart], _old, onCleanup) => {
  let cancelled = false
  onCleanup(() => { cancelled = true })
  try {
    const res = await api<PreviewResponse>('/schedule/preview', { method: 'POST', body: { rrule, dtstart, count: 3 } })
    if (cancelled) return
    preview.value = res
    if (!policyTouched.value) draft.value.overduePolicy = res.defaultOverduePolicy
  } catch {
    if (!cancelled) preview.value = null
  }
}, { immediate: true })

const needsStart = computed(() => when.value.kind === 'EVERY' || when.value.kind === 'ONCE')
</script>

<template>
  <main class="screen when">
    <ScreenHeader title="Kiedy" :overline="draft.name || undefined" @back="router.back()" />

    <fieldset class="options">
      <legend class="sr-only">Jak często blok się powtarza</legend>
      <div v-for="k in KINDS" :key="k.kind" :class="['option', { 'option--on': when.kind === k.kind }]">
        <div class="option-head">
          <input :id="`${uid}-${k.kind}`" v-model="when.kind" type="radio" :value="k.kind" :name="`${uid}-when`" class="radio" />
          <label :for="`${uid}-${k.kind}`" class="option-label">{{ k.label }}</label>
        </div>

        <div v-if="when.kind === k.kind && k.kind !== 'DAILY'" class="option-body">
          <div v-if="k.kind === 'WEEKDAYS'" class="weekdays" role="group" aria-label="Dni tygodnia">
            <button v-for="c in WEEKDAY_CODES" :key="c" type="button" :aria-pressed="when.weekdays.includes(c)"
              :class="['wd', { 'wd--on': when.weekdays.includes(c) }]" @click="toggleWeekday(c)">{{ WEEKDAY_SHORT[c] }}</button>
          </div>

          <div v-if="k.kind === 'EVERY'" class="every">
            <label :for="`${uid}-n`" class="every-label">Powtarzaj co</label>
            <input :id="`${uid}-n`" v-model.number="when.every" type="number" min="1" max="99" inputmode="numeric" class="num" />
            <select v-model="when.unit" aria-label="Jednostka okresu" class="unit">
              <option v-for="u in UNITS" :key="u.value" :value="u.value">{{ u.label }}</option>
            </select>
          </div>

          <div v-if="k.kind === 'MONTH_DAY'" class="every">
            <label :for="`${uid}-md`" class="every-label">Dzień miesiąca</label>
            <select :id="`${uid}-md`" v-model.number="when.monthDay" class="unit">
              <option v-for="d in 31" :key="d" :value="d">{{ d }}.</option>
              <option :value="-1">ostatni</option>
            </select>
          </div>

          <div v-if="needsStart" class="field">
            <label :for="`${uid}-from`" class="section-label">{{ k.kind === 'ONCE' ? 'Którego dnia' : 'Od dnia' }}</label>
            <div class="date">
              <span aria-hidden="true">{{ when.dtstart ? longDate(when.dtstart) : '' }}</span>
              <input :id="`${uid}-from`" v-model="when.dtstart" type="date" class="date-native" required />
            </div>
          </div>

          <div class="field">
            <div class="section-label">Najbliższe terminy</div>
            <div v-for="o in preview?.dates ?? []" :key="o.date" :class="['occ', { 'occ--today': o.today }]">
              <span>{{ shortWeekdayLongDate(o.date) }}</span>
              <span v-if="o.today" class="occ-today">dziś</span>
            </div>
          </div>
        </div>
      </div>
    </fieldset>

    <section aria-label="Jeśli nie zrobię w terminie" class="policy">
      <div class="section-label">Jeśli nie zrobię w terminie</div>
      <SegmentedControl v-model="policy" label="Jeśli nie zrobię w terminie" :options="POLICIES" />
      <div class="hint">
        <template v-if="policy === 'CARRY_OVER'">Zaległy blok przechodzi na kolejny dzień, najwyżej 2 razy. Potem zostaje w historii jako niezaliczony. Wpis nigdy nie znika.</template>
        <template v-else>Niezrobiony blok zostaje w swoim dniu jako niezaliczony i widać to w podsumowaniu tygodnia. Wpis nigdy nie znika.</template>
      </div>
    </section>

    <button type="button" class="btn-primary" @click="router.back()">Gotowe</button>
  </main>
</template>

<style scoped>
.when { gap: 14px; }
.options { border: 0; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.option { border-radius: 16px; border: 1px solid var(--border); background: var(--surface); padding: 0 14px; }
.option--on { border: 2px solid var(--accent); box-shadow: 0 8px 22px rgba(45, 91, 255, 0.12); padding-bottom: 16px; }
.option:not(.option--on) .option-head { min-height: 60px; }
.option-head { min-height: 58px; display: flex; align-items: center; gap: 12px; }
.radio { width: 22px; height: 22px; margin: 0; accent-color: var(--accent); flex: none; }
.option-label { flex: 1; min-height: 58px; display: flex; align-items: center; font-size: 16px; font-weight: 600; }
.option-body { display: flex; flex-direction: column; gap: 14px; }
.every { display: flex; align-items: center; gap: 10px; }
.every-label { font-size: 15px; font-weight: 500; }
.num { width: 64px; height: 48px; border-radius: 12px; border: 1px solid var(--border); background: var(--surface-2); padding: 0 8px; font-size: 18px; font-weight: 600; text-align: center; }
.unit { flex: 1; min-width: 0; height: 48px; border-radius: 12px; border: 1px solid var(--border); background: var(--surface-2); padding: 0 10px; font-size: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.date { position: relative; height: 48px; border-radius: 12px; border: 1px solid var(--border); background: var(--surface-2); padding: 0 12px; font-size: 16px; display: flex; align-items: center; box-sizing: border-box; }
.date:focus-within { outline: 3px solid var(--accent); outline-offset: 2px; }
.date-native { position: absolute; inset: 0; opacity: 0; width: 100%; height: 100%; font-size: 16px; }
.occ { display: flex; align-items: center; justify-content: space-between; min-height: 44px; padding: 0 12px; border-radius: 12px; background: var(--bg); font-size: 14px; font-weight: 500; }
.occ--today { background: var(--accent-soft); color: var(--accent-ink); font-weight: 600; }
.occ-today { font-size: 12px; font-weight: 600; }
.weekdays { display: flex; gap: 6px; }
.wd { flex: 1; min-height: 44px; border-radius: 12px; border: 1px solid var(--border); background: var(--surface-2); font-size: 13px; font-weight: 600; color: var(--text-2); }
.wd--on { background: var(--accent); border-color: var(--accent); color: #fff; }
.policy { display: flex; flex-direction: column; gap: 8px; }
</style>
