import { defineStore } from 'pinia'
import { ref, computed, watch, onMounted } from 'vue'
import { chatApi } from '../api/chat'
import type { ChatMessage, Conversation } from '../api/chat'
import { ElMessage } from 'element-plus'
import { MessageRole } from '@/types/chat'
import { useAuthStore } from './auth'
import { authApi } from '../api/auth'

export const useChatStore = defineStore('chat', () => {
  // 从localStorage恢复状态
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<number | null>(null)
  const loading = ref(false)
  const loadingMore = ref(false)
  const error = ref<string | null>(null)

  // 初始化时从localStorage加载状态
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

  // 保存状态到localStorage
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

  // 监听状态变化并保存
  watch([conversations, currentConversationId], () => {
    saveToStorage()
  }, { deep: true })

  const currentConversation = computed(() => {
    if (!currentConversationId.value) return null
    return conversations.value.find(c => c.id === currentConversationId.value) || null
  })

  // 加载会话列表
  async function loadConversations() {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.getConversations()
      if (response.code === 200) {
        conversations.value = response.data
        
        // 如果有会话但没有选中的会话，选择第一个
        if (conversations.value.length > 0 && !currentConversationId.value) {
          currentConversationId.value = conversations.value[0].id
        }
        
        // 保存到localStorage
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

  // 创建新会话
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

  // 切换会话
  async function switchConversation(id: number) {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.getConversationById(id)
      if (response.code === 200) {
        const conversation = response.data
        
        // 更新会话列表中的会话
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

  // 发送消息
  async function sendMessage(content: string) {
    if (!content.trim()) {
      throw new Error('消息内容不能为空')
    }

    try {
      loading.value = true
      error.value = null

      // 如果没有当前会话，先创建一个新会话
      if (!currentConversationId.value) {
        const conversation = await createConversation()
        if (!conversation) {
          throw new Error('创建会话失败')
        }
      }

      if (!currentConversation.value) {
        throw new Error('没有选中的会话')
      }

      // 先添加用户消息到界面
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
        // 创建AI消息占位
        const aiMessage: ChatMessage = {
          id: Date.now() + 1,
          content: '',
          role: MessageRole.ASSISTANT,
          createdAt: new Date().toISOString()
        }
        currentConversation.value.messages.push(aiMessage)

        // 发送消息到服务器并处理流式响应
        const response = await chatApi.sendMessage(
          currentConversationId.value, 
          content,
          // 实时更新消息内容的回调函数
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
          // 更新最后一条消息的内容和ID
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
        // 发送失败时移除AI消息，保留用户消息
        if (currentConversation.value?.messages) {
          currentConversation.value.messages = currentConversation.value.messages.filter(
            msg => msg.role !== MessageRole.ASSISTANT
          )
        }

        // 处理认证错误
        if (err.message?.includes('未登录') || err.message?.includes('认证已过期')) {
          const authStore = useAuthStore()
          // 尝试刷新token
          try {
            await authApi.refreshToken()
            // 刷新成功，重试发送消息
            return await sendMessage(content)
          } catch (refreshError) {
            // 刷新失败，清除认证状态并跳转到登录页
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

  // 加载更多消息
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
        
        // 更新会话消息
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

  // 重命名会话
  async function renameConversation(id: number, title: string) {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.renameConversation(id, title)
      if (response.code === 200) {
        // 更新会话列表中的标题
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

  // 删除会话
  async function deleteConversation(id: number) {
    try {
      loading.value = true
      error.value = null
      const response = await chatApi.deleteConversation(id)
      if (response.code === 200) {
        // 从会话列表中移除
        const index = conversations.value.findIndex(c => c.id === id)
        if (index !== -1) {
          conversations.value.splice(index, 1)
        }

        // 如果删除的是当前会话，切换到第一个会话
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

  // 清除store状态
  function clearStore() {
    conversations.value = []
    currentConversationId.value = null
    localStorage.removeItem('conversations')
    localStorage.removeItem('currentConversationId')
  }

  // 初始化store
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