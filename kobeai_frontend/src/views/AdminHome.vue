<template>
  <div class="admin-home">
    <el-row :gutter="20" class="welcome-section">
      <el-col :span="24">
        <el-card>
          <div class="welcome-content">
            <h1>欢迎回来，{{ authStore.user?.username }}</h1>
            <p>管理员控制面板</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stats-section">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon"><User /></el-icon>
            <div class="stat-info">
              <span class="stat-label">总用户数</span>
              <span class="stat-value">{{ stats.totalUsers }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon"><ChatDotRound /></el-icon>
            <div class="stat-info">
              <span class="stat-label">今日对话数</span>
              <span class="stat-value">{{ stats.todayChats }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon"><Star /></el-icon>
            <div class="stat-info">
              <span class="stat-label">会员用户数</span>
              <span class="stat-value">{{ stats.vipUsers }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon"><Bell /></el-icon>
            <div class="stat-info">
              <span class="stat-label">未处理反馈</span>
              <span class="stat-value">{{ stats.pendingFeedbacks }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="management-section">
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/admin/chats')" class="management-card">
          <div class="card-content">
            <el-icon class="management-icon"><ChatDotRound /></el-icon>
            <h2>对话管理</h2>
            <p>查看和管理所有用户的对话记录，监控对话质量</p>
            <ul class="feature-list">
              <li>查看对话历史</li>
              <li>审核对话内容</li>
              <li>管理敏感词</li>
            </ul>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/admin/users')" class="management-card">
          <div class="card-content">
            <el-icon class="management-icon"><User /></el-icon>
            <h2>用户管理</h2>
            <p>管理用户账号、权限和会员状态</p>
            <ul class="feature-list">
              <li>用户信息管理</li>
              <li>权限设置</li>
              <li>会员管理</li>
            </ul>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/admin/notifications')" class="management-card">
          <div class="card-content">
            <el-icon class="management-icon"><Bell /></el-icon>
            <h2>通知管理</h2>
            <p>发布和管理系统通知，处理用户反馈</p>
            <ul class="feature-list">
              <li>发布系统通知</li>
              <li>处理用户反馈</li>
              <li>通知历史记录</li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="quick-actions">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <h2>快捷操作</h2>
            </div>
          </template>
          <div class="quick-actions-content">
            <el-button type="primary" @click="router.push('/admin/notifications/new')">
              <el-icon><Plus /></el-icon>
              发布通知
            </el-button>
            <el-button type="success" @click="router.push('/admin/users/new')">
              <el-icon><Plus /></el-icon>
              添加用户
            </el-button>
            <el-button type="warning" @click="router.push('/admin/feedbacks')">
              <el-icon><Warning /></el-icon>
              处理反馈
            </el-button>
            <el-button type="info" @click="router.push('/admin/settings')">
              <el-icon><Setting /></el-icon>
              系统设置
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { 
  User, 
  ChatDotRound, 
  Star, 
  Bell, 
  Plus, 
  Warning, 
  Setting 
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const stats = ref({
  totalUsers: 0,
  todayChats: 0,
  vipUsers: 0,
  pendingFeedbacks: 0
})

onMounted(async () => {
  try {
    // TODO: 从API获取统计数据
    const response = await fetch('/api/admin/stats', {
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    if (response.ok) {
      const data = await response.json()
      stats.value = data
    }
  } catch (error) {
    console.error('Failed to fetch admin stats:', error)
  }
})
</script>

<style scoped>
.admin-home {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.welcome-section {
  margin-bottom: 24px;
}

.welcome-content {
  text-align: center;
  padding: 20px;
}

.welcome-content h1 {
  margin: 0;
  font-size: 24px;
  color: var(--el-text-color-primary);
}

.welcome-content p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
}

.stats-section {
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
}

.stat-icon {
  font-size: 32px;
  color: var(--el-color-primary);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.management-section {
  margin-bottom: 24px;
}

.management-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
}

.management-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--el-box-shadow-light);
}

.card-content {
  padding: 20px;
  text-align: center;
}

.management-icon {
  font-size: 48px;
  color: var(--el-color-primary);
  margin-bottom: 16px;
}

.card-content h2 {
  margin: 0 0 12px;
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.card-content p {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  text-align: left;
}

.feature-list li {
  margin: 8px 0;
  color: var(--el-text-color-regular);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.feature-list li::before {
  content: "•";
  color: var(--el-color-primary);
}

.quick-actions {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.quick-actions-content {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.quick-actions-content .el-button {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .admin-home {
    padding: 10px;
  }

  .stats-section .el-col,
  .management-section .el-col {
    margin-bottom: 16px;
  }

  .quick-actions-content {
    flex-direction: column;
  }

  .quick-actions-content .el-button {
    width: 100%;
  }
}
</style> 