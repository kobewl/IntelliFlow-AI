<template>
  <div class="home-container">
    <div class="user-profile">
      <div class="user-header">
        <el-avatar :size="64" :src="userInfo?.avatar || '/default-avatar.png'" />
        <div class="user-info">
          <h2>{{ userInfo?.username }}</h2>
          <p class="user-role">{{ formatRole(userInfo?.userRole) }}</p>
        </div>
      </div>
      
      <div class="user-details">
        <div class="detail-item">
          <el-icon><Message /></el-icon>
          <span>{{ userInfo?.email || '未设置邮箱' }}</span>
        </div>
        <div class="detail-item">
          <el-icon><Phone /></el-icon>
          <span>{{ userInfo?.phone || '未设置手机' }}</span>
        </div>
        <div class="detail-item">
          <el-icon><Timer /></el-icon>
          <span>会员到期时间：{{ formatDate(userInfo?.membershipEndTime) }}</span>
        </div>
        <div class="detail-item">
          <el-icon><Edit /></el-icon>
          <span>{{ userInfo?.bio || '这个人很懒，什么都没写~' }}</span>
        </div>
      </div>

      <div class="action-buttons">
        <el-button type="primary" @click="handleEditProfile">编辑资料</el-button>
        <el-button type="success" @click="handleUpgrade">升级会员</el-button>
      </div>
    </div>

    <el-divider />

    <div class="quick-actions">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="action-card" @click="router.push('/chat')">
            <el-icon><ChatDotRound /></el-icon>
            <h3>开始对话</h3>
            <p>与AI助手进行智能对话</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="action-card" @click="router.push('/templates')">
            <el-icon><Document /></el-icon>
            <h3>提示词模板</h3>
            <p>使用预设模板快速开始</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="action-card" @click="router.push('/notifications')">
            <el-icon><Bell /></el-icon>
            <h3>系统通知</h3>
            <p>查看最新系统通知</p>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Message,
  Phone,
  Timer,
  Edit,
  ChatDotRound,
  Document,
  Bell
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = ref(userStore.userInfo)

// 格式化用户角色
const formatRole = (role: string) => {
  const roleMap = {
    'NORMAL': '普通用户',
    'VIP': 'VIP会员',
    'SVIP': 'SVIP会员',
    'ADMIN': '管理员'
  }
  return roleMap[role] || role
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '未开通会员'
  return new Date(date).toLocaleDateString()
}

// 处理编辑资料
const handleEditProfile = () => {
  router.push('/profile/edit')
}

// 处理升级会员
const handleUpgrade = () => {
  router.push('/membership')
}

onMounted(() => {
  // 如果需要，这里可以重新获取最新的用户信息
  userStore.getUserInfo()
})
</script>

<style scoped>
.home-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.user-profile {
  background: var(--el-bg-color);
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.user-info h2 {
  margin: 0;
  color: var(--el-text-color-primary);
}

.user-info .user-role {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
}

.user-details .detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  color: var(--el-text-color-regular);
}

.user-details .detail-item .el-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.action-buttons {
  display: flex;
  gap: 15px;
  margin-top: 20px;
}

.quick-actions {
  margin-top: 30px;
}

.action-card {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.action-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2);
}

.action-card .el-icon {
  font-size: 40px;
  color: var(--el-color-primary);
  margin-bottom: 15px;
}

.action-card h3 {
  margin: 10px 0;
  color: var(--el-text-color-primary);
}

.action-card p {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 0;
}
</style> 