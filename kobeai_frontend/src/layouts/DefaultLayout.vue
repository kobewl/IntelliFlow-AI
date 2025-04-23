<template>
  <div class="layout-container">
    <el-container class="layout-wrapper">
      <el-header height="64px" class="layout-header">
        <div class="header-content">
          <div class="logo">
            <router-link to="/" class="logo-link">
              <h1>KobeAI</h1>
            </router-link>
          </div>
          <div class="nav-menu">
            <el-menu 
              mode="horizontal" 
              :router="true"
              :default-active="activeMenu"
              class="main-menu"
            >
              <el-menu-item index="/">首页</el-menu-item>
              <el-menu-item index="/chat">对话</el-menu-item>
            </el-menu>
          </div>
          <div class="user-actions">
            <template v-if="authStore.isAuthenticated">
              <UserProfile />
            </template>
            <template v-else>
              <el-button type="primary" @click="$router.push('/auth/login')">
                登录
              </el-button>
              <el-button @click="$router.push('/auth/register')">注册</el-button>
            </template>
          </div>
        </div>
      </el-header>
      <el-main class="layout-main">
        <div class="main-content">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessageBox, ElMessage } from 'element-plus'
import UserProfile from '../components/UserProfile.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 计算当前激活的菜单项
const activeMenu = computed(() => {
  return route.path
})

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await authStore.logout()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出失败，请刷新页面重试')
      console.error('Logout failed:', error)
    }
  }
}

const handleUpgrade = () => {
  router.push('/vip-plans')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.layout-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.layout-header {
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0;
  position: relative;
  z-index: 1000;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.logo {
  margin-right: 40px;
  flex-shrink: 0;
}

.logo-link {
  text-decoration: none;
}

.logo h1 {
  margin: 0;
  font-size: 24px;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, #409eff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
  min-width: 0;
}

.main-menu {
  border-bottom: none !important;
  background: transparent;
}

.user-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-shrink: 0;
}

.layout-main {
  flex: 1;
  overflow: hidden;
  padding: 0;
  position: relative;
}

.main-content {
  height: 100%;
  padding: 20px;
  overflow-y: auto;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 12px;
  }
  
  .logo {
    margin-right: 20px;
  }
  
  .logo h1 {
    font-size: 20px;
  }

  .main-content {
    padding: 12px;
  }
  
  .nav-menu :deep(.el-menu-item) {
    padding: 0 10px;
  }
  
  .user-actions {
    gap: 8px;
  }
}

/* 深色模式适配 */
:root[data-theme='dark'] {
  .layout-header {
    background-color: var(--el-bg-color-overlay);
  }
  
  .main-content {
    background-color: var(--el-bg-color);
  }
}
</style> 