<template>
  <div class="virtual-message-list" ref="containerRef" @click="handleCodeCopy">
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
          <template v-if="message.role === 'assistant'">
            <el-avatar 
              :size="36"
              class="ai-avatar"
              :src="aiAvatarUrl"
              @error="handleAvatarError"
              @load="handleAvatarLoad"
              :style="{
                background: 'transparent',
                border: 'none',
                boxShadow: 'none'
              }"
            >
              <template #error>
                <el-icon><ChatRound /></el-icon>
              </template>
            </el-avatar>
          </template>
          <template v-else>
            <template v-if="user?.avatar">
              <el-avatar
                :size="36"
                :src="user.avatar"
                class="user-avatar"
              />
            </template>
            <template v-else>
              <el-avatar 
                :size="36"
                class="user-avatar"
                :style="{ background: generateAvatarBackground(user?.username || '') }"
              >
                {{ user?.username?.charAt(0)?.toUpperCase() || 'U' }}
              </el-avatar>
            </template>
            <el-button 
              v-if="user?.userRole === 'NORMAL'"
              class="upgrade-btn" 
              type="primary" 
              size="small"
              @click="handleUpgrade"
            >
              升级VIP
            </el-button>
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
import { storeToRefs } from 'pinia'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const userType = ref('personal')
const currentPlan = ref('NORMAL')
const router = useRouter()

// 复制代码到剪贴板
const copyToClipboard = async (code: string) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('代码已复制')
  } catch (err) {
    console.error('Copy failed:', err)
    ElMessage.error('复制失败')
  }
}

// 创建一个唯一的ID生成器
const generateId = () => `_${Math.random().toString(36).substr(2, 9)}`

// 在 script setup 部分添加 SQL 格式化函数
const formatSql = (sql: string): string => {
  return sql
    .replace(/\s+/g, ' ')
    .replace(/\s*([,()])\s*/g, '$1 ')
    .replace(/\b(SELECT|FROM|WHERE|GROUP BY|HAVING|ORDER BY|LIMIT|INSERT INTO|VALUES|UPDATE|SET|DELETE FROM|JOIN|LEFT JOIN|RIGHT JOIN|INNER JOIN|OUTER JOIN|ON|AND|OR|UNION|ALL|AS)\b/gi, '\n$1')
    .replace(/\(/g, '\n(')
    .replace(/\)/g, ')\n')
    .replace(/,/g, ',\n  ')
    .split('\n')
    .map(line => line.trim())
    .filter(line => line)
    .join('\n')
    .trim()
}

