/// <reference lib="webworker" />
// Service worker: precache aplikacji, ostatnia odpowiedź GET /api/days/* do odczytu offline
// (dokument, sekcja 8) i obsługa przypomnień Web Push (tylko w zainstalowanej PWA).
import { precacheAndRoute, cleanupOutdatedCaches, createHandlerBoundToURL } from 'workbox-precaching'
import { registerRoute, NavigationRoute } from 'workbox-routing'
import { NetworkFirst } from 'workbox-strategies'
import { ExpirationPlugin } from 'workbox-expiration'
import { clientsClaim } from 'workbox-core'

declare const self: ServiceWorkerGlobalScope

// Nowa wersja przejmuje kontrolę bez przeładowania otwartej strony (żeby nie zgubić np. wpisywanej notatki).
self.skipWaiting()
clientsClaim()
cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)
registerRoute(new NavigationRoute(createHandlerBoundToURL('/index.html'), { denylist: [/^\/api\//] }))

registerRoute(
  ({ url, request }) => request.method === 'GET' && url.pathname.startsWith('/api/days/'),
  new NetworkFirst({
    cacheName: 'stepday-days',
    networkTimeoutSeconds: 4,
    plugins: [new ExpirationPlugin({ maxEntries: 30, maxAgeSeconds: 14 * 24 * 3600 })],
  }),
)

self.addEventListener('push', (event) => {
  const data = (() => {
    try { return event.data?.json() as { title?: string; body?: string; url?: string } } catch { return {} }
  })() ?? {}
  event.waitUntil(self.registration.showNotification(data.title ?? 'Stepday', {
    body: data.body ?? '',
    icon: '/icon-192.png',
    badge: '/icon-192.png',
    lang: 'pl',
    data: { url: data.url ?? '/' },
  }))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = (event.notification.data as { url?: string })?.url ?? '/'
  event.waitUntil((async () => {
    const all = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    const open = all.find((c) => 'focus' in c)
    if (open) {
      await open.focus()
      await (open as WindowClient).navigate(url)
    } else {
      await self.clients.openWindow(url)
    }
  })())
})
