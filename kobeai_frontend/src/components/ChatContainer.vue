<template>
  <div class="chat-container" :class="{ 'dark-mode': isDarkMode }">
    <div v-if="!isOnline" class="offline-indicator">
      <el-icon class="warning-icon"><WarningFilled /></el-icon>
      <span>您当前处于离线模式，部分功能可能受限</span>
    </div>
    
    <div class="chat-layout">
      <!-- 侧边栏 -->
      <ChatSidebar 
        :conversations="conversations"
        :currentConversation="currentConversation"
        :isOnline="isOnline"
        @select-conversation="handleSelectConversation"
        @new-chat="handleNewChat"
        @rename-conversation="handleRenameConversation"
        @delete-conversation="handleDeleteConversation"
      >
        <div v-if="conversations.length > 0" class="conversation-list">
          <div 
            v-for="conv in conversations" 
            :key="conv.id"
            :class="['conversation-item', { 'active': currentConversation?.id === conv.id }]"
            @click="handleSelectConversation(conv.id)"
          >
            <span class="conversation-title">{{ conv.title }}</span>
            <div class="conversation-actions">
              <el-button 
                v-if="isOnline"
                size="small" 
                circle 
                @click.stop="handleRenameConversation(conv.id)"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button 
                size="small" 
                circle 
                @click.stop="handleDeleteConversation(conv.id)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        <div v-else class="empty-conversations">
          <p>没有会话记录</p>
          <el-button type="primary" @click="handleNewChat">创建新会话</el-button>
        </div>
      </ChatSidebar>
      
      <!-- 主聊天区域 -->
      <div class="chat-main">
        <!-- 聊天头部 -->
        <ChatHeader 
          :title="currentConversation?.title || '新对话'"
          :isOnline="isOnline"
          :networkStatus="networkStatus"
        >
          <template #actions>
            <el-tooltip content="网络状态" placement="bottom">
              <el-badge v-if="isOnline" is-dot type="success">
                <el-icon><Connection /></el-icon>
              </el-badge>
              <el-badge v-else is-dot type="danger">
                <el-icon><WarningFilled /></el-icon>
              </el-badge>
            </el-tooltip>
          </template>
        </ChatHeader>
        
        <!-- 消息内容区域 -->
        <div class="chat-content">
          <!-- 加载状态 -->
          <LoadingState v-if="isLoading" message="正在加载对话..." />
          
          <!-- 错误状态 -->
          <ErrorState 
            v-else-if="error" 
            :message="error"
            @retry="handleRetry"
          />
          
          <!-- 空聊天状态 -->
          <EmptyChat 
            v-else-if="!currentConversation || !messages.length"
            @select-preset="handleSendMessage"
          />
          
          <!-- 消息列表 -->
          <VirtualMessageList 
            v-else
            :messages="messages"
            :hasMore="hasMoreMessages"
            :loadingMore="loadingMoreMessages"
            :userAvatar="userAvatarUrl"
            :aiAvatar="aiAvatarUrl"
            @load-more="loadMoreMessages"
          />
        </div>
        
        <!-- 聊天输入区域 -->
        <ChatInput
          v-model="messageInput"
          :isLoading="sendingMessage"
          :disabled="!isOnline && !offlineSupport"
          :offlineMode="!isOnline"
          @send="handleSendMessage"
          @file-select="handleFileSelect"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { WarningFilled, Edit, Delete, Connection } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

// 引入组件
import ChatSidebar from './ChatSidebar.vue'
import ChatHeader from './ChatHeader.vue'
import ChatInput from './ChatInput.vue'
import VirtualMessageList from './VirtualMessageList.vue'
import LoadingState from './LoadingState.vue'
import ErrorState from './ErrorState.vue'
import EmptyChat from './EmptyChat.vue'

// 引入工具和存储
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../store/chat'
import { networkStatus } from '../utils/networkStatus'

// 存储实例
const authStore = useAuthStore()
const chatStore = useChatStore()
const router = useRouter()

// 状态变量
const isLoading = ref(true)
const error = ref<string | null>(null)
const messages = ref<any[]>([])
const conversations = ref<any[]>([])
const currentConversation = ref<any>(null)
const messageInput = ref('')
const sendingMessage = ref(false)
const hasMoreMessages = ref(false)
const loadingMoreMessages = ref(false)
const isDarkMode = ref(false)

