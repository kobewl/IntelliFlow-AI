<template>
  <div class="virtual-message-list" ref="containerRef">
    <div v-if="hasMore" class="load-more">
      <el-button 
        :loading="loadingMore" 
        @click="$emit('load-more')"
      >
        加载更多
      </el-button>
    </div>
    
    <template v-if="sortedMessages.length > 0">
      <div
        v-for="message in sortedMessages"
        :key="message.id"
        :class="['message-item', message.role.toLowerCase()]"
      >
        <div class="avatar">
          <template v-if="message.role === 'ASSISTANT'">
            <el-avatar 
              :size="24"
              class="ai-avatar"
            >
              <el-icon><ChatRound /></el-icon>
            </el-avatar>
          </template>
          <template v-else>
            <el-avatar 
              :size="24"
              class="user-avatar"
            >
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
          </template>
        </div>
        <div class="message-content">
          <div class="message-text" v-html="formatMessage(message.content || '')" />
          <div class="message-time">
            {{ formatTime(message.createdAt) }}
          </div>
        </div>
      </div>
    </template>
    <div v-else class="empty-message">
      暂无消息
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, onUnmounted } from 'vue'
import type { ChatMessage } from '../api/chat'
import { formatTime } from '../utils/time'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { ChatRound, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 在 script 开头添加 window 类型声明
declare global {
  interface Window {
    copyCode: (code: string) => void;
  }
}

const props = defineProps<{
  messages: ChatMessage[]
  hasMore: boolean
  loadingMore: boolean
}>()

const emit = defineEmits<{
  (e: 'load-more'): void
}>()

const containerRef = ref<HTMLElement | null>(null)
const userScrolling = ref(false)
const lastUserInteraction = ref(Date.now())
const md: MarkdownIt = new MarkdownIt({
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        const highlighted = hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
        return highlighted
      } catch (__) {
        console.warn(`Failed to highlight code block with language: ${lang}`)
      }
    }
    return md.utils.escapeHtml(str)
  },
  html: true,
  breaks: true,
  linkify: true,
  typographer: true
})

// 缓存已渲染的消息内容
const renderedMessages = new Map<string, string>()

// 按创建时间排序的消息列表
const sortedMessages = computed(() => {
  if (!props.messages) return []
  console.log('Messages:', props.messages)
  return props.messages
})

// 复制代码到剪贴板
function copyCode(code: string) {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('代码已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 修改 formatMessage 函数，添加复制按钮和语言标记
function formatMessage(content: string) {
  if (!content) return ''
  
  const cached = renderedMessages.get(content)
  if (cached) return cached
  
  try {
    // 预处理代码块，保持缩进和对齐
    const processedContent = content.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
      const trimmedCode = code.split('\n').map((line: string) => line.trimEnd()).join('\n')
      return `\`\`\`${lang || 'plaintext'}\n${trimmedCode}\`\`\``
    })
    
    let rendered = md.render(processedContent)
    
    // 为代码块添加复制按钮和语言标记
    rendered = rendered.replace(/<pre><code class="language-(\w+)">([\s\S]*?)<\/code><\/pre>/g, (match, lang, codeContent) => {
      const code = codeContent.replace(/<[^>]+>/g, '').trim()
      return `<pre data-language="${lang}"><code class="hljs language-${lang}">${codeContent}<button class="copy-btn" onclick="window.copyCode(\`${code.replace(/`/g, '\\`')}\`)"><svg class="copy-icon" viewBox="0 0 24 24" width="12" height="12"><path fill="currentColor" d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg></button></code></pre>`
    })
    
    // 处理没有指定语言的代码块
    rendered = rendered.replace(/<pre><code>([\s\S]*?)<\/code><\/pre>/g, (match, codeContent) => {
      const code = codeContent.replace(/<[^>]+>/g, '').trim()
      return `<pre data-language="plaintext"><code class="hljs">${codeContent}<button class="copy-btn" onclick="window.copyCode(\`${code.replace(/`/g, '\\`')}\`)"><svg class="copy-icon" viewBox="0 0 24 24" width="12" height="12"><path fill="currentColor" d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg></button></code></pre>`
    })
    
    renderedMessages.set(content, rendered)
    return rendered
  } catch (error) {
    console.error('Failed to render markdown:', error)
    return content
  }
}

// 在window上暴露copyCode函数，供内联事件调用
window.copyCode = copyCode

// 检查是否需要加载更多
function checkLoadMore() {
  if (!containerRef.value || !props.hasMore || props.loadingMore) return
  
  const { scrollTop } = containerRef.value
  if (scrollTop < 100) {
    emit('load-more')
  }
}

// 处理用户滚动事件
function handleScroll() {
  if (!containerRef.value) return
  
  const container = containerRef.value
  const { scrollHeight, clientHeight, scrollTop } = container
  
  // 更新用户滚动状态
  lastUserInteraction.value = Date.now()
  userScrolling.value = true
  
  // 检查是否需要加载更多
  if (scrollTop < 100) {
    checkLoadMore()
  }
  
  // 如果滚动到底部，重置用户滚动状态
  if (scrollHeight - scrollTop - clientHeight < 50) {
    userScrolling.value = false
  }
}

