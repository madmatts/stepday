import type { BlockView } from '@/api/types'
import { TYPE_LABEL } from '@/utils/blocks'
import { plural } from '@/utils/polish'

/** Podpis bloku w zwiniętej karcie („Lista · 3 pozycje”, „Tak / nie · z kalendarza”…). */
export function subtitle(b: BlockView): string {
  const parts: string[] = []
  switch (b.type) {
    case 'LIST': {
      const n = b.items?.length ?? 0
      parts.push('Lista', `${n} ${plural(n, 'pozycja', 'pozycje', 'pozycji')}`)
      break
    }
    case 'BOOLEAN': parts.push('Tak / nie'); break
    case 'TIMER': parts.push('Timer', `${b.minutes} ${plural(b.minutes ?? 0, 'minuta', 'minuty', 'minut')}`); break
    case 'NOTE': parts.push('Tekst', 'jedno zdanie'); break
    case 'COUNTER': parts.push('Licznik', `cel ${b.target}`); break
  }
  if (b.fromCalendar && b.carryCount === 0) parts.push('z kalendarza')
  if (b.carryCount > 0) parts.push('zaległe')
  return parts.join(' · ')
}

export function typeLabel(b: BlockView): string { return TYPE_LABEL[b.type] }

/** Status po prawej stronie zwiniętej karty. */
export function statusText(b: BlockView): { text: string; tone: 'ok' | 'muted' | 'miss' } | null {
  if (b.state === 'DONE') return { text: 'Zrobione', tone: 'ok' }
  if (b.state === 'SKIPPED') return { text: 'Pominięte', tone: 'muted' }
  if (b.state === 'MISSED') return { text: 'Niezaliczone', tone: 'miss' }
  if (b.deferred) return { text: 'Na później', tone: 'muted' }
  return null
}
