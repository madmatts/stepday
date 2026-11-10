import { defineStore } from 'pinia'
import { computed, reactive, shallowRef } from 'vue'
import { api } from '@/api/client'
import type { BlockRole, BlockType, OverduePolicy, TemplateRequest, TemplateResponse } from '@/api/types'
import { fromRrule, toRrule, type When } from '@/utils/rrule'

interface DraftItem { key: string; id: string | null; label: string }

/** Szkic bloku przechodzący przez ekrany: Typ → Lista/Licznik → Kiedy → zapis. */
export const useTemplateDraft = defineStore('templateDraft', () => {
  const editingId = shallowRef<string | null>(null)
  const draft = reactive({
    type: 'BOOLEAN' as BlockType,
    name: '',
    role: 'STEP' as BlockRole,
    items: [] as DraftItem[],
    target: 8,
    unit: '',
    step: 1,
    thresholdPct: 80,
    minutes: 20,
    when: null as When | null,
    overduePolicy: null as OverduePolicy | null,
    reminderOn: false,
    reminderTime: '07:30',
    afterTemplateId: null as string | null,
  })
  let itemSeq = 0
  const newItem = (label = '', id: string | null = null): DraftItem => ({ key: `i${itemSeq++}`, id, label })

  function start(type: BlockType, today: string) {
    editingId.value = null
    Object.assign(draft, {
      type, name: '', role: type === 'COUNTER' ? 'ALL_DAY' : 'STEP',
      items: type === 'LIST' ? [newItem()] : [], target: 8, unit: '', step: 1, thresholdPct: 80, minutes: 20,
      when: fromRrule('FREQ=DAILY', today), overduePolicy: null, reminderOn: false, reminderTime: '07:30',
      afterTemplateId: null,
    })
  }

  function changeType(type: BlockType) {
    draft.type = type
    if (type === 'LIST' && draft.items.length === 0) draft.items.push(newItem())
  }

  async function load(id: string) {
    const t = await api<TemplateResponse>(`/templates/${id}`)
    editingId.value = id
    const c = t.config as Record<string, number | string>
    Object.assign(draft, {
      type: t.type, name: t.name, role: t.role,
      items: t.items.map((i) => newItem(i.label, i.id ?? null)),
      target: Number(c.target ?? 8), unit: String(c.unit ?? ''), step: Number(c.step ?? 1),
      thresholdPct: Number(c.threshold_pct ?? 80), minutes: Number(c.minutes ?? 20),
      when: fromRrule(t.schedule.rrule, t.schedule.dtstart), overduePolicy: t.overduePolicy,
      reminderOn: t.reminderTime !== null, reminderTime: (t.reminderTime ?? '07:30').slice(0, 5),
      afterTemplateId: null,
    })
  }

  const config = computed<Record<string, unknown>>(() => {
    switch (draft.type) {
      case 'COUNTER':
        return { target: draft.target, unit: draft.unit, step: draft.step, threshold_pct: draft.thresholdPct }
      case 'TIMER':
        return { minutes: draft.minutes }
      default:
        return {}
    }
  })

  const canSave = computed(() => draft.name.trim().length > 0 && draft.when !== null
    && (draft.type !== 'LIST' || draft.items.some((i) => i.label.trim())))

  function request(): TemplateRequest {
    const when = draft.when!
    return {
      type: draft.type,
      role: draft.role,
      name: draft.name.trim(),
      config: config.value,
      items: draft.type === 'LIST'
        ? draft.items.filter((i) => i.label.trim()).map((i) => ({ id: i.id, label: i.label.trim() }))
        : [],
      schedule: { rrule: toRrule(when), dtstart: when.dtstart },
      overduePolicy: draft.overduePolicy,
      reminderTime: draft.reminderOn ? draft.reminderTime : null,
      afterTemplateId: draft.role === 'STEP' ? draft.afterTemplateId : null,
    }
  }

  async function save(): Promise<TemplateResponse> {
    const body = request()
    return editingId.value
      ? api<TemplateResponse>(`/templates/${editingId.value}`, { method: 'PUT', body })
      : api<TemplateResponse>('/templates', { method: 'POST', body })
  }

  async function archive() {
    if (editingId.value) await api<void>(`/templates/${editingId.value}`, { method: 'DELETE' })
  }

  return { editingId, draft, config, canSave, start, changeType, load, save, archive, newItem, request }
})
