import { defineStore } from 'pinia'
import { ref, computed, watch, onMounted } from 'vue'
import { chatApi } from '../api/chat'
import type { ChatMessage, Conversation } from '../api/chat'
import { ElMessage } from 'element-plus'
import { MessageRole } from '@/types/chat'
import { useAuthStore } from './auth'
import { authApi } from '../api/auth'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<number | null>(null)
  const loading = ref(false)
  const loadingMore = ref(false)
  const error = ref<string | null>(null)

  function initializeFromStorage() {
    try {
      const storedConversations = localStorage.getItem('conversations')
      const storedCurrentId = localStorage.getItem('currentConversationId')

      if (storedConversations) {
        conversations.value = JSON.parse(storedConversations)
      }

      if (storedCurrentId) {
        currentConversationId.value = parseInt(storedCurrentId)
      }
    } catch (err) {
      console.error('Failed to load state from storage:', err)
    }
  }

  function saveToStorage() {
    try {
      localStorage.setItem('conversations', JSON.stringify(conversations.value))
      if (currentConversationId.value) {
        localStorage.setItem('currentConversationId', currentConversationId.value.toString())
      } else {
        localStorage.removeItem('currentConversationId')
      }
    } catch (err) {
      console.error('Failed to save state to storage:', err)
    }
  }

  watch([conversations, currentConversationId], () => {
    saveToStorage()
  }, { deep: true })

  const currentConversation = computed(() => {
    if (!currentConversationId.value) return null
    return conversations.value.find(c => c.id === currentConversationId.value) || null
  })

  async function loadConversations() {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.getConversations()
      if (response.code === 200) {
        conversations.value = response.data

        if (conversations.value.length > 0 && !currentConversationId.value) {
          currentConversationId.value = conversations.value[0].id
        }

        saveToStorage()
      } else {
        throw new Error(response.message || '加载会话列表失败')
      }
    } catch (err: any) {
      error.value = err.toString()
      ElMessage.error(error.value)
      throw error.value
    } finally {
      loading.value = false
    }
  }

  async function createConversation() {
    try {
      loading.value = true
      error.value = null

      const response = await chatApi.createConversation()
      if (response.code === 200 && response.data) {
        const conversation: Conversation = {
          ...response.data,
          messages: [],
          hasMore: false
        }
        conversations.value.unshift(conversation)
        currentConversationId.value = conversation.id
        return conversation
      }
      throw new Error(response.message || '创建会话失败')
    } catch (error: any) {
      console.error('创建会话失败:', error)
      throw new Error(error.message || '创建会话失败，请重试')
    } finally {
      loading.value = false
    }
  }

  async function switchConversation(id: number) {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.getConversationById(id)
      if (response.code === 200) {
        const conversation = response.data

        const index = conversations.value.findIndex(c => c.id === id)
        if (index !== -1) {
          conversations.value[index] = conversation
        }

        currentConversationId.value = id
      } else {
        throw new Error(response.message || '切换会话失败')
      }
    } catch (err: any) {
      error.value = err.toString()
      ElMessage.error(error.value)
      throw error.value
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(content: string) {
    if (!content.trim()) {
      throw new Error('消息内容不能为空')
    }

    try {
      loading.value = true
      error.value = null

      if (!currentConversationId.value) {
        const conversation = await createConversation()
        if (!conversation) {
          throw new Error('创建会话失败')
        }
      }

      if (!currentConversation.value) {
        throw new Error('没有选中的会话')
      }

      const userMessage: ChatMessage = {
        id: Date.now(),
        content,
        role: MessageRole.USER,
        createdAt: new Date().toISOString()
      }

      if (!currentConversation.value.messages) {
        currentConversation.value.messages = []
      }
      currentConversation.value.messages.push(userMessage)

      try {
        const aiMessage: ChatMessage = {
          id: Date.now() + 1,
          content: '',
          role: MessageRole.ASSISTANT,
          createdAt: new Date().toISOString()
        }
        currentConversation.value.messages.push(aiMessage)

        const response = await chatApi.sendMessage(
          currentConversationId.value,
          content,
          (newContent: string) => {
            if (currentConversation.value) {
              const lastMessage = currentConversation.value.messages[currentConversation.value.messages.length - 1]
              if (lastMessage && lastMessage.role === MessageRole.ASSISTANT) {
                console.log('Updating AI message content:', newContent)
                lastMessage.content = newContent
              }
            }
          }
        )

        if (response.code === 200 && response.data) {
          const lastMessage = currentConversation.value.messages[currentConversation.value.messages.length - 1]
          if (lastMessage && lastMessage.role === MessageRole.ASSISTANT) {
            lastMessage.id = response.data.id
            lastMessage.content = response.data.content
            lastMessage.createdAt = response.data.createdAt
          }
          return lastMessage
        }

        throw new Error(response.message || '发送消息失败')
      } catch (err: any) {
        if (currentConversation.value?.messages) {
          currentConversation.value.messages = currentConversation.value.messages.filter(
            msg => msg.role !== MessageRole.ASSISTANT
          )
        }

        if (err.message?.includes('未登录') || err.message?.includes('认证已过期')) {
          const authStore = useAuthStore()
          try {
            await authApi.refreshToken()
            return await sendMessage(content)
          } catch (refreshError) {
            await authStore.logout()
            ElMessage.error('登录已过期，请重新登录')
            throw new Error('认证已过期，请重新登录')
          }
        }

        error.value = err.message || '发送消息失败'
        ElMessage.error(error.value)
        throw err
      }
    } finally {
      loading.value = false
    }
  }

  async function loadMoreMessages() {
    if (!currentConversation.value || loadingMore.value || !currentConversation.value.hasMore) {
      return
    }

    try {
      loadingMore.value = true
      error.value = null
      const response = await chatApi.getConversationMessages(
        currentConversation.value.id,
        {
          cursor: currentConversation.value.nextCursor,
          limit: 20
        }
      )

      if (response.code === 200) {
        const { messages, hasMore, nextCursor } = response.data

        if (currentConversation.value) {
          currentConversation.value.messages.push(...messages)
          currentConversation.value.hasMore = hasMore
          currentConversation.value.nextCursor = nextCursor
        }
      } else {
        throw new Error(response.message || '加载更多消息失败')
      }
    } catch (err: any) {
      error.value = err.toString()
      ElMessage.error(error.value)
      throw error.value
    } finally {
      loadingMore.value = false
    }
  }

  async function renameConversation(id: number, title: string) {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.renameConversation(id, title)
      if (response.code === 200) {
        const conversation = conversations.value.find(c => c.id === id)
        if (conversation) {
          conversation.title = title
        }
      } else {
        throw new Error(response.message || '重命名会话失败')
      }
    } catch (err: any) {
      error.value = err.toString()
      ElMessage.error(error.value)
      throw error.value
    } finally {
      loading.value = false
    }
  }

  async function deleteConversation(id: number) {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.deleteConversation(id)
      if (response.code === 200) {
        const index = conversations.value.findIndex(c => c.id === id)
        if (index !== -1) {
          conversations.value.splice(index, 1)
        }

        if (currentConversationId.value === id) {
          currentConversationId.value = conversations.value[0]?.id || null
        }
      } else {
        throw new Error(response.message || '删除会话失败')
      }
    } catch (err: any) {
      error.value = err.toString()
      ElMessage.error(error.value)
      throw error.value
    } finally {
      loading.value = false
    }
  }

  function clearStore() {
    conversations.value = []
    currentConversationId.value = null
    localStorage.removeItem('conversations')
    localStorage.removeItem('currentConversationId')
  }

  onMounted(() => {
    initializeFromStorage()
  })

  return {
    conversations,
    currentConversationId,
    currentConversation,
    loading,
    loadingMore,
    error,
    loadConversations,
    createConversation,
    switchConversation,
    sendMessage,
    loadMoreMessages,
    renameConversation,
    deleteConversation,
    clearStore
  }
})
