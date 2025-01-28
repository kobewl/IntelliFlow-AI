<template>
  <div class="vip-plans">
    <h1 class="title">会员套餐</h1>
    
    <div class="plan-tabs">
      <el-radio-group v-model="userType" size="large">
        <el-radio-button label="personal">个人</el-radio-button>
        <el-radio-button label="enterprise">企业</el-radio-button>
      </el-radio-group>
    </div>

    <div class="plans-container">
      <!-- NORMAL 套餐 -->
      <div class="plan-card">
        <div class="plan-header">
          <h2>NORMAL</h2>
          <div class="price">
            <span class="currency">¥</span>
            <span class="amount">0</span>
            <span class="period">/月</span>
          </div>
          <p class="description">基础功能免费体验</p>
        </div>
        <div class="plan-content">
          <el-button class="subscribe-btn" :class="{ 'current': currentPlan === 'NORMAL' }">
            {{ currentPlan === 'NORMAL' ? '当前套餐' : '免费使用' }}
          </el-button>
          <ul class="features">
            <li>
              <el-icon><Check /></el-icon>
              每天 10 次对话限制
            </li>
            <li>
              <el-icon><Check /></el-icon>
              基础AI助手对话
            </li>
            <li>
              <el-icon><Check /></el-icon>
              基础文本生成
            </li>
            <li>
              <el-icon><Check /></el-icon>
              社区支持
            </li>
          </ul>
        </div>
      </div>

      <!-- VIP 套餐 -->
      <div class="plan-card recommended">
        <div class="plan-header">
          <div class="badge">推荐</div>
          <h2>VIP</h2>
          <div class="price">
            <span class="currency">¥</span>
            <span class="amount">19.9</span>
            <span class="period">/月</span>
          </div>
          <p class="description">解锁更多高级功能</p>
        </div>
        <div class="plan-content">
          <el-button type="primary" class="subscribe-btn" :class="{ 'current': currentPlan === 'VIP' }" @click="handleSubscribe('VIP')">
            {{ currentPlan === 'VIP' ? '当前套餐' : '立即升级' }}
          </el-button>
          <ul class="features">
            <li>
              <el-icon><Check /></el-icon>
              包含 NORMAL 所有功能
            </li>
            <li>
              <el-icon><Check /></el-icon>
              每天 100 次对话
            </li>
            <li>
              <el-icon><Check /></el-icon>
              高级AI模型支持
            </li>
            <li>
              <el-icon><Check /></el-icon>
              优先响应速度
            </li>
            <li>
              <el-icon><Check /></el-icon>
              自定义AI助手
            </li>
            <li>
              <el-icon><Check /></el-icon>
              专属客服支持
            </li>
          </ul>
        </div>
      </div>

      <!-- SVIP 套餐 -->
      <div class="plan-card">
        <div class="plan-header">
          <h2>SVIP</h2>
          <div class="price">
            <span class="currency">¥</span>
            <span class="amount">49.9</span>
            <span class="period">/月</span>
          </div>
          <p class="description">尊享顶级特权服务</p>
        </div>
        <div class="plan-content">
          <el-button type="warning" class="subscribe-btn" :class="{ 'current': currentPlan === 'SVIP' }" @click="handleSubscribe('SVIP')">
            {{ currentPlan === 'SVIP' ? '当前套餐' : '尊享升级' }}
          </el-button>
          <ul class="features">
            <li>
              <el-icon><Check /></el-icon>
              包含 VIP 所有功能
            </li>
            <li>
              <el-icon><Check /></el-icon>
              无限次数对话
            </li>
            <li>
              <el-icon><Check /></el-icon>
              最新AI模型优先体验
            </li>
            <li>
              <el-icon><Check /></el-icon>
              API 接口调用
            </li>
            <li>
              <el-icon><Check /></el-icon>
              团队多人共享
            </li>
            <li>
              <el-icon><Check /></el-icon>
              7×24小时专属服务
            </li>
            <li>
              <el-icon><Check /></el-icon>
              个性化定制服务
            </li>
          </ul>
        </div>
      </div>
    </div>

    <div class="enterprise-hint" v-if="userType === 'enterprise'">
      <el-icon><InfoFilled /></el-icon>
      需要更多定制化服务？
      <el-link type="primary" @click="contactEnterprise">联系我们获取企业版</el-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Check, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { UserRole } from '../types/user'

const authStore = useAuthStore()
const userType = ref('personal')
const currentPlan = ref(authStore.user?.userRole || UserRole.NORMAL)

// 处理订阅
const handleSubscribe = (plan: string) => {
  if (currentPlan.value === plan) {
    ElMessage.info('您已经是该套餐用户')
    return
  }
  
  // TODO: 实现支付逻辑
  ElMessage.success('正在为您跳转到支付页面...')
}

// 联系企业版
const contactEnterprise = () => {
  ElMessage.success('正在为您跳转到企业咨询页面...')
  // TODO: 实现企业版咨询逻辑
}
</script>

<style scoped>
.vip-plans {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.title {
  text-align: center;
  font-size: 32px;
  margin-bottom: 40px;
  color: var(--el-text-color-primary);
}

.plan-tabs {
  text-align: center;
  margin-bottom: 40px;
}

.plans-container {
  display: flex;
  gap: 24px;
  justify-content: center;
  flex-wrap: wrap;
}

.plan-card {
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 32px;
  width: 320px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  position: relative;
  border: 1px solid var(--el-border-color-light);
}

.plan-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.plan-card.recommended {
  border: 2px solid var(--el-color-primary);
  transform: scale(1.05);
}

.plan-card.recommended:hover {
  transform: scale(1.05) translateY(-5px);
}

.badge {
  position: absolute;
  top: -12px;
  right: 24px;
  background: var(--el-color-primary);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
}

.plan-header {
  text-align: center;
  margin-bottom: 24px;
}

.plan-header h2 {
  font-size: 24px;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

.price {
  font-size: 48px;
  color: var(--el-text-color-primary);
  line-height: 1;
  margin-bottom: 16px;
}

.currency {
  font-size: 24px;
  vertical-align: super;
}

.period {
  font-size: 16px;
  color: var(--el-text-color-secondary);
}

.description {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.subscribe-btn {
  width: 100%;
  margin-bottom: 24px;
}

.subscribe-btn.current {
  background-color: var(--el-color-success);
  border-color: var(--el-color-success);
  color: white;
}

.features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.features li {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--el-text-color-regular);
}

.features li .el-icon {
  color: var(--el-color-success);
}

.enterprise-hint {
  text-align: center;
  margin-top: 40px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .vip-plans {
    padding: 20px;
  }

  .plans-container {
    flex-direction: column;
    align-items: center;
  }

  .plan-card {
    width: 100%;
    max-width: 320px;
  }

  .plan-card.recommended {
    transform: none;
  }

  .plan-card.recommended:hover {
    transform: translateY(-5px);
  }
}
</style> 
