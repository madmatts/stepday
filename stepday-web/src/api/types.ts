// Typy 1:1 z DTO backendu (pl.stepday.*Dtos). Zmieniaj razem z backendem.

export type BlockType = 'LIST' | 'BOOLEAN' | 'COUNTER' | 'TIMER' | 'NOTE'
export type BlockRole = 'STEP' | 'ALL_DAY'
export type InstanceState = 'PENDING' | 'DONE' | 'SKIPPED' | 'CARRIED' | 'MISSED'
export type DayStatus = 'OPEN' | 'FULL' | 'PARTIAL' | 'MISSED'
export type Completion = 'FULL' | 'PARTIAL' | 'NONE' | 'EXCLUDED'
export type OverduePolicy = 'CARRY_OVER' | 'DROP'

export interface ItemView { id: string; label: string; checked: boolean }

export interface BlockView {
  id: string
  name: string
  type: BlockType
  role: BlockRole
  state: InstanceState
  completion: Completion
  deferred: boolean
  carryCount: number
  fromCalendar: boolean
  items: ItemView[] | null
  value: number | null
  target: number | null
  threshold: number | null
  unit: string | null
  step: number | null
  minutes: number | null
  timerStartedAt: string | null
  timerStoppedAt: string | null
  note: string | null
}

export interface WeekDay { date: string; status: DayStatus | null; today: boolean }

export interface DayView {
  date: string
  today: boolean
  status: DayStatus
  closed: boolean
  progress: { done: number; total: number }
  steps: BlockView[]
  allDay: BlockView[]
  week: WeekDay[]
  board: { position: number; total: number; year: number; month: number }
  streak: number
}

export interface TemplateItem { id?: string | null; label: string }

export interface Schedule { rrule: string; dtstart: string; until?: string | null }

export interface TemplateRequest {
  type: BlockType
  role: BlockRole
  name: string
  config: Record<string, unknown>
  items: TemplateItem[]
  schedule: Schedule
  overduePolicy: OverduePolicy | null
  reminderTime: string | null
  afterTemplateId: string | null
  applyToday?: boolean
}

export interface TemplateResponse {
  id: string
  type: BlockType
  role: BlockRole
  name: string
  config: Record<string, unknown>
  items: TemplateItem[]
  schedule: Schedule
  overduePolicy: OverduePolicy
  reminderTime: string | null
  sortKey: string
}

export interface PreviewResponse {
  dates: { date: string; today: boolean }[]
  defaultOverduePolicy: OverduePolicy
}

export interface LayoutBlock { template: TemplateResponse; dueOnDate: boolean; everyDay: boolean }
export interface Layout { date: string; steps: LayoutBlock[]; allDay: LayoutBlock[] }

export interface GapBlock {
  instanceId: string
  name: string
  type: BlockType
  state: InstanceState
  value: number | null
  target: number | null
  threshold: number | null
  unit: string | null
  unchecked: string[] | null
  checked: string[] | null
}

export interface GapDay { date: string; status: DayStatus; blocks: GapBlock[] }

export interface WeekSummary {
  weekStart: string
  weekEnd: string
  outcome: 'SUCCESS' | 'GAPS'
  finished: boolean
  counts: { full: number; partial: number; missed: number; completed: number }
  days: { date: string; status: DayStatus | null }[]
  gaps: GapDay[]
}

export interface Me { timezone: string; dayStart: string; today: string }
