import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from './types'
import { getAuthToken, clearAuth } from './auth'

// ---- 类型定义 ----

export interface ChatMessage {
  id: number
  content: string
  role: 'USER' | 'ASSISTANT'
  createdAt: string
  updatedAt?: string
}

export interface ToolCall {
  name: string
  input?: string
  output?: string
}

export interface AgentEvent {
  type: 'reasoning' | 'tool_result' | 'agent_result' | 'error'
  content: string
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

// ---- Axios 实例 ----

const apiInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || window.location.origin + '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
  validateStatus: (status) => status >= 200 && status < 500
})

apiInstance.interceptors.request.use((config) => {
  const { token } = getAuthToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  }
  return config
})

apiInstance.interceptors.response.use(
  (response) => {
    if (response.data.code === 200) return response.data
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

// ---- SSE 流式调用 (AgentScope) ----

export function streamAgentChat(
  message: string,
  callbacks: {
    onReasoning?: (content: string) => void
    onToolResult?: (toolCall: ToolCall) => void
    onAgentResult?: (content: string) => void
    onError?: (error: string) => void
    onDone?: (fullContent: string) => void
  },
  options?: {
    model?: string
    sessionId?: string
    conversationId?: number
  }
): { abort: () => void } {
  const { token } = getAuthToken()
  if (!token) {
    callbacks.onError?.('未登录或认证已过期')
    return { abort: () => {} }
  }

  const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  const params = new URLSearchParams({ message })
  if (options?.model) params.set('model', options.model)

  const baseUrl = import.meta.env.VITE_API_URL || window.location.origin + '/api'
  const url = `${baseUrl}/agent/chat?${params.toString()}`

  const controller = new AbortController()

  fetch(url, {
    method: 'GET',
    headers: {
      Authorization: authToken,
      Accept: 'text/event-stream',
      'Cache-Control': 'no-cache'
    },
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok) {
        if (response.status === 401) {
          callbacks.onError?.('认证已过期，请重新登录')
          return
        }
        callbacks.onError?.(`请求失败: ${response.status}`)
        return
      }

      const reader = response.body!.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''
      let fullResponse = ''

      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (!data) continue

            switch (currentEvent) {
              case 'reasoning':
                callbacks.onReasoning?.(data)
                break
              case 'tool_result':
                try {
                  const parsed = JSON.parse(data)
                  callbacks.onToolResult?.({
                    name: parsed.name || parsed.tool || 'unknown',
                    input: parsed.input || parsed.arguments,
                    output: parsed.output || parsed.result || data
                  })
                } catch {
                  callbacks.onToolResult?.({ name: 'tool', output: data })
                }
                break
              case 'agent_result':
                fullResponse += data
                callbacks.onAgentResult?.(fullResponse)
                break
              case 'error':
                callbacks.onError?.(data)
                break
              default:
                // 没有 event 类型时当作 agent_result 处理
                fullResponse += data
                callbacks.onAgentResult?.(fullResponse)
            }
          }
        }
      }

      callbacks.onDone?.(fullResponse)
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        callbacks.onError?.(err.message || '网络错误')
      }
    })

  return {
    abort: () => controller.abort()
  }
}

// ---- Agent API ----

export interface ToolInfo {
  name: string
  description: string
}

export interface AgentSessionsInfo {
  activeSessions: number
  currentSession?: {
    sessionId: string
    messageCount: number
    createdAt: string
  }
}

export const agentApi = {
  async getTools(): Promise<{ code: number; data: ToolInfo[]; total: number }> {
    return apiInstance.get('/agent/tools')
  },

  async getSessions(detail?: boolean, sessionId?: string): Promise<{ code: number } & AgentSessionsInfo> {
    const params: Record<string, string> = {}
    if (detail) params.detail = 'true'
    return apiInstance.get('/agent/sessions', { params, headers: sessionId ? { 'X-Session-Id': sessionId } : {} })
  },

  async closeSession(sessionId: string): Promise<void> {
    return apiInstance.delete(`/agent/session/${sessionId}`)
  }
}

// ---- REST API ----

export const chatApi = {
  async getConversations(): Promise<ApiResponse<Conversation[]>> {
    return apiInstance.get('/chat/conversations')
  },

  async createConversation(): Promise<ApiResponse<Conversation>> {
    return apiInstance.post('/chat/conversations', { title: '新对话' })
  },

  async deleteConversation(id: number): Promise<ApiResponse<void>> {
    return apiInstance.delete(`/chat/conversations/${id}`)
  },

  async getConversationById(id: number): Promise<ApiResponse<Conversation>> {
    return apiInstance.get(`/chat/conversations/${id}`)
  },

  async renameConversation(id: number, title: string): Promise<ApiResponse<void>> {
    return apiInstance.put(`/chat/conversations/${id}/title?title=${encodeURIComponent(title)}`)
  },

  async getConversationMessages(
    id: number,
    params: { cursor?: string; limit?: number }
  ): Promise<ApiResponse<{ messages: ChatMessage[]; hasMore: boolean; nextCursor?: string }>> {
    return apiInstance.get(`/chat/conversations/${id}/messages`, { params })
  },

  async uploadFile(
    conversationId: number,
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<AxiosResponse<ApiResponse<FileMessage>>> {
    const formData = new FormData()
    formData.append('file', file)
    return apiInstance.post(`/chat/conversation/${conversationId}/files`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
        }
      }
    })
  },

  async downloadFile(fileId: string): Promise<Blob> {
    const response = await apiInstance.get(`/chat/files/${fileId}`, { responseType: 'blob' })
    return response.data
  }
}

export default apiInstance
