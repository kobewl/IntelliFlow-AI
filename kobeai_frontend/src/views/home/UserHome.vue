<template>
  <div class="user-home">
    <!-- 欢迎标语 -->
    <div class="welcome-section">
      <h1>欢迎使用 KobeAI 智能助手平台，{{ userStore.userInfo?.username }}</h1>
      <p>一站式智能服务平台，为您提供全方位的AI助手服务</p>
    </div>

    <!-- 功能卡片区域 -->
    <div class="feature-cards">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="feature-card" @click="router.push('/chat')">
            <div class="card-content">
              <i class="el-icon chat-icon">
                <el-icon><ChatLineRound /></el-icon>
              </i>
              <h3>AI对话</h3>
              <p>智能对话助手，解答您的各种问题</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="feature-card" @click="router.push('/notifications')">
            <div class="card-content">
              <i class="el-icon notification-icon">
                <el-icon><Bell /></el-icon>
              </i>
              <h3>系统通知</h3>
              <p>及时获取平台最新动态和重要通知</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="feature-card" @click="router.push('/knowledge')">
            <div class="card-content">
              <i class="el-icon knowledge-icon">
                <el-icon><Collection /></el-icon>
              </i>
              <h3>知识库</h3>
              <p>海量资源，助力学习和工作</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="mt-4">
        <el-col :span="8">
          <el-card class="feature-card" @click="router.push('/tools')">
            <div class="card-content">
              <i class="el-icon tools-icon">
                <el-icon><Tools /></el-icon>
              </i>
              <h3>工具箱</h3>
              <p>实用工具集合，提高工作效率</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="feature-card" @click="router.push('/profile')">
            <div class="card-content">
              <i class="el-icon profile-icon">
                <el-icon><User /></el-icon>
              </i>
              <h3>个人中心</h3>
              <p>管理个人信息和使用记录</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="feature-card" @click="router.push('/vip-plans')">
            <div class="card-content">
              <i class="el-icon vip-icon">
                <el-icon><Star /></el-icon>
              </i>
              <h3>会员专区</h3>
              <p>解锁更多高级功能和特权</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 使用统计 -->
    <div class="usage-stats">
      <h2>使用统计</h2>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <h4>对话次数</h4>
              <p class="stat-number">{{ stats.chatCount || 128 }}</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <h4>使用时长</h4>
              <p class="stat-number">{{ stats.usageTime || '6小时0分钟' }}</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <h4>剩余积分</h4>
              <p class="stat-number">{{ stats.remainingPoints || 1000 }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <h2>快速入口</h2>
      <div class="action-buttons">
        <el-button type="primary" @click="router.push('/chat')">开始对话</el-button>
        <el-button type="success" @click="router.push('/notifications')">查看通知记录</el-button>
        <el-button type="warning" @click="router.push('/vip-plans')">升级会员</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ChatLineRound, Bell, Collection, Tools, User, Star } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 使用统计数据
const stats = ref({
  chatCount: 0,
  usageTime: '',
  remainingPoints: 0
})

// 获取统计数据
onMounted(async () => {
  // TODO: 从API获取实际统计数据
})
</script>

<style scoped>
.user-home {
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

.feature-cards {
  margin-bottom: 40px;
}

.feature-card {
  height: 180px;
  cursor: pointer;
  transition: all 0.3s;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.el-icon {
  font-size: 40px;
  margin-bottom: 15px;
  color: #409EFF;
}

.card-content h3 {
  margin: 10px 0;
  font-size: 18px;
  color: #303133;
}

.card-content p {
  color: #606266;
  font-size: 14px;
}

.usage-stats {
  margin-bottom: 40px;
}

.usage-stats h2 {
  margin-bottom: 20px;
  font-size: 20px;
  color: #303133;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-content h4 {
  color: #606266;
  font-size: 16px;
  margin-bottom: 10px;
}

.stat-number {
  font-size: 24px;
  color: #409EFF;
  font-weight: bold;
}

.quick-actions {
  text-align: center;
}

.quick-actions h2 {
  margin-bottom: 20px;
  font-size: 20px;
  color: #303133;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.mt-4 {
  margin-top: 20px;
}
</style> 