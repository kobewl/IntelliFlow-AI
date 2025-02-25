import { defineStore } from 'pinia'
import { ref, computed, watch, onMounted } from 'vue'
import { chatApi } from '../api/chat'
import type { ChatMessage, Conversation } from '../api/chat'
import { ElMessage } from 'element-plus'
import { MessageRole } from '@/types/chat'
import { useAuthStore } from './auth'
import { authApi } from '../api/auth'
import { offlineStorage } from '../utils/offlineStorage'
import { networkStatus } from '../utils/networkStatus'

export const useChatStore = defineStore('chat', {
  state: () => ({
    conversations: ref<Conversation[]>([]),
    currentConversationId: ref<number | null>(null),
    loading: ref(false),
    loadingMore: ref(false),
    error: ref<string | null>(null),
    isOfflineMode: ref(false)
  }),
  getters: {
    currentConversation: computed(() => {
      if (!useChatStore().currentConversationId) return null
      return useChatStore().conversations.find(c => c.id === useChatStore().currentConversationId) || null
    }),
    // 添加离线支持的计算属性
    hasOfflineSupport: computed(() => {
      // 默认启用离线支持
      return true
    }),
    // 检查是否有待处理的离线消息
    hasPendingMessages: computed(() => {
      return offlineStorage.hasPendingMessages()
    })
  },
  actions: {
    // 从localStorage恢复状态
    initializeFromStorage() {
      try {
        const storedConversations = localStorage.getItem('conversations')
        const storedCurrentId = localStorage.getItem('currentConversationId')
        
        if (storedConversations) {
          this.conversations = JSON.parse(storedConversations)
        }
        
        if (storedCurrentId) {
          this.currentConversationId = parseInt(storedCurrentId)
        }
      } catch (err) {
        console.error('Failed to load state from storage:', err)
      }
    },

    // 保存状态到localStorage
    saveToStorage() {
      try {
        localStorage.setItem('conversations', JSON.stringify(this.conversations))
        if (this.currentConversationId) {
          localStorage.setItem('currentConversationId', this.currentConversationId.toString())
        } else {
          localStorage.removeItem('currentConversationId')
        }
      } catch (err) {
        console.error('Failed to save state to storage:', err)
      }
    },

    // 监听状态变化并保存
    watchState() {
      watch([() => this.conversations, () => this.currentConversationId], () => {
        this.saveToStorage()
      }, { deep: true })
    },

    // 加载会话列表
    async loadConversations() {
      try {
        this.loading = true
        this.error = null
        const response = await chatApi.getConversations()
        if (response.code === 200) {
          this.conversations = response.data
          
          // 如果有会话但没有选中的会话，选择第一个
          if (this.conversations.length > 0 && !this.currentConversationId) {
            this.currentConversationId = this.conversations[0].id
          }
          
          // 保存到localStorage
          this.saveToStorage()
        } else {
          throw new Error(response.message || '加载会话列表失败')
        }
      } catch (err: any) {
        this.error = err.toString()
        ElMessage.error(this.error)
        throw this.error
      } finally {
        this.loading = false
      }
    },

    // 创建新会话
    async createConversation() {
      try {
        this.loading = true
        this.error = null
        
        const response = await chatApi.createConversation()
        if (response.code === 200 && response.data) {
          const conversation: Conversation = {
            ...response.data,
            messages: [],
            hasMore: false
          }
          this.conversations.unshift(conversation)
          this.currentConversationId = conversation.id
          return conversation
        }
        throw new Error(response.message || '创建会话失败')
      } catch (error: any) {
        console.error('创建会话失败:', error)
        throw new Error(error.message || '创建会话失败，请重试')
      } finally {
        this.loading = false
      }
    },

    // 切换会话
    async switchConversation(id: number) {
      try {
        this.loading = true
        this.error = null
        const response = await chatApi.getConversationById(id)
        if (response.code === 200) {
          const conversation = response.data
          
          // 更新会话列表中的会话
          const index = this.conversations.findIndex(c => c.id === id)
          if (index !== -1) {
            this.conversations[index] = conversation
          }
          
          this.currentConversationId = id
        } else {
          throw new Error(response.message || '切换会话失败')
        }
      } catch (err: any) {
        this.error = err.toString()
        ElMessage.error(this.error)
        throw this.error
      } finally {
        this.loading = false
      }
    },

    // 检查网络状态并设置离线模式
    async checkNetworkAndSetMode() {
      const isOnline = await networkStatus.checkConnection()
      this.isOfflineMode = !isOnline

      if (!isOnline) {
        console.log('已切换到离线模式')
        ElMessage.warning('网络连接不可用，已切换到离线模式')
        
        // 加载本地存储的对话
        const offlineConversations = offlineStorage.loadConversations()
        if (offlineConversations && offlineConversations.length > 0) {
          this.conversations = offlineConversations
          
          // 如果当前没有选中对话但离线有对话，选中第一个
          if (!this.currentConversationId && this.conversations.length > 0) {
            this.currentConversationId = this.conversations[0].id
          }
        }
      } else if (this.isOfflineMode) {
        // 从离线模式恢复到在线模式
        this.isOfflineMode = false
        console.log('已恢复到在线模式')
        ElMessage.success('网络连接已恢复')
        
        // 尝试发送所有待发送的消息
        this.syncPendingMessages()
      }
      
      return isOnline
    },
    
    // 同步待发送的消息
    async syncPendingMessages() {
      const pendingMessages = offlineStorage.getPendingMessages()
      
      if (pendingMessages.length === 0) return
      
      ElMessage.info(`正在同步 ${pendingMessages.length} 条离线消息...`)
      
      for (let i = 0; i < pendingMessages.length; i++) {
        const { conversationId, content, timestamp } = pendingMessages[i]
        
        try {
          // 发送消息
          await this.sendMessage(content, conversationId)
          
          // 移除已发送的消息
          offlineStorage.removePendingMessage(i)
        } catch (error) {
          console.error('同步离线消息失败:', error)
        }
      }
      
      ElMessage.success('离线消息同步完成')
    },
    
    // 覆盖发送消息方法以支持离线模式
    async sendMessage(content: string, specificConversationId?: number) {
      if (!content.trim()) {
        throw new Error('消息内容不能为空')
      }
      
      // 检查网络状态
      const isOnline = await networkStatus.checkConnection()
      
      if (!isOnline) {
        // 如果离线，存储到待发送队列
        const targetConversationId = specificConversationId || this.currentConversationId
        
        if (!targetConversationId) {
          // 如果没有会话，创建一个本地会话
          const offlineConversation = {
            id: Date.now(),
            title: '离线会话',
            messages: [],
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }
          
          this.conversations.unshift(offlineConversation)
          this.currentConversationId = offlineConversation.id
          
          // 保存到本地存储
          offlineStorage.saveConversations(this.conversations)
        }
        
        // 添加用户消息
        const userMessage = {
          id: Date.now(),
          role: MessageRole.USER,
          content: content,
          createdAt: new Date().toISOString()
        }
        
        // 把消息添加到当前对话
        if (!this.currentConversation.messages) {
          this.currentConversation.messages = []
        }
        this.currentConversation.messages.push(userMessage)
        
        // 添加离线指示消息
        const offlineIndicationMessage = {
          id: Date.now() + 1,
          role: MessageRole.SYSTEM,
          content: '⚠️ 您当前处于离线模式，消息将在网络恢复后发送',
          createdAt: new Date().toISOString()
        }
        this.currentConversation.messages.push(offlineIndicationMessage)
        
        // 保存消息到待发送队列
        offlineStorage.savePendingMessage(
          this.currentConversationId,
          content
        )
        
        // 保存对话到本地
        offlineStorage.saveConversations(this.conversations)
        
        return
      }
      
      // 原有的在线发送逻辑
      try {
        this.loading = true
        this.error = null
        
        // 如果没有当前会话，先创建一个新会话
        if (!this.currentConversationId) {
          const conversation = await this.createConversation()
          if (!conversation) {
            throw new Error('创建会话失败')
          }
        }

        if (!this.currentConversation) {
          throw new Error('没有选中的会话')
        }

        // 先添加用户消息到界面
        const userMessage: ChatMessage = {
          id: Date.now(),
          content,
          role: MessageRole.USER,
          createdAt: new Date().toISOString()
        }
        
        if (!this.currentConversation.messages) {
          this.currentConversation.messages = []
        }
        this.currentConversation.messages.push(userMessage)

        try {
          // 创建AI消息占位
          const aiMessage: ChatMessage = {
            id: Date.now() + 1,
            content: '',
            role: MessageRole.ASSISTANT,
            createdAt: new Date().toISOString()
          }
          this.currentConversation.messages.push(aiMessage)

          // 发送消息到服务器并处理流式响应
          const response = await chatApi.sendMessage(
            this.currentConversationId, 
            content,
            // 实时更新消息内容的回调函数
            (newContent: string) => {
              if (this.currentConversation) {
                const lastMessage = this.currentConversation.messages[this.currentConversation.messages.length - 1]
                if (lastMessage && lastMessage.role === MessageRole.ASSISTANT) {
                  console.log('Updating AI message content:', newContent)
                  lastMessage.content = newContent
                }
              }
            }
          )

          if (response.code === 200 && response.data) {
            // 更新最后一条消息的内容和ID
            const lastMessage = this.currentConversation.messages[this.currentConversation.messages.length - 1]
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
          if (this.currentConversation?.messages) {
            this.currentConversation.messages = this.currentConversation.messages.filter(
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
              return await this.sendMessage(content)
            } catch (refreshError) {
              // 刷新失败，清除认证状态并跳转到登录页
              await authStore.logout()
              ElMessage.error('登录已过期，请重新登录')
              throw new Error('认证已过期，请重新登录')
            }
          }

          this.error = err.message || '发送消息失败'
          ElMessage.error(this.error)
          throw err
        }
      } finally {
        this.loading = false
      }
    },

    // 加载更多消息
    async loadMoreMessages() {
      if (!this.currentConversation || this.loadingMore || !this.currentConversation.hasMore) {
        return
      }

      try {
        this.loadingMore = true
        this.error = null
        const response = await chatApi.getConversationMessages(
          this.currentConversation.id,
          {
            cursor: this.currentConversation.nextCursor,
            limit: 20
          }
        )
        
        if (response.code === 200) {
          const { messages, hasMore, nextCursor } = response.data
          
          // 更新会话消息
          if (this.currentConversation) {
            this.currentConversation.messages.push(...messages)
            this.currentConversation.hasMore = hasMore
            this.currentConversation.nextCursor = nextCursor
          }
        } else {
          throw new Error(response.message || '加载更多消息失败')
        }
      } catch (err: any) {
        this.error = err.toString()
        ElMessage.error(this.error)
        throw this.error
      } finally {
        this.loadingMore = false
      }
    },

    // 重命名会话
    async renameConversation(id: number, title: string) {
      try {
        this.loading = true
        this.error = null
        const response = await chatApi.renameConversation(id, title)
        if (response.code === 200) {
          // 更新会话列表中的标题
          const conversation = this.conversations.find(c => c.id === id)
          if (conversation) {
            conversation.title = title
          }
        } else {
          throw new Error(response.message || '重命名会话失败')
        }
      } catch (err: any) {
        this.error = err.toString()
        ElMessage.error(this.error)
        throw this.error
      } finally {
        this.loading = false
      }
    },

    // 删除会话
    async deleteConversation(id: number) {
      try {
        this.loading = true
        this.error = null
        const response = await chatApi.deleteConversation(id)
        if (response.code === 200) {
          // 从会话列表中移除
          const index = this.conversations.findIndex(c => c.id === id)
          if (index !== -1) {
            this.conversations.splice(index, 1)
          }

          // 如果删除的是当前会话，切换到第一个会话
          if (this.currentConversationId === id) {
            this.currentConversationId = this.conversations[0]?.id || null
          }
        } else {
          throw new Error(response.message || '删除会话失败')
        }
      } catch (err: any) {
        this.error = err.toString()
        ElMessage.error(this.error)
        throw this.error
      } finally {
        this.loading = false
      }
    },

    // 清除store状态
    clearStore() {
      this.conversations = []
      this.currentConversationId = null
      localStorage.removeItem('conversations')
      localStorage.removeItem('currentConversationId')
    },

    // 添加消息到当前会话
    addMessageToCurrentConversation(message: any) {
      if (this.currentConversation && this.currentConversation.messages) {
        this.currentConversation.messages.push(message);
      }
    },

    // 设置当前会话
    setCurrentConversation(id: number) {
      this.currentConversationId = id;
      const conversation = this.conversations.find(c => c.id === id);
      if (conversation) {
        this.currentConversation = conversation;
      }
    },

    // 加载更多消息，带会话 ID 参数
    async loadMoreMessages(conversationId: number) {
      if (!this.currentConversation || !this.currentConversation.nextCursor) {
        return;
      }

      this.loadingMore = true;
      try {
        // 这里调用 API 加载更多消息
        // 示例代码，根据实际 API 调整
        const response = await chatApi.getMessages(
          conversationId,
          this.currentConversation.nextCursor,
          20
        );

        if (response.data && response.data.data) {
          // 添加新消息到当前会话
          this.currentConversation.messages = [
            ...response.data.data.messages,
            ...this.currentConversation.messages
          ];
          this.currentConversation.hasMore = response.data.data.hasMore;
          this.currentConversation.nextCursor = response.data.data.nextCursor;
        }
      } catch (error) {
        console.error('加载更多消息失败:', error);
      } finally {
        this.loadingMore = false;
      }
    },

    // 初始化store
    onMounted() {
      this.initializeFromStorage()
    },

    // 初始化 Store 时设置网络状态监听
    initNetworkMonitoring() {
      // 监听网络状态变化
      watch(() => networkStatus.isOnline.value, async (isOnline) => {
        if (isOnline && this.isOfflineMode) {
          // 从离线切换到在线
          await this.checkNetworkAndSetMode()
        } else if (!isOnline && !this.isOfflineMode) {
          // 从在线切换到离线
          await this.checkNetworkAndSetMode()
        }
      })
    },
    
    // 在 Chat.vue 的 onMounted 中调用此方法
    async initialize() {
      // 初始化网络监听
      this.initNetworkMonitoring()
      
      // 从本地存储加载
      this.initializeFromStorage()
      
      // 检查网络并设置模式
      const isOnline = await this.checkNetworkAndSetMode()
      
      // 如果在线，加载远程会话
      if (isOnline) {
        try {
          await this.loadConversations()
        } catch (error) {
          console.error('加载远程会话失败，使用本地会话', error)
        }
      }
    },

    // 加载离线会话
    async loadOfflineConversations() {
      try {
        const storedConversations = offlineStorage.loadConversations()
        if (storedConversations && storedConversations.length > 0) {
          this.conversations = storedConversations
          
          // 如果没有选中会话但有离线会话，选择第一个
          if (!this.currentConversationId && this.conversations.length > 0) {
            this.currentConversationId = this.conversations[0].id
          }
        }
        return this.conversations
      } catch (error) {
        console.error('加载离线会话失败:', error)
        return []
      }
    },
    
    // 获取离线消息
    async getOfflineMessages(conversationId: number | string): Promise<any[]> {
      try {
        // 查找选中的会话
        const conversation = this.conversations.find(c => c.id === conversationId)
        if (!conversation) return []
        
        // 返回会话中的消息
        return conversation.messages || []
      } catch (error) {
        console.error('获取离线消息失败:', error)
        return []
      }
    },
    
    // 添加离线消息
    async addOfflineMessage(conversationId: number | string, message: any) {
      try {
        // 查找选中的会话
        const conversation = this.conversations.find(c => c.id === conversationId)
        if (!conversation) return false
        
        // 如果会话没有消息数组，创建一个
        if (!conversation.messages) {
          conversation.messages = []
        }
        
        // 添加消息
        conversation.messages.push(message)
        
        // 保存到本地存储
        offlineStorage.saveConversations(this.conversations)
        
        return true
      } catch (error) {
        console.error('添加离线消息失败:', error)
        return false
      }
    },
    
    // 添加消息（通用方法，可以用于在线或离线模式）
    async addMessage(conversationId: number | string, message: any) {
      try {
        // 如果处于离线模式，使用离线添加方法
        if (this.isOfflineMode) {
          return await this.addOfflineMessage(conversationId, message)
        }
        
        // 查找选中的会话
        const conversation = this.conversations.find(c => c.id === conversationId)
        if (!conversation) return false
        
        // 如果会话没有消息数组，创建一个
        if (!conversation.messages) {
          conversation.messages = []
        }
        
        // 添加消息
        conversation.messages.push(message)
        
        return true
      } catch (error) {
        console.error('添加消息失败:', error)
        return false
      }
    }
  }
}) 