// 网络状态
const isOnline = computed(() => networkStatus.isOnline.value)
const offlineSupport = computed(() => chatStore.hasOfflineSupport)

// 用户和AI头像
const userAvatarUrl = computed(() => {
  return authStore.user?.avatar || '/avatars/default-avatar.png'
})

const aiAvatarUrl = computed(() => {
  return '/images/ai-avatar.png'
})

// 初始化
onMounted(async () => {
  try {
    isLoading.value = true
    error.value = null
    
    // 检查本地存储中是否有离线会话
    if (!isOnline.value && offlineSupport.value) {
      await chatStore.loadOfflineConversations()
      conversations.value = chatStore.conversations
      
      if (conversations.value.length > 0) {
        await handleSelectConversation(conversations.value[0].id)
      }
    } else {
      await chatStore.loadConversations()
      conversations.value = chatStore.conversations
      
      if (conversations.value.length > 0) {
        await handleSelectConversation(conversations.value[0].id)
      }
    }
    
    // 检查深色模式设置
    isDarkMode.value = document.documentElement.classList.contains('dark')
  } catch (err) {
    console.error('初始化聊天失败:', err)
    error.value = '加载对话失败，请重试'
  } finally {
    isLoading.value = false
  }
})

// 监听网络状态变化
watch(() => networkStatus.isOnline.value, async (newStatus) => {
  if (newStatus && chatStore.hasPendingMessages) {
    const result = await ElMessageBox.confirm(
      '检测到您在离线时发送了消息，是否要将它们同步到服务器？',
      '网络已恢复',
      {
        confirmButtonText: '同步',
        cancelButtonText: '不同步',
        type: 'info'
      }
    ).catch(() => 'cancel')
    
    if (result === 'confirm') {
      try {
        await chatStore.syncPendingMessages()
        ElMessage.success('离线消息同步成功')
      } catch (err) {
        console.error('同步离线消息失败:', err)
        ElMessage.error('同步离线消息失败')
      }
    }
  }
})

// 监听深色模式变化
watch(
  () => document.documentElement.classList.contains('dark'),
  (isDark) => {
    isDarkMode.value = isDark
  }
)

// 重试加载
async function handleRetry() {
  try {
    isLoading.value = true
    error.value = null
    
    await chatStore.loadConversations()
    conversations.value = chatStore.conversations
    
    if (conversations.value.length > 0) {
      await handleSelectConversation(conversations.value[0].id)
    }
    
    isLoading.value = false
  } catch (err) {
    console.error('重试加载失败:', err)
    error.value = '加载对话失败，请重试'
    isLoading.value = false
  }
}

// 检查预设问题
function checkPresetQuestion(content: string) {
  const presetResponses: Record<string, string> = {
    '你是谁?': '我是科比AI，一个由科比科技开发的人工智能助手。我可以回答问题、提供信息和协助完成各种任务。',
    '你能做什么?': '我可以回答问题、提供信息、协助学习、创作内容、编写代码、分析数据等。有什么我可以帮到你的吗？',
    '谢谢': '不客气！如果还有其他问题，随时可以问我。',
    '帮助': '我可以回答问题、提供信息、协助学习、创作内容、编写代码等。只需直接输入您的问题或需求，我会尽力提供帮助。'
  }
  
  return presetResponses[content] || null
}

// 创建新对话
async function handleNewChat() {
  try {
    if (!isOnline.value && !offlineSupport.value) {
      ElMessage.warning('离线模式下无法创建新对话')
      return
    }
    
    const newConversation = await chatStore.createConversation('新对话')
    conversations.value = chatStore.conversations
    currentConversation.value = newConversation
    messages.value = []
    
    // 添加一个欢迎消息
    const welcomeMessage = {
      id: Date.now(),
      role: 'assistant',
      content: '你好！我是科比AI助手，有什么我可以帮到你的吗？',
      createdAt: new Date().toISOString()
    }
    
    if (!isOnline.value && offlineSupport.value) {
      await chatStore.addOfflineMessage(currentConversation.value.id, welcomeMessage)
    } else {
      await chatStore.addMessage(currentConversation.value.id, welcomeMessage)
    }
    
    messages.value = [welcomeMessage]
  } catch (err) {
    console.error('创建新对话失败:', err)
    ElMessage.error('创建新对话失败')
  }
}

