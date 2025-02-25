<template>
  <div class="virtual-message-list" @scroll="handleScroll" ref="containerRef">
    <!-- 加载更多指示器 -->
    <div v-if="hasMore" class="loading-more" :class="{ 'active': loadingMore }">
      <div v-if="loadingMore" class="spinner"></div>
      <el-button v-else size="small" @click="$emit('load-more')">加载更多</el-button>
    </div>
    
    <!-- 消息列表 -->
    <div v-for="message in visibleMessages" :key="message.id" 
         :class="['message-wrapper', message.isUser ? 'user-message-wrapper' : 'ai-message-wrapper']">
      <div class="message-container">
        <!-- AI消息的头像 -->
        <div v-if="!message.isUser" class="avatar-container">
          <el-avatar :size="36" :src="aiAvatar" alt="AI助手" />
        </div>
        
        <!-- 消息气泡 -->
        <div class="message-bubble" :class="{'user-bubble': message.isUser, 'ai-bubble': !message.isUser}">
          <div class="message-header">
            <span class="message-role">{{ message.isUser ? '我' : 'AI助手' }}</span>
            <span class="message-time">
              {{ formatTime(message.createdAt) }}
              <span v-if="message.updatedAt && message.updatedAt !== message.createdAt" class="edited-tag">已编辑</span>
            </span>
          </div>
          <div class="message-body">
            <MessageRenderer :content="message.content" :useMarkdown="!message.isUser" />
          </div>
        </div>
        
        <!-- 用户消息的头像 -->
        <div v-if="message.isUser" class="avatar-container">
          <el-avatar :size="36" :src="userAvatar" alt="用户" />
        </div>
      </div>
    </div>
    
    <!-- 空状态提示 -->
    <div v-if="messages.length === 0" class="empty-message">
      <p>没有消息记录</p>
    </div>
    
    <!-- 底部参考元素，用于自动滚动到底部 -->
    <div ref="bottomRef" class="scroll-target"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, nextTick } from 'vue'
import { format, isToday, isYesterday } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import MessageRenderer from './MessageRenderer.vue'

// 消息类型定义
interface Message {
  id: number | string
  role: string
  content: string
  createdAt: string
  updatedAt?: string
  isDeleted?: boolean
}

// 组件属性
const props = defineProps({
  messages: {
    type: Array as () => Message[],
    required: true
  },
  hasMore: {
    type: Boolean,
    default: false
  },
  loadingMore: {
    type: Boolean,
    default: false
  },
  userAvatar: {
    type: String,
    default: '/avatars/default-avatar.png'
  },
  aiAvatar: {
    type: String,
    default: '/images/ai-avatar.png'
  }
})

// 事件
const emit = defineEmits(['load-more'])

// DOM 引用
const containerRef = ref<HTMLElement | null>(null)
const bottomRef = ref<HTMLElement | null>(null)

// 虚拟滚动相关状态
const visibleMessages = computed(() => {
  // 过滤掉已删除的消息，然后规范化消息并确保角色正确
  return props.messages
    .filter(msg => msg.isDeleted !== true) // 只显示未删除的消息
    .map(msg => {
      const role = (msg.role || '').toLowerCase();
      return {
        ...msg,
        // 规范化角色：'user'是用户，其他('assistant', 'system', 'ai'等)都视为AI
        isUser: role === 'user'
      };
    });
})

// 处理滚动事件
function handleScroll(event: Event) {
  const container = event.target as HTMLElement
  
  // 检测是否滚动到顶部，用于加载更多消息
  if (container.scrollTop < 50 && props.hasMore && !props.loadingMore) {
    // 记录当前滚动高度，以便加载后维持位置
    const scrollHeight = container.scrollHeight
    const scrollTop = container.scrollTop
    
    // 通知父组件加载更多
    // 注意：父组件在实际加载成功后，需要考虑恢复滚动位置
    // 这通常可以通过记录第一条可见消息的 ID 来实现
    emit('load-more')
    
    // 在加载后调整滚动位置，保持用户正在查看的内容稳定
    nextTick(() => {
      if (container) {
        // 计算新的滚动位置，保持相对位置不变
        container.scrollTop = container.scrollHeight - scrollHeight + scrollTop
      }
    })
  }
}

// 格式化时间
function formatTime(timestamp: string): string {
  try {
    const date = new Date(timestamp)
    
    if (isToday(date)) {
      return format(date, "'今天' HH:mm", { locale: zhCN })
    } else if (isYesterday(date)) {
      return format(date, "'昨天' HH:mm", { locale: zhCN })
    } else {
      return format(date, 'yyyy-MM-dd HH:mm', { locale: zhCN })
    }
  } catch (error) {
    console.error('Invalid date format:', timestamp)
    return '未知时间'
  }
}

