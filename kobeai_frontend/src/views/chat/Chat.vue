<template>
  <div class="agent-chat">
    <!-- 左侧栏 -->
    <aside class="sidebar" :class="{ collapsed: !showSidebar }">
      <div class="sidebar-top">
        <router-link to="/" class="sidebar-logo">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" class="logo-icon">
            <rect width="24" height="24" rx="6" fill="url(#logo-grad)" />
            <path d="M7 9h10M7 12h7M7 15h4" stroke="#fff" stroke-width="1.5" stroke-linecap="round" />
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="24" y2="24">
                <stop stop-color="#6366f1" /><stop offset="1" stop-color="#8b5cf6" />
              </linearGradient>
            </defs>
          </svg>
          <span>KobeAI</span>
        </router-link>
        <el-button class="new-chat-btn" @click="handleNewChat">
          <el-icon><Plus /></el-icon>
          <span>新对话</span>
        </el-button>
      </div>

      <!-- 会话搜索 -->
      <div class="search-box" v-if="conversations.length > 3">
        <el-input v-model="searchText" size="small" placeholder="搜索会话..." clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <!-- 会话列表 -->
      <div class="conversation-list" ref="listRef">
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === currentConversationId }"
          @click="handleSelectConv(conv.id)"
        >
          <el-icon class="conv-icon"><ChatRound /></el-icon>
          <div class="conv-info">
            <div class="conv-title">{{ conv.title || '新对话' }}</div>
            <div class="conv-meta">
              <span class="conv-time">{{ formatTime(conv.updatedAt) }}</span>
              <span class="conv-count" v-if="conv.messageCount">{{ conv.messageCount }} 条</span>
            </div>
          </div>
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

        <div v-if="filteredConversations.length === 0 && conversations.length === 0" class="empty-list">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none" class="empty-icon">
            <rect x="6" y="8" width="36" height="32" rx="4" stroke="#d1d5db" stroke-width="2" />
            <line x1="12" y1="18" x2="36" y2="18" stroke="#e5e7eb" stroke-width="1.5" />
            <line x1="12" y1="24" x2="30" y2="24" stroke="#e5e7eb" stroke-width="1.5" />
            <line x1="12" y1="30" x2="24" y2="30" stroke="#e5e7eb" stroke-width="1.5" />
          </svg>
          <p>开始你的第一次对话</p>
        </div>
        <div v-else-if="filteredConversations.length === 0" class="empty-list">
          <p>没有匹配的会话</p>
        </div>
      </div>

      <!-- 底部状态区 -->
      <div class="sidebar-bottom">
        <div class="agent-status">
          <span class="status-dot"></span>
          <span>Agent 就绪</span>
          <span class="tool-count" v-if="toolCount > 0">{{ toolCount }} 个工具</span>
        </div>
        <div class="user-area" v-if="authStore.isAuthenticated">
          <el-avatar :size="28" :src="authStore.user?.avatar || '/ai-avatar.png'" />
          <span class="user-name">{{ authStore.user?.username }}</span>
          <el-icon class="logout-icon" @click="handleLogout" title="退出登录">
            <SwitchButton />
          </el-icon>
        </div>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <div class="main-area">
      <!-- 欢迎页 -->
      <div v-if="!currentConversationId" class="welcome">
        <div class="welcome-bg"></div>
        <div class="welcome-content">
          <div class="welcome-icon">
            <svg width="72" height="72" viewBox="0 0 72 72" fill="none">
              <circle cx="36" cy="36" r="36" fill="url(#welcome-grad)" />
              <circle cx="36" cy="36" r="36" fill="url(#welcome-grad2)" opacity="0.5" />
              <path d="M24 28h24M24 36h18M24 44h12" stroke="#fff" stroke-width="3" stroke-linecap="round" />
              <defs>
                <linearGradient id="welcome-grad" x1="0" y1="0" x2="72" y2="72">
                  <stop stop-color="#6366f1" /><stop offset="1" stop-color="#8b5cf6" />
                </linearGradient>
                <radialGradient id="welcome-grad2" cx="0.3" cy="0.3" r="1">
                  <stop stop-color="#a5b4fc" /><stop offset="1" stop-color="transparent" />
                </radialGradient>
              </defs>
            </svg>
          </div>
          <h1>{{ greetingText }}</h1>
          <p class="welcome-sub">我可以帮你编程、计算、搜索知识库，试试下面的问题吧</p>

          <!-- 快捷提示 -->
          <div class="quick-prompts">
            <div
              v-for="prompt in quickPrompts"
              :key="prompt.text"
              class="quick-prompt"
              @click="handleQuickPrompt(prompt.text)"
            >
              <span class="prompt-icon">{{ prompt.icon }}</span>
              <span class="prompt-text">{{ prompt.text }}</span>
            </div>
          </div>

          <!-- 工具能力展示 -->
          <div class="capability-bar" v-if="toolCount > 0">
            <span class="cap-title">可用能力</span>
            <div class="cap-tags">
              <span v-for="t in topTools" :key="t.name" class="cap-tag">{{ t.description }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 对话区域 -->
      <template v-else>
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
              @change="handleModelChange"
            >
              <el-option label="自动选择" value="auto" />
              <el-option label="DeepSeek V4 Flash" value="deepseek-v4-flash" />
              <el-option label="DeepSeek V4 Pro" value="deepseek-v4-pro" />
            </el-select>
            <!-- 分支选择器 -->
            <el-select
              v-if="branchList.length > 1"
              v-model="chatStore.activeBranchId"
              size="small"
              class="branch-select"
              @change="handleBranchSwitch"
            >
              <el-option
                v-for="b in branchList"
                :key="b.id"
                :label="b.name + ' (' + b.messageCount + '条)'"
                :value="b.id"
              />
            </el-select>
            <el-button text class="clear-btn" @click="handleClearChat" title="清空对话">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </header>

        <!-- 消息列表 -->
        <div class="messages-container" ref="msgContainerRef">
          <div v-if="loadingMore" class="load-more">
            <el-button text size="small" :loading="loadingMore" @click="handleLoadMore">
              加载更早的消息
            </el-button>
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
                <span class="bubble-time">{{ formatMsgTime(msg.createdAt) }}</span>
              </div>
              <div class="user-msg-actions">
                <el-tooltip content="从这里分叉对话" placement="left">
                  <el-button
                    class="fork-btn"
                    size="small"
                    circle
                    text
                    @click="handleFork(i)"
                    :disabled="loading"
                  >
                    <el-icon><Share /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
              <el-avatar :size="30" :src="authStore.user?.avatar || '/ai-avatar.png'" class="msg-avatar" />
            </template>

            <!-- Agent 消息 -->
            <template v-else>
              <div class="agent-avatar">
                <svg width="30" height="30" viewBox="0 0 30 30" fill="none">
                  <rect width="30" height="30" rx="8" fill="url(#ai-grad)" />
                  <path d="M8 10h14M8 15h10M8 20h7" stroke="#fff" stroke-width="1.5" stroke-linecap="round" />
                  <defs>
                    <linearGradient id="ai-grad" x1="0" y1="0" x2="30" y2="30">
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
                  :class="{ collapsed: !msg._thinkingOpen }"
                >
                  <div class="thinking-header" @click="msg._thinkingOpen = !msg._thinkingOpen">
                    <div class="thinking-header-left">
                      <span class="thinking-dot" :class="{ active: msg.isThinking }"></span>
                      <span class="thinking-label">
                        {{ msg.isThinking ? '深度思考中...' : '思考过程' }}
                      </span>
                    </div>
                    <el-icon class="expand-icon" :class="{ expanded: msg._thinkingOpen }">
                      <ArrowDown />
                    </el-icon>
                  </div>
                  <transition name="collapse">
                    <div class="thinking-body" v-show="msg._thinkingOpen">
                      <div class="thinking-text">{{ msg.reasoning }}</div>
                      <div v-if="msg.isThinking" class="thinking-pending">
                        <span class="step-loading"></span>
                        <span class="step-text dim">思考中...</span>
                      </div>
                    </div>
                  </transition>
                </div>

                <!-- 工作台时间线 -->
                <AgentWorkbench :steps="msg.steps" />

                <!-- 工具执行中状态指示器 -->
                <div v-if="hasRunningTool(msg)" class="tool-running-bar">
                  <span class="running-spinner"></span>
                  <span class="running-text">正在执行 {{ runningToolName(msg) }}...</span>
                </div>

                <!-- 工具调用结果卡片 -->
                <div
                  v-for="(tool, ti) in msg.toolCalls"
                  :key="'tool-' + ti"
                  class="tool-card"
                  :class="{ 'is-error': tool.status === 'error' }"
                >
                  <div class="tool-header" @click="toggleToolResult(tool)">
                    <el-icon class="tool-icon" :class="{ spinning: tool.status === 'running' }">
                      <Loading v-if="tool.status === 'running'" />
                      <Tools v-else />
                    </el-icon>
                    <span class="tool-name">{{ tool.name }}</span>
                    <span class="tool-badge" :class="tool.status">
                      {{ toolStatusText(tool) }}
                    </span>
                    <el-icon class="tool-expand" :class="{ open: isToolExpanded(tool) }">
                      <ArrowDown />
                    </el-icon>
                  </div>
                  <div class="tool-body" v-show="isToolExpanded(tool)">
                    <div v-if="tool.input" class="tool-section">
                      <span class="tool-label">输入参数</span>
                      <code class="tool-code">{{ tool.input }}</code>
                    </div>
                    <div v-if="tool.output" class="tool-section">
                      <span class="tool-label">返回结果</span>
                      <code class="tool-code" :class="{ 'collapsed': isLongOutput(tool.output) && !expandedOutputs.has(tool.toolCallId || '') }">
                        {{ tool.output }}
                      </code>
                      <span
                        v-if="isLongOutput(tool.output)"
                        class="expand-output-btn"
                        @click="toggleOutputExpand(tool.toolCallId || '')"
                      >
                        {{ expandedOutputs.has(tool.toolCallId || '') ? '收起结果' : '展开完整结果' }}
                        <el-icon><ArrowDown /></el-icon>
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 最终回复 -->
                <div v-if="msg.content" class="agent-bubble" :class="{ streaming: msg.isStreaming }">
                  <MarkdownRenderer :content="msg.content" />
                  <span class="bubble-time agent-time">{{ formatMsgTime(msg.createdAt) }}</span>
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
              ref="inputRef"
              v-model="inputText"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 5 }"
              :placeholder="inputPlaceholder"
              :disabled="loading"
              @keydown.enter.exact.prevent="handleSend"
              @keydown.enter.shift.exact="inputText += '\n'"
              @keydown.escape="inputText = ''"
              @input="handleInputChange"
            />
            <div class="input-actions">
              <div class="input-hints">
                <span class="hint" v-if="inputText.startsWith('/')">
                  <span class="hint-cmd" @click="handleCommand('/help')">/help</span>
                  <span class="hint-cmd" @click="handleCommand('/clear')">/clear</span>
                  <span class="hint-cmd" @click="handleCommand('/tools')">/tools</span>
                </span>
                <span class="hint" v-else>Enter 发送 · Shift+Enter 换行 · Esc 清空</span>
              </div>
              <div class="input-right">
                <span class="char-count" v-if="inputText.length > 500">
                  {{ inputText.length }}
                </span>
                <el-button
                  v-if="loading"
                  type="danger"
                  plain
                  size="small"
                  @click="handleStop"
                >
                  <el-icon><CloseBold /></el-icon>
                  停止
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  :disabled="!inputText.trim()"
                  @click="handleSend"
                >
                  <el-icon><Position /></el-icon>
                  发送
                </el-button>
              </div>
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
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Position, Delete, Edit, MoreFilled,
  Loading, Check, ArrowDown, Operation,
  WarningFilled, SwitchButton, Fold, ChatRound,
  Search, Tools, CloseBold, Share
} from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import { agentApi } from '../../api/chat'
import type { Conversation, ToolInfo } from '../../api/chat'
import MarkdownRenderer from '../../components/MarkdownRenderer.vue'
import AgentWorkbench from '../../components/AgentWorkbench.vue'
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
  loadingMore,
  activeBranchId,
  branchMeta
} = storeToRefs(chatStore)

