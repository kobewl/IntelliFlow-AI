<template>
  <div class="chat-item" :class="role.toLowerCase()">
    <!-- 左侧消息（助手） -->
    <template v-if="role === MessageRole.ASSISTANT">
      <div class="avatar">
        <el-avatar :size="40" :icon="ChatRound" class="assistant-avatar" />
      </div>
      <div class="content">
        <div class="bubble">
          <MarkdownRenderer :content="content" />
        </div>
        <div class="time">{{ formatTime(createdAt) }}</div>
      </div>
    </template>

    <!-- 右侧消息（用户） -->
    <template v-else>
      <div class="content">
        <div class="bubble">
          <MarkdownRenderer :content="content" />
        </div>
        <div class="time">{{ formatTime(createdAt) }}</div>
      </div>
      <div class="avatar">
        <template v-if="user?.avatar">
          <el-image
            class="user-avatar-image"
            :src="user.avatar"
            fit="cover"
          >
            <template #error>
              <el-avatar :size="40" :icon="User" class="user-avatar-fallback" />
            </template>
          </el-image>
        </template>
        <template v-else>
          <el-avatar :size="40" :icon="User" class="user-avatar-fallback" />
        </template>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { User, ChatRound } from '@element-plus/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'
import { MessageRole } from '../types/chat'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)

defineProps<{
  role: MessageRole
  content: string
  createdAt: string
}>()

// 格式化时间
function formatTime(timestamp: string) {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // 24小时内显示相对时间
  if (diff < 24 * 60 * 60 * 1000) {
    if (diff < 60 * 1000) return '刚刚'
    if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)}分钟前`
    return `${Math.floor(diff / 3600000)}小时前`
  }
  
  // 超过24小时显示具体日期
  return date.toLocaleString()
}

defineOptions({
  name: 'ChatMessage'
})
</script>

<script lang="ts">
export default {
  name: 'ChatMessage'
}
</script>

<style scoped>
.chat-item {
  display: flex;
  align-items: flex-start;
  padding: 1px 16px;
  margin: 0;
  transition: background-color 0.2s ease;
}

.chat-item:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  margin-top: 4px;
  border-radius: 50%;
  overflow: hidden;
}

.user-avatar-image {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.user-avatar-fallback,
.assistant-avatar {
  width: 40px !important;
  height: 40px !important;
  border-radius: 50% !important;
  background: #fff;
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.user-avatar-image:hover,
.user-avatar-fallback:hover,
.assistant-avatar:hover {
  transform: scale(1.05);
}

.content {
  max-width: 85%;
  margin: 0 8px;
  position: relative;
}

/* 用户消息样式 */
.user .content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user .bubble {
  background: linear-gradient(135deg, #00B2FF 0%, #0085FF 100%);
  color: white;
  border-radius: 18px 18px 4px 18px;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 133, 255, 0.15);
  transform-origin: right bottom;
}

.user .bubble:hover {
  background: linear-gradient(135deg, #00B9FF 0%, #008CFF 100%);
}

/* 助手消息样式 */
.assistant .content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.assistant .bubble {
  background: #ffffff;
  color: #1a1a1a;
  border-radius: 18px 18px 18px 4px;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform-origin: left bottom;
}

.bubble {
  padding: 8px 14px;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
  transition: all 0.2s ease;
  animation: bubbleIn 0.3s ease-out;
}

.bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.time {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
  padding: 0 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.chat-item:hover .time {
  opacity: 0.8;
}

/* 深色模式 */
:deep(.dark) .chat-item:hover {
  background-color: rgba(255, 255, 255, 0.02);
}

:deep(.dark) .assistant .bubble {
  background: #2C2C2C;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

:deep(.dark) .user .bubble {
  background: linear-gradient(135deg, #0099FF 0%, #0070FF 100%);
  color: white;
}

:deep(.dark) .time {
  color: #888;
}

/* 动画效果 */
@keyframes bubbleIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(5px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .chat-item {
    padding: 1px 12px;
  }

  .content {
    max-width: 90%;
    margin: 0 6px;
  }

  .avatar {
    width: 24px;
    height: 24px;
    margin-top: 3px;
  }

  :deep(.el-avatar) {
    width: 24px !important;
    height: 24px !important;
  }

  .bubble {
    padding: 6px 12px;
    font-size: 13px;
    border-radius: 16px 16px 16px 4px;
  }

  .user .bubble {
    border-radius: 16px 16px 4px 16px;
  }

  .time {
    font-size: 10px;
    margin-top: 1px;
  }
}

/* 代码块样式优化 */
:deep(pre) {
  margin: 6px 0;
  padding: 12px;
  border-radius: 8px;
  background: #1E1E1E !important;
  overflow-x: auto;
  position: relative;
}

:deep(pre code) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #D4D4D4;
}

:deep(.dark) pre {
  background: #000000 !important;
}

/* 代码块滚动条美化 */
:deep(pre::-webkit-scrollbar) {
  height: 6px;
}

:deep(pre::-webkit-scrollbar-thumb) {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

:deep(pre::-webkit-scrollbar-track) {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}
</style> 