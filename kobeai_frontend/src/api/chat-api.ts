import type { ApiResponse } from './types'
import { request } from './auth'
import type { MessageRole } from '../types/chat'

// 聊天消息接口
export interface Message {
  id: number
  content: string
  role: MessageRole
  createdAt: string
}

export interface ChatRequest {
  messages: Message[]
}

export interface ChatResponse {
  message: Message
  done: boolean
}

// API函数
export const chatApi = {
  // 发送消息
  async sendMessage(messages: Message[]): Promise<ChatResponse> {
    return request.post('/chat/send', { messages })
  },

  // 获取历史消息
  async getHistory(cursor?: string, limit: number = 20): Promise<ApiResponse<Message[]>> {
    return request.get('/chat/history', {
      params: { cursor, limit }
    })
  },

  // 清空历史记录
  async clearHistory(): Promise<ApiResponse<void>> {
    return request.delete('/chat/history')
  }
} 