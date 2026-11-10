// Opis reguły po polsku i budowanie reguły z opcji ekranu „Kiedy”.
// Terminy (daty) liczy wyłącznie backend: POST /schedule/preview. Tu tylko tekst i składnia.

export type WhenKind = 'DAILY' | 'WEEKDAYS' | 'EVERY' | 'MONTH_DAY' | 'ONCE'
export type EveryUnit = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export const WEEKDAY_CODES = ['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU'] as const
export type WeekdayCode = (typeof WEEKDAY_CODES)[number]

export interface When {
  kind: WhenKind
  weekdays: WeekdayCode[]
  every: number
  unit: EveryUnit
  monthDay: number
  dtstart: string
}

const WEEKDAY_ACC: Record<WeekdayCode, string> = {
  MO: 'poniedziałek', TU: 'wtorek', WE: 'środę', TH: 'czwartek', FR: 'piątek', SA: 'sobotę', SU: 'niedzielę',
}
const WEEKDAY_EVERY: Record<WeekdayCode, string> = {
  MO: 'co poniedziałek', TU: 'co wtorek', WE: 'co środę', TH: 'co czwartek', FR: 'co piątek', SA: 'co sobotę',
  SU: 'co niedzielę',
}
export const WEEKDAY_SHORT: Record<WeekdayCode, string> = {
  MO: 'Pn', TU: 'Wt', WE: 'Śr', TH: 'Cz', FR: 'Pt', SA: 'So', SU: 'Nd',
}

/** Mapowanie z dokumentu (sekcja 6, tabela „Opcja w UI → rrule”). */
export function toRrule(w: When): string {
  switch (w.kind) {
    case 'DAILY': return 'FREQ=DAILY'
    case 'WEEKDAYS': return `FREQ=WEEKLY;BYDAY=${w.weekdays.join(',')}`
    case 'EVERY': return w.every > 1 ? `FREQ=${w.unit};INTERVAL=${w.every}` : `FREQ=${w.unit}`
    case 'MONTH_DAY': return `FREQ=MONTHLY;BYMONTHDAY=${w.monthDay}`
    case 'ONCE': return 'FREQ=DAILY;COUNT=1'
  }
}

function parts(rrule: string): Record<string, string> {
  return Object.fromEntries(rrule.replace(/^RRULE:/, '').split(';').map((p) => p.split('=') as [string, string]))
}

export function fromRrule(rrule: string, dtstart: string): When {
  const p = parts(rrule)
  const base: When = { kind: 'DAILY', weekdays: ['MO', 'WE', 'FR'], every: 3, unit: 'MONTHLY', monthDay: 15, dtstart }
  const interval = Number(p.INTERVAL ?? 1)
  if (p.COUNT === '1') return { ...base, kind: 'ONCE' }
  if (p.FREQ === 'DAILY' && interval === 1) return base
  if (p.FREQ === 'WEEKLY' && p.BYDAY && interval === 1) {
    return { ...base, kind: 'WEEKDAYS', weekdays: p.BYDAY.split(',') as WeekdayCode[] }
  }
  if (p.FREQ === 'MONTHLY' && p.BYMONTHDAY && interval === 1) {
    return { ...base, kind: 'MONTH_DAY', monthDay: Number(p.BYMONTHDAY) }
  }
  return { ...base, kind: 'EVERY', every: interval, unit: (p.FREQ as EveryUnit) ?? 'DAILY' }
}

/** „codziennie”, „co sobotę”, „co 3 miesiące”, „w pn, śr, pt”, „15. dnia miesiąca”, „jednorazowo” */
export function describe(rrule: string): string {
  const p = parts(rrule)
  const n = Number(p.INTERVAL ?? 1)
  if (p.COUNT === '1') return 'jednorazowo'
  if (p.FREQ === 'DAILY') return n === 1 ? 'codziennie' : `co ${n} dni`
  if (p.FREQ === 'WEEKLY') {
    const days = (p.BYDAY ?? '').split(',').filter(Boolean) as WeekdayCode[]
    if (n === 1 && days.length === 1) return WEEKDAY_EVERY[days[0]]
    if (n === 1 && days.length > 1) return 'w ' + days.map((d) => WEEKDAY_SHORT[d].toLowerCase()).join(', ')
    return n === 1 ? 'co tydzień' : `co ${n} ${n < 5 ? 'tygodnie' : 'tygodni'}`
  }
  if (p.FREQ === 'MONTHLY') {
    if (p.BYMONTHDAY) return p.BYMONTHDAY === '-1' ? 'ostatniego dnia miesiąca' : `${p.BYMONTHDAY}. dnia miesiąca`
    if (n === 1) return 'co miesiąc'
    return `co ${n} ${n < 5 ? 'miesiące' : 'miesięcy'}`
  }
  if (p.FREQ === 'YEARLY') return 'co rok'
  return rrule
}

export function weekdayAccusative(code: WeekdayCode): string { return WEEKDAY_ACC[code] }
