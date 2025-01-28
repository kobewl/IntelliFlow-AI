<template>
  <div class="layout">
    <el-container>
      <el-header height="64px">
        <div class="header-content">
          <div class="logo">
            <router-link to="/" class="logo-link">
              <h1>KobeAI</h1>
            </router-link>
          </div>
          <div class="nav-menu">
            <el-menu mode="horizontal" :router="true">
              <el-menu-item index="/">首页</el-menu-item>
              <el-menu-item index="/chat">对话</el-menu-item>
            </el-menu>
          </div>
          <div class="user-actions">
            <template v-if="authStore.isAuthenticated">
              <el-dropdown>
                <el-button>
                  {{ authStore.user?.username }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessageBox, ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

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
    
    // 执行退出，重定向会在store中处理
    await authStore.logout()
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出失败，请刷新页面重试')
      console.error('Logout failed:', error)
    }
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.el-container {
  height: 100%;
}

.el-header {
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0;
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
}

.nav-menu {
  flex: 1;
}

:deep(.el-menu) {
  border-bottom: none;
}

.user-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.el-main {
  padding: 20px;
  background-color: var(--el-bg-color-page);
}

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
}
</style> 