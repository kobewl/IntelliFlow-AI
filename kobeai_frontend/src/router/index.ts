import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/home/Home.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/auth/login',
      name: 'login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { requiresAuth: false, layout: 'auth' }
    },
    {
      path: '/auth/register',
      name: 'register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { requiresAuth: false, layout: 'auth' }
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/chat/Chat.vue'),
      meta: { requiresAuth: true, layout: 'chat' }
    },
    {
      path: '/chat/:id',
      name: 'chat-conversation',
      component: () => import('@/views/chat/Chat.vue'),
      meta: { requiresAuth: true, layout: 'chat' }
    },
    {
      path: '/vip-plans',
      name: 'vipPlans',
      component: () => import('@/views/vip/VipPlans.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/profile/Profile.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('@/views/notification/SystemNotifications.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/demo/markdown',
      name: 'markdown-demo',
      component: () => import('@/views/demo/MarkdownDemo.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  if (!authStore.isAuthenticated) {
    await authStore.initializeAuth()
  }

  if (requiresAuth && !authStore.isAuthenticated) {
    next({ path: '/auth/login', query: { redirect: to.fullPath } })
    return
  }

  if (authStore.isAuthenticated && (to.name === 'login' || to.name === 'register')) {
    next({ path: '/chat' })
    return
  }

  next()
})

export default router
