/**
 * 离线数据存储工具
 * 实现在网络断开时的数据持久化和恢复
 */

import { Conversation, Message } from '../types/chat'

class OfflineStorage {
  private readonly STORAGE_KEY = 'kobeai_offline_data'
  private readonly PENDING_MESSAGES_KEY = 'kobeai_pending_messages'
  private readonly VERSION = '1.0'

  /**
   * 保存对话到本地存储
   * @param conversations 要保存的对话列表
   */
  saveConversations(conversations: Conversation[]): void {
    try {
      const data = {
        version: this.VERSION,
        timestamp: new Date().toISOString(),
        conversations: conversations
      }
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(data))
      console.log('对话数据已保存到本地存储')
    } catch (error) {
      console.error('保存对话到本地存储失败:', error)
    }
  }

  /**
   * 从本地存储中加载对话
   * @returns 存储的对话列表或空数组
   */
  loadConversations(): Conversation[] {
    try {
      const storedData = localStorage.getItem(this.STORAGE_KEY)
      if (!storedData) return []

      const data = JSON.parse(storedData)
      if (data.version !== this.VERSION) {
        console.warn('存储的数据版本不匹配，使用空数据')
        return []
      }

      return data.conversations || []
    } catch (error) {
      console.error('从本地存储加载对话失败:', error)
      return []
    }
  }

  /**
   * 保存待发送的消息到队列中
   * @param conversationId 会话ID
   * @param content 消息内容
   */
  savePendingMessage(conversationId: number, content: string): void {
    try {
      const pendingMessages = this.getPendingMessages()
      pendingMessages.push({
        conversationId,
        content,
        timestamp: new Date().toISOString()
      })
      
      localStorage.setItem(this.PENDING_MESSAGES_KEY, JSON.stringify(pendingMessages))
    } catch (error) {
      console.error('保存待发送消息失败:', error)
    }
  }

  /**
   * 获取待发送的消息队列
   */
  getPendingMessages(): Array<{conversationId: number, content: string, timestamp: string}> {
    try {
      const data = localStorage.getItem(this.PENDING_MESSAGES_KEY)
      return data ? JSON.parse(data) : []
    } catch (error) {
      console.error('获取待发送消息失败:', error)
      return []
    }
  }

  /**
   * 清除已发送的待发消息
   * @param index 要清除的消息索引
   */
  removePendingMessage(index: number): void {
    try {
      const pendingMessages = this.getPendingMessages()
      pendingMessages.splice(index, 1)
      localStorage.setItem(this.PENDING_MESSAGES_KEY, JSON.stringify(pendingMessages))
    } catch (error) {
      console.error('移除待发送消息失败:', error)
    }
  }

  /**
   * 清除所有离线数据
   */
  clearAllOfflineData(): void {
    localStorage.removeItem(this.STORAGE_KEY)
    localStorage.removeItem(this.PENDING_MESSAGES_KEY)
  }

  /**
   * 检查是否有离线数据
   */
  hasOfflineData(): boolean {
    return !!localStorage.getItem(this.STORAGE_KEY)
  }

  /**
   * 检查是否有待发送的消息
   */
  hasPendingMessages(): boolean {
    const messages = this.getPendingMessages()
    return messages.length > 0
  }
}

// 导出单例
export const offlineStorage = new OfflineStorage() 