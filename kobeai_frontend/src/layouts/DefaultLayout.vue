<template>
  <div class="default-layout">
    <header class="layout-header">
      <div class="header-content">
        <router-link to="/" class="logo">
          <span class="logo-text">KobeAI</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/chat" class="nav-link">对话</router-link>
        </nav>
        <div class="header-right">
          <template v-if="authStore.isAuthenticated">
            <el-dropdown trigger="click">
              <span class="user-trigger">
                <el-avatar :size="32" :src="authStore.user?.avatar || '/ai-avatar.png'" />
                <span class="username">{{ authStore.user?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/vip-plans')">会员套餐</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="$router.push('/auth/login')">登录</el-button>
            <el-button size="small" @click="$router.push('/auth/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>
    <main class="layout-main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await authStore.logout()
    router.push('/')
  } catch {
    // 取消
  }
}
</script>

<style scoped>
.default-layout {
  height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-color-page, #f5f6f8);
}

.layout-header {
  height: 56px;
  flex-shrink: 0;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 32px;
}

.logo {
  text-decoration: none;
  flex-shrink: 0;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-links {
  display: flex;
  gap: 8px;
  flex: 1;
}

.nav-link {
  text-decoration: none;
  color: #6b7280;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.15s ease;
}

.nav-link:hover,
.nav-link.router-link-exact-active {
  color: #6366f1;
  background: #f3f4f6;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s;
}

.user-trigger:hover {
  background: #f3f4f6;
}

.username {
  font-size: 14px;
  color: #374151;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout-main {
  flex: 1;
  overflow: hidden;
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 12px;
    gap: 16px;
  }
  .username {
    display: none;
  }
}
</style>