// 滚动到底部
function scrollToBottom(smooth = false) {
  if (bottomRef.value && containerRef.value) {
    bottomRef.value.scrollIntoView({ 
      behavior: smooth ? 'smooth' : 'auto',
      block: 'end'
    })
  }
}

// 监听消息变化，自动滚动到底部
watch(() => props.messages, (newMessages, oldMessages) => {
  // 如果是新增消息，自动滚动到底部
  if (newMessages.length > (oldMessages?.length || 0)) {
    nextTick(() => {
      scrollToBottom(true)
    })
  }
}, { deep: true })

// 组件挂载后滚动到底部
onMounted(() => {
  nextTick(() => {
    scrollToBottom()
  })
})
</script>

<script lang="ts">
export default {
  name: 'VirtualMessageList'
}
</script>

<style scoped>
.virtual-message-list {
  height: 100%;
  padding: 16px 24px;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
  background-color: var(--el-bg-color-page, #f5f7fa);
  transition: background-color 0.3s ease;
}

:global(.dark-mode) .virtual-message-list {
  background-color: #1a1b26;
}

.scroll-target {
  height: 1px;
  width: 100%;
}

.message-wrapper {
  margin-bottom: 28px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-container {
  display: flex;
  position: relative;
  max-width: 90%;
}

.user-message-wrapper {
  display: flex;
  justify-content: flex-end;
}

.avatar-container {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  flex-shrink: 0;
}

:global(.dark-mode) .avatar-container {
  border-color: #2c2e3b;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  max-width: calc(100% - 50px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  margin: 0 12px;
  overflow: hidden;
}

.user-bubble {
  background: var(--el-color-primary, #409eff);
  color: white;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 12px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.2);
}

.ai-bubble {
  background: white;
  color: var(--el-text-color-primary, #303133);
  border-top-left-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

:global(.dark-mode) .ai-bubble {
  background-color: #24273a;
  color: #c0caf5;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding-bottom: 6px;
}

:global(.dark-mode) .message-header {
  border-bottom-color: rgba(255, 255, 255, 0.1);
}

.user-bubble .message-header {
  border-bottom-color: rgba(255, 255, 255, 0.1);
}

.message-role {
  font-weight: 500;
  font-size: 14px;
}

.message-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.4);
  margin-left: 8px;
}

.user-bubble .message-time {
  color: rgba(255, 255, 255, 0.7);
}

:global(.dark-mode) .message-time {
  color: rgba(255, 255, 255, 0.5);
}

.edited-tag {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
  margin-left: 6px;
}

.user-bubble .edited-tag {
  background-color: rgba(255, 255, 255, 0.2);
}

:global(.dark-mode) .edited-tag {
  background-color: rgba(255, 255, 255, 0.1);
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 16px 0;
  margin-bottom: 16px;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--el-color-primary-light-7, #c6e2ff);
  border-top-color: var(--el-color-primary, #409eff);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--el-text-color-secondary, #909399);
  text-align: center;
  opacity: 0.7;
}

/* 消息反馈按钮 */
.message-feedback {
  position: absolute;
  bottom: -20px;
  right: 0;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.message-container:hover .message-feedback {
  opacity: 1;
}

.feedback-button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: var(--el-bg-color, #fff);
  color: var(--el-text-color-secondary, #909399);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
}

.feedback-button:hover {
  color: var(--el-color-primary, #409eff);
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  transform: scale(1.1);
}

.feedback-button.liked {
  color: var(--el-color-success, #67c23a);
  background-color: var(--el-color-success-light-9, #f0f9eb);
}

.feedback-button.disliked {
  color: var(--el-color-danger, #f56c6c);
  background-color: var(--el-color-danger-light-9, #fef0f0);
}

:global(.dark-mode) .feedback-button {
  background-color: #24273a;
  border-color: #2c2e3b;
  color: #c0caf5;
}

:global(.dark-mode) .feedback-button:hover {
  background-color: rgba(122, 162, 247, 0.1);
}

:global(.dark-mode) .feedback-button.liked {
  color: #9ece6a;
  background-color: rgba(158, 206, 106, 0.1);
}

:global(.dark-mode) .feedback-button.disliked {
  color: #f7768e;
  background-color: rgba(247, 118, 142, 0.1);
}

/* 进入和离开动画 */
.message-enter-active, .message-leave-active {
  transition: all 0.3s ease;
}

.message-enter-from, .message-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style> 