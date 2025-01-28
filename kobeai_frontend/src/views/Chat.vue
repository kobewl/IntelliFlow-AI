<template>
  <div class="chat-container">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="handleNewChat">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>
      
      <!-- 会话列表 -->
      <ConversationList
        :conversations="conversations"
        :current-id="currentConversationId"
        @select="handleSelectConversation"
        @rename="handleRenameConversation"
        @delete="handleDeleteConversation"
      />
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <div class="chat-header">
        <div class="chat-title">
          <el-icon><ChatRound /></el-icon>
          {{ currentConversation?.title || '新对话' }}
        </div>
        
        <div class="header-actions">
          <el-tooltip content="切换主题">
          <ThemeSwitch />
          </el-tooltip>
          
          <el-tooltip content="清空对话">
            <el-button
              circle
              plain
              @click="handleClearChat"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>

          <UserProfile />
        </div>
      </div>

      <!-- 消息列表区域 -->
      <div class="messages-container">
        <VirtualMessageList
          v-if="currentConversation?.messages"
          :messages="currentConversation.messages"
          :has-more="currentConversation.hasMore || false"
          :loading-more="loadingMore"
          @load-more="handleLoadMore"
        />
        <div v-else class="empty-state">
          <el-icon :size="64" class="empty-icon">
            <ChatRound />
          </el-icon>
          <h2>开始新的对话</h2>
          <p>发送消息开始与AI助手对话</p>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-container">
        <div class="input-wrapper">
          <el-input
            v-model="messageInput"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            :placeholder="loading ? '正在思考中...' : '输入消息，Enter发送，Shift+Enter换行'"
            @keydown.enter.exact.prevent="handleSend"
            @keydown.enter.shift.exact="messageInput += '\n'"
          />
          <div class="action-buttons">
            <el-upload
              ref="fileUploadRef"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleFileSelect"
            >
              <el-button circle plain>
                <el-icon><Paperclip /></el-icon>
              </el-button>
            </el-upload>
            
            <el-button
              type="primary"
              circle
              :loading="loading"
              :disabled="!canSendMessage"
              @click="handleSend"
            >
              <el-icon><Position /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ChatRound, 
  Plus, 
  Delete, 
  Promotion,
  Upload,
  Position,
  DArrowRight,
  User,
  Setting,
  SwitchButton,
  UserFilled,
  Paperclip
} from '@element-plus/icons-vue'
import { useChatStore } from '../store/chat'
import { useAuthStore } from '../store/auth'
import { MessageRole } from '../types/chat'
import ConversationList from '../components/ConversationList.vue'
import VirtualMessageList from '../components/VirtualMessageList.vue'
import ThemeSwitch from '../components/ThemeSwitch.vue'
import FileUpload from '../components/FileUpload.vue'
import FileMessage from '../components/FileMessage.vue'
import UserProfile from '../components/UserProfile.vue'
import { useRouter } from 'vue-router'

const chatStore = useChatStore()
const authStore = useAuthStore()
const { 
  conversations,
  currentConversationId,
  currentConversation,
  loading,
  loadingMore 
} = storeToRefs(chatStore)

const messageInput = ref('')
const fileUploadRef = ref()
const router = useRouter()

// 当前会话标题
const currentTitle = computed(() => {
  if (!currentConversation.value) return 'AI助手'
  return currentConversation.value.title || `新会话 ${currentConversation.value.id}`
})

// 是否可以发送消息
const canSendMessage = computed(() => {
  return messageInput.value.trim().length > 0 && !loading.value
})

// 加载会话列表
onMounted(async () => {
  try {
    await chatStore.loadConversations()
  } catch (error) {
    ElMessage.error('加载会话列表失败')
  }
})

// 添加预设问答
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
function checkPresetQuestion(message: string): string | null {
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
  
  return null
}

// 获取随机回复
function getRandomResponse(type: keyof typeof presetResponses): string {
  const responses = presetResponses[type]
  return responses[Math.floor(Math.random() * responses.length)]
}

