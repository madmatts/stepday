import { describe, expect, it } from 'vitest'
import type { GapBlock } from '@/api/types'
import { gapLine } from './gapText'

const base: GapBlock = { instanceId: 'x', name: 'X', type: 'TIMER', state: 'MISSED', value: null, target: null,
  threshold: null, unit: null, unchecked: null, checked: null }

describe('gapLine (makieta Braki)', () => {
  it('list with partial progress', () => {
    expect(gapLine({ ...base, type: 'LIST', unchecked: ['Magnez', 'Omega-3'], checked: ['Witamina D3'] }))
      .toEqual({ text: 'Nie odhaczono: Magnez, Omega-3. Zrobione: Witamina D3.', tone: 'miss' })
  })
  it('counter below threshold', () => {
    expect(gapLine({ ...base, type: 'COUNTER', value: 5, target: 8, threshold: 7, unit: 'szklanek' }).text)
      .toBe('5/8 szklanek. Próg zaliczenia dnia to 7.')
  })
  it('counter between threshold and target', () => {
    expect(gapLine({ ...base, type: 'COUNTER', state: 'DONE', value: 7, target: 8, threshold: 7, unit: 'szklanek' }))
      .toEqual({ text: '7/8 szklanek, brakowało 1.', tone: 'partial' })
  })
  it('skipped and not done', () => {
    expect(gapLine({ ...base, state: 'SKIPPED' })).toEqual({ text: 'Pominięty.', tone: 'skip' })
    expect(gapLine(base).text).toBe('Nie zrobiony.')
  })
})
