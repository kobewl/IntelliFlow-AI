<template>
  <div class="agent-chat">
    <!-- 左侧栏 -->
    <aside class="sidebar">
      <div class="sidebar-top">
        <router-link to="/" class="sidebar-logo">
          <span>KobeAI</span>
        </router-link>
        <el-button class="new-chat-btn" @click="handleNewChat">
          <el-icon><Plus /></el-icon>
          <span>新对话</span>
        </el-button>
      </div>

      <div class="conversation-list" ref="listRef">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === currentConversationId }"
          @click="handleSelectConv(conv.id)"
        >
          <div class="conv-title">{{ conv.title || '新对话' }}</div>
          <div class="conv-time">{{ formatTime(conv.updatedAt) }}</div>
          <div class="conv-actions" @click.stop>
            <el-dropdown trigger="click" placement="bottom-start">
              <span class="conv-more"><el-icon><MoreFilled /></el-icon></span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="startRename(conv)">
                    <el-icon><Edit /></el-icon>重命名
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleDeleteConv(conv.id)" divided>
                    <el-icon><Delete /></el-icon>
                    <span class="danger-text">删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div v-if="conversations.length === 0" class="no-conversations">
          <p>暂无对话</p>
          <span>点击上方按钮开始</span>
        </div>
      </div>

      <div class="sidebar-bottom">
        <div class="user-area" v-if="authStore.isAuthenticated">
          <el-avatar :size="32" :src="authStore.user?.avatar || '/ai-avatar.png'" />
          <span class="user-name">{{ authStore.user?.username }}</span>
          <el-icon class="logout-icon" @click="handleLogout" title="退出登录">
            <SwitchButton />
          </el-icon>
        </div>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <div class="main-area">
      <!-- 空状态 -->
      <div v-if="!currentConversationId" class="welcome">
        <div class="welcome-icon">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <rect width="64" height="64" rx="16" fill="url(#grad)" />
            <path d="M20 24h24M20 32h18M20 40h12" stroke="#fff" stroke-width="2.5" stroke-linecap="round" />
            <defs>
              <linearGradient id="grad" x1="0" y1="0" x2="64" y2="64">
                <stop stop-color="#6366f1" /><stop offset="1" stop-color="#8b5cf6" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h1>有什么我可以帮你的？</h1>
        <p class="welcome-sub">Agent 会一步步思考，解决你的问题</p>
        <div class="quick-prompts">
          <div
            v-for="prompt in quickPrompts"
            :key="prompt"
            class="quick-prompt"
            @click="handleQuickPrompt(prompt)"
          >
            {{ prompt }}
          </div>
        </div>
      </div>

      <!-- 对话区域 -->
      <template v-else>
        <!-- 顶栏 -->
        <header class="chat-header">
          <div class="header-left">
            <el-button class="sidebar-toggle" text @click="showSidebar = !showSidebar">
              <el-icon><Fold /></el-icon>
            </el-button>
            <span class="header-title">{{ currentConversation?.title || '新对话' }}</span>
          </div>
          <div class="header-right">
            <el-select
              v-model="selectedModel"
              size="small"
              class="model-select"
              placeholder="选择模型"
            >
              <el-option label="DeepSeek-Chat" value="deepseek-chat" />
              <el-option label="DeepSeek-Reasoner" value="deepseek-reasoner" />
            </el-select>
          </div>
        </header>

        <!-- 消息列表 -->
        <div class="messages-container" ref="msgContainerRef">
          <div
            v-if="loadingMore"
            class="load-more"
            @click="handleLoadMore"
          >
            <el-button text size="small" :loading="loadingMore">加载更早的消息</el-button>
          </div>

          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-row"
            :class="msg.role"
          >
            <!-- 用户消息 -->
            <template v-if="msg.role === 'user'">
              <div class="user-bubble">
                <div class="bubble-text">{{ msg.content }}</div>
              </div>
              <el-avatar :size="28" :src="authStore.user?.avatar || '/ai-avatar.png'" class="msg-avatar" />
            </template>

            <!-- Agent 消息 -->
            <template v-else>
              <div class="agent-avatar">
                <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                  <rect width="28" height="28" rx="8" fill="url(#ai-grad)" />
                  <path d="M8 10h12M8 14h8M8 18h5" stroke="#fff" stroke-width="1.5" stroke-linecap="round" />
                  <defs>
                    <linearGradient id="ai-grad" x1="0" y1="0" x2="28" y2="28">
                      <stop stop-color="#6366f1" /><stop offset="1" stop-color="#8b5cf6" />
                    </linearGradient>
                  </defs>
                </svg>
              </div>
              <div class="agent-content">
                <!-- 思考过程卡片 -->
                <div
                  v-if="msg.reasoning.length > 0 || msg.isThinking"
                  class="thinking-card"
                >
                  <div class="thinking-header" @click="msg._thinkingOpen = !msg._thinkingOpen">
                    <el-icon class="thinking-icon" :class="{ spinning: msg.isThinking }">
                      <Loading v-if="msg.isThinking" />
                      <Check v-else />
                    </el-icon>
                    <span>{{ msg.isThinking ? '正在思考...' : `思考过程 (${msg.reasoning.length} 步)` }}</span>
                    <el-icon class="expand-icon" :class="{ expanded: msg._thinkingOpen }">
                      <ArrowDown />
                    </el-icon>
                  </div>
                  <div class="thinking-body" v-show="msg._thinkingOpen">
                    <div
                      v-for="(step, i) in msg.reasoning"
                      :key="i"
                      class="thinking-step"
                    >
                      <span class="step-num">{{ i + 1 }}</span>
                      <span class="step-text">{{ step }}</span>
                    </div>
                    <div v-if="msg.isThinking" class="thinking-step thinking-pending">
                      <span class="step-dot"></span>
                      <span class="step-text dim">分析中...</span>
                    </div>
                  </div>
                </div>

                <!-- 工具调用 -->
                <div
                  v-for="(tool, ti) in msg.toolCalls"
                  :key="'tool-' + ti"
                  class="tool-card"
                >
                  <div class="tool-header">
                    <el-icon><Operation /></el-icon>
                    <span>调用工具：{{ tool.name }}</span>
                  </div>
                  <div class="tool-body">
                    <div v-if="tool.input" class="tool-section">
                      <span class="tool-label">输入</span>
                      <code class="tool-code">{{ tool.input }}</code>
                    </div>
                    <div v-if="tool.output" class="tool-section">
                      <span class="tool-label">输出</span>
                      <code class="tool-code">{{ tool.output }}</code>
                    </div>
                  </div>
                </div>

                <!-- 最终回复 -->
                <div v-if="msg.content" class="agent-bubble">
                  <MarkdownRenderer :content="msg.content" />
                </div>

                <!-- 错误 -->
                <div v-if="msg.error" class="agent-error">
                  <el-icon><WarningFilled /></el-icon>
                  <span>{{ msg.error }}</span>
                </div>
              </div>
            </template>
          </div>

          <div ref="scrollAnchorRef"></div>
        </div>

        <!-- 输入区 -->
        <div class="input-area">
          <div class="input-box">
            <el-input
              v-model="inputText"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 5 }"
              placeholder="输入消息，Enter 发送，Shift+Enter 换行"
              :disabled="loading"
              @keydown.enter.exact.prevent="handleSend"
              @keydown.enter.shift.exact="inputText += '\n'"
            />
            <div class="input-actions">
              <div></div>
              <el-button
                type="primary"
                :disabled="!inputText.trim() || loading"
                @click="handleSend"
              >
                <template v-if="loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>停止</span>
                </template>
                <template v-else>
                  <el-icon><Position /></el-icon>
                  <span>发送</span>
                </template>
              </el-button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameVisible" title="重命名会话" width="360px" append-to-body>
      <el-input v-model="renameText" placeholder="输入新名称" @keydown.enter="confirmRename" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Position, Delete, Edit, MoreFilled,
  Loading, Check, ArrowDown, Operation,
  WarningFilled, SwitchButton, Fold
} from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import type { Conversation } from '../../api/chat'
import MarkdownRenderer from '../../components/MarkdownRenderer.vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const {
  conversations,
  currentConversationId,
  currentConversation,
  messages,
  loading,
  loadingMore
} = storeToRefs(chatStore)

