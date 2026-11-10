<script setup lang="ts">
// Makieta „Typ”: wybór typu bloku i gotowe szablony.
import { useRouter } from 'vue-router'
import type { BlockType } from '@/api/types'
import { useTemplateDraft } from '@/stores/templateDraft'
import { fetchToday } from '@/composables/useToday'
import ScreenHeader from '@/components/common/ScreenHeader.vue'
import TypeCard from '@/components/editor/TypeCard.vue'

const props = defineProps<{ change?: boolean }>()
const router = useRouter()
const store = useTemplateDraft()

const TYPES: { type: BlockType; title: string; text: string; tag?: string }[] = [
  { type: 'LIST', title: 'Lista', text: 'Kilka rzeczy, każda odhaczana osobno. Np. tabletki, zakupy.' },
  { type: 'BOOLEAN', title: 'Tak / nie', text: 'Jedna czynność, zrobiona albo nie. Np. wymiana filtra.' },
  { type: 'COUNTER', title: 'Licznik', text: 'Zbierasz do celu, po trochu. Np. woda, kroki.', tag: 'zwykle w ciągu dnia' },
  { type: 'TIMER', title: 'Timer', text: 'Czynność, która ma trwać określony czas. Np. spacer, nauka.' },
  { type: 'NOTE', title: 'Notatka', text: 'Krótki tekst. Np. jedno zdanie na koniec dnia.' },
]

// Treść gotowych szablonów poda właściciel (decyzja D8) — do tego czasu przyciski są nieaktywne.
const PRESETS = ['Poranek', 'Wieczór', 'Dzień roboczy']

async function pick(type: BlockType) {
  if (props.change || router.currentRoute.value.query.change) {
    store.changeType(type)
  } else {
    store.start(type, await fetchToday())
  }
  await router.push('/blocks/edit')
}
</script>

<template>
  <main class="screen picker">
    <ScreenHeader title="Nowy blok" @back="router.back()" />
    <p class="lead">Jak chcesz to zaliczać? Typ decyduje, co zobaczysz, gdy blok przyjdzie na ciebie.</p>
    <div class="types">
      <TypeCard v-for="t in TYPES" :key="t.type" v-bind="t" @pick="pick(t.type)" />
    </div>
    <section aria-label="Szablony" class="presets">
      <div class="section-label">Albo zacznij od gotowego szablonu</div>
      <div class="chips">
        <button v-for="p in PRESETS" :key="p" type="button" class="preset" disabled
          :aria-label="`${p} — wkrótce`" title="Wkrótce">{{ p }}</button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.picker { gap: 16px; }
.lead { margin: 0; font-size: 16px; line-height: 1.4; color: var(--text-2); }
.types { display: flex; flex-direction: column; gap: 10px; }
.presets { display: flex; flex-direction: column; gap: 10px; margin-top: 4px; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; }
.preset { min-height: 44px; padding: 0 16px; border-radius: 999px; border: 1px solid var(--border); background: var(--surface); font-size: 14px; font-weight: 600; color: var(--text); }
.preset:disabled { opacity: 0.55; }
</style>
