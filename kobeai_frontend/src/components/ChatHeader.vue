<template>
  <div class="chat-header">
    <div class="chat-title">
      <el-icon><ChatRound /></el-icon>
      <span>{{ title || '新对话' }}</span>
    </div>
    
    <div class="header-actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatRound } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: ''
  }
})
</script>

<script lang="ts">
export default {
  name: 'ChatHeader'
}
</script>

<style scoped>
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--el-border-color-light, #e6e6e6);
  background-color: rgba(var(--el-bg-color-rgb, 255, 255, 255), 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  height: 64px;
  transition: all 0.3s ease;
  position: sticky;
  top: 0;
  z-index: 10;
}

:global(.dark-mode) .chat-header {
  background-color: rgba(30, 31, 44, 0.9);
  border-color: #2f3241;
}

.chat-title {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 500;
  gap: 10px;
  color: var(--el-text-color-primary, #303133);
  transition: color 0.3s ease;
}

:global(.dark-mode) .chat-title {
  color: #c0caf5;
}

.chat-title .el-icon {
  color: var(--el-color-primary, #409eff);
  font-size: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  padding: 6px;
  transition: all 0.3s ease;
}

:global(.dark-mode) .chat-title .el-icon {
  color: #7aa2f7;
  background-color: rgba(122, 162, 247, 0.1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .chat-header {
    padding: 12px 16px;
    height: 56px;
  }
  
  .chat-title {
    font-size: 16px;
  }
}
</style> 