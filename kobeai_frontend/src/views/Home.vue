<template>
  <div class="home">
    <div class="bg-animation">
      <div class="gradient-bg"></div>
      <div class="cyber-grid"></div>
    </div>

    <!-- 第一页：主标题和按钮 -->
    <section class="hero" v-show="currentPage === 1" :class="{ 'fade-out': isTransitioning }">
      <div class="hero-content">
        <div class="logo-container">
          <div class="logo-circle">
            <el-icon :size="40" color="var(--el-color-primary)">
              <ChatRound />
            </el-icon>
          </div>
          <h1>KobeAI</h1>
        </div>
        <div class="tagline">
          <h2>智能对话，从未如此简单</h2>
          <p class="subtitle">智能对话助手，为您提供便捷的智能服务</p>
        </div>
        <div class="cta-buttons">
          <el-button
            type="primary"
            size="large"
            class="cta-button"
            @click="$router.push('/auth/login')"
          >
            立即登录
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button
            size="large"
            class="cta-button outline"
            @click="$router.push('/auth/register')"
          >
            免费注册
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="scroll-hint" @click="switchPage(2)">
          <span>了解更多</span>
          <el-icon class="scroll-icon"><ArrowDown /></el-icon>
        </div>
      </div>
    </section>

    <!-- 第二页：特性介绍 -->
    <section class="features" v-show="currentPage === 2" :class="{ 'fade-in': isTransitioning }">
      <div class="back-to-top" @click="switchPage(1)">
        <el-icon class="back-icon"><ArrowUp /></el-icon>
        <span>返回</span>
      </div>
      <div class="features-grid">
        <div class="feature-card" v-for="(feature, index) in features" :key="index">
          <div class="feature-content">
            <div class="feature-icon">
              <el-icon :size="36">
                <component :is="feature.icon" />
              </el-icon>
            </div>
            <h3>{{ feature.title }}</h3>
            <p v-html="feature.description"></p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowRight, ArrowDown, ArrowUp, ChatRound, Timer, Lock, Connection } from '@element-plus/icons-vue'

const currentPage = ref(1)
const isTransitioning = ref(false)

const features = [
  {
    icon: 'ChatRound',
    title: '智能对话',
    description: '基于先进的AI模型<br>提供流畅自然的对话体验'
  },
  {
    icon: 'Timer',
    title: '快速响应',
    description: '毫秒级响应速度<br>让您的对话畅通无阻'
  },
  {
    icon: 'Lock',
    title: '安全可靠',
    description: '严格的数据加密<br>和隐私保护机制'
  },
  {
    icon: 'Connection',
    title: '多场景支持',
    description: '适配多种对话场景<br>满足不同需求'
  }
]

const switchPage = (page: number) => {
  isTransitioning.value = true
  setTimeout(() => {
    currentPage.value = page
    setTimeout(() => {
      isTransitioning.value = false
    }, 300)
  }, 300)
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  position: relative;
  overflow: hidden;
}

.bg-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.gradient-bg {
  position: absolute;
  width: 150vw;
  height: 150vh;
  top: -25vh;
  left: -25vw;
  background: radial-gradient(circle at center,
    rgba(var(--el-color-primary-rgb), 0.1) 0%,
    rgba(var(--el-color-primary-rgb), 0.05) 25%,
    transparent 100%
  );
  animation: rotate 60s linear infinite;
  transform-origin: center center;
}

.cyber-grid {
  position: absolute;
  width: 200%;
  height: 200%;
  top: -50%;
  left: -50%;
  background-image: 
    linear-gradient(rgba(var(--el-color-primary-rgb), 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(var(--el-color-primary-rgb), 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  transform: perspective(1000px) rotateX(60deg);
  animation: grid-move 20s linear infinite;
}

.hero, .features {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.fade-out {
  opacity: 0;
  transform: translateY(-20px);
}

.fade-in {
  opacity: 0;
  transform: translateY(20px);
}

.hero-content {
  max-width: 800px;
  text-align: center;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 48px;
  animation: fadeInDown 1s ease;
}

.logo-circle {
  width: 80px;
  height: 80px;
  border-radius: 40px;
  background: linear-gradient(135deg, var(--el-color-primary-light-8) 0%, var(--el-color-primary-light-9) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 24px rgba(var(--el-color-primary-rgb), 0.15);
  transition: transform 0.3s ease;
}

.logo-circle:hover {
  transform: scale(1.05) rotate(5deg);
}

h1 {
  font-size: 48px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, #409eff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.tagline {
  margin-bottom: 64px;
  animation: fadeInUp 1s ease 0.3s both;
}

h2 {
  font-size: 64px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 24px;
  line-height: 1.2;
}

.subtitle {
  font-size: 24px;
  color: var(--el-text-color-regular);
  margin: 0;
}

.cta-buttons {
  display: flex;
  gap: 24px;
  justify-content: center;
  margin-bottom: 80px;
  animation: fadeInUp 1s ease 0.6s both;
}

.cta-button {
  min-width: 180px;
  height: 56px;
  font-size: 18px;
  font-weight: 500;
  border-radius: 28px;
  transition: all 0.3s ease;
}

.cta-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(var(--el-color-primary-rgb), 0.2);
}

.scroll-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  animation: bounce 2s infinite;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.scroll-hint:hover {
  opacity: 1;
}

.scroll-hint span {
  font-size: 16px;
  color: var(--el-text-color-regular);
}

.scroll-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.back-to-top {
  position: absolute;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.back-to-top:hover {
  opacity: 1;
}

.back-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 32px;
  box-shadow: 
    0 4px 6px rgba(0, 0, 0, 0.02),
    0 10px 15px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 
    0 8px 12px rgba(0, 0, 0, 0.03),
    0 16px 24px rgba(0, 0, 0, 0.04);
}

.feature-content {
  padding: 48px;
  text-align: center;
}

.feature-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 32px;
  background: linear-gradient(135deg, var(--el-color-primary-light-8) 0%, var(--el-color-primary-light-9) 100%);
  border-radius: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  transition: all 0.3s ease;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1) rotate(5deg);
  background: linear-gradient(135deg, var(--el-color-primary-light-7) 0%, var(--el-color-primary-light-8) 100%);
}

.feature-card h3 {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 16px;
}

.feature-card p {
  font-size: 16px;
  color: var(--el-text-color-regular);
  margin: 0;
  line-height: 1.8;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes grid-move {
  0% { transform: perspective(1000px) rotateX(60deg) translateY(0); }
  100% { transform: perspective(1000px) rotateX(60deg) translateY(50px); }
}

@media (max-width: 768px) {
  .hero-content {
    padding: 20px;
  }

  h1 {
    font-size: 36px;
  }

  h2 {
    font-size: 48px;
  }

  .subtitle {
    font-size: 20px;
  }

  .cta-buttons {
    flex-direction: column;
    gap: 16px;
  }

  .cta-button {
    width: 100%;
  }

  .features-grid {
    grid-template-columns: 1fr;
    padding: 60px 20px;
    gap: 24px;
  }

  .feature-content {
    padding: 32px;
  }

  .feature-card h3 {
    font-size: 24px;
  }
}
</style> 