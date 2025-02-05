<template>
  <div class="chat-container">
    <!-- 聊天消息列表 -->
    <div class="chat-messages" ref="messagesRef">
      <div v-for="message in messages" :key="message.id" class="message" :class="message.role">
        <div class="message-avatar">
          <el-avatar :size="40" :src="message.role === 'user' ? userAvatar : aiAvatar" />
        </div>
        <div class="message-content">
          <div class="message-text" v-html="formatMessage(message.content)"></div>
          <div class="message-time">{{ formatTime(message.createdAt) }}</div>
        </div>
      </div>
    </div>

    <!-- 输入框区域 -->
    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="3"
        placeholder="输入您的问题..."
        @keyup.enter.exact="handleSend"
      />
      <div class="input-actions">
        <el-button type="primary" @click="handleSend" :loading="loading">
          发送
        </el-button>
        <el-button @click="handleClear">
          清空对话
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { chatApi } from '@/api/chat'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    return hljs.highlight(code, { language }).value;
  },
  langPrefix: 'hljs language-'
});

const userStore = useUserStore()
const messagesRef = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)

// 使用固定的AI头像
const aiAvatar = '/ai-avatar.png'
const userAvatar = userStore.userInfo?.avatar || '/default-avatar.png'

// 格式化消息内容（支持 Markdown）
const formatMessage = (content: string) => {
  return marked(content)
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleTimeString()
}

// 发送消息
const handleSend = async () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  const userMessage = {
    id: Date.now(),
    role: 'user',
    content: inputMessage.value,
    createdAt: new Date().toISOString()
  }

  messages.value.push(userMessage)
  const tempMessage = inputMessage.value
  inputMessage.value = ''
  loading.value = true

  try {
    const response = await chatApi.sendMessage(tempMessage)
    if (response.code === 200) {
      messages.value.push({
        id: Date.now() + 1,
        role: 'assistant',
        content: response.data,
        createdAt: new Date().toISOString()
      })
    } else {
      ElMessage.error(response.message || '发送失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 清空对话
const handleClear = () => {
  messages.value = []
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

onMounted(() => {
  // 如果需要，这里可以加载历史消息
})
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      background-color: var(--el-color-primary-light-9);
    }
  }
}

.message-content {
  max-width: 70%;
  background-color: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.message-text {
  word-break: break-word;
  line-height: 1.5;

  :deep(pre) {
    margin: 10px 0;
    padding: 12px;
    border-radius: 4px;
    background-color: #f6f8fa;
    overflow-x: auto;
  }

  :deep(code) {
    font-family: Monaco, Consolas, Courier New, monospace;
  }

  :deep(p) {
    margin: 8px 0;
  }
}

.message-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  text-align: right;
}

.chat-input {
  padding: 20px;
  background-color: #fff;
  border-top: 1px solid var(--el-border-color-light);
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
</style> 