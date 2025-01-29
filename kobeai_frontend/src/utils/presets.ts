// 预设回答
const presetResponses = {
  greetings: [
    '你好呀！我是你的AI助手KobeAI，很开心能和你聊天 😊',
    '嗨！今天有什么我可以帮你的吗？',
    '你好啊！希望今天能帮到你 ✨'
  ],
  identity: [
    '我是KobeAI，你的AI小助手，随时都在这里陪你聊天 😊',
    '叫我KobeAI就好啦，很高兴认识你！',
    '我是KobeAI，你的专属AI助手，让我们开始愉快的对话吧 ✨'
  ],
  thanks: [
    '不客气哦，能帮到你我很开心 😊',
    '这是我应该做的啦，和你聊天很愉快！',
    '别客气，下次还有问题随时问我哦 ✨'
  ],
  greeting_back: [
    '你也好呀！今天过得怎么样？',
    '嗨，见到你真开心！有什么我可以帮你的吗？',
    '你好啊！希望你今天心情愉快 ✨'
  ]
}

// 检查是否是预设问题
export function checkPresetQuestion(message: string): string {
  message = message.toLowerCase().trim()
  
  // 身份相关问题
  if (message.includes('你是谁') || 
      message.includes('你叫什么') || 
      message.includes('你的名字') ||
      message.includes('你是')) {
    return getRandomResponse('identity')
  }
  
  // 问候语
  if (message === '你好' || message === 'hi' || message === 'hello') {
    return getRandomResponse('greeting_back')
  }
  
  if (message.includes('在吗') || 
      message.includes('在不在') ||
      message.includes('你好啊') ||
      message.includes('你好呀')) {
    return getRandomResponse('greetings')
  }
  
  // 感谢
  if (message.includes('谢谢') || 
      message.includes('感谢') ||
      message.includes('thank')) {
    return getRandomResponse('thanks')
  }
  
  return message
}

// 获取随机回复
export function getRandomResponse(type: keyof typeof presetResponses): string {
  const responses = presetResponses[type]
  return responses[Math.floor(Math.random() * responses.length)]
} 