const inputText = ref('')
const selectedModel = ref('deepseek-chat')
const showSidebar = ref(true)
const msgContainerRef = ref<HTMLElement | null>(null)
const scrollAnchorRef = ref<HTMLElement | null>(null)
const renameVisible = ref(false)
const renameText = ref('')
const renameTarget = ref<Conversation | null>(null)

const quickPrompts = [
  '帮我写一个 Python 爬虫脚本',
  '解释一下什么是机器学习',
  '帮我优化这段代码的性能',
  '写一份项目计划书'
]

function formatTime(t: string) {
  return dayjs(t).fromNow()
}

function scrollToBottom() {
  nextTick(() => {
    scrollAnchorRef.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

// 监听消息变化自动滚动
watch(() => messages.value.length, scrollToBottom)
watch(() => {
  const lastMsg = messages.value[messages.value.length - 1]
  return lastMsg?.content?.length ?? 0
}, scrollToBottom)

onMounted(async () => {
  await chatStore.loadConversations()
  if (currentConversationId.value) {
    await chatStore.switchConversation(currentConversationId.value)
    scrollToBottom()
  }
})

async function handleNewChat() {
  try {
    await chatStore.createConversation()
    scrollToBottom()
  } catch (err: any) {
    ElMessage.error(err.message || '创建失败')
  }
}

async function handleSelectConv(id: number) {
  await chatStore.switchConversation(id)
  scrollToBottom()
}

async function handleDeleteConv(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这个会话吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await chatStore.deleteConversation(id)
  } catch { /* 取消 */ }
}

function startRename(conv: Conversation) {
  renameTarget.value = conv
  renameText.value = conv.title || ''
  renameVisible.value = true
}

async function confirmRename() {
  if (renameTarget.value && renameText.value.trim()) {
    await chatStore.renameConversation(renameTarget.value.id, renameText.value.trim())
  }
  renameVisible.value = false
}

function handleQuickPrompt(prompt: string) {
  handleNewChat().then(() => {
    inputText.value = prompt
    handleSend()
  })
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''

  try {
    await chatStore.sendMessage(text)
  } catch (err: any) {
    if (err.message !== 'AbortError') {
      ElMessage.error(err.message || '发送失败')
    }
  } finally {
    scrollToBottom()
  }
}

async function handleLoadMore() {
  await chatStore.loadMoreMessages()
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    chatStore.clearStore()
    await authStore.logout()
    router.push('/')
  } catch { /* 取消 */ }
}
</script>

<style lang="scss" scoped>
.agent-chat {
  height: 100vh;
  display: flex;
  background: #f8f9fb;
  overflow: hidden;
}

// ---- 左侧栏 ----

.sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e8eaef;
}

