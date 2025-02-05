<template>
  <div class="admin-home">
    <!-- 欢迎标语 -->
    <div class="welcome-section">
      <h1>欢迎回来，{{ userStore.userInfo?.username }}</h1>
      <p>管理员控制面板</p>
    </div>

    <!-- 数据统计卡片 -->
    <div class="statistics">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <h3>总用户数</h3>
              <p class="stat-number">{{ stats.totalUsers || 0 }}</p>
              <div class="stat-trend">
                <span>较昨日</span>
                <span class="trend-up">+{{ stats.newUsers || 0 }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <h3>今日对话数</h3>
              <p class="stat-number">{{ stats.todayChats || 0 }}</p>
              <div class="stat-trend">
                <span>较昨日</span>
                <span class="trend-up">+{{ stats.chatIncrease || 0 }}%</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <h3>VIP用户数</h3>
              <p class="stat-number">{{ stats.vipUsers || 0 }}</p>
              <div class="stat-trend">
                <span>转化率</span>
                <span class="trend-up">{{ stats.vipRate || 0 }}%</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <h3>未处理反馈</h3>
              <p class="stat-number">{{ stats.pendingFeedbacks || 0 }}</p>
              <div class="stat-trend">
                <span>待处理</span>
                <span class="trend-down">{{ stats.pendingRate || 0 }}%</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 管理功能区 -->
    <div class="management-section">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="management-card" shadow="hover" @click="router.push('/admin/users')">
            <div class="card-content">
              <el-icon class="management-icon"><User /></el-icon>
              <h3>用户管理</h3>
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
          <el-card class="management-card" shadow="hover" @click="router.push('/admin/chat')">
            <div class="card-content">
              <el-icon class="management-icon"><ChatLineRound /></el-icon>
              <h3>对话管理</h3>
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
          <el-card class="management-card" shadow="hover" @click="router.push('/admin/notifications')">
            <div class="card-content">
              <el-icon class="management-icon"><Bell /></el-icon>
              <h3>通知管理</h3>
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
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <el-button type="primary" @click="router.push('/admin/users/add')">添加用户</el-button>
      <el-button type="success" @click="router.push('/admin/notifications/new')">发布通知</el-button>
      <el-button type="warning" @click="handleSystemSettings">系统设置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, ChatLineRound, Bell } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 统计数据
const stats = ref({
  totalUsers: 0,
  newUsers: 0,
  todayChats: 0,
  chatIncrease: 0,
  vipUsers: 0,
  vipRate: 0,
  pendingFeedbacks: 0,
  pendingRate: 0
})

// 获取统计数据
onMounted(async () => {
  // TODO: 从API获取实际统计数据
})

// 系统设置
const handleSystemSettings = () => {
  // TODO: 实现系统设置功能
}
</script>

<style scoped>
.admin-home {
  padding: 20px;
}

.welcome-section {
  text-align: center;
  margin-bottom: 40px;
}

.welcome-section h1 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 10px;
}

.welcome-section p {
  color: #606266;
  font-size: 16px;
}

.statistics {
  margin-bottom: 40px;
}

.stat-card {
  height: 140px;
}

.stat-content {
  text-align: center;
}

.stat-content h3 {
  font-size: 16px;
  color: #606266;
  margin-bottom: 15px;
}

.stat-number {
  font-size: 28px;
  color: #303133;
  font-weight: bold;
  margin-bottom: 10px;
}

.stat-trend {
  font-size: 14px;
  color: #909399;
}

.trend-up {
  color: #67C23A;
  margin-left: 5px;
}

.trend-down {
  color: #F56C6C;
  margin-left: 5px;
}

.management-section {
  margin-bottom: 40px;
}

.management-card {
  height: 280px;
  cursor: pointer;
  transition: all 0.3s;
}

.management-card:hover {
  transform: translateY(-5px);
}

.card-content {
  text-align: center;
  padding: 20px;
}

.management-icon {
  font-size: 40px;
  color: #409EFF;
  margin-bottom: 15px;
}

.card-content h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 10px;
}

.card-content p {
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
}

.feature-list {
  text-align: left;
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
  padding-left: 20px;
  position: relative;
}

.feature-list li::before {
  content: "•";
  color: #409EFF;
  position: absolute;
  left: 0;
}

.quick-actions {
  text-align: center;
}

.quick-actions .el-button {
  margin: 0 10px;
}
</style> 