// 发送消息
async function handleSendMessage(content: string) {
  if (!content.trim()) return
  
  try {
    sendingMessage.value = true
    
    // 检查是否是预设问题，如果是，直接使用预设回答
    const presetResponse = checkPresetQuestion(content)
    
    // 创建用户消息对象
    const userMessage = {
      id: Date.now(),
      role: 'user',
      content,
      createdAt: new Date().toISOString()
    }
    
    // 如果没有当前对话，创建一个新的
    if (!currentConversation.value) {
      await handleNewChat()
    }
    
    // 添加用户消息到UI
    messages.value.push(userMessage)
    messageInput.value = ''
    
    // 保存用户消息
    if (!isOnline.value && offlineSupport.value) {
      await chatStore.addOfflineMessage(currentConversation.value.id, userMessage)
    } else {
      await chatStore.addMessage(currentConversation.value.id, userMessage)
    }
    
    // 如果是预设问题，添加预设回答
    if (presetResponse) {
      const assistantMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: presetResponse,
        createdAt: new Date().toISOString()
      }
      
      setTimeout(async () => {
        messages.value.push(assistantMessage)
        
        if (!isOnline.value && offlineSupport.value) {
          await chatStore.addOfflineMessage(currentConversation.value.id, assistantMessage)
        } else {
          await chatStore.addMessage(currentConversation.value.id, assistantMessage)
        }
        
        sendingMessage.value = false
      }, 500) // 添加短暂延迟，让回复更自然
      
      return
    }
    
    // 如果离线且没有离线支持，提示用户
    if (!isOnline.value && !offlineSupport.value) {
      ElMessage.warning('您当前处于离线模式，无法发送消息到服务器')
      sendingMessage.value = false
      return
    }
    
    // 在线状态下，发送消息到服务器
    if (isOnline.value) {
      const response = await chatStore.sendMessage(currentConversation.value.id, content)
      
      if (response) {
        messages.value.push({
          id: response.id || Date.now() + 1,
          role: 'assistant',
          content: response.content,
          createdAt: response.createdAt || new Date().toISOString()
        })
      }
    } 
    // 离线状态下的特殊处理
    else if (offlineSupport.value) {
      // 添加一个模拟的AI回复
      const offlineResponse = {
        id: Date.now() + 1,
        role: 'assistant',
        content: '我目前处于离线模式，无法处理您的请求。网络恢复后，我会尽快回复您。',
        createdAt: new Date().toISOString()
      }
      
      messages.value.push(offlineResponse)
      await chatStore.addOfflineMessage(currentConversation.value.id, offlineResponse)
    }
  } catch (err) {
    console.error('发送消息失败:', err)
    ElMessage.error('发送消息失败')
  } finally {
    sendingMessage.value = false
  }
}

// 处理文件选择
function handleFileSelect(file: File) {
  if (!isOnline.value) {
    ElMessage.warning('离线模式下无法上传文件')
    return false
  }
  
  // 在这里处理文件上传逻辑
  console.log('选择文件:', file.name)
  ElMessage.info(`文件 ${file.name} 已选择，上传功能即将上线`)
  return true
}

// 选择对话
async function handleSelectConversation(conversationId: string | number) {
  try {
    isLoading.value = true
    error.value = null
    
    // 查找选中的对话
    const selected = conversations.value.find(c => c.id === conversationId)
    if (!selected) {
      throw new Error('对话不存在')
    }
    
    currentConversation.value = selected
    
    // 获取对话消息
    if (!isOnline.value && offlineSupport.value) {
      messages.value = await chatStore.getOfflineMessages(conversationId)
    } else {
      await chatStore.switchConversation(conversationId)
      messages.value = currentConversation.value.messages || []
      hasMoreMessages.value = currentConversation.value.hasMore || false
    }
  } catch (err) {
    console.error('选择对话失败:', err)
    error.value = '加载对话消息失败'
  } finally {
    isLoading.value = false
  }
}