.sidebar-top {
  padding: 16px;
  border-bottom: 1px solid #f0f1f5;
}

.sidebar-logo {
  display: block;
  text-decoration: none;
  margin-bottom: 12px;

  span {
    font-size: 20px;
    font-weight: 700;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.new-chat-btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  font-size: 14px;
  background: #6366f1;
  border-color: #6366f1;

  &:hover {
    background: #5558e6;
    border-color: #5558e6;
  }
}

// 会话列表

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  transition: all 0.15s;
  gap: 8px;

  &:hover {
    background: #f3f4f6;

    .conv-actions {
      opacity: 1;
    }
  }

  &.active {
    background: #eef0ff;

    .conv-title {
      color: #6366f1;
    }
  }
}

.conv-title {
  flex: 1;
  font-size: 14px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.conv-time {
  font-size: 11px;
  color: #9ca3af;
  flex-shrink: 0;
}

.conv-actions {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.conv-more {
  display: flex;
  padding: 2px;
  border-radius: 4px;
  color: #9ca3af;
  cursor: pointer;

  &:hover {
    background: #e5e7eb;
    color: #374151;
  }
}

.danger-text {
  color: #ef4444;
}

.no-conversations {
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;

  p { font-size: 14px; margin: 0 0 4px; }
  span { font-size: 12px; }
}

// 底部用户区

.sidebar-bottom {
  padding: 12px 16px;
  border-top: 1px solid #f0f1f5;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  flex: 1;
  font-size: 14px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-icon {
  color: #9ca3af;
  cursor: pointer;
  font-size: 18px;

  &:hover { color: #ef4444; }
}

// ---- 右侧主区域 ----

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

// 欢迎页

.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;

  h1 {
    font-size: 26px;
    font-weight: 600;
    color: #1f2937;
    margin: 20px 0 8px;
  }
}

.welcome-sub {
  color: #9ca3af;
  font-size: 15px;
  margin: 0 0 36px;
}

.quick-prompts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 520px;
  width: 100%;
}

.quick-prompt {
  padding: 14px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;

  &:hover {
    border-color: #6366f1;
    color: #6366f1;
    box-shadow: 0 2px 12px rgba(99, 102, 241, 0.1);
  }
}

// 对话顶栏

.chat-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e8eaef;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-select {
  width: 170px;
}

// 消息区域

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.load-more {
  text-align: center;
  padding: 12px;
}

// 消息行

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
  max-width: 800px;

  &.user {
    margin-left: auto;
    flex-direction: row-reverse;
  }

  &.agent {
    margin-right: auto;
  }
}

// 用户消息

.user-bubble {
  background: #6366f1;
  color: #fff;
  padding: 10px 16px;
  border-radius: 14px 4px 14px 14px;
  font-size: 14px;
  line-height: 1.6;
  max-width: 70%;
  word-break: break-word;
}

.msg-avatar {
  flex-shrink: 0;
}

// Agent 消息

.agent-avatar {
  flex-shrink: 0;
}

.agent-content {
  flex: 1;
  min-width: 0;
}

// 思考卡片

.thinking-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 13px;
  color: #6b7280;
  user-select: none;
  transition: background 0.15s;

  &:hover { background: #f9fafb; }
}

.thinking-icon {
  font-size: 16px;
  color: #6366f1;

  &.spinning {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.expand-icon {
  margin-left: auto;
  font-size: 12px;
  transition: transform 0.2s;

  &.expanded {
    transform: rotate(180deg);
  }
}

.thinking-body {
  padding: 0 14px 12px;
  border-top: 1px solid #f3f4f6;
}

.thinking-step {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  align-items: flex-start;

  & + & {
    border-top: 1px solid #f9fafb;
  }
}

.step-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #eef0ff;
  color: #6366f1;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #6366f1;
  flex-shrink: 0;
  margin: 6px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.step-text {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
  word-break: break-word;

  &.dim {
    color: #9ca3af;
    font-style: italic;
  }
}

// 工具调用卡片

.tool-card {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  margin-bottom: 8px;
  overflow: hidden;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: #92400e;
  background: #fffbeb;
}

.tool-body {
  padding: 0 12px 10px;
}

.tool-section {
  margin-top: 8px;
}

.tool-label {
  display: block;
  font-size: 11px;
  color: #a16207;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tool-code {
  display: block;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 12px;
  color: #78350f;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 120px;
  overflow-y: auto;
}

// Agent 最终回复

.agent-bubble {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px 14px 14px 14px;
  padding: 14px 18px;
  font-size: 14px;
  line-height: 1.7;
  color: #1f2937;
}

// 错误提示

.agent-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #dc2626;
  font-size: 13px;
}

// 输入区

.input-area {
  padding: 16px 24px 24px;
  flex-shrink: 0;
}

.input-box {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 8px;
  transition: box-shadow 0.2s;

  &:focus-within {
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
    border-color: #6366f1;
  }

  :deep(.el-textarea__inner) {
    border: none !important;
    box-shadow: none !important;
    padding: 8px 12px;
    font-size: 14px;
    line-height: 1.6;
    resize: none;
    background: transparent;
  }
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px 4px;
}

// ---- 响应式 ----

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    width: 85%;
    max-width: 280px;
    box-shadow: 4px 0 24px rgba(0,0,0,0.1);

    &:not(.open) {
      display: none;
    }
  }

  .quick-prompts {
    grid-template-columns: 1fr;
    max-width: 100%;
  }

  .messages-container {
    padding: 16px;
  }

  .input-area {
    padding: 12px 16px;
  }
}
</style>