// ---- 状态 ----
const inputText = ref('')
const selectedModel = ref('auto')
const showSidebar = ref(true)
const searchText = ref('')
const msgContainerRef = ref<HTMLElement | null>(null)
const scrollAnchorRef = ref<HTMLElement | null>(null)
const inputRef = ref<any>(null)
const renameVisible = ref(false)
const renameText = ref('')
const renameTarget = ref<Conversation | null>(null)
const toolInfos = ref<ToolInfo[]>([])
const toolCount = ref(0)
const expandedToolIds = ref(new Set<string>())
const expandedOutputs = ref(new Set<string>())

// ---- 计算属性 ----

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，早点休息'
  if (h < 12) return '早上好，有什么我可以帮你的？'
  if (h < 14) return '中午好，需要我帮你做些什么？'
  if (h < 18) return '下午好，一起开始高效工作吧'
  return '晚上好，有什么需要我帮忙的？'
})

const inputPlaceholder = computed(() =>
  loading.value ? 'Agent 正在回复中...' : '输入消息，Enter 发送，Shift+Enter 换行'
)

const filteredConversations = computed(() => {
  if (!searchText.value.trim()) return conversations.value
  const q = searchText.value.toLowerCase()
  return conversations.value.filter(c => (c.title || '').toLowerCase().includes(q))
})

