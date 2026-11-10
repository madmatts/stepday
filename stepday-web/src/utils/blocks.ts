import type { BlockType } from '@/api/types'

export const TYPE_LABEL: Record<BlockType, string> = {
  LIST: 'Lista',
  BOOLEAN: 'Tak / nie',
  COUNTER: 'Licznik',
  TIMER: 'Timer',
  NOTE: 'Notatka',
}

/** Ikona typu bloku (makiety: Typ, Układ dnia). */
export const TYPE_ICON: Record<BlockType, IconName> = {
  LIST: 'list',
  BOOLEAN: 'check',
  COUNTER: 'drop',
  TIMER: 'timer',
  NOTE: 'pencil',
}

/** Blok tak/nie z kalendarza (np. wymiana filtra) ma w makietach ikonę kalendarza. */
export function blockIcon(type: BlockType, fromCalendar: boolean): IconName {
  return type === 'BOOLEAN' && fromCalendar ? 'calendar' : TYPE_ICON[type]
}

export type IconName =
  | 'back' | 'close' | 'check' | 'chevron' | 'plus' | 'minus' | 'dash' | 'warn' | 'list' | 'drop'
  | 'timer' | 'pencil' | 'calendar' | 'grip' | 'arrow-right' | 'arrow-down' | 'arrow-down-short'
