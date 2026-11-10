import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './styles/base.css'
import App from './App.vue'
import { router } from './router'
import { setSender } from './api/mutations'
import { offlineSender, startSync } from './sync/outbox'
import { registerSW } from 'virtual:pwa-register'

// Zapisy idą przez kolejkę offline (IndexedDB), PWA działa bez zasięgu.
setSender(offlineSender)
startSync()
registerSW({ immediate: true })

createApp(App).use(createPinia()).use(router).mount('#app')