// 修改 handleNewChat 函数
async function handleNewChat() {
  try {
    loading.value = true
    await chatStore.createConversation()
    messageInput.value = ''
    
    // 随机选择一个问候语
    const greeting = presetResponses.greetings[Math.floor(Math.random() * presetResponses.greetings.length)]
    
    // 直接添加到消息列表中，无需调用后端
    if (currentConversation.value) {
      currentConversation.value.messages = [{
        id: Date.now(),
        role: MessageRole.ASSISTANT,
        content: greeting,
        createdAt: new Date().toISOString()
      }]
    }
  } catch (error: any) {
    console.error('创建会话失败:', error)
    ElMessage.error(error.message || '创建会话失败，请重试')
  } finally {
    loading.value = false
  }
}

// 选择会话
async function handleSelectConversation(id: number) {
  try {
    await chatStore.switchConversation(id)
  } catch (error) {
    ElMessage.error('切换会话失败')
  }
}

// 加载更多消息
async function handleLoadMore() {
  try {
    await chatStore.loadMoreMessages()
  } catch (error) {
    ElMessage.error('加载更多消息失败')
  }
}

// 重命名会话
async function handleRenameConversation(id: number, title: string) {
  try {
    await chatStore.renameConversation(id, title)
    ElMessage.success('重命名成功')
  } catch (error) {
    ElMessage.error('重命名失败')
  }
}

