<template>
  <div class="agent-chat">
    <!-- 左侧栏 -->
    <ConversationSidebar
      :show-sidebar="showSidebar"
      :tool-count="toolCount"
      @new-chat="handleNewChat"
    />

    <!-- 右侧主区域 -->
    <div class="main-area">
      <!-- 欢迎页 -->
      <ChatWelcome
        v-if="!currentConversationId"
        :tool-infos="toolInfos"
        @prompt="handleQuickPrompt"
      />

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
        <ChatMessages @fork="handleFork" />

        <!-- 输入区 -->
        <ChatInputBox
          ref="inputBoxRef"
          v-model="inputText"
          :loading="loading"
          @send="handleSend"
          @stop="handleStop"
          @command="handleCommand"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Fold, Delete } from '@element-plus/icons-vue'
import { useChatStore } from '../../stores/chat'
import { agentApi } from '../../api/chat'
import type { ToolInfo } from '../../api/chat'
import ConversationSidebar from '../../components/chat/ConversationSidebar.vue'
import ChatWelcome from '../../components/chat/ChatWelcome.vue'
import ChatMessages from '../../components/chat/ChatMessages.vue'
import ChatInputBox from '../../components/chat/ChatInputBox.vue'

const chatStore = useChatStore()

const { currentConversationId, currentConversation, loading, branchMeta } =
  storeToRefs(chatStore)

// ---- 状态 ----
const inputText = ref('')
const selectedModel = ref('auto')
const showSidebar = ref(true)
const toolInfos = ref<ToolInfo[]>([])
const toolCount = ref(0)
const inputBoxRef = ref<InstanceType<typeof ChatInputBox> | null>(null)

const branchList = computed(() => chatStore.getBranchList())

// ---- 初始化 ----
onMounted(async () => {
  await chatStore.loadConversations()
  if (currentConversationId.value) {
    await chatStore.switchConversation(currentConversationId.value)
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
    nextTickFocus()
  } catch (err: any) {
    ElMessage.error(err.message || '创建失败')
  }
}

function nextTickFocus() {
  nextTick(() => inputBoxRef.value?.focus())
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
    chatStore.messages = []
    ElMessage.success('已清空')
  } catch { /* 取消 */ }
}

function handleModelChange(val: string) {
  ElMessage.success(val === 'auto' ? '已切换为自动选择模型' : `已切换模型: ${val}`)
}

// ---- 分支操作 ----

function handleBranchSwitch(branchId: string) {
  chatStore.switchBranch(branchId)
}

function handleFork(msgIndex: number) {
  ElMessageBox.prompt('输入分支名称（可选）', '创建对话分支', {
    confirmButtonText: '分叉',
    cancelButtonText: '取消',
    inputPlaceholder: '默认: 分支 N'
  }).then(({ value }) => {
    const branchId = chatStore.forkBranch(msgIndex, value || undefined)
    ElMessage.success(`已创建分支: ${branchMeta.value[branchId]?.name || '分支'}`)
  }).catch(() => { /* 取消 */ })
}
</script>

<style lang="scss" scoped>
@use '../../styles/chat-vars' as *;

// ---- 容器 ----
.agent-chat {
  height: 100vh;
  display: flex;
  background: $bg-page;
  overflow: hidden;
  font-family: var(--font-sans, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
}

// ---- 主区域 ----
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
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

// ---- 响应式 ----
@media (max-width: 768px) {
  .chat-header { padding: 0 12px; }
}
</style>
