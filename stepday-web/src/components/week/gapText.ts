import type { GapBlock } from '@/api/types'

export type GapTone = 'miss' | 'partial' | 'skip'

/** Tekst i ikona wiersza „Co dokładnie” (makieta Braki). */
export function gapLine(b: GapBlock): { text: string; tone: GapTone } {
  if (b.state === 'SKIPPED') return { text: 'Pominięty.', tone: 'skip' }
  if (b.type === 'COUNTER') {
    const unit = b.unit ? ` ${b.unit}` : ''
    const value = Math.max(0, b.value ?? 0)
    if (value >= (b.threshold ?? 0)) {
      return { text: `${value}/${b.target}${unit}, brakowało ${(b.target ?? 0) - value}.`, tone: 'partial' }
    }
    return { text: `${value}/${b.target}${unit}. Próg zaliczenia dnia to ${b.threshold}.`, tone: 'miss' }
  }
  if (b.type === 'LIST' && b.unchecked?.length) {
    const done = b.checked?.length ? ` Zrobione: ${b.checked.join(', ')}.` : ''
    return { text: `Nie odhaczono: ${b.unchecked.join(', ')}.${done}`, tone: 'miss' }
  }
  return { text: 'Nie zrobiony.', tone: 'miss' }
}
