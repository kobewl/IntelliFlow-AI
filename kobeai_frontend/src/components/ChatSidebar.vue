<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <el-button type="primary" class="new-chat-btn" @click="$emit('new-chat')">
        <el-icon><Plus /></el-icon>
        <span>新对话</span>
      </el-button>
    </div>
    
    <!-- 会话列表 -->
    <div class="conversation-list">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'

defineEmits(['new-chat'])
</script>

<script lang="ts">
export default {
  name: 'ChatSidebar'
}
</script>

<style scoped>
.chat-sidebar {
  display: flex;
  flex-direction: column;
  width: 260px;
  height: 100%;
  background-color: #f8f9fa;
  border-right: 1px solid #e6e6e6;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e6e6e6;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.new-chat-btn .el-icon {
  margin-right: 8px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}
</style> 