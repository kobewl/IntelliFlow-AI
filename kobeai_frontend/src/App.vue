<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { useAuthStore } from './store/auth'

const router = useRouter()
const authStore = useAuthStore()

// 在应用启动时初始化认证状态
onMounted(async () => {
  try {
    await authStore.initializeAuth()
  } catch (error) {
    console.error('Failed to initialize auth state:', error)
  }
})

defineOptions({
  name: 'App'
})
</script>

<template>
  <el-config-provider :locale="zhCn">
    <router-view v-slot="{ Component }">
      <component :is="Component" />
    </router-view>
  </el-config-provider>
</template>

<style>
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji',
    'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji';
}

#app {
  height: 100vh;
}

* {
  box-sizing: border-box;
}

/* 全局过渡效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* 设置打开时的背景遮罩效果 */
.settings-open .app-container {
  filter: blur(3px);
  opacity: 0.5;
  transition: all 0.3s ease;
  pointer-events: none;
}

/* 确保设置对话框不受影响 */
.settings-open .el-overlay {
  backdrop-filter: none;
}

.settings-open .el-dialog {
  filter: none !important;
  opacity: 1 !important;
}
</style>
