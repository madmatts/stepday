// Odpowiedzi /weeks/{start}/summary w kształcie z backendu, z danymi z makiet „Sukces” i „Braki”.
// Kolejność bloków jak z backendu (sort_key), nie jak w makiecie — patrz docs/mockup-diff.md.
// Poprawność samych danych sprawdza WeekSummaryTest (backend) na tym samym tygodniu.
const days = (statuses: string[]) => statuses.map((status, i) => ({ date: `2026-11-0${2 + i}`, status }))

export const SUCCESS_WEEK = {
  weekStart: '2026-11-02', weekEnd: '2026-11-08', outcome: 'SUCCESS', finished: true,
  counts: { full: 7, partial: 0, missed: 0, completed: 7 },
  days: days(Array(7).fill('FULL')), gaps: [],
}

export const GAPS_WEEK = {
  weekStart: '2026-11-02', weekEnd: '2026-11-08', outcome: 'GAPS', finished: true,
  counts: { full: 5, partial: 1, missed: 1, completed: 6 },
  days: days(['FULL', 'FULL', 'MISSED', 'FULL', 'PARTIAL', 'FULL', 'FULL']),
  gaps: [
    { date: '2026-11-04', status: 'MISSED', blocks: [
      { instanceId: 'a', name: 'Tabletki rano', type: 'LIST', state: 'MISSED', value: null, target: null, threshold: null, unit: null,
        unchecked: ['Magnez', 'Omega-3'], checked: ['Witamina D3'] },
      { instanceId: 'b', name: 'Spacer', type: 'TIMER', state: 'MISSED', value: null, target: null, threshold: null, unit: null, unchecked: null, checked: null },
      { instanceId: 'c', name: 'Woda', type: 'COUNTER', state: 'MISSED', value: 5, target: 8, threshold: 7, unit: 'szklanek', unchecked: null, checked: null },
    ] },
    { date: '2026-11-06', status: 'PARTIAL', blocks: [
      { instanceId: 'e', name: 'Spacer', type: 'TIMER', state: 'SKIPPED', value: null, target: null, threshold: null, unit: null, unchecked: null, checked: null },
      { instanceId: 'd', name: 'Woda', type: 'COUNTER', state: 'DONE', value: 7, target: 8, threshold: 7, unit: 'szklanek', unchecked: null, checked: null },
    ] },
  ],
}
