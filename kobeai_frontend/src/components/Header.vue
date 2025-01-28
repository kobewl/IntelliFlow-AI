<script setup lang="ts">
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  ElMessage.success('退出登录成功')
}
</script>

<template>
  <div class="header">
    <div class="nav-links">
      <router-link to="/" class="nav-link">首页</router-link>
      <router-link to="/chat" class="nav-link">对话</router-link>
    </div>

    <div class="user-section">
      <router-link to="/vip" class="upgrade-button" v-if="authStore.user && authStore.user.role !== 'svip'">
        升级到{{ authStore.user.role === 'vip' ? 'SVIP' : 'VIP' }}
      </router-link>
      <el-dropdown v-if="authStore.user" trigger="click">
        <div class="avatar-wrapper">
          <el-avatar :size="32" class="user-avatar">
            {{ authStore.user.username.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="username">{{ authStore.user.username }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.nav-link {
  text-decoration: none;
  color: var(--el-text-color-regular);
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link:hover {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}

.nav-link.router-link-active {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}

.user-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.upgrade-button {
  text-decoration: none;
  background: var(--el-color-warning);
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.upgrade-button:hover {
  opacity: 0.9;
  transform: scale(1.05);
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.avatar-wrapper:hover {
  background: var(--el-fill-color-light);
}

.user-avatar {
  background: var(--el-color-primary);
  color: white;
  font-weight: bold;
}

.username {
  color: var(--el-text-color-regular);
  font-size: 14px;
}
</style> 
