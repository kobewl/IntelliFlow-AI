<template>
  <div class="chat-container">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="handleNewChat">
          <el-icon><Plus /></el-icon>
          <span>新对话</span>
        </el-button>
      </div>
      
      <!-- 会话列表 -->
      <div class="conversation-list">
        <ConversationList
          :conversations="conversations"
          :current-id="currentConversationId"
          @select="handleSelectConversation"
          @rename="handleRenameConversation"
          @delete="handleDeleteConversation"
        />
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <div class="chat-header">
        <div class="chat-title">
          <el-icon><ChatRound /></el-icon>
          <span>{{ currentConversation?.title || '新对话' }}</span>
        </div>
        
        <div class="header-actions">
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
  Position,
  Paperclip
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../store/chat'
import ConversationList from '../components/ConversationList.vue'
import VirtualMessageList from '../components/VirtualMessageList.vue'
import UserProfile from '../components/UserProfile.vue'
import { MessageRole } from '../types/chat'
import { presetResponses, getRandomResponse } from '../utils/presets'
import { chatApi } from '../api/chat'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

// Store 实例
const authStore = useAuthStore()
const chatStore = useChatStore()
const { user } = storeToRefs(authStore)
const { 
  conversations,
  currentConversationId,
  currentConversation,
  loading,
  loadingMore 
} = storeToRefs(chatStore)

const messageInput = ref('')
const fileUploadRef = ref()

// 使用固定的AI头像
const aiAvatar = '/avatars/ai-avatar.png'
const userAvatar = computed(() => user.value?.avatar || '/avatars/default-avatar.png')

// 是否可以发送消息
const canSendMessage = computed(() => {
  return messageInput.value.trim().length > 0 && !loading.value
})

// 当前会话标题
const currentTitle = computed(() => {
  if (!currentConversation.value) return 'AI助手'
  return currentConversation.value.title || `新会话 ${currentConversation.value.id}`
})

// 加载会话列表
onMounted(async () => {
  try {
    await chatStore.loadConversations()
  } catch (error) {
    ElMessage.error('加载会话列表失败')
  }
})

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

    if (!currentConversation.value) {
      throw new Error('当前会话不存在')
    }

    if (!currentConversation.value.messages) {
      currentConversation.value.messages = []
    }

    // 检查是否是预设问题
    const presetResponse = checkPresetQuestion(message)
    if (presetResponse) {
      // 如果是预设问题，直接在前端添加消息和回复
      currentConversation.value.messages.push({
        id: Date.now(),
        role: MessageRole.USER,
        content: message,
        createdAt: new Date().toISOString()
      })

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
  
        loading.value = true
        
        try {
          // 1. 先清除聊天状态
          chatStore.clearStore()
          
          // 2. 执行退出登录
          await authStore.logout()
          
          // 3. 显示成功消息
          ElMessage.success('已退出登录')
          
          // 4. 获取当前的基础URL
          const baseUrl = window.location.origin
          const port = window.location.port || (window.location.protocol === 'https:' ? '443' : '80')
          const targetUrl = `${window.location.protocol}//${window.location.hostname}:${port}`

          // 5. 强制跳转到首页
          window.location.href = targetUrl

        } catch (error) {
          console.error('Logout failed:', error)
          ElMessage.error('退出失败，请重试')
        } finally {
          loading.value = false
  }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Logout confirmation failed:', error)
}
}
      break
    }
}
</script>

<style lang="scss" scoped>
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

  &.collapsed {
    width: 60px;

    .new-chat-btn {
      width: auto;
      padding: 8px;
    }

    .collapse-btn {
      margin-top: 8px;
    }
  }
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(to right, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.8));
  display: flex;
  flex-direction: column;
  gap: 8px;
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

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(var(--el-color-primary-rgb), 0.3);
  }
}

.collapse-btn {
  align-self: center;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
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

  .el-icon {
    font-size: 18px;
    color: var(--el-color-primary);
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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

  .empty-icon {
    margin-bottom: 24px;
    color: var(--el-color-primary);
    opacity: 0.8;
  }

  h2 {
    font-size: 24px;
    font-weight: 600;
    margin: 0 0 12px;
    background: linear-gradient(120deg, var(--el-color-primary), #409eff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  p {
    font-size: 16px;
    margin: 0;
    opacity: 0.8;
  }
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

  &:focus-within {
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

    &:focus {
      box-shadow: none !important;
    }
  }
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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

    &.show {
      transform: translateX(0);
    }

    &.collapsed {
      width: 60px;
      transform: translateX(0);
    }
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
}

/* 深色模式 */
:root.dark {
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