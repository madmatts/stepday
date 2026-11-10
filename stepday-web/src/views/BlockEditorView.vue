<script setup lang="ts">
// Makiety „Lista” i „Licznik” (ten sam edytor, sekcje zależą od typu).
import { computed, onMounted, shallowRef, useId } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { api } from '@/api/client'
import type { BlockRole, BlockView, TemplateResponse } from '@/api/types'
import { useTemplateDraft } from '@/stores/templateDraft'
import { TYPE_ICON, TYPE_LABEL } from '@/utils/blocks'
import { describe, toRrule } from '@/utils/rrule'
import AppIcon from '@/components/icons/AppIcon.vue'
import ScreenHeader from '@/components/common/ScreenHeader.vue'
import SegmentedControl from '@/components/editor/SegmentedControl.vue'
import NavRow from '@/components/editor/NavRow.vue'
import SelectRow from '@/components/editor/SelectRow.vue'
import ReminderRow from '@/components/editor/ReminderRow.vue'
import ListItemsEditor from '@/components/editor/ListItemsEditor.vue'
import CounterConfig from '@/components/editor/CounterConfig.vue'
import StepperRow from '@/components/editor/StepperRow.vue'
import CounterRow from '@/components/day/CounterRow.vue'

const props = defineProps<{ id?: string }>()
const router = useRouter()
const store = useTemplateDraft()
const { draft, editingId, canSave } = storeToRefs(store)
const nameId = useId()
const error = shallowRef<string | null>(null)
const saving = shallowRef(false)
const pathTemplates = shallowRef<TemplateResponse[]>([])

onMounted(async () => {
  if (props.id && editingId.value !== props.id) {
    await store.load(props.id)
    await router.replace('/blocks/edit')
  }
  if (!draft.value.when) {
    await router.replace('/blocks/new')
    return
  }
  pathTemplates.value = (await api<TemplateResponse[]>('/templates'))
    .filter((t) => t.role === 'STEP' && t.id !== editingId.value)
})

const ROLE_OPTIONS: { value: BlockRole; label: string }[] = [
  { value: 'STEP', label: 'Krok w ścieżce' },
  { value: 'ALL_DAY', label: 'W ciągu dnia' },
]
const roleHint = computed(() => draft.value.type === 'COUNTER' && draft.value.role === 'ALL_DAY'
  ? 'Licznik czeka na ekranie dnia obok ścieżki i nie zatrzymuje kolejnych kroków. Próg powyżej decyduje, od kiedy liczy się do zaliczenia dnia.'
  : 'Krok pojawia się w ścieżce we właściwym miejscu i prowadzi dalej, gdy go zaliczysz. Blok „w ciągu dnia” czeka obok i nie blokuje ścieżki.')

const whenLabel = computed(() => {
  const w = draft.value.when
  if (!w) return ''
  const d = describe(toRrule(w))
  return d[0].toUpperCase() + d.slice(1)
})

const placeOptions = computed(() => [
  { value: null as string | null, label: editingId.value ? 'Bez zmian' : 'Na końcu ścieżki' },
  ...pathTemplates.value.map((t) => ({ value: t.id as string | null, label: `Po: ${t.name}` })),
])

/** Podgląd licznika „Tak będzie wyglądać na ekranie dnia”. */
const preview = computed<BlockView>(() => ({
  id: 'preview', name: draft.value.name || 'Licznik', type: 'COUNTER', role: draft.value.role, state: 'PENDING',
  completion: 'NONE', deferred: false, carryCount: 0, fromCalendar: false, items: null, value: 0,
  target: draft.value.target, threshold: Math.ceil((draft.value.target * draft.value.thresholdPct) / 100),
  unit: draft.value.unit, step: draft.value.step, minutes: null, timerStartedAt: null, timerStoppedAt: null, note: null,
}))

