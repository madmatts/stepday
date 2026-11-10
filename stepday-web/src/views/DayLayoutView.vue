<script setup lang="ts">
// Makieta „Układ dnia”: ścieżka z przeciąganiem, bloki „Nie dziś” wyszarzone, „W ciągu dnia”, „Dodaj blok”.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLayout } from '@/composables/useLayout'
import { useDragReorder } from '@/composables/useDragReorder'
import { weekday } from '@/utils/dates'
import ScreenHeader from '@/components/common/ScreenHeader.vue'
import LayoutRow from '@/components/editor/LayoutRow.vue'
import AppIcon from '@/components/icons/AppIcon.vue'

const router = useRouter()
const { layout, error, reorder } = useLayout()
const { items: steps, draggingIndex, onPointerDown, onKeyDown } = useDragReorder(
  () => layout.value?.steps ?? [], (next) => reorder(next))

const subtitle = computed(() => layout.value ? `Tak wygląda ${weekday(layout.value.date)}` : '')
</script>

<template>
  <main class="screen layout">
    <ScreenHeader title="Układ dnia" :subtitle="subtitle" @back="router.push('/')" />
    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <section v-if="layout" aria-label="Ścieżka" class="path" data-reorder>
      <div class="section-label label">Ścieżka</div>
      <template v-for="(b, i) in steps" :key="b.template.id">
        <LayoutRow :block="b" :dragging="draggingIndex === i" data-reorder-row
          @grip-down="onPointerDown(i, $event)" @grip-key="onKeyDown(i, $event)"
          @open="router.push(`/blocks/${b.template.id}`)" />
        <div v-if="i < steps.length - 1" class="arrow"><AppIcon name="arrow-down-short" :size="24" color="var(--icon-muted)" /></div>
      </template>
      <p v-if="!steps.length" class="hint">Ścieżka jest pusta.</p>
    </section>

    <div class="hint">Bloki, które dziś nie wypadają, są wyszarzone i nie pojawią się na ekranie dnia.</div>

    <section v-if="layout?.allDay.length" aria-label="W ciągu dnia" class="all-day">
      <div class="section-label">W ciągu dnia</div>
      <LayoutRow v-for="b in layout.allDay" :key="b.template.id" :block="b" @open="router.push(`/blocks/${b.template.id}`)" />
    </section>

    <RouterLink to="/blocks/new" class="btn-primary add">
      <AppIcon name="plus" color="#fff" />
      Dodaj blok
    </RouterLink>
  </main>
</template>

<style scoped>
.layout { gap: 14px; }
.path { display: flex; flex-direction: column; }
.label { margin-bottom: 10px; }
.arrow { margin-left: 48px; line-height: 0; }
.all-day { display: flex; flex-direction: column; gap: 10px; }
.add { margin-top: 6px; text-decoration: none; }
.error { color: var(--miss); }
</style>