// 重命名对话
async function handleRenameConversation(conversationId: string | number) {
  if (!isOnline.value) {
    ElMessage.warning('离线模式下无法重命名对话')
    return
  }
  
  try {
    // 查找当前会话
    const conversation = conversations.value.find(c => c.id === conversationId)
    if (!conversation) {
      throw new Error('对话不存在')
    }
    
    // 使用对话框让用户输入新标题
    const { value: newTitle } = await ElMessageBox.prompt(
      '请输入新的对话标题',
      '重命名对话',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: conversation.title || '',
        inputValidator: (value) => !!value.trim() || '标题不能为空',
        inputErrorMessage: '标题不能为空'
      }
    )
    
    // 如果用户取消或输入为空，则不执行重命名
    if (!newTitle || !newTitle.trim()) {
      return
    }
    
    await chatStore.renameConversation(conversationId, newTitle.trim())
    
    // 更新本地对话列表
    conversations.value = chatStore.conversations
    
    // 如果重命名的是当前对话，更新当前对话数据
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value = {...currentConversation.value, title: newTitle.trim()}
    }
    
    ElMessage.success('对话重命名成功')
  } catch (err) {
    console.error('重命名对话失败:', err)
    ElMessage.error('重命名对话失败')
  }
}

// 删除对话
async function handleDeleteConversation(conversationId: string | number) {
  if (!isOnline.value && !offlineSupport.value) {
    ElMessage.warning('离线模式下无法删除对话')
    return
  }
  
  try {
    await chatStore.deleteConversation(conversationId)
    
    // 更新本地对话列表
    conversations.value = chatStore.conversations
    
    // 如果删除的是当前对话，选择第一个对话或清空当前对话
    if (currentConversation.value?.id === conversationId) {
      if (conversations.value.length > 0) {
        await handleSelectConversation(conversations.value[0].id)
      } else {
        currentConversation.value = null
        messages.value = []
      }
    }
    
    ElMessage.success('对话删除成功')
  } catch (err) {
    console.error('删除对话失败:', err)
    ElMessage.error('删除对话失败')
  }
}

// 加载更多消息
async function loadMoreMessages() {
  if (loadingMoreMessages.value || !hasMoreMessages.value || !currentConversation.value) {
    return
  }
  
  try {
    loadingMoreMessages.value = true
    
    // 记录第一条消息的ID，用于后续定位
    const firstMessageId = messages.value.length > 0 ? messages.value[0].id : null
    
    // 获取更早的消息
    await chatStore.loadMoreMessages(currentConversation.value.id)
    
    // 更新消息列表
    messages.value = currentConversation.value.messages || []
    hasMoreMessages.value = currentConversation.value.hasMore || false
    
  } catch (err) {
    console.error('加载更多消息失败:', err)
    ElMessage.error('加载更多消息失败')
  } finally {
    loadingMoreMessages.value = false
  }
}
</script>

<script lang="ts">
export default {
  name: 'ChatContainer'
}
</script>

<style scoped>
.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color, #f9fafb);
  transition: all 0.3s ease;
  font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}

.dark-mode {
  background-color: #1a1b26;
  color: #c0caf5;
}

.offline-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  background-color: rgba(255, 152, 0, 0.1);
  color: #ff9800;
  font-size: 14px;
  border-radius: 0;
  backdrop-filter: blur(4px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 1px;
}

.dark-mode .offline-indicator {
  background-color: rgba(255, 183, 77, 0.1);
  color: #ffb74d;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.warning-icon {
  margin-right: 8px;
  font-size: 16px;
}

.chat-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  background-color: var(--el-bg-color-page, #fff);
  transition: all 0.3s ease;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.03);
}

.dark-mode .chat-main {
  background-color: #1e1f2c;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.2);
}

.chat-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  padding: 0;
}

/* 会话列表样式 */
.conversation-list {
  padding: 12px;
}

.conversation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
  background-color: rgba(255, 255, 255, 0.6);
}

.conversation-item:hover {
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  transform: translateY(-1px);
  border-color: var(--el-color-primary-light-7, #c6e2ff);
}

.conversation-item.active {
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  border-color: var(--el-color-primary-light-5, #a0cfff);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.dark-mode .conversation-item {
  background-color: rgba(255, 255, 255, 0.05);
}

.dark-mode .conversation-item:hover {
  background-color: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.2);
}

.dark-mode .conversation-item.active {
  background-color: rgba(64, 158, 255, 0.15);
  border-color: rgba(64, 158, 255, 0.3);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.conversation-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--el-text-color-primary, #303133);
  transition: color 0.3s;
}

.dark-mode .conversation-title {
  color: #c0caf5;
}

.conversation-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.3s, transform 0.2s;
  transform: scale(0.9);
}

.conversation-item:hover .conversation-actions {
  opacity: 1;
  transform: scale(1);
}

