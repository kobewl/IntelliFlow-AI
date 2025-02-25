<template>
  <div class="loading-container">
    <div class="loading-content">
      <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'
import { defineProps } from 'vue'

defineProps({
  title: {
    type: String,
    default: '加载中...'
  },
  message: {
    type: String,
    default: '请稍等片刻，我们正在处理您的请求'
  }
})
</script>

<script lang="ts">
export default {
  name: 'LoadingState'
}
</script>

<style scoped>
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.loading-icon {
  margin-bottom: 20px;
  animation: spin 1.5s linear infinite;
  color: var(--el-color-primary);
}

h3 {
  margin: 0 0 10px;
  font-size: 20px;
  color: var(--el-color-primary);
}

p {
  margin: 0;
  color: #606266;
  max-width: 300px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 深色模式 */
.dark-mode .loading-container {
  background-color: rgba(0, 0, 0, 0.7);
}

.dark-mode h3 {
  color: var(--el-color-primary-light-3);
}

.dark-mode p {
  color: #a6a6a6;
}
</style> 