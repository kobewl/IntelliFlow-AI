import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi, streamAgentChat } from '../api/chat'
import type { ChatMessage, Conversation, ToolCall } from '../api/chat'
import { useAuthStore } from './auth'

// ---- Agent 消息类型 ----

export interface WorkbenchStep {
  type: 'thinking' | 'tool_call'
  name?: string
  input?: string
  output?: string
  content?: string
  startTime: number
  endTime?: number
}

export interface AgentMessage {
  id: number
  role: 'user' | 'agent'
  content: string
  createdAt: string
  // Agent思考过程 (流式拼接的纯文本)
  reasoning: string
  toolCalls: ToolCall[]
  // 工作台步骤 (时间线)
  steps: WorkbenchStep[]
  isStreaming: boolean
  isThinking: boolean
  error?: string
}

function msgToAgent(msg: ChatMessage): AgentMessage {
  return {
    id: msg.id,
    role: msg.role === 'USER' ? 'user' : 'agent',
    content: msg.content,
    createdAt: msg.createdAt,
    reasoning: '',
    toolCalls: [],
    steps: [],
    isStreaming: false,
    isThinking: false
  }
}

// ---- 分支类型 ----

export interface Branch {
  id: string
  name: string
  forkMessageIndex: number
  messageCount: number
  createdAt: string
}