.conversation-actions .el-button {
  padding: 6px;
  border: none;
  background: transparent;
}

.conversation-actions .el-button:hover {
  background-color: rgba(0, 0, 0, 0.03);
  color: var(--el-color-primary, #409eff);
}

.dark-mode .conversation-actions .el-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #7aa2f7;
}

.empty-conversations {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  color: var(--el-text-color-secondary, #909399);
  text-align: center;
  height: 100%;
}

.empty-conversations p {
  margin-bottom: 20px;
  font-size: 15px;
}

.empty-conversations .el-button {
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.3s;
}

.empty-conversations .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

/* 响应式布局 */
@media (max-width: 768px) {
  .chat-layout {
    flex-direction: column;
  }
  
  .chat-sidebar {
    width: 100%;
    height: auto;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light, #e5e7eb);
    z-index: 10;
  }
  
  .dark-mode .chat-sidebar {
    border-color: #2f3241;
  }
  
  .conversation-actions {
    opacity: 1;
    transform: scale(1);
  }
}

/* ChatSidebar样式增强 */
:deep(.chat-sidebar) {
  border-right: 1px solid var(--el-border-color-light, #e6e6e6);
  background-color: var(--el-bg-color, #f8f9fa);
  transition: all 0.3s ease;
}

.dark-mode :deep(.chat-sidebar) {
  border-color: #2f3241;
  background-color: #16171e;
}

:deep(.sidebar-header) {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-light, #e6e6e6);
}

.dark-mode :deep(.sidebar-header) {
  border-color: #2f3241;
}

:deep(.new-chat-btn) {
  border-radius: 10px;
  transition: all 0.3s ease;
  height: 42px;
  font-weight: 500;
}

:deep(.new-chat-btn:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

/* 美化ChatHeader */
:deep(.chat-header) {
  border-bottom: 1px solid var(--el-border-color-light, #e6e6e6);
  padding: 16px 24px;
  backdrop-filter: blur(10px);
  background-color: rgba(var(--el-bg-color-rgb, 255, 255, 255), 0.8);
}

.dark-mode :deep(.chat-header) {
  border-color: #2f3241;
  background-color: rgba(30, 31, 44, 0.8);
}

:deep(.header-title) {
  font-size: 18px;
  font-weight: 500;
}

/* 美化输入区域 */
:deep(.chat-input) {
  border-top: 1px solid var(--el-border-color-light, #e6e6e6);
  padding: 16px 24px;
  background-color: var(--el-bg-color, #fff);
  transition: all 0.3s ease;
}

.dark-mode :deep(.chat-input) {
  border-color: #2f3241;
  background-color: #1e1f2c;
}

:deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 16px;
  resize: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid var(--el-border-color, #dcdfe6);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 2px 12px rgba(var(--el-color-primary-rgb), 0.15);
  border-color: var(--el-color-primary, #409eff);
}

.dark-mode :deep(.el-textarea__inner) {
  background-color: #24273a;
  border-color: #2f3241;
  color: #c0caf5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode :deep(.el-textarea__inner:focus) {
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
  border-color: #7aa2f7;
}

:deep(.input-actions) {
  margin-top: 12px;
  gap: 12px;
  display: flex;
  justify-content: flex-end;
}

:deep(.input-actions .el-button) {
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.3s;
}

:deep(.input-actions .el-button:not(.is-disabled):hover) {
  transform: translateY(-1px);
}

:deep(.input-actions .el-button--primary:not(.is-disabled):hover) {
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

/* 美化消息列表 */
:deep(.message-list) {
  padding: 24px;
}

:deep(.message-item) {
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

:deep(.message-bubble) {
  max-width: 80%;
  border-radius: 16px;
  padding: 12px 18px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

:deep(.message-bubble.user) {
  background-color: var(--el-color-primary, #409eff);
  color: white;
  border-top-right-radius: 4px;
}

:deep(.message-bubble.assistant) {
  background-color: var(--el-bg-color, #f4f4f5);
  color: var(--el-text-color-primary, #303133);
  border-top-left-radius: 4px;
}

.dark-mode :deep(.message-bubble.assistant) {
  background-color: #292b3d;
  color: #c0caf5;
}

/* 美化头像 */
:deep(.avatar-container) {
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid white;
}

.dark-mode :deep(.avatar-container) {
  border-color: #2f3241;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
</style> 