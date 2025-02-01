import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from './types'
import { api, getAuthToken, shouldRefreshToken, clearAuth } from './auth'
import type { MessageRole } from '../types/chat'

// 聊天消息接口
export interface ChatMessage {
  id: number
  content: string
  role: MessageRole
  createdAt: string
}

export interface Conversation {
  id: number
  title?: string
  messages: ChatMessage[]
  createdAt: string
  updatedAt: string
  hasMore?: boolean
  nextCursor?: string
}

// 文件消息接口
export interface FileMessage extends ChatMessage {
  file: {
    id: string
    name: string
    size: number
    type: string
    url: string
    thumbnailUrl?: string
  }
}

// 创建axios实例
const apiInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || window.location.origin + '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  validateStatus: function (status) {
    return status >= 200 && status < 500
  }
})

// 请求拦截器
apiInstance.interceptors.request.use(
  async (config) => {
    const { token } = getAuthToken()
    if (!token || typeof token !== 'string') {
      return config
    }

    config.headers = config.headers || {}
    config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiInstance.interceptors.response.use(
  (response) => {
    if (response.data.code === 200) {
      return response.data
    }
    return Promise.reject(new Error(response.data.message || '请求失败'))
  },
  async (error) => {
    if (error.response?.status === 401) {
      clearAuth()
      window.location.href = '/auth/login'
      return Promise.reject(new Error('认证已过期'))
    }
    return Promise.reject(error)
  }
)

// 检查是否是身份询问
function isIdentityQuestion(message: string): boolean {
  const identityKeywords = [
    '你是谁',
    '你叫什么',
    '你的名字',
    '你的身份',
    '你是什么',
    '介绍一下你自己',
    '自我介绍',
    'who are you',
    'what is your name',
    'what are you'
  ]
  return identityKeywords.some(keyword => message.toLowerCase().includes(keyword.toLowerCase()))
}

// 获取身份回复内容
function getIdentityResponse(): string {
  return '您好！我是KobeAI，一个智能助手。我可以帮助您解答问题、完成任务，让我们开始对话吧！'
}

// 统一的API实现
export const chatApi = {
  // 发送消息
  async sendMessage(
    conversationId: number, 
    message: string,
    onUpdate?: (content: string) => void
  ): Promise<ApiResponse<ChatMessage>> {
    let { token } = getAuthToken()
    if (!token || typeof token !== 'string') {
      throw new Error('未登录或认证已过期')
    }

    // 检查是否是身份询问
    if (isIdentityQuestion(message)) {
      const response = getIdentityResponse()
      const identityResponse = {
        code: 200,
        message: 'success',
        data: {
          id: Date.now(),
          content: response,
          role: 'ASSISTANT' as MessageRole,
          createdAt: new Date().toISOString()
        }
      }
      
      if (onUpdate) {
        onUpdate(identityResponse.data.content)
      }
      
      return Promise.resolve(identityResponse)
    }

    // 处理普通消息
    let retryCount = 0
    const maxRetries = 3
    const retryDelay = 1000 // 1秒
    const maxKeepAliveCount = 3 // 最多允许连续收到3次keep-alive

    async function attemptSendMessage(): Promise<ApiResponse<ChatMessage>> {
      try {
        // 检查token是否需要刷新
        if (shouldRefreshToken()) {
          await api.refreshToken()
          const { token: newToken } = getAuthToken()
          if (!newToken || typeof newToken !== 'string') {
            throw new Error('刷新token失败')
          }
          token = newToken
        }

        const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
        
        // 设置请求超时
        const controller = new AbortController()
        const timeoutId = setTimeout(() => controller.abort(), 10000) // 10秒超时

        try {
          const response = await fetch(`${import.meta.env.VITE_API_URL || window.location.origin + '/api'}/chat/completions`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': authToken,
              'Accept': 'text/event-stream',
              'Cache-Control': 'no-cache',
              'Connection': 'keep-alive'
            },
            body: JSON.stringify({
              message,
              conversationId
            }),
            signal: controller.signal
          })

          clearTimeout(timeoutId)

          if (!response.ok) {
            if (response.status === 401) {
              clearAuth()
              window.location.href = '/auth/login'
              throw new Error('认证已过期，请重新登录')
            }
            throw new Error(`发送消息失败: ${response.status}`)
          }

          return new Promise((resolve, reject) => {
            let fullResponse = ''
            let lastUpdateTime = Date.now()
            let keepAliveCount = 0
            let hasReceivedData = false
            const decoder = new TextDecoder()
            const reader = response.body!.getReader()
            let buffer = ''

            // 设置心跳检查
            const heartbeatInterval = setInterval(() => {
              const now = Date.now()
              if (now - lastUpdateTime > 5000) { // 5秒没有更新
                if (!hasReceivedData && keepAliveCount >= maxKeepAliveCount) {
                  clearInterval(heartbeatInterval)
                  reader.cancel()
                  reject(new Error('DeepSeek API 响应延迟，正在重试...'))
                }
              }
            }, 1000)

            async function pump(): Promise<void> {
              try {
                let keepAliveStartTime = Date.now()
                while (true) {
                  const { value, done } = await reader.read()
                  
                  if (done) {
                    console.log('Stream complete')
                    break
                  }

                  lastUpdateTime = Date.now()
                  const chunk = decoder.decode(value, { stream: true })
                  console.log('Received chunk:', chunk)
                  
                  if (chunk.includes('keep-alive')) {
                    keepAliveCount++
                    // 如果持续15秒只收到keep-alive，主动重试
                    if (!hasReceivedData && (Date.now() - keepAliveStartTime > 15000)) {
                      throw new Error('DeepSeek API 调度延迟，正在重试...')
                    }
                    continue
                  }

                  if (chunk.trim() === '') {
                    continue
                  }

                  // 重置keep-alive计数器，因为收到了实际数据
                  keepAliveCount = 0
                  hasReceivedData = true
                  buffer += chunk

                  // 尝试解析响应
                  try {
                    const lines = buffer.split('\n')
                    buffer = lines.pop() || ''

                    for (const line of lines) {
                      if (line.startsWith('data:')) {
                        const data = line.slice(5).trim()
                        if (data === '[DONE]') {
                          break
                        }

                        try {
                          // 处理嵌套的数据结构
                          const parsedData = JSON.parse(data)
                          if (Array.isArray(parsedData)) {
                            for (const item of parsedData) {
                              if (item.data && typeof item.data === 'object') {
                                if (item.data.choices && item.data.choices[0]?.delta?.content) {
                                  const content = item.data.choices[0].delta.content
                                  fullResponse += content
                                  onUpdate?.(fullResponse)
                                  hasReceivedData = true
                                  keepAliveCount = 0
                                }
                              }
                            }
                          } else if (parsedData.data && parsedData.data.choices) {
                            const content = parsedData.data.choices[0]?.delta?.content
                            if (content) {
                              fullResponse += content
                              onUpdate?.(fullResponse)
                              hasReceivedData = true
                              keepAliveCount = 0
                            }
                          }
                        } catch (e) {
                          console.warn('Failed to parse SSE data:', e)
                        }
                      }
                    }
                  } catch (error) {
                    console.error('Failed to parse response:', error)
                  }
                }

                clearInterval(heartbeatInterval)
                
                if (!hasReceivedData) {
                  throw new Error('服务器未返回有效数据，正在重试...')
                }

                if (!fullResponse.trim()) {
                  throw new Error('未收到有效响应，正在重试...')
                }

                // 检查响应内容是否为连接建立消息
                if (fullResponse === '连接已建立') {
                  throw new Error('等待AI响应，正在重试...')
                }

                resolve({
                  code: 200,
                  message: 'success',
                  data: {
                    id: Date.now(),
                    content: fullResponse,
                    role: 'ASSISTANT',
                    createdAt: new Date().toISOString()
                  }
                })
              } catch (error) {
                clearInterval(heartbeatInterval)
                reject(error)
              } finally {
                reader.cancel()
              }
            }

            pump().catch(reject)
          })
        } catch (error: any) {
          if (error.name === 'AbortError') {
            throw new Error('请求超时，正在重试...')
          }
          throw error
        } finally {
          clearTimeout(timeoutId)
        }
      } catch (error: any) {
        if (retryCount < maxRetries) {
          retryCount++
          console.log(`重试第 ${retryCount} 次...`)
          await new Promise(resolve => setTimeout(resolve, retryDelay * retryCount))
          return attemptSendMessage()
        }
        throw new Error(error.message || '发送消息失败，请重试')
      }
    }

    return attemptSendMessage()
  },

  // 获取当前会话
  async getCurrentConversation(): Promise<ApiResponse<Conversation>> {
    const response = await apiInstance.get('/chat/conversations/current')
    return response
  },

  // 删除当前会话
  async deleteCurrentConversation(): Promise<ApiResponse<void>> {
    const response = await apiInstance.delete('/chat/conversations/current')
    return response
  },

  // 获取所有会话
  async getConversations(): Promise<ApiResponse<Conversation[]>> {
    const response = await apiInstance.get('/chat/conversations')
    return response
  },

  // 创建新会话
  async createConversation(): Promise<ApiResponse<Conversation>> {
    const response = await apiInstance.post('/chat/conversations', {
      title: '新对话'
    })
    return response
  },

  // 删除会话
  async deleteConversation(id: number): Promise<ApiResponse<void>> {
    const response = await apiInstance.delete(`/chat/conversations/${id}`)
    return response
  },

  // 获取会话详情
  async getConversationById(id: number): Promise<ApiResponse<Conversation>> {
    const response = await apiInstance.get(`/chat/conversations/${id}`)
    return response
  },

  // 重命名会话
  async renameConversation(id: number, title: string): Promise<ApiResponse<void>> {
    const response = await apiInstance.put(`/chat/conversations/${id}/title?title=${encodeURIComponent(title)}`)
    return response
  },

  // 获取会话消息
  async getConversationMessages(
    id: number,
    params: { cursor?: string; limit?: number }
  ): Promise<ApiResponse<{ messages: ChatMessage[]; hasMore: boolean; nextCursor?: string }>> {
    const response = await apiInstance.get(`/chat/conversations/${id}/messages`, { params })
    return response
  },

  // 上传文件
  async uploadFile(
    conversationId: number,
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<AxiosResponse<ApiResponse<FileMessage>>> {
    const formData = new FormData()
    formData.append('file', file)

    return apiInstance.post(`/chat/conversation/${conversationId}/files`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
  },

  // 下载文件
  async downloadFile(fileId: string): Promise<Blob> {
    const response = await apiInstance.get(`/chat/files/${fileId}`, {
      responseType: 'blob'
    })
    return response.data
  }
}

export type { ChatMessage, Conversation, FileMessage }
export default apiInstance 