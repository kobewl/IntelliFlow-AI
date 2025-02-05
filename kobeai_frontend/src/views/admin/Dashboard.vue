<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><User /></el-icon>
              <span>总用户数</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.totalUsers }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><ChatDotRound /></el-icon>
              <span>今日对话数</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.todayChats }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Star /></el-icon>
              <span>全部用户数</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.totalUsers }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Bell /></el-icon>
              <span>未处理反馈</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.pendingFeedbacks }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 功能卡片 -->
    <el-row :gutter="20" class="feature-cards">
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/admin/chats')">
          <div class="feature-card">
            <el-icon><ChatDotRound /></el-icon>
            <h3>对话管理</h3>
            <p>查看和管理所有用户的对话记录</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/admin/users')">
          <div class="feature-card">
            <el-icon><User /></el-icon>
            <h3>用户管理</h3>
            <p>管理用户账号和权限设置</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/admin/notifications')">
          <div class="feature-card">
            <el-icon><Bell /></el-icon>
            <h3>通知管理</h3>
            <p>发布和管理系统通知</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h2>快捷操作</h2>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" @click="router.push('/admin/notifications/create')">
            <el-icon><Plus /></el-icon>
            发布通知
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" @click="router.push('/admin/users/create')">
            <el-icon><Plus /></el-icon>
            添加用户
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" @click="router.push('/admin/feedbacks')">
            <el-icon><Warning /></el-icon>
            处理反馈
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button @click="router.push('/admin/settings')">
            <el-icon><Setting /></el-icon>
            系统设置
          </el-button>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, ChatDotRound, Star, Bell, Plus, Warning, Setting } from '@element-plus/icons-vue'
import { adminApi } from '../../api/admin'

const router = useRouter()

// 统计数据
const stats = ref({
  totalUsers: 0,
  todayChats: 0,
  totalChats: 0,
  pendingFeedbacks: 0
})

// 获取统计数据
const getStats = async () => {
  try {
    const res = await adminApi.getStats()
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

onMounted(() => {
  getStats()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.stat-card :deep(.card-header) {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-regular);
}

.stat-card :deep(.card-header .el-icon) {
  font-size: 20px;
}

.stat-card :deep(.stat-value) {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  text-align: center;
  margin-top: 10px;
}

.feature-cards {
  margin-top: 20px;
}

.feature-card {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
}

.feature-card .el-icon {
  font-size: 40px;
  color: var(--el-color-primary);
  margin-bottom: 16px;
}

.feature-card h3 {
  margin: 10px 0;
  color: var(--el-text-color-primary);
}

.feature-card p {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.quick-actions {
  margin-top: 40px;
}

.quick-actions h2 {
  margin-bottom: 20px;
  color: var(--el-text-color-primary);
}

.quick-actions .el-button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.el-card {
  height: 100%;
}
</style> 