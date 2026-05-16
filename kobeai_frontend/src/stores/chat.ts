import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi, streamAgentChat } from '../api/chat'
import type { ChatMessage, Conversation, ToolCall } from '../api/chat'
import { useAuthStore } from './auth'

// ---- Agent 消息类型 ----

export interface AgentMessage {
  id: number
  role: 'user' | 'agent'
  content: string
  createdAt: string
  // Agent思考过程 (流式拼接的纯文本)
  reasoning: string
  toolCalls: ToolCall[]
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
    isStreaming: false,
    isThinking: false
  }
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
        messages.value = (res.data.messages || []).map(msgToAgent)
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
            }
          },
          onToolResult(tool) {
            const msg = messages.value.find(m => m.id === agentMsg.id)
            if (msg) {
              msg.toolCalls.push(tool)
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
            }
            loading.value = false
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
