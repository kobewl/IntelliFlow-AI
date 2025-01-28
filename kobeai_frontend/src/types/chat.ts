export enum MessageRole {
  USER = 'USER',
  ASSISTANT = 'ASSISTANT'
}

export interface ChatMessage {
  id: number;
  content: string;
  role: MessageRole;
  createdAt: string;
}

export interface Conversation {
  id: number;
  title: string;
  messages: ChatMessage[];
  hasMore?: boolean;
  nextCursor?: string;
  createdAt: string;
} 