// 删除会话
async function handleDeleteConversation(id: number) {
  try {
    await chatStore.deleteConversation(id)
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 清空当前会话
async function handleClearChat() {
  try {
    await ElMessageBox.confirm(
      '确定要清空当前会话吗？此操作不可恢复。',
      '清空会话',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (currentConversation.value) {
      currentConversation.value.messages = []
      ElMessage.success('清空成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

// 修改发送消息函数
async function handleSend() {
  if (!canSendMessage.value) return

  const message = messageInput.value.trim()
  messageInput.value = ''

  try {
    // 如果没有当前会话，先创建一个新会话
    if (!currentConversationId.value) {
      await chatStore.createConversation()
    }

    if (!currentConversationId.value) {
      throw new Error('创建会话失败')
    }

    // 确保消息数组已初始化
    if (!currentConversation.value) {
      throw new Error('当前会话不存在')
    }
    
    if (!currentConversation.value.messages) {
      currentConversation.value.messages = []
    }

    // 添加用户消息
    currentConversation.value.messages.push({
      id: Date.now(),
      role: MessageRole.USER,
      content: message,
      createdAt: new Date().toISOString()
    })

    // 检查是否是预设问题
    const presetResponse = checkPresetQuestion(message)
    if (presetResponse) {
      // 如果是预设问题，直接在前端返回答案
      currentConversation.value.messages.push({
        id: Date.now() + 1,
        role: MessageRole.ASSISTANT,
        content: presetResponse,
        createdAt: new Date().toISOString()
      })
    } else {
      // 如果不是预设问题，发送到后端处理
      loading.value = true
      await chatStore.sendMessage(message)
    }
  } catch (error: any) {
    messageInput.value = message // 发送失败时恢复消息内容
    console.error('发送消息失败:', error)
    ElMessage.error(error.message || '发送消息失败，请重试')
  } finally {
    loading.value = false
  }
}

// 处理文件选择
async function handleFileSelect(file: File) {
  ElMessage.warning('文件上传功能暂未实现')
  return

  // if (!currentConversationId.value) {
  //   try {
  //     await chatStore.createConversation()
  //   } catch (error) {
  //     ElMessage.error('创建会话失败')
  //     return
  //   }
  // }

  // try {
  //   fileUploadRef.value?.startUpload(0)
  //   const response = await chatStore.uploadFile(file, (progress) => {
  //     fileUploadRef.value?.updateUpload(progress)
  //   })
  //   fileUploadRef.value?.finishUpload()
  //   ElMessage.success('上传成功')
  // } catch (error) {
  //   fileUploadRef.value?.finishUpload()
  //   ElMessage.error('上传失败')
  // }
}

// 处理用户相关操作
async function handleUserAction(command: string) {
  switch (command) {
    case 'profile':
      // TODO: 实现查看个人信息
      ElMessage.info('个人信息功能开发中')
      break
    case 'settings':
      // TODO: 实现设置功能
      ElMessage.info('设置功能开发中')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(
          '确定要退出登录吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        await authStore.logout()
        ElMessage.success('已退出登录')
        router.push('/auth/login')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('退出失败，请重试')
          console.error('Logout failed:', error)
        }
      }
      break
  }
}
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  background: linear-gradient(to bottom right, #f0f2f5, #ffffff);
  overflow: hidden;
}

.sidebar {
  width: 300px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-header {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(to right, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.8));
}

.new-chat-btn {
  width: 100%;
  justify-content: center;
  height: 44px;
  font-size: 15px;
  border-radius: 12px;
  background: var(--el-color-primary);
  color: #fff;
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.2);
}

.new-chat-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(var(--el-color-primary-rgb), 0.3);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  position: relative;
  overflow: hidden;
}

.chat-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}

.chat-title {
  font-size: 16px;
  font-weight: 500;
  color: #000;
  flex: 1;
  margin: 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-title .el-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions :deep(.theme-switch) {
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.header-actions :deep(.theme-switch:hover) {
  background: var(--el-fill-color-light);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.user-profile:hover {
  background: #f3f4f6;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.user-info {
  display: flex;
  flex-direction: column;
  font-size: 12px;
}

.username {
  font-weight: 500;
  color: #111827;
}

.user-role {
  color: #6b7280;
  font-size: 11px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.empty-state .empty-icon {
  margin-bottom: 24px;
  color: var(--el-color-primary);
  opacity: 0.8;
}

.empty-state h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px;
  background: linear-gradient(120deg, var(--el-color-primary), #409eff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.empty-state p {
  font-size: 16px;
  margin: 0;
  opacity: 0.8;
}

.input-container {
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  position: relative;
}

.input-wrapper {
  background: #ffffff;
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.input-wrapper:focus-within {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

:deep(.el-textarea__inner) {
  border: none;
  padding: 12px 16px;
  font-size: 15px;
  line-height: 1.6;
  resize: none;
  box-shadow: none !important;
  background: transparent;
}

:deep(.el-textarea__inner:focus) {
  box-shadow: none !important;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.send-button {
  min-width: 80px;
  height: 36px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  background: var(--el-color-primary);
  border: none;
  color: white;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(var(--el-color-primary-rgb), 0.2);
}

.send-button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

.send-button:disabled {
  background: var(--el-color-primary-light-5);
  cursor: not-allowed;
  opacity: 0.7;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    width: 85%;
    max-width: 300px;
    transform: translateX(-100%);
  }

  .sidebar.show {
    transform: translateX(0);
  }

  .chat-main {
    width: 100%;
  }

  .chat-header {
    padding: 0 16px;
    height: 56px;
  }

  .messages-container {
    padding: 16px;
  }

  .input-container {
    padding: 16px;
  }

  .input-wrapper {
    padding: 8px;
    border-radius: 12px;
  }

  :deep(.el-textarea__inner) {
    padding: 8px 12px;
    font-size: 14px;
    min-height: 24px !important;
  }

  .action-buttons {
    margin-top: 8px;
    padding-top: 8px;
  }

  .send-button {
    min-width: 70px;
    height: 32px;
    font-size: 13px;
  }
}

/* 深色模式 */
html.dark {
  .chat-container {
    background: linear-gradient(to bottom right, #1a1a1a, #2d2d2d);
  }

  .sidebar {
    background: rgba(30, 30, 30, 0.9);
    border-right-color: rgba(255, 255, 255, 0.1);
  }

  .chat-main {
    background: #1a1a1a;
  }

  .input-wrapper {
    background: rgba(40, 40, 40, 0.9);
  }

  .empty-state {
    color: rgba(255, 255, 255, 0.7);
  }

  :deep(.el-textarea__inner) {
    color: rgba(255, 255, 255, 0.9);
    background: transparent;
  }

  .action-buttons {
    border-top-color: rgba(255, 255, 255, 0.1);
  }
}
</style> 