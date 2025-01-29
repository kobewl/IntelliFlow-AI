import type { Conversation } from '../types/chat'

export function exportToMarkdown(conversation: Conversation): string {
  const lines: string[] = []
  
  // 添加标题
  lines.push(`# ${conversation.title || '对话记录'}`)
  lines.push(`\n创建时间：${new Date(conversation.createdAt).toLocaleString()}\n`)
  
  // 添加消息记录
  conversation.messages.forEach((message) => {
    const role = message.role === 'assistant' ? 'AI' : '用户'
    lines.push(`\n### ${role} (${new Date(message.createdAt).toLocaleString()})\n`)
    lines.push(message.content)
  })
  
  return lines.join('\n')
} 