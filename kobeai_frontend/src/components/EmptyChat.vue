<template>
  <div class="empty-chat">
    <el-icon :size="64" class="empty-icon">
      <ChatRound />
    </el-icon>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
  </div>
</template>

<script setup lang="ts">
import { ChatRound } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '开始新的对话'
  },
  description: {
    type: String,
    default: '发送消息开始与AI助手对话'
  }
})
</script>

<script lang="ts">
export default {
  name: 'EmptyChat'
}
</script>

<style scoped>
.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  color: #666;
}

.empty-icon {
  margin-bottom: 20px;
  color: var(--el-color-primary-light-3);
}

h2 {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 500;
}

p {
  margin: 0;
  font-size: 14px;
}
</style> 