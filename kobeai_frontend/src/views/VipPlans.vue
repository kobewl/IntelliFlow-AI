<template>
  <div class="vip-plans-container">
    <h1 class="title">升级套餐</h1>
    
    <div class="plan-type-switch">
      <span :class="{ active: planType === 'personal' }" @click="planType = 'personal'">个人</span>
      <span :class="{ active: planType === 'enterprise' }" @click="planType = 'enterprise'">企业</span>
    </div>

    <div class="plans-wrapper">
      <!-- Normal Plan -->
      <div class="plan-card">
        <h2>Normal</h2>
        <div class="price">
          <span class="currency">¥</span>
          <span class="amount">0</span>
          <span class="period">/月</span>
        </div>
        <p class="description">基础功能免费体验</p>
        <button class="current-plan">您当前的套餐</button>
        <div class="features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>每日免费对话次数：10次</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>基础AI模型对话</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>基础问答功能</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>基础文本生成</span>
          </div>
        </div>
        <div class="plan-footer">
          <a href="#" class="help-link">了解更多</a>
        </div>
      </div>

      <!-- VIP Plan -->
      <div class="plan-card highlighted">
        <div class="plan-badge">推荐</div>
        <h2>VIP</h2>
        <div class="price">
          <span class="currency">¥</span>
          <span class="amount">29</span>
          <span class="period">/月</span>
        </div>
        <p class="description">解锁更多高级功能</p>
        <button class="upgrade-button" @click="handleUpgrade('vip')">升级 VIP</button>
        <div class="features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>包含 Normal 所有功能</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>每日对话次数：100次</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>高级AI模型对话</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>图片生成功能</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>文档解析功能</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>优先客服支持</span>
          </div>
        </div>
        <div class="plan-footer">
          <span class="promo">限时优惠</span>
        </div>
      </div>

      <!-- SVIP Plan -->
      <div class="plan-card">
        <h2>SVIP</h2>
        <div class="price">
          <span class="currency">¥</span>
          <span class="amount">99</span>
          <span class="period">/月</span>
        </div>
        <p class="description">尊享顶级AI体验</p>
        <button class="upgrade-button" @click="handleUpgrade('svip')">升级 SVIP</button>
        <div class="features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>包含 VIP 所有功能</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>无限制对话次数</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>最新AI模型优先体验</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>高级图片生成</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>API接口调用</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>24小时专属客服</span>
          </div>
        </div>
        <div class="plan-footer">
          <span class="promo">尊享特权</span>
        </div>
      </div>
    </div>

    <div class="enterprise-info">
      <div class="icon-wrapper">
        <el-icon><User /></el-icon>
      </div>
      <p>贵组织需要更多功能？</p>
      <a href="#" class="enterprise-link">查看 ChatGPT Enterprise</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Check, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const planType = ref('personal')

const handleUpgrade = (plan: string) => {
  ElMessage.success(`正在升级到 ${plan} 套餐，即将跳转到支付页面...`)
}
</script>

<style scoped>
.vip-plans-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.title {
  text-align: center;
  font-size: 32px;
  margin-bottom: 40px;
}

.plan-type-switch {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 40px;
  background: var(--el-fill-color-light);
  padding: 4px;
  border-radius: 20px;
  width: fit-content;
  margin: 0 auto 40px;
}

.plan-type-switch span {
  padding: 8px 24px;
  cursor: pointer;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.plan-type-switch span.active {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.plans-wrapper {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.plan-card {
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 32px;
  position: relative;
  transition: all 0.3s ease;
  border: 1px solid var(--el-border-color-light);
}

.plan-card.highlighted {
  border: 2px solid var(--el-color-primary);
  transform: scale(1.02);
}

.plan-badge {
  position: absolute;
  top: -12px;
  right: 24px;
  background: var(--el-color-primary);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
}

.plan-card h2 {
  font-size: 24px;
  margin-bottom: 16px;
}

.price {
  margin-bottom: 24px;
}

.currency {
  font-size: 24px;
  vertical-align: top;
}

.amount {
  font-size: 48px;
  font-weight: bold;
}

.period {
  font-size: 16px;
  color: var(--el-text-color-secondary);
}

.description {
  color: var(--el-text-color-secondary);
  margin-bottom: 24px;
  min-height: 48px;
}

button {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: none;
  font-size: 16px;
  cursor: pointer;
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

.current-plan {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

.upgrade-button {
  background: var(--el-color-primary);
  color: white;
}

.upgrade-button:hover {
  opacity: 0.9;
}

.features {
  margin-bottom: 24px;
}

.feature-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.feature-item .el-icon {
  color: var(--el-color-success);
  font-size: 20px;
  flex-shrink: 0;
}

.plan-footer {
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.help-link {
  color: var(--el-color-primary);
  text-decoration: none;
}

.help-link:hover {
  text-decoration: underline;
}

.enterprise-info {
  text-align: center;
  margin-top: 60px;
  padding: 40px;
  background: var(--el-fill-color-light);
  border-radius: 16px;
}

.icon-wrapper {
  width: 48px;
  height: 48px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.icon-wrapper .el-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.enterprise-info p {
  margin-bottom: 8px;
  font-size: 16px;
}

.enterprise-link {
  color: var(--el-color-primary);
  text-decoration: none;
  font-weight: 500;
}

.enterprise-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .plans-wrapper {
    grid-template-columns: 1fr;
  }

  .plan-card.highlighted {
    transform: none;
  }
}
</style> 
