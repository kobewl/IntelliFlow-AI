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
    return status >= 200 && status < 500 // 只有状态码在此范围内的响应会被视为成功
  }
})

// 请求拦截器
apiInstance.interceptors.request.use(
  async (config) => {
    const { token } = getAuthToken()
    if (!token) {
      return config
    }

    // 检查token是否需要刷新
    if (shouldRefreshToken()) {
      try {
        await api.refreshToken()
        const { token: newToken } = getAuthToken()
        if (newToken) {
          config.headers = config.headers || {}
          config.headers.Authorization = newToken
        }
      } catch (error) {
        console.error('Failed to refresh token:', error)
      }
    } else {
      config.headers = config.headers || {}
      config.headers.Authorization = token
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiInstance.interceptors.response.use(
  (response) => {
    // 检查响应状态
    if (response.data.code === 200) {
      return response.data
    }
    // 如果不是成功状态，抛出错误
    return Promise.reject(new Error(response.data.message || '请求失败'))
  },
  async (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      try {
        // 尝试刷新 token
        await api.refreshToken()
        // 重试原始请求
        const originalRequest = error.config
        return apiInstance(originalRequest)
      } catch (refreshError) {
        // 刷新失败，清除认证状态并重定向到登录页
        clearAuth()
        window.location.href = '/auth/login'
        return Promise.reject(new Error('认证已过期，请重新登录'))
      }
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '网络错误，请稍后重试'))
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

// 发送消息
export async function sendMessage(
  conversationId: number, 
  message: string,
  onUpdate?: (content: string) => void
): Promise<ApiResponse<ChatMessage>> {
  const { token } = getAuthToken()
  if (!token) {
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
  try {
    // 检查token是否需要刷新
    if (shouldRefreshToken()) {
      await api.refreshToken()
      const { token: newToken } = getAuthToken()
      if (!newToken) {
        throw new Error('刷新token失败')
      }
    }

    const response = await fetch(`${import.meta.env.VITE_API_URL || window.location.origin + '/api'}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token,
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive'
      },
      body: JSON.stringify({
        message,
        conversationId
      })
    })

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
      const decoder = new TextDecoder()
      const reader = response.body!.getReader()
      let buffer = ''

      async function pump(): Promise<void> {
        try {
          while (true) {
            const { value, done } = await reader.read()
            
            if (done) {
              console.log('Stream complete')
              break
            }

            const chunk = decoder.decode(value, { stream: true })
            console.log('Received chunk:', chunk)
            buffer += chunk

            // 尝试找到完整的JSON数组
            let startIndex = 0
            while (true) {
              const leftBracket = buffer.indexOf('[', startIndex)
              if (leftBracket === -1) break

              let rightBracket = -1
              let bracketCount = 1
              for (let i = leftBracket + 1; i < buffer.length; i++) {
                if (buffer[i] === '[') bracketCount++
                if (buffer[i] === ']') bracketCount--
                if (bracketCount === 0) {
                  rightBracket = i
                  break
                }
              }

              if (rightBracket === -1) break

              try {
                const jsonStr = buffer.substring(leftBracket, rightBracket + 1)
                const jsonArray = JSON.parse(jsonStr)
                
                // 处理JSON数组中的每个项
                for (const item of jsonArray) {
                  if (item.data && typeof item.data === 'object' && item.data.choices) {
                    const delta = item.data.choices[0]?.delta?.content || ''
                    if (delta) {
                      fullResponse += delta
                      onUpdate?.(fullResponse)
                    }
                  } else if (item.data && typeof item.data === 'string' && !item.data.startsWith('event:')) {
                    // 处理最终的完整响应
                    if (item.data.includes('你好') || item.data.includes('很高兴')) {
                      fullResponse = item.data
                      onUpdate?.(fullResponse)
                    }
                  }
                }

                // 移除已处理的部分
                buffer = buffer.substring(rightBracket + 1)
                startIndex = 0
              } catch (error) {
                console.error('Failed to parse JSON array:', error)
                startIndex = leftBracket + 1
              }
            }
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
          console.error('处理响应流时出错:', error)
          reject(error)
        } finally {
          reader.cancel()
        }
      }

      const timeout = setTimeout(() => {
        reader.cancel()
        reject(new Error('接收消息超时'))
      }, 60000)

      pump().catch(reject).finally(() => {
        clearTimeout(timeout)
      })
    })
  } catch (error: any) {
    console.error('发送消息失败:', error)
    throw new Error(error.message || '发送消息失败')
  }
}

// 统一的API实现
export const chatApi = {
  // 发送消息
  async sendMessage(
    conversationId: number, 
    message: string,
    onUpdate?: (content: string) => void
  ): Promise<ApiResponse<ChatMessage>> {
    const token = getAuthToken()
    if (!token) {
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

    try {
      // 发送消息并获取流式响应
      const response = await fetch(`${import.meta.env.VITE_API_URL || window.location.origin + '/api'}/chat/completions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`,
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache',
          'Connection': 'keep-alive'
        },
        body: JSON.stringify({
          message,
          conversationId
        })
      })

      if (!response.ok) {
        if (response.status === 401) {
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          window.location.href = '/auth/login'
          throw new Error('认证已过期，请重新登录')
        }
        throw new Error(`发送消息失败: ${response.status}`)
      }

      return new Promise((resolve, reject) => {
        let fullResponse = ''
        const decoder = new TextDecoder()
        const reader = response.body!.getReader()
        let buffer = ''

        async function pump(): Promise<void> {
          try {
            while (true) {
              const { value, done } = await reader.read()
              
              if (done) {
                console.log('Stream complete')
                break
              }

              const chunk = decoder.decode(value, { stream: true })
              console.log('Received chunk:', chunk)
              buffer += chunk

              // 尝试找到完整的JSON数组
              let startIndex = 0
              while (true) {
                const leftBracket = buffer.indexOf('[', startIndex)
                if (leftBracket === -1) break

                let rightBracket = -1
                let bracketCount = 1
                for (let i = leftBracket + 1; i < buffer.length; i++) {
                  if (buffer[i] === '[') bracketCount++
                  if (buffer[i] === ']') bracketCount--
                  if (bracketCount === 0) {
                    rightBracket = i
                    break
                  }
                }

                if (rightBracket === -1) break

                try {
                  const jsonStr = buffer.substring(leftBracket, rightBracket + 1)
                  const jsonArray = JSON.parse(jsonStr)
                  
                  // 处理JSON数组中的每个项
                  for (const item of jsonArray) {
                    if (item.data && typeof item.data === 'object' && item.data.choices) {
                      const delta = item.data.choices[0]?.delta?.content || ''
                      if (delta) {
                        fullResponse += delta
                        onUpdate?.(fullResponse)
                      }
                    } else if (item.data && typeof item.data === 'string' && !item.data.startsWith('event:')) {
                      // 处理最终的完整响应
                      if (item.data.includes('你好') || item.data.includes('很高兴')) {
                        fullResponse = item.data
                        onUpdate?.(fullResponse)
                      }
                    }
                  }

                  // 移除已处理的部分
                  buffer = buffer.substring(rightBracket + 1)
                  startIndex = 0
                } catch (error) {
                  console.error('Failed to parse JSON array:', error)
                  startIndex = leftBracket + 1
                }
              }
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
            console.error('处理响应流时出错:', error)
            reject(error)
          } finally {
            reader.cancel()
          }
        }

        const timeout = setTimeout(() => {
          reader.cancel()
          reject(new Error('接收消息超时'))
        }, 60000)

        pump().catch(reject).finally(() => {
          clearTimeout(timeout)
        })
      })
    } catch (error: any) {
      console.error('发送消息失败:', error)
      throw new Error(error.message || '发送消息失败')
    }
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

// 删除重复的接口和实现
export type { ChatMessage, Conversation, FileMessage } 

export default apiInstance 