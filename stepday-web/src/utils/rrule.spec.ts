import { describe, expect, it } from 'vitest'
import { describe as describeRule, fromRrule, toRrule } from './rrule'

describe('rrule', () => {
  it.each([
    ['FREQ=DAILY', 'codziennie'],
    ['FREQ=WEEKLY;BYDAY=SA', 'co sobotę'],
    ['FREQ=WEEKLY;BYDAY=MO,WE,FR', 'w pn, śr, pt'],
    ['FREQ=MONTHLY;INTERVAL=3', 'co 3 miesiące'],
    ['FREQ=MONTHLY;INTERVAL=6', 'co 6 miesięcy'],
    ['FREQ=MONTHLY;BYMONTHDAY=15', '15. dnia miesiąca'],
    ['FREQ=MONTHLY;BYMONTHDAY=-1', 'ostatniego dnia miesiąca'],
    ['FREQ=DAILY;COUNT=1', 'jednorazowo'],
  ])('%s → %s', (rule, text) => {
    expect(describeRule(rule)).toBe(text)
  })

  it('round-trips the options from the doc mapping table (section 6)', () => {
    for (const rule of ['FREQ=DAILY', 'FREQ=WEEKLY;BYDAY=MO,WE,FR', 'FREQ=MONTHLY;INTERVAL=3',
      'FREQ=MONTHLY;BYMONTHDAY=15', 'FREQ=DAILY;COUNT=1']) {
      expect(toRrule(fromRrule(rule, '2026-11-10'))).toBe(rule)
    }
  })
})
