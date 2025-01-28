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
        layout: 'default'
      }
    },
    {
      path: '/auth/login',
      name: 'login',
      component: () => import('@/views/auth/Login.vue'),
      meta: {
        requiresGuest: true,
        layout: 'auth'
      }
    },
    {
      path: '/auth/register',
      name: 'register',
      component: () => import('@/views/auth/Register.vue'),
      meta: {
        requiresGuest: true,
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

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  
  // 如果需要登录
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next({
      path: '/auth/login',
      query: { redirect: to.fullPath }
    })
  }
  
  // 如果已登录不能访问的页面（登录和注册页面）
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    return next('/chat')
  }
  
  // 如果是根路径且已登录，重定向到聊天页面
  if (to.path === '/' && authStore.isAuthenticated) {
    return next('/chat')
  }
  
  next()
})

export default router 