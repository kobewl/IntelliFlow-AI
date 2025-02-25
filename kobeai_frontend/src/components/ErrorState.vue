<template>
  <div class="error-container">
    <div class="error-content">
      <el-icon class="error-icon" :size="48"><WarningFilled /></el-icon>
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
      <div class="actions">
        <el-button type="primary" @click="$emit('retry')">重试</el-button>
        <el-button @click="$emit('cancel')">取消</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { WarningFilled } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '出错了'
  },
  message: {
    type: String,
    default: '发生了一些错误，请稍后重试'
  }
})

defineEmits(['retry', 'cancel'])
</script>

<script lang="ts">
export default {
  name: 'ErrorState'
}
</script>

<style scoped>
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 400px;
  padding: 30px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.error-icon {
  margin-bottom: 20px;
  color: var(--el-color-danger);
}

h3 {
  margin: 0 0 10px;
  font-size: 20px;
  color: var(--el-color-danger);
}

p {
  margin: 0 0 20px;
  color: #606266;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

/* 深色模式 */
.dark-mode .error-container {
  background-color: rgba(0, 0, 0, 0.7);
}

.dark-mode .error-content {
  background-color: #222;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.dark-mode h3 {
  color: var(--el-color-danger-light-3);
}

.dark-mode p {
  color: #a6a6a6;
}
</style>