// 监听消息变化，自动滚动到底部
watch(() => props.messages, async (newMessages, oldMessages) => {
  console.log('Messages updated:', newMessages)
  await nextTick()
  if (containerRef.value) {
    // 如果是加载更多消息，保持当前滚动位置
    if (oldMessages && newMessages.length > oldMessages.length && 
        newMessages.slice(0, oldMessages.length).every((msg, i) => msg.id === oldMessages[i].id)) {
      return
    }
    
    // 获取容器和内容的高度
    const container = containerRef.value
    const scrollHeight = container.scrollHeight
    const clientHeight = container.clientHeight
    const scrollTop = container.scrollTop
    
    // 如果距离用户最后一次滚动超过2秒，或者用户本来就在底部，则自动滚动
    const timeSinceLastInteraction = Date.now() - lastUserInteraction.value
    const isAtBottom = scrollHeight - scrollTop - clientHeight < 100
    
    if (!userScrolling.value || isAtBottom || timeSinceLastInteraction > 2000) {
      container.scrollTop = scrollHeight
      userScrolling.value = false
    }
  }
}, { deep: true })

// 组件挂载时滚动到底部
onMounted(async () => {
  await nextTick()
  if (containerRef.value) {
    containerRef.value.scrollTop = containerRef.value.scrollHeight
    // 添加滚动事件监听
    containerRef.value.addEventListener('scroll', handleScroll)
  }
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('scroll', handleScroll)
  }
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
  overflow-y: auto;
  padding: 20px;
  background: linear-gradient(to bottom, #f0f2f5, #ffffff);
  scroll-behavior: smooth;
}

.load-more {
  text-align: center;
  margin-bottom: 12px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 0 12px;
}

.message-item {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  opacity: 0;
  transform: translateY(20px);
  animation: messageIn 0.3s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  align-items: flex-start;
  padding: 0 4px;
}

.message-item.assistant {
  flex-direction: row;
}

.message-item.user {
  flex-direction: row-reverse;
}

@keyframes messageIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: relative;
}

.message-item.user .message-content {
  align-items: flex-end;
}

.message-text {
  position: relative;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
  transition: all 0.2s ease;
}

.message-item.assistant .message-text {
  background: #ffffff;
  color: #333;
  border-top-left-radius: 2px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  margin-right: auto;
}

.message-item.user .message-text {
  background: var(--el-color-primary);
  color: #ffffff;
  border-top-right-radius: 2px;
  box-shadow: 0 1px 6px rgba(var(--el-color-primary-rgb), 0.15);
  margin-left: auto;
}

.message-item.assistant .message-text::before,
.message-item.user .message-text::before {
  content: '';
  position: absolute;
  top: 14px;
  width: 8px;
  height: 8px;
  transform: rotate(45deg);
  z-index: 1;
}

.message-item.assistant .message-text::before {
  left: -4px;
  background: #ffffff;
}

.message-item.user .message-text::before {
  right: -4px;
  background: var(--el-color-primary);
}

.message-time {
  font-size: 11px;
  color: #999;
  margin: 1px 6px;
  opacity: 0.7;
}

.message-item.user .message-time {
  text-align: right;
}

.ai-avatar {
  background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);
  color: white;
  width: 100% !important;
  height: 100% !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  width: 100% !important;
  height: 100% !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar :deep(.el-avatar) {
  width: 100% !important;
  height: 100% !important;
  font-size: 20px;
}

.avatar :deep(.el-icon) {
  font-size: 20px;
}

.message-text :deep(pre) {
  margin: 8px 0;
  padding: 16px;
  padding-top: 32px;
  border-radius: 8px;
  background: #1e1e1e;
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow-x: auto;
}

.message-text :deep(pre)::before {
  content: attr(data-language);
  position: absolute;
  top: 4px;
  left: 12px;
  font-size: 12px;
  color: #888;
  text-transform: uppercase;
}

/* 深色模式适配 */
:deep(.dark) {
  .virtual-message-list {
    background: linear-gradient(to bottom, #1a1a1a, #2d2d2d);
  }
  
  .message-item.assistant .message-text {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.9);
  }
  
  .message-item.assistant .message-text::before {
    background: rgba(255, 255, 255, 0.1);
  }
  
  .message-time {
    color: rgba(255, 255, 255, 0.5);
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .virtual-message-list {
    padding: 16px;
  }
  
  .message-item {
    gap: 8px;
    margin-bottom: 16px;
  }
  
  .avatar {
    width: 36px;
    height: 36px;
  }
  
  .avatar :deep(.el-icon) {
    font-size: 18px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .message-text {
    padding: 10px 14px;
    font-size: 14px;
    border-radius: 16px;
  }
}

.empty-message {
  text-align: center;
  color: #909399;
  padding: 10px 0;
  font-size: 12px;
}

.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: rgba(var(--el-color-primary-rgb), 0.1);
  border-radius: 12px;
  margin: 4px 0;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: typingAnimation 1.4s infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typingAnimation {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-4px);
  }
}
</style> 