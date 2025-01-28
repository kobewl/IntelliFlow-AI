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
  
  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    // 初始化认证状态
    const isAuthenticated = await authStore.initializeAuth()
    
    if (!isAuthenticated) {
      // 保存原始目标路由
      next({
        path: '/auth/login',
        query: { redirect: to.fullPath }
      })
      return
    }
  } else if (to.path.startsWith('/auth/') && authStore.isAuthenticated) {
    // 如果用户已登录，访问登录/注册页面，重定向到首页
    next({ path: '/' })
    return
  }
  
  next()
})

export default router 