// 修改 formatMessage 函数
const formatMessage = (content: string): string => {
  // 预处理特殊字符和换行
  content = content
    .replace(/\\n/g, '\n')
    .replace(/\\\`/g, '`')
    .replace(/\\\*/g, '*')
    .replace(/\\\[/g, '[')
    .replace(/\\\]/g, ']')
    .replace(/\\\(/g, '(')
    .replace(/\\\)/g, ')')
    .replace(/\\\{/g, '{')
    .replace(/\\\}/g, '}')
    .replace(/\\\#/g, '#')
    .replace(/\\\-/g, '-')
    .replace(/\\\+/g, '+')
    .replace(/\\\./g, '.')
    .replace(/\\\!/g, '!')

  // 处理代码块
  content = content.replace(/```(\w+)?\s*\n([\s\S]*?)\n```/g, (match, lang, code) => {
    return `\n${match}\n`
  })

  const md = new MarkdownIt({
    highlight: (str: string, lang: string): string => {
      const uniqueId = generateId()
      const escapedCode = str
        .replace(/`/g, '\\`')
        .replace(/\$/g, '\\$')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')

      if (lang && hljs.getLanguage(lang)) {
        try {
          const highlighted = hljs.highlight(str, { language: lang }).value
          const lines = highlighted.split('\n')
          const numberedLines = lines.map((line, i) => 
            `<span class="line"><span class="line-number">${i + 1}</span>${line}</span>`
          ).join('\n')
          
          return `<div class="code-block ${lang}-block">
            <div class="code-header">
              <span>${lang}</span>
              <button class="copy-btn" data-code="${uniqueId}">复制</button>
            </div>
            <pre><code class="hljs ${lang}" id="${uniqueId}">${numberedLines}</code></pre>
          </div>`
        } catch (err) {
          console.error('Code highlighting failed:', err)
          return `<pre><code>${escapedCode}</code></pre>`
        }
      }
      
      return `<pre><code>${escapedCode}</code></pre>`
    },
    html: true,
    breaks: true,
    linkify: true,
    typographer: true
  })

  // 自定义渲染规则
  md.renderer.rules.heading_open = (tokens, idx) => {
    const token = tokens[idx]
    const level = parseInt(token.tag.slice(1))
    return `<${token.tag} class="markdown-heading level-${level}">`
  }

  md.renderer.rules.paragraph_open = () => '<p class="markdown-paragraph">'
  
  // 渲染 markdown
  let rendered = md.render(content)

  // 后处理渲染结果
  rendered = rendered
    .replace(/\n/g, '<br>')
    .replace(/<br><pre>/g, '<pre>')
    .replace(/<\/pre><br>/g, '</pre>')
    .replace(/```(\w+)?\s*<br>/g, '```$1\n')
    .replace(/<br>```/g, '\n```')
    .replace(/<p><br>/g, '<p>')
    .replace(/<br><\/p>/g, '</p>')
    .replace(/^\s*<br>/gm, '')
    .replace(/<br>\s*$/gm, '')

  return rendered
}

// 添加事件处理器
const handleCodeCopy = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (target.classList.contains('copy-btn')) {
    const codeId = target.getAttribute('data-code')
    if (codeId) {
      const codeElement = document.getElementById(codeId)
      if (codeElement) {
        copyToClipboard(codeElement.textContent || '')
      }
    }
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

// 按创建时间排序的消息列表
const sortedMessages = computed(() => {
  if (!props.messages) return []
  console.log('Messages:', props.messages)
  return props.messages
})

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

// 组件挂载时添加事件监听
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

// 添加深色模式变量
const darkModeVars = {
  '--bg-gradient-start': '#1a1a1a',
  '--bg-gradient-end': '#2d2d2d',
  '--assistant-bg': 'rgba(255, 255, 255, 0.1)',
  '--assistant-text': 'rgba(255, 255, 255, 0.9)',
  '--time-color': 'rgba(255, 255, 255, 0.5)',
  '--empty-text': 'rgba(255, 255, 255, 0.5)'
}

// 监听深色模式变化
watch(() => document.documentElement.classList.contains('dark'), (isDark) => {
  const container = containerRef.value
  if (container) {
    if (isDark) {
      Object.entries(darkModeVars).forEach(([key, value]) => {
        container.style.setProperty(key, value)
      })
    } else {
      Object.keys(darkModeVars).forEach(key => {
        container.style.removeProperty(key)
      })
    }
  }
}, { immediate: true })

// 模拟获取当前用户套餐
const getCurrentPlan = () => {
  // 从用户角色获取当前套餐信息
  currentPlan.value = authStore.user?.userRole || 'NORMAL'
}

// 初始化
getCurrentPlan()

// 处理升级按钮点击
const handleUpgrade = () => {
  router.push('/vip-plans')
}

// 在 script setup 部分添加 generateAvatarBackground 函数
const generateAvatarBackground = (username: string): string => {
  const colors = [
    'linear-gradient(135deg, #FF6B6B 0%, #FF4949 100%)',
    'linear-gradient(135deg, #4ECDC4 0%, #45B7AF 100%)',
    'linear-gradient(135deg, #45B649 0%, #31A032 100%)',
    'linear-gradient(135deg, #FF851B 0%, #FF7701 100%)',
    'linear-gradient(135deg, #7F00FF 0%, #6B00DB 100%)',
    'linear-gradient(135deg, #00B2FF 0%, #0085FF 100%)'
  ]
  
  // 根据用户名生成固定的颜色索引
  const index = username.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length
  return colors[index]
}

// 添加AI头像相关的状态
const aiAvatarLoaded = ref(false)
const aiAvatarError = ref(false)

// AI头像URL数组
const aiAvatarUrls = [
  '/images/ai-avatar.png',
  '/images/fallback/default-ai-avatar.png',
  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiPjxjaXJjbGUgY3g9IjEyIiBjeT0iMTIiIHI9IjEwIi8+PHBhdGggZD0iTTIgMTJoMjAiLz48L3N2Zz4=' // 内嵌的SVG作为最后的备用
]

// 当前使用的头像URL
const currentAiAvatarIndex = ref(0)
const aiAvatarUrl = computed(() => aiAvatarUrls[currentAiAvatarIndex.value])

// 改进的错误处理函数
const handleAvatarError = (event: Event) => {
  const target = event.target as HTMLImageElement
  console.error(`AI头像加载失败: ${target.src}`)
  
  // 尝试下一个备用头像
  if (currentAiAvatarIndex.value < aiAvatarUrls.length - 1) {
    currentAiAvatarIndex.value++
    target.src = aiAvatarUrl.value
  } else {
    aiAvatarError.value = true
    console.error('所有AI头像都加载失败')
  }
}

// 头像加载成功处理
const handleAvatarLoad = () => {
  aiAvatarLoaded.value = true
  aiAvatarError.value = false
}
</script>

<script lang="ts">
export default {
  name: 'VirtualMessageList'
}
</script>

<style lang="scss" scoped>
.virtual-message-list {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
  background: #ffffff;
  scroll-behavior: smooth;

  &.dark {
    background: #1a1b26;
  }

  .message-content {
    max-width: 85%;
    min-width: 100px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    position: relative;
    padding: 0;
    word-break: break-word;
    flex-shrink: 1;
    width: fit-content;
  }

  .message-item {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;
    opacity: 0;
    transform: translateY(20px);
    animation: messageIn 0.3s cubic-bezier(0.4, 0, 0.2, 1) forwards;
    align-items: flex-start;
    padding: 0;
    width: 100%;
    
    &.assistant {
      flex-direction: row;
      
      .message-content {
        margin-right: auto;
        margin-left: 0;
      }
      
      .message-text {
        background: #ffffff;
        color: #333333;
        border: 1px solid #e5e7eb;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
        
        &:hover {
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
        }
      }
    }
    
    &.user {
      flex-direction: row-reverse;
      
      .message-content {
        margin-left: auto;
        margin-right: 0;
        align-items: flex-end;
      }
      
      .message-text {
        background: #2563eb;
        color: #ffffff;
        border: none;
        box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
        
        &:hover {
          box-shadow: 0 4px 8px rgba(37, 99, 235, 0.25);
        }
        
        :deep(.sql-block) {
          margin: 8px 0 4px 0 !important;
        }
      }
      
      .message-time {
        text-align: right;
        color: rgba(255, 255, 255, 0.7);
      }
    }
  }

  .message-text {
    position: relative;
    padding: 16px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.6;
    transition: all 0.2s ease;
    max-width: 100%;
    background: #ffffff;
    
    :deep(.markdown-heading) {
      font-weight: 600;
      line-height: 1.25;
      margin: 0;
      padding: 0;
      color: #333;
      
      &.level-1 {
        font-size: 20px;
        margin-bottom: 16px;
        padding-bottom: 8px;
        border-bottom: 1px solid #e5e7eb;
      }
      
      &.level-2 {
        font-size: 18px;
        margin: 16px 0 12px 0;
      }
      
      &.level-3 {
        font-size: 16px;
        margin: 14px 0 10px 0;
      }
    }
    
    :deep(.markdown-paragraph) {
      margin: 8px 0;
      line-height: 1.6;
      
      &:first-child {
        margin-top: 0;
      }
      
      &:last-child {
        margin-bottom: 0;
      }
    }
    
    :deep(.code-block) {
      margin: 12px 0 !important;
      border-radius: 6px !important;
      overflow: hidden;
      background: #1a1b26;
      border: none;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
      width: 100%;
      
      .code-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 6px 12px;
        background: #24283b;
        border-bottom: 1px solid #2f3241;
        
        span {
          font-size: 12px;
          font-weight: normal;
          color: #7982a9;
          text-transform: lowercase;
          opacity: 0.8;
        }
        
        .copy-btn {
          padding: 3px 10px;
          font-size: 12px;
          border: 1px solid #2f3241;
          border-radius: 3px;
          background: transparent;
          color: #7982a9;
          cursor: pointer;
          transition: all 0.2s;
          
          &:hover {
            background: #2f3241;
            color: #c0caf5;
          }
        }
      }
      
      pre {
        margin: 0 !important;
        padding: 12px !important;
        background: #1a1b26 !important;
        font-family: 'JetBrains Mono', 'Fira Code', monospace !important;
        font-size: 13px !important;
        line-height: 1.5 !important;
        overflow-x: auto;
        
        code {
          .line {
            display: block;
            padding-left: 2.5em;
            position: relative;
            white-space: pre;
            
            .line-number {
              position: absolute;
              left: 0;
              width: 2em;
              text-align: right;
              padding-right: 0.5em;
              color: #565f89;
              opacity: 0.5;
              user-select: none;
            }
            
            &:hover {
              background: #1e202e;
            }
          }
          
          &.hljs {
            background: transparent !important;
            padding: 0 !important;
            color: #a9b1d6;
            
            .hljs-keyword {
              color: #bb9af7;
              font-weight: normal;
            }
            
            .hljs-string {
              color: #9ece6a;
            }
            
            .hljs-number {
              color: #ff9e64;
            }
            
            .hljs-comment {
              color: #565f89;
              font-style: italic;
            }
            
            .hljs-operator {
              color: #89ddff;
            }
          }
        }
      }
    }
  }

  .message-time {
    font-size: 12px;
    color: #6b7280;
    margin-top: 4px;
    opacity: 0.8;
  }

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    overflow: hidden;
    flex-shrink: 0;
    position: relative;
    
    :deep(.el-avatar) {
      width: 100% !important;
      height: 100% !important;
      border: none;
      
      &.ai-avatar {
        background: transparent !important;
        padding: 0 !important;
        border: none !important;
        box-shadow: none !important;
        
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
      
      &.user-avatar {
        color: white;
        font-weight: 600;
        font-size: 16px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }
    }
  }

  .empty-message {
    text-align: center;
    color: var(--empty-text, #909399);
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
    
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }

  @keyframes typingAnimation {
    0%, 60%, 100% {
      transform: translateY(0);
    }
    30% {
      transform: translateY(-4px);
    }
  }

  .user-info {
    position: relative;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .upgrade-btn {
    position: absolute;
    bottom: -20px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 12px;
    padding: 2px 8px;
    height: 24px;
  }

  @media (max-width: 768px) {
    .virtual-message-list {
      padding: 16px;
    }
    
    .message-content {
      max-width: 90%;
      min-width: 60px;
    }
    
    .message-text {
      padding: 10px 14px;
      font-size: 14px;
    }
    
    .avatar {
      width: 36px;
      height: 36px;
    }
  }
}

@keyframes messageIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 深色模式
:deep([data-theme="dark"]) {
  .virtual-message-list {
    background: #1a1b26;
  }
  
  .message-item {
    &.assistant .message-text {
      background: #24283b;
      border-color: #2f3241;
      color: #c0caf5;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
      
      &:hover {
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
      }
    }
    
    &.user .message-text {
      background: #7aa2f7;
      color: #ffffff;
      box-shadow: 0 2px 4px rgba(122, 162, 247, 0.3);
      
      &:hover {
        box-shadow: 0 4px 8px rgba(122, 162, 247, 0.4);
      }
    }
  }
  
  .message-time {
    color: #565f89;
  }
  
  .avatar :deep(.el-avatar) {
    background: #24283b;
    border-color: #2f3241;
    color: #7982a9;
  }
}

:deep(.sql-block) {
  margin: 12px 0 !important;
  border-radius: 6px !important;
  overflow: hidden;
  background: #1a1b26;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  width: 100%;
  
  .sql-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 12px;
    background: #24283b;
    border-bottom: 1px solid #2f3241;
    
    span {
      font-size: 12px;
      font-weight: normal;
      color: #7982a9;
      text-transform: lowercase;
      opacity: 0.8;
    }
    
    .copy-btn {
      padding: 3px 10px;
      font-size: 12px;
      border: 1px solid #2f3241;
      border-radius: 3px;
      background: transparent;
      color: #7982a9;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover {
        background: #2f3241;
        color: #c0caf5;
      }
    }
  }
  
  pre {
    margin: 0 !important;
    padding: 12px !important;
    background: #1a1b26 !important;
    font-family: 'JetBrains Mono', 'Fira Code', monospace !important;
    font-size: 13px !important;
    line-height: 1.5 !important;
    overflow-x: auto;
    
    code {
      .line {
        display: block;
        padding-left: 2.5em;
        position: relative;
        white-space: pre;
        
        .line-number {
          position: absolute;
          left: 0;
          width: 2em;
          text-align: right;
          padding-right: 0.5em;
          color: #565f89;
          opacity: 0.5;
          user-select: none;
        }
        
        &:hover {
          background: #1e202e;
        }
      }
      
      &.hljs {
        background: transparent !important;
        padding: 0 !important;
        color: #a9b1d6;
        
        .hljs-keyword {
          color: #bb9af7;
          font-weight: normal;
        }
        
        .hljs-string {
          color: #9ece6a;
        }
        
        .hljs-number {
          color: #ff9e64;
        }
        
        .hljs-comment {
          color: #565f89;
          font-style: italic;
        }
        
        .hljs-operator {
          color: #89ddff;
        }
      }
    }
  }
}

// 深色模式适配
:deep([data-theme="dark"]) {
  .message-text {
    background: #24283b;
    color: #c0caf5;
    
    :deep(.markdown-heading) {
      color: #c0caf5;
      
      &.level-1 {
        border-bottom-color: #2f3241;
      }
    }
    
    :deep(.markdown-paragraph) {
      color: #a9b1d6;
    }
    
    :deep(.code-block) {
      background: #1a1b26;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
      
      .code-header {
        background: #24283b;
        border-bottom-color: #2f3241;
        
        span {
          color: #7982a9;
        }
        
        .copy-btn {
          border-color: #2f3241;
          color: #7982a9;
          
          &:hover {
            background: #2f3241;
            color: #c0caf5;
          }
        }
      }
      
      pre {
        background: #1a1b26 !important;
        
        code {
          color: #a9b1d6;
          
          .line-number {
            color: #565f89;
          }
          
          &:hover {
            background: #1e202e;
          }
        }
      }
    }
  }
}
</style> 