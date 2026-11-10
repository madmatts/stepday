<script setup lang="ts">
// „Pozycje na liście” (makieta Lista): uchwyt, pole tekstowe, usuń; „Dodaj pozycję”.
import { nextTick } from 'vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { useDragReorder } from '@/composables/useDragReorder'

interface Item { key: string; label: string }
const model = defineModel<Item[]>({ required: true })
const emit = defineEmits<{ add: [] }>()

const { items, draggingIndex, onPointerDown, onKeyDown } = useDragReorder(() => model.value, (next) => { model.value = next })

function remove(index: number) {
  model.value = model.value.filter((_, i) => i !== index)
}

async function add() {
  emit('add')
  await nextTick()
  const inputs = document.querySelectorAll<HTMLInputElement>('.item-input')
  inputs[inputs.length - 1]?.focus()
}
</script>

<template>
  <section aria-label="Pozycje" class="items" data-reorder>
    <div class="section-label">Pozycje na liście</div>
    <div v-for="(item, i) in items" :key="item.key" :class="['item', { 'item--drag': draggingIndex === i }]" data-reorder-row>
      <button type="button" class="grip" :aria-label="`Zmień kolejność: ${item.label || 'pozycja ' + (i + 1)}`"
        @pointerdown="onPointerDown(i, $event)" @keydown="onKeyDown(i, $event)">
        <AppIcon name="grip" :size="14" color="var(--icon-muted)" />
      </button>
      <label :for="`item-${item.key}`" class="sr-only">Pozycja {{ i + 1 }}</label>
      <input :id="`item-${item.key}`" v-model="model[i].label" class="item-input" type="text" maxlength="120" />
      <button type="button" class="remove" :aria-label="`Usuń pozycję: ${item.label || i + 1}`" @click="remove(i)">
        <AppIcon name="close" :size="16" color="var(--text-3)" />
      </button>
    </div>
    <button type="button" class="add" @click="add">
      <AppIcon name="plus" :size="18" color="var(--accent-ink)" />
      Dodaj pozycję
    </button>
  </section>
</template>

<style scoped>
.items { display: flex; flex-direction: column; gap: 8px; }
.item { display: flex; align-items: center; gap: 4px; min-height: 56px; padding: 0 6px 0 4px; border-radius: 14px; border: 1px solid var(--border); background: var(--surface); }
.item--drag { box-shadow: var(--shadow-active); border-color: var(--accent); }
.grip { width: 36px; height: 44px; border: 0; background: transparent; display: flex; align-items: center; justify-content: center; touch-action: none; cursor: grab; }
.item-input { flex: 1; min-width: 0; height: 44px; border: 0; background: transparent; font-size: 16px; }
.remove { width: 44px; height: 44px; border: 0; background: transparent; display: flex; align-items: center; justify-content: center; }
.add { display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 52px; border-radius: 14px; border: 2px dashed var(--icon-muted); background: transparent; font-size: 15px; font-weight: 600; color: var(--accent-ink); }
</style>
