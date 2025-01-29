import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/Home.vue'),
      meta: {
        layout: 'default',
        requiresAuth: false
      }
    },
    {
      path: '/auth/login',
      name: 'login',
      component: () => import('@/views/auth/Login.vue'),
      meta: {
        requiresAuth: false,
        layout: 'auth'
      }
    },
    {
      path: '/auth/register',
      name: 'register',
      component: () => import('@/views/auth/Register.vue'),
      meta: {
        requiresAuth: false,
        layout: 'auth'
      }
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/Chat.vue'),
      meta: {
        requiresAuth: true,
        layout: 'default'
      }
    },
    {
      path: '/vip-plans',
      name: 'vipPlans',
      component: () => import('@/views/VipPlans.vue'),
      meta: {
        requiresAuth: true,
        layout: 'default'
      }
    }
  ]
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  // 初始化认证状态
  if (!authStore.isAuthenticated) {
    await authStore.initializeAuth()
  }

  // 需要认证但未登录
  if (requiresAuth && !authStore.isAuthenticated) {
    next({
      path: '/auth/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 已登录用户访问登录/注册页面
  if (authStore.isAuthenticated && (to.name === 'login' || to.name === 'register')) {
    next({ path: '/chat' })
    return
  }

  // 未登录用户访问任何页面，如果不需要认证则允许访问
  if (!authStore.isAuthenticated && !requiresAuth) {
    next()
    return
  }

  next()
})

export default router 