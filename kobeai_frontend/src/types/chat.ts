export enum MessageRole {
  USER = 'user',
  ASSISTANT = 'assistant',
  SYSTEM = 'system'
}

export interface Message {
  id: number
  role: MessageRole
  content: string
  createdAt: string
}

export interface Conversation {
  id: number
  title: string
  messages: Message[]
  category?: string
  createdAt: string
  updatedAt: string
  hasMore?: boolean
  nextCursor?: string
}

export interface ChatState {
  conversations: Conversation[]
  currentConversationId: number | null
  loading: boolean
  loadingMore: boolean
} 