const topTools = computed(() => toolInfos.value.slice(0, 4))

const branchList = computed(() => chatStore.getBranchList())

const iframePath = '/ai-avatar.png'

const quickPrompts = [
  { icon: '⏰', text: '现在几点了？帮我算一下 3^10' },
  { icon: '💻', text: '写一个 Python 快速排序算法' },
  { icon: '📊', text: '帮我分析一下这个项目的架构' },
  { icon: '🔍', text: '搜索知识库中关于 Spring Boot 的内容' }
]

// ---- 时间格式化 ----
function formatTime(t: string) {
  return dayjs(t).fromNow()
}

function formatMsgTime(t: string) {
  return dayjs(t).format('HH:mm')
}

// ---- 滚动 ----
function scrollToBottom() {
  nextTick(() => {
    scrollAnchorRef.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

watch(() => messages.value.length, scrollToBottom)
watch(() => {
  const last = messages.value[messages.value.length - 1]
  return last?.content?.length ?? 0
}, scrollToBottom)

// ---- 初始化 ----
onMounted(async () => {
  await chatStore.loadConversations()
  if (currentConversationId.value) {
    await chatStore.switchConversation(currentConversationId.value)
    scrollToBottom()
  }
  // 获取工具列表
  try {
    const res = await agentApi.getTools()
    if (res.code === 200) {
      toolInfos.value = res.data
      toolCount.value = res.total
    }
  } catch { /* 后端不可用 */ }
})

// ---- 会话操作 ----
async function handleNewChat() {
  try {
    await chatStore.createConversation()
    scrollToBottom()
    nextTick(() => inputRef.value?.focus())
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
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
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

// ---- 消息操作 ----
function handleQuickPrompt(prompt: string) {
  handleNewChat().then(() => {
    inputText.value = prompt
    handleSend()
  })
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // 处理 / 命令
  if (text.startsWith('/')) {
    handleCommand(text)
    return
  }

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

function handleCommand(cmd: string) {
  inputText.value = ''
  switch (cmd) {
    case '/help':
      ElMessageBox.alert(
        '可用命令：<br>/help - 显示帮助<br>/clear - 清空当前对话<br>/tools - 查看可用工具',
        '帮助',
        { dangerouslyUseHTMLString: true }
      )
      break
    case '/clear':
      handleClearChat()
      break
    case '/tools':
      if (toolInfos.value.length > 0) {
        const toolList = toolInfos.value.map(t => `<b>${t.name}</b>: ${t.description}`).join('<br>')
        ElMessageBox.alert(toolList, '可用工具', { dangerouslyUseHTMLString: true })
      } else {
        ElMessage.info('暂无可用的 Agent 工具')
      }
      break
    default:
      ElMessage.warning('未知命令。输入 /help 查看可用命令')
  }
}

function handleStop() {
  chatStore.stopStreaming()
}

async function handleClearChat() {
  try {
    await ElMessageBox.confirm('确定清空当前对话吗？', '提示', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
    messages.value = []
    ElMessage.success('已清空')
  } catch { /* 取消 */ }
}

function handleInputChange() {
  // 字符计数用
}

function handleModelChange(val: string) {
  ElMessage.success(val === 'auto' ? '已切换为自动选择模型' : `已切换模型: ${val}`)
}

// ---- 工具状态辅助函数 ----

function hasRunningTool(msg: any): boolean {
  return msg.steps?.some((s: any) => s.type === 'tool_call' && s.status === 'running')
}

function runningToolName(msg: any): string {
  const step = msg.steps?.find((s: any) => s.type === 'tool_call' && s.status === 'running')
  return step?.name || '工具'
}

function toolStatusText(tool: any): string {
  if (tool.status === 'running') return '执行中'
  if (tool.status === 'error') return '失败'
  return '完成'
}

function toggleToolResult(tool: any) {
  const id = tool.toolCallId || tool.name
  if (expandedToolIds.value.has(id)) {
    expandedToolIds.value.delete(id)
  } else {
    expandedToolIds.value.add(id)
  }
}

function isToolExpanded(tool: any): boolean {
  const id = tool.toolCallId || tool.name
  return expandedToolIds.value.has(id)
}

function isLongOutput(output: string): boolean {
  return output && output.length > 300
}

function toggleOutputExpand(toolCallId: string) {
  if (expandedOutputs.value.has(toolCallId)) {
    expandedOutputs.value.delete(toolCallId)
  } else {
    expandedOutputs.value.add(toolCallId)
  }
}

async function handleLoadMore() {
  await chatStore.loadMoreMessages()
}

// ---- 分支操作 ----

function handleBranchSwitch(branchId: string) {
  chatStore.switchBranch(branchId)
  scrollToBottom()
}

function handleFork(msgIndex: number) {
  ElMessageBox.prompt('输入分支名称（可选）', '创建对话分支', {
    confirmButtonText: '分叉',
    cancelButtonText: '取消',
    inputPlaceholder: '默认: 分支 N'
  }).then(({ value }) => {
    const branchId = chatStore.forkBranch(msgIndex, value || undefined)
    ElMessage.success(`已创建分支: ${branchMeta.value[branchId]?.name || '分支'}`)
    scrollToBottom()
  }).catch(() => { /* 取消 */ })
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
    chatStore.clearStore()
    await authStore.logout()
    router.push('/')
  } catch { /* 取消 */ }
}
</script>

<style lang="scss" scoped>
// ---- 变量 ----
$sidebar-width: 280px;
$accent: #6366f1;
$accent-light: #eef0ff;
$border: #e8eaef;
$bg-page: #f8f9fb;
$text-1: #1f2937;
$text-2: #6b7280;
$text-3: #9ca3af;

// ---- 容器 ----
.agent-chat {
  height: 100vh;
  display: flex;
  background: $bg-page;
  overflow: hidden;
  font-family: var(--font-sans, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
}

// ---- 左侧栏 ----
.sidebar {
  width: $sidebar-width;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid $border;
  transition: width 0.25s ease;

  &.collapsed {
    width: 0;
    overflow: hidden;
    border: none;
  }
}

.sidebar-top {
  padding: 16px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  margin-bottom: 14px;

  span {
    font-size: 20px;
    font-weight: 700;
    background: linear-gradient(135deg, $accent, #8b5cf6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.new-chat-btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  background: $accent;
  border-color: $accent;

  &:hover { background: #5558e6; border-color: #5558e6; }
}

// 搜索
.search-box {
  padding: 0 12px 8px;
}

// 会话列表
.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: #f3f4f6;

    .conv-actions { opacity: 1; }
  }

  &.active {
    background: $accent-light;

    .conv-title { color: $accent; font-weight: 500; }
  }
}

.conv-icon {
  font-size: 16px;
  color: $text-3;
  flex-shrink: 0;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-title {
  font-size: 13px;
  color: $text-1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-meta {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.conv-time {
  font-size: 11px;
  color: $text-3;
}

.conv-count {
  font-size: 11px;
  color: $accent;
}

.conv-actions {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.conv-more {
  display: flex;
  padding: 3px;
  border-radius: 4px;
  color: $text-3;
  cursor: pointer;

  &:hover { background: #e5e7eb; color: $text-1; }
}

.danger-text { color: #ef4444; }

// 空列表
.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  color: $text-3;
  text-align: center;

  p { font-size: 13px; margin: 12px 0 0; }

  .empty-icon { opacity: 0.4; }
}

// 底部
.sidebar-bottom {
  padding: 10px 16px;
  border-top: 1px solid #f0f1f5;
}

.agent-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-2;
  margin-bottom: 10px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.4);
}

.tool-count {
  margin-left: auto;
  color: $accent;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  flex: 1;
  font-size: 13px;
  color: $text-1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-icon {
  color: $text-3;
  cursor: pointer;
  font-size: 16px;

  &:hover { color: #ef4444; }
}

// ---- 主区域 ----
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

// ---- 欢迎页 ----
.welcome {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.welcome-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 50% at 50% 40%, rgba(99, 102, 241, 0.04), transparent),
    radial-gradient(ellipse 80% 60% at 50% 60%, rgba(139, 92, 246, 0.03), transparent);
  pointer-events: none;
}

.welcome-content {
  position: relative;
  text-align: center;
  max-width: 560px;
  padding: 40px 24px;
}

.welcome-icon {
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.welcome-content h1 {
  font-size: 26px;
  font-weight: 600;
  color: $text-1;
  margin: 0 0 8px;
}

.welcome-sub {
  color: $text-2;
  font-size: 15px;
  margin: 0 0 36px;
  line-height: 1.6;
}

// 快捷提示
.quick-prompts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  max-width: 520px;
  margin: 0 auto;
}

.quick-prompt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  font-size: 13px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  text-align: left;

  &:hover {
    border-color: $accent;
    color: $accent;
    box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);
    transform: translateY(-1px);
  }
}

.prompt-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.prompt-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 能力条
.capability-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.cap-title {
  font-size: 12px;
  color: $text-3;
}

.cap-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.cap-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  background: $accent-light;
  color: $accent;
}

// ---- 对话顶栏 ----
.chat-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid $border;
  flex-shrink: 0;
  z-index: 10;
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
  color: $text-1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-select { width: 160px; }

.branch-select { width: 150px; }

.clear-btn { color: $text-3; }

// ---- 消息区域 ----
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

.load-more {
  text-align: center;
  padding: 8px 0 16px;
}

// 消息行
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 28px;
  max-width: 820px;
  animation: fadeInUp 0.3s ease;

  &.user {
    margin-left: auto;
    flex-direction: row-reverse;
  }

  &.agent {
    margin-right: auto;
  }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

// 用户消息
.user-bubble {
  background: linear-gradient(135deg, $accent, #7c3aed);
  color: #fff;
  padding: 10px 16px;
  border-radius: 14px 4px 14px 14px;
  font-size: 14px;
  line-height: 1.6;
  max-width: 70%;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

.bubble-time {
  display: block;
  font-size: 10px;
  opacity: 0.7;
  margin-top: 4px;
  text-align: right;
}

.msg-avatar {
  flex-shrink: 0;
  align-self: flex-end;
}

// 用户消息操作按钮
.user-msg-actions {
  display: flex;
  align-items: center;
  opacity: 0;
  transition: opacity 0.15s;

  .message-row:hover & {
    opacity: 1;
  }
}

.fork-btn {
  color: #9ca3af;
  font-size: 15px;
  padding: 4px;

  &:hover { color: $accent; background: $accent-light; }
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
  border: 1px solid $border;
  border-left: 3px solid $accent;
  border-radius: 0 10px 10px 0;
  margin-bottom: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
}

.thinking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
}

.thinking-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thinking-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $accent;
  flex-shrink: 0;

  &.active {
    animation: pulse 1.2s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.3); }
}

.thinking-label {
  font-size: 13px;
  color: $text-2;
  font-weight: 500;
}

.thinking-count {
  font-size: 11px;
  color: $text-3;
  background: #f3f4f6;
  padding: 1px 8px;
  border-radius: 10px;
}

.expand-icon {
  font-size: 12px;
  color: $text-3;
  transition: transform 0.2s;

  &.expanded { transform: rotate(180deg); }
}

.thinking-body {
  padding: 12px 14px;
  border-top: 1px solid #f3f4f6;
  max-height: 240px;
  overflow-y: auto;
}

.thinking-text {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.thinking-pending {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}

.step-loading {
  width: 14px;
  height: 14px;
  border: 2px solid #e5e7eb;
  border-top-color: $accent;
  border-radius: 50%;
  flex-shrink: 0;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.step-text {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;

  &.dim { color: $text-3; font-style: italic; }
}

// 工具调用卡片
.tool-card {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;

  &.is-error {
    background: #fef2f2;
    border-color: #fecaca;
  }
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid #dcfce7;
  cursor: pointer;
  user-select: none;

  .tool-card.is-error & {
    border-bottom-color: #fecaca;
  }
}

.tool-icon {
  color: #16a34a;
  font-size: 16px;
  flex-shrink: 0;

  &.spinning {
    animation: spin 0.8s linear infinite;
  }

  .tool-card.is-error & {
    color: #dc2626;
  }
}

.tool-name {
  font-size: 13px;
  font-weight: 500;
  color: #166534;

  .tool-card.is-error & {
    color: #991b1b;
  }
}

.tool-expand {
  font-size: 12px;
  color: #9ca3af;
  margin-left: auto;
  transition: transform 0.2s;

  &.open { transform: rotate(180deg); }
}

.tool-badge {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  background: #dcfce7;
  color: #16a34a;

  &.running {
    background: #fef3c7;
    color: #d97706;
    animation: badgePulse 1.5s ease-in-out infinite;
  }

  &.error {
    background: #fecaca;
    color: #dc2626;
  }
}

@keyframes badgePulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.tool-body {
  padding: 10px 14px;
}

.tool-section {
  & + & { margin-top: 8px; }
}

.tool-label {
  display: block;
  font-size: 10px;
  color: #15803d;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

.tool-code {
  display: block;
  background: #fff;
  border: 1px solid #dcfce7;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: #14532d;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 120px;
  overflow-y: auto;
  font-family: var(--font-mono, monospace);

  &.collapsed {
    max-height: 72px;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 36px;
      background: linear-gradient(transparent, #fff);
      pointer-events: none;
    }
  }
}

// 展开结果按钮
.expand-output-btn {
  font-size: 11px;
  color: $accent;
  margin-top: 4px;
  padding: 2px 4px;

  .el-icon {
    font-size: 10px;
    margin-left: 2px;
    transition: transform 0.2s;
  }

  &:hover {
    color: #5558e6;
    background: $accent-light;
  }
}

// 工具执行中状态栏
.tool-running-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  margin-bottom: 8px;
}

.running-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #fde68a;
  border-top-color: #f59e0b;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

.running-text {
  font-size: 13px;
  color: #92400e;
}

// Agent 最终回复
.agent-bubble {
  background: #fff;
  border: 1px solid $border;
  border-radius: 4px 14px 14px 14px;
  padding: 16px 20px;
  font-size: 14px;
  line-height: 1.75;
  color: $text-1;
  transition: border-color 0.2s;

  &.streaming { border-color: $accent; }
}

.agent-time {
  display: block;
  margin-top: 8px;
  text-align: left;
}

// 错误
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

// ---- 输入区 ----
.input-area {
  padding: 16px 24px 24px;
  flex-shrink: 0;
}

.input-box {
  max-width: 820px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid $border;
  border-radius: 16px;
  padding: 6px 6px 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  transition: border-color 0.2s, box-shadow 0.2s;

  &:focus-within {
    border-color: $accent;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
  }

  :deep(.el-textarea__inner) {
    border: none !important;
    box-shadow: none !important;
    padding: 8px 12px;
    font-size: 14px;
    line-height: 1.6;
    resize: none;
    background: transparent;
    min-height: 24px;
  }
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
}

.input-hints {
  font-size: 11px;
  color: $text-3;
}

.hint-cmd {
  display: inline-block;
  padding: 2px 6px;
  margin: 0 2px;
  border-radius: 4px;
  background: #f3f4f6;
  cursor: pointer;
  font-family: var(--font-mono, monospace);
  color: $accent;
  font-size: 11px;

  &:hover { background: $accent-light; }
}

.input-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.char-count {
  font-size: 11px;
  color: #f59e0b;
}

// ---- 过渡 ----
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.2s ease;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

// ---- 响应式 ----
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    box-shadow: 4px 0 24px rgba(0,0,0,0.08);

    &.collapsed { display: none; }
  }

  .chat-header { padding: 0 12px; }

  .quick-prompts {
    grid-template-columns: 1fr;
  }

  .messages-container { padding: 16px; }

  .input-area { padding: 12px 16px; }

  .message-row {
    max-width: 100%;

    &.user .user-bubble { max-width: 85%; }
  }
}
</style>
