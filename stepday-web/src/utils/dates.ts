// Formatowanie dat po polsku. Operuje na datach lokalnych 'YYYY-MM-DD' (bez stref — dzień liczy serwer).

const MONTHS_GEN = ['stycznia', 'lutego', 'marca', 'kwietnia', 'maja', 'czerwca', 'lipca', 'sierpnia',
  'września', 'października', 'listopada', 'grudnia']
const MONTHS_NOM = ['styczeń', 'luty', 'marzec', 'kwiecień', 'maj', 'czerwiec', 'lipiec', 'sierpień',
  'wrzesień', 'październik', 'listopad', 'grudzień']
const WEEKDAYS = ['niedziela', 'poniedziałek', 'wtorek', 'środa', 'czwartek', 'piątek', 'sobota']
const WEEKDAYS_SHORT = ['nd', 'pn', 'wt', 'śr', 'cz', 'pt', 'so']
const WEEKDAYS_ABBR = ['Nd', 'Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'So']

export function parseDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

export function toIso(date: Date): string {
  return date.toISOString().slice(0, 10)
}

export function addDays(iso: string, days: number): string {
  const d = parseDate(iso)
  d.setUTCDate(d.getUTCDate() + days)
  return toIso(d)
}

export function weekday(iso: string): string { return WEEKDAYS[parseDate(iso).getUTCDay()] }
export function weekdayCap(iso: string): string { const w = weekday(iso); return w[0].toUpperCase() + w.slice(1) }
export function weekdayAbbr(iso: string): string { return WEEKDAYS_ABBR[parseDate(iso).getUTCDay()] }
export function dayOfMonth(iso: string): number { return parseDate(iso).getUTCDate() }

/** „10 listopada” */
export function dayMonth(iso: string): string {
  const d = parseDate(iso)
  return `${d.getUTCDate()} ${MONTHS_GEN[d.getUTCMonth()]}`
}

/** „10 listopada 2026” */
export function longDate(iso: string): string {
  return `${dayMonth(iso)} ${parseDate(iso).getUTCFullYear()}`
}

/** „wt, 10 listopada 2026” */
export function shortWeekdayLongDate(iso: string): string {
  return `${WEEKDAYS_SHORT[parseDate(iso).getUTCDay()]}, ${longDate(iso)}`
}

export function monthName(month: number): string { return MONTHS_NOM[month - 1] }

/** „2–8 listopada” albo „30 listopada – 6 grudnia” */
export function range(startIso: string, endIso: string): string {
  const a = parseDate(startIso)
  const b = parseDate(endIso)
  if (a.getUTCMonth() === b.getUTCMonth()) {
    return `${a.getUTCDate()}–${b.getUTCDate()} ${MONTHS_GEN[b.getUTCMonth()]}`
  }
  return `${dayMonth(startIso)} – ${dayMonth(endIso)}`
}

export function mondayOf(iso: string): string {
  const day = parseDate(iso).getUTCDay() // 0 = niedziela
  return addDays(iso, day === 0 ? -6 : 1 - day)
}
