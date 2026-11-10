import { describe, expect, it } from 'vitest'
import { dayMonth, mondayOf, range, shortWeekdayLongDate, weekday } from './dates'

describe('dates (pl)', () => {
  it('formats like the mockups', () => {
    expect(weekday('2026-11-10')).toBe('wtorek')
    expect(dayMonth('2026-11-10')).toBe('10 listopada')
    expect(shortWeekdayLongDate('2027-02-10')).toBe('śr, 10 lutego 2027')
    expect(range('2026-11-02', '2026-11-08')).toBe('2–8 listopada')
    expect(range('2026-11-30', '2026-12-06')).toBe('30 listopada – 6 grudnia')
  })
  it('finds monday', () => {
    expect(mondayOf('2026-11-10')).toBe('2026-11-09')
    expect(mondayOf('2026-11-15')).toBe('2026-11-09')
    expect(mondayOf('2026-11-09')).toBe('2026-11-09')
  })
})
