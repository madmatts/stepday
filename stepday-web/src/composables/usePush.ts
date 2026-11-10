// Web Push działa tylko w zainstalowanej PWA na iOS (16.4+), w innych przeglądarkach — w zwykłej karcie.
import { shallowRef } from 'vue'
import { api } from '@/api/client'

export const isIos = /iPhone|iPad|iPod/.test(navigator.userAgent)
export const isStandalone = window.matchMedia?.('(display-mode: standalone)').matches
  || (navigator as Navigator & { standalone?: boolean }).standalone === true
export const pushSupported = 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
/** Czy w tym miejscu przypomnienia w ogóle mogą dotrzeć. */
export const pushPossible = pushSupported && (!isIos || isStandalone)

function keyBytes(b64url: string): Uint8Array<ArrayBuffer> {
  const pad = '='.repeat((4 - (b64url.length % 4)) % 4)
  const raw = atob((b64url + pad).replace(/-/g, '+').replace(/_/g, '/'))
  return Uint8Array.from(raw, (c) => c.charCodeAt(0))
}

export function usePush() {
  const permission = shallowRef<NotificationPermission>(pushSupported ? Notification.permission : 'denied')
  const error = shallowRef<string | null>(null)

  async function enable(): Promise<boolean> {
    error.value = null
    try {
      permission.value = await Notification.requestPermission()
      if (permission.value !== 'granted') return false
      const { publicKey } = await api<{ publicKey: string }>('/push/config')
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.getSubscription()
        ?? await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: keyBytes(publicKey) })
      await api<void>('/push/subscriptions', { method: 'POST', body: sub.toJSON() })
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Nie udało się włączyć przypomnień'
      return false
    }
  }

  return { permission, error, enable }
}
