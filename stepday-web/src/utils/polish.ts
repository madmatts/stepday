// Odmiana liczebników po polsku: 1 pozycja, 2–4 pozycje, 5+ pozycji.
export function plural(n: number, one: string, few: string, many: string): string {
  if (n === 1) return one
  const d = n % 10
  const t = n % 100
  return d >= 2 && d <= 4 && (t < 12 || t > 14) ? few : many
}
