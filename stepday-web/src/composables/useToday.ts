import { shallowRef } from 'vue'
import { api } from '@/api/client'
import type { Me } from '@/api/types'

const today = shallowRef<string | null>(null)

/** „Dziś” użytkownika liczone przez serwer (strefa + day_start). */
export async function fetchToday(): Promise<string> {
  if (!today.value) today.value = (await api<Me>('/me')).today
  return today.value
}
