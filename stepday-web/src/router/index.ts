import { createRouter, createWebHistory } from 'vue-router'
import { hasCredentials, setUnauthorizedHandler } from '@/api/client'
import DayView from '@/views/DayView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'today', component: DayView },
    { path: '/day/:date(\\d{4}-\\d{2}-\\d{2})', name: 'day', component: DayView },
    { path: '/blocks/new', name: 'block-type', component: () => import('@/views/BlockTypePickerView.vue') },
    { path: '/blocks/edit', name: 'block-edit', component: () => import('@/views/BlockEditorView.vue') },
    { path: '/blocks/edit/when', name: 'block-when', component: () => import('@/views/ScheduleEditorView.vue') },
    { path: '/blocks/:id', name: 'block-load', component: () => import('@/views/BlockEditorView.vue'), props: true },
    { path: '/layout', name: 'layout', component: () => import('@/views/DayLayoutView.vue') },
    { path: '/week/:start(\\d{4}-\\d{2}-\\d{2})', name: 'week', component: () => import('@/views/WeekSummaryView.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  if (to.name !== 'login' && !hasCredentials()) return { name: 'login' }
})

setUnauthorizedHandler(() => {
  if (router.currentRoute.value.name !== 'login') void router.replace({ name: 'login' })
})