// ---- Store ----

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<number | null>(null)
  const messages = ref<AgentMessage[]>([])
  const loading = ref(false)
  const loadingMore = ref(false)
  let abortController: (() => void) | null = null

  // 从 localStorage 恢复
  function initFromStorage() {
    try {
      const saved = localStorage.getItem('chat_state')
      if (saved) {
        const state = JSON.parse(saved)
        conversations.value = state.conversations || []
        currentConversationId.value = state.currentConversationId || null
      }
    } catch { /* ignore */ }
  }

  function saveToStorage() {
    try {
      localStorage.setItem('chat_state', JSON.stringify({
        conversations: conversations.value,
        currentConversationId: currentConversationId.value
      }))
    } catch { /* ignore */ }
  }

  watch([conversations, currentConversationId], saveToStorage, { deep: true })

  const currentConversation = computed(() =>
    conversations.value.find(c => c.id === currentConversationId.value) || null
  )

  // ---- 会话 CRUD ----

  async function loadConversations() {
    try {
      loading.value = true
      const res = await chatApi.getConversations()
      if (res.code === 200) {
        conversations.value = res.data
        if (conversations.value.length > 0 && !currentConversationId.value) {
          currentConversationId.value = conversations.value[0].id
        }
      }
    } catch (err: any) {
      ElMessage.error(err.message || '加载会话失败')
    } finally {
      loading.value = false
    }
  }

  async function createConversation() {
    const res = await chatApi.createConversation()
    if (res.code === 200 && res.data) {
      const conv = { ...res.data, messages: [], hasMore: false }
      conversations.value.unshift(conv)
      currentConversationId.value = conv.id
      messages.value = []
      loadBranches()
      return conv
    }
    throw new Error('创建会话失败')
  }

  async function switchConversation(id: number) {
    loading.value = true
    try {
      const res = await chatApi.getConversationById(id)
      if (res.code === 200) {
        const idx = conversations.value.findIndex(c => c.id === id)
        if (idx !== -1) conversations.value[idx] = res.data
        currentConversationId.value = id
        loadBranches()
        // 如果有已保存的分支消息则用分支，否则用服务端消息
        if (branches.value['main'] && branches.value['main'].length > 0) {
          messages.value = branches.value[activeBranchId.value]
            ? [...branches.value[activeBranchId.value]]
            : [...branches.value['main']]
        } else {
          messages.value = (res.data.messages || []).map(msgToAgent)
          branches.value['main'] = [...messages.value]
        }
      }
    } finally {
      loading.value = false
    }
  }

  async function renameConversation(id: number, title: string) {
    await chatApi.renameConversation(id, title)
    const conv = conversations.value.find(c => c.id === id)
    if (conv) conv.title = title
  }

  async function deleteConversation(id: number) {
    await chatApi.deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConversationId.value === id) {
      currentConversationId.value = conversations.value[0]?.id || null
      messages.value = []
    }
  }

  async function loadMoreMessages() {
    if (!currentConversation.value || loadingMore.value || !currentConversation.value.hasMore) return
    loadingMore.value = true
    try {
      const res = await chatApi.getConversationMessages(currentConversation.value.id, {
        cursor: currentConversation.value.nextCursor,
        limit: 20
      })
      if (res.code === 200 && currentConversation.value) {
        const older = res.data.messages.map(msgToAgent)
        messages.value = [...older, ...messages.value]
        currentConversation.value.hasMore = res.data.hasMore
        currentConversation.value.nextCursor = res.data.nextCursor
      }
    } finally {
      loadingMore.value = false
    }
  }

  // ---- 分支管理 ----

  const branches = ref<Record<string, AgentMessage[]>>({})
  const activeBranchId = ref<string>('main')
  const branchMeta = ref<Record<string, Branch>>({})

  function getBranchKey() {
    return `chat_branches_${currentConversationId.value}`
  }

  function loadBranches() {
    try {
      const saved = localStorage.getItem(getBranchKey())
      if (saved) {
        const data = JSON.parse(saved)
        branches.value = data.branches || { main: [] }
        activeBranchId.value = data.active || 'main'
        branchMeta.value = data.meta || {}
        messages.value = branches.value[activeBranchId.value]
          ? [...branches.value[activeBranchId.value]]
          : []
      } else {
        branches.value = { main: [] }
        activeBranchId.value = 'main'
        branchMeta.value = { main: { id: 'main', name: '主分支', forkMessageIndex: 0, messageCount: 0, createdAt: new Date().toISOString() } }
      }
    } catch {
      branches.value = { main: [] }
      activeBranchId.value = 'main'
      messages.value = []
    }
  }

  function saveBranches() {
    // 同步当前消息到活跃分支
    branches.value[activeBranchId.value] = [...messages.value]
    // 更新分支元信息
    const meta = branchMeta.value[activeBranchId.value]
    if (meta) {
      meta.messageCount = messages.value.length
    }
    try {
      localStorage.setItem(getBranchKey(), JSON.stringify({
        branches: branches.value,
        active: activeBranchId.value,
        meta: branchMeta.value
      }))
    } catch { /* ignore */ }
  }

  function forkBranch(msgIndex: number, name?: string) {
    saveBranches()
    const forkMessages = messages.value.slice(0, msgIndex + 1)
    const branchId = `branch-${Date.now()}`
    branches.value[branchId] = [...forkMessages]
    branchMeta.value[branchId] = {
      id: branchId,
      name: name || `分支 ${Object.keys(branches.value).length}`,
      forkMessageIndex: msgIndex,
      messageCount: forkMessages.length,
      createdAt: new Date().toISOString()
    }
    activeBranchId.value = branchId
    messages.value = [...forkMessages]
    saveBranches()
    return branchId
  }

  function switchBranch(branchId: string) {
    if (branchId === activeBranchId.value) return
    saveBranches()
    activeBranchId.value = branchId
    messages.value = branches.value[branchId] ? [...branches.value[branchId]] : []
    saveBranches()
  }

  function deleteBranch(branchId: string) {
    if (branchId === 'main') return
    delete branches.value[branchId]
    delete branchMeta.value[branchId]
    if (activeBranchId.value === branchId) {
      activeBranchId.value = 'main'
      messages.value = branches.value['main'] ? [...branches.value['main']] : []
    }
    saveBranches()
  }

  function getBranchList() {
    return Object.values(branchMeta.value).sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  }

  // ---- 发送消息 (Agent SSE) ----

  async function sendMessage(content: string) {
    if (!content.trim()) return

    if (!currentConversationId.value) {
      await createConversation()
    }
    if (!currentConversationId.value) throw new Error('创建会话失败')

    const authStore = useAuthStore()

    // 添加用户消息
    const userMsg: AgentMessage = {
      id: Date.now(),
      role: 'user',
      content,
      createdAt: new Date().toISOString(),
      reasoning: '',
      toolCalls: [],
      steps: [],
      isStreaming: false,
      isThinking: false
    }
    messages.value.push(userMsg)

    // 添加 Agent 消息占位
    const agentMsg: AgentMessage = {
      id: Date.now() + 1,
      role: 'agent',
      content: '',
      createdAt: new Date().toISOString(),
      reasoning: '',
      toolCalls: [],
      steps: [],
      isStreaming: true,
      isThinking: true
    }
    messages.value.push(agentMsg)

    loading.value = true

    return new Promise<void>((resolve, reject) => {
      const { abort } = streamAgentChat(
        content,
        {
          onReasoning(data) {
            const msg = messages.value.find(m => m.id === agentMsg.id)
            if (msg) {
              msg.reasoning += data
              msg.isThinking = true
              // 工作台: 追加思考步骤
              const lastStep = msg.steps[msg.steps.length - 1]
              if (!lastStep || lastStep.type !== 'thinking') {
                msg.steps.push({ type: 'thinking', content: data, startTime: Date.now() })
              } else {
                lastStep.content = (lastStep.content || '') + data
              }
            }
          },
          onToolResult(tool) {
            const msg = messages.value.find(m => m.id === agentMsg.id)
            if (msg) {
              msg.toolCalls.push(tool)
              // 工作台: 结束思考步骤，添加工具步骤
              const lastStep = msg.steps[msg.steps.length - 1]
              if (lastStep && lastStep.type === 'thinking' && !lastStep.endTime) {
                lastStep.endTime = Date.now()
              }
              msg.steps.push({
                type: 'tool_call',
                name: tool.name,
                input: tool.input,
                output: tool.output,
                startTime: Date.now(),
                endTime: Date.now()
              })
            }
          },
          onAgentResult(text) {
            const msg = messages.value.find(m => m.id === agentMsg.id)
            if (msg) {
              msg.content = text
              msg.isThinking = false
            }
          },
          onError(err) {
            const msg = messages.value.find(m => m.id === agentMsg.id)
            if (msg) {
              msg.error = err
              msg.isStreaming = false
              msg.isThinking = false
            }
            loading.value = false
            reject(new Error(err))
          },
          onDone(fullContent) {
            const msg = messages.value.find(m => m.id === agentMsg.id)
            if (msg) {
              msg.content = fullContent
              msg.isStreaming = false
              msg.isThinking = false
              // 工作台: 结束最后一步
              const lastStep = msg.steps[msg.steps.length - 1]
              if (lastStep && !lastStep.endTime) {
                lastStep.endTime = Date.now()
              }
            }
            loading.value = false
            saveBranches()
            resolve()
          }
        }
      )

      abortController = abort
    })
  }

  function stopStreaming() {
    abortController?.()
    loading.value = false
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'agent') {
      lastMsg.isStreaming = false
      lastMsg.isThinking = false
    }
  }

  function clearStore() {
    conversations.value = []
    currentConversationId.value = null
    messages.value = []
    branches.value = {}
    activeBranchId.value = 'main'
    branchMeta.value = {}
    localStorage.removeItem('chat_state')
  }

  initFromStorage()

  return {
    conversations,
    currentConversationId,
    currentConversation,
    messages,
    loading,
    loadingMore,
    // 分支
    activeBranchId,
    branchMeta,
    getBranchList,
    forkBranch,
    switchBranch,
    deleteBranch,
    loadBranches,
    saveBranches,
    // 会话
    loadConversations,
    createConversation,
    switchConversation,
    renameConversation,
    deleteConversation,
    loadMoreMessages,
    sendMessage,
    stopStreaming,
    clearStore
  }
})