async function save() {
  saving.value = true
  error.value = null
  try {
    await store.save()
    await router.push('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Nie udało się zapisać'
  } finally {
    saving.value = false
  }
}

async function archive() {
  if (!window.confirm('Usunąć blok? Historia zostaje, blok zniknie z kolejnych dni.')) return
  await store.archive()
  await router.push('/layout')
}
</script>

<template>
  <main class="screen editor">
    <ScreenHeader :title="editingId ? 'Edytuj blok' : 'Nowy blok'" @back="router.back()" />

    <div class="type-row">
      <span class="chip chip--accent type-chip">
        <AppIcon :name="TYPE_ICON[draft.type]" :size="16" color="var(--accent-ink)" />
        {{ TYPE_LABEL[draft.type] }}
      </span>
      <RouterLink to="/blocks/new?change=1" class="change">Zmień typ</RouterLink>
    </div>

    <div class="field">
      <label :for="nameId" class="section-label">Nazwa</label>
      <input :id="nameId" v-model="draft.name" type="text" class="input" maxlength="80" required />
    </div>

    <ListItemsEditor v-if="draft.type === 'LIST'" v-model="draft.items" @add="draft.items.push(store.newItem())" />
    <CounterConfig v-else-if="draft.type === 'COUNTER'" v-model:target="draft.target" v-model:unit="draft.unit"
      v-model:step="draft.step" v-model:threshold-pct="draft.thresholdPct" />
    <section v-else-if="draft.type === 'TIMER'" aria-label="Czas" class="card timer-box">
      <StepperRow v-model="draft.minutes" label="Czas w minutach" noun="czas" :max="600" :step="5" :min="5" />
    </section>

    <section aria-label="Kiedy i gdzie" class="group">
      <template v-if="draft.type === 'COUNTER'">
        <div class="role">
          <div class="section-label">Rola w dniu</div>
          <SegmentedControl v-model="draft.role" label="Rola w dniu" :options="ROLE_OPTIONS" />
          <div class="hint">{{ roleHint }}</div>
        </div>
        <NavRow label="Kiedy" :value="whenLabel" @open="router.push('/blocks/edit/when')" />
      </template>
      <template v-else>
        <NavRow label="Kiedy" :value="whenLabel" @open="router.push('/blocks/edit/when')" />
        <div class="role">
          <div class="section-label">Rola w dniu</div>
          <SegmentedControl v-model="draft.role" label="Rola w dniu" :options="ROLE_OPTIONS" />
          <div class="hint">{{ roleHint }}</div>
        </div>
      </template>
      <SelectRow v-if="draft.role === 'STEP'" v-model="draft.afterTemplateId" label="Miejsce w ścieżce" :options="placeOptions" />
      <ReminderRow v-model:on="draft.reminderOn" v-model:time="draft.reminderTime" />
    </section>

    <section v-if="draft.type === 'COUNTER'" aria-label="Podgląd" class="group">
      <div class="section-label">Tak będzie wyglądać na ekranie dnia</div>
      <div class="card preview"><CounterRow :block="preview" preview /></div>
    </section>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <button type="button" class="btn-primary save" :disabled="!canSave || saving" @click="save">Zapisz blok</button>
    <button v-if="editingId" type="button" class="remove" @click="archive">Usuń blok</button>
  </main>
</template>

<style scoped>
.editor { gap: 18px; }
.type-row { display: flex; align-items: center; justify-content: space-between; }
.type-chip { gap: 8px; font-size: 13px; padding: 7px 12px; }
.change { min-height: 44px; padding: 0 8px; display: flex; align-items: center; font-size: 14px; font-weight: 600; color: var(--accent-ink); text-decoration: none; }
.field { display: flex; flex-direction: column; gap: 6px; }
.input { height: 52px; border-radius: 14px; border: 1px solid var(--border); background: var(--surface); padding: 0 14px; font-size: 16px; width: 100%; }
.group { display: flex; flex-direction: column; gap: 10px; }
.role { display: flex; flex-direction: column; gap: 8px; }
.timer-box { border-radius: 16px; }
.preview { padding: 14px; }
.save { margin-top: 4px; }
.remove { min-height: 44px; border: 0; background: transparent; color: var(--miss); font-size: 14px; font-weight: 600; }
.error { color: var(--miss); font-size: 14px; margin: 0; }
</style>
