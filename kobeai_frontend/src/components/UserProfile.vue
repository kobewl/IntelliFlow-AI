<template>
  <div class="user-profile-wrapper">
    <el-button 
      v-if="user?.userRole === UserRole.NORMAL"
      class="upgrade-btn" 
      type="danger" 
      size="small"
      @click="handleUpgrade"
    >
      升级VIP
    </el-button>
    <el-dropdown trigger="click" @command="handleCommand">
      <div class="user-profile">
        <div class="avatar-container">
          <div class="avatar-halo" v-if="isVIP || isSVIP" :class="{
            'vip-halo': isVIP,
            'svip-halo': isSVIP
          }"></div>
          <div class="user-avatar" :class="{
            'vip-avatar': isVIP,
            'svip-avatar': isSVIP
          }">
            <template v-if="user?.avatar">
              <el-image
                :src="user.avatar"
                fit="cover"
                class="avatar-image"
              >
                <template #error>
                  <div class="avatar-fallback">
                    {{ user.username[0]?.toUpperCase() }}
                  </div>
                </template>
              </el-image>
            </template>
            <template v-else>
              <el-icon v-if="!user?.username"><UserFilled /></el-icon>
              <template v-else>{{ user.username[0]?.toUpperCase() }}</template>
            </template>
          </div>
        </div>
        <div class="user-info">
          <span class="username">{{ user?.username || '未登录' }}</span>
          <span class="user-role" :class="{
            'vip-role': isVIP,
            'svip-role': isSVIP
          }">
            {{ getRoleText }}
            <span v-if="isVIP || isSVIP" class="crown-icon">👑</span>
          </span>
        </div>
      </div>
      
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">
            <el-icon><User /></el-icon>
            个人信息
          </el-dropdown-item>
          <el-dropdown-item command="settings">
            <el-icon><Setting /></el-icon>
            设置
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    <UserInfo ref="userInfoRef" />
    <Settings ref="settingsRef" />
  </div>
</template>

<script lang="ts">
export default {
  name: 'UserProfile'
}
</script>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { User, Setting, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { storeToRefs } from 'pinia'
import UserInfo from './UserInfo.vue'
import Settings from './Settings.vue'
import { UserRole } from '../types/user'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)

const userInfoRef = ref()
const settingsRef = ref()

const router = useRouter()

// 用户角色相关计算属性
const isVIP = computed(() => user.value?.userRole === UserRole.VIP)
const isSVIP = computed(() => user.value?.userRole === UserRole.SVIP)

const getRoleText = computed(() => {
  switch (user.value?.userRole) {
    case UserRole.VIP:
      return 'VIP会员'
    case UserRole.SVIP:
      return 'SVIP会员'
    case UserRole.ADMIN:
      return '管理员'
    default:
      return '普通用户'
  }
})

// 处理用户相关操作
async function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      userInfoRef.value?.show()
      break
    case 'settings':
      settingsRef.value?.show()
      break
    case 'logout':
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
        ElMessage.success('已退出登录')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('退出失败')
        }
      }
      break
  }
}

// 处理升级按钮点击
const handleUpgrade = () => {
  router.push('/vip-plans')
}
</script>

<style scoped>
.user-profile-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upgrade-btn {
  font-size: 12px;
  padding: 4px 12px;
  height: 28px;
  border-radius: 14px;
  background: linear-gradient(45deg, var(--el-color-danger), var(--el-color-danger-light-3));
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.upgrade-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(var(--el-color-danger-rgb), 0.3);
}

@media (max-width: 768px) {
  .upgrade-btn {
    padding: 2px 8px;
    font-size: 11px;
    height: 24px;
  }
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.user-profile:hover {
  background: var(--el-fill-color-light);
}

.avatar-container {
  position: relative;
  width: 32px;
  height: 32px;
}

.avatar-halo {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  z-index: 0;
  animation: rotate 3s linear infinite;
}

.vip-halo {
  background: conic-gradient(
    from 0deg,
    transparent,
    #ffd700,
    #ffa500,
    transparent
  );
}

.svip-halo {
  background: conic-gradient(
    from 0deg,
    transparent,
    #ff00ff,
    #ff0000,
    transparent
  );
}

.user-avatar {
  position: relative;
  z-index: 1;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--el-color-success) 0%, var(--el-color-success-dark-2) 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.3s ease;
}

.vip-avatar {
  background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
  box-shadow: 0 0 10px rgba(255, 165, 0, 0.5);
}

.svip-avatar {
  background: linear-gradient(135deg, #ff00ff 0%, #ff0000 100%);
  box-shadow: 0 0 10px rgba(255, 0, 255, 0.5);
}

.user-info {
  display: flex;
  flex-direction: column;
  font-size: 12px;
}

.username {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.user-role {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.vip-role {
  color: #ffa500 !important;
  font-weight: 600;
  text-shadow: 0 0 2px rgba(255, 165, 0, 0.3);
}

.svip-role {
  color: #ff00ff !important;
  font-weight: 600;
  text-shadow: 0 0 2px rgba(255, 0, 255, 0.3);
}

.crown-icon {
  margin-left: 2px;
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 8px;
}

/* 深色模式适配 */
:deep(.dark) .user-profile:hover {
  background: var(--el-fill-color-dark);
}

:deep(.dark) .username {
  color: var(--el-text-color-primary);
}

:deep(.dark) .user-role {
  color: var(--el-text-color-secondary);
}

.avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: white;
}
</style> 