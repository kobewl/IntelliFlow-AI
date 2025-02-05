<template>
  <div class="notifications-container">
    <div v-if="isAdmin" class="admin-controls mb-4">
      <el-button type="primary" @click="showCreateDialog">
        发布新通知
      </el-button>
    </div>

    <!-- 通知列表 -->
    <el-timeline>
      <el-timeline-item
        v-for="notification in notifications"
        :key="notification.id"
        :type="getNotificationType(notification.type)"
        :timestamp="formatTime(notification.createdAt)"
      >
        <el-card class="notification-card">
          <template #header>
            <div class="notification-header">
              <h3>{{ notification.title }}</h3>
              <div v-if="isAdmin" class="notification-actions">
                <el-button
                  v-if="notification.status === 'ACTIVE'"
                  type="warning"
                  size="small"
                  @click="updateStatus(notification.id, 'INACTIVE')"
                >
                  停用
                </el-button>
                <el-button
                  v-else
                  type="success"
                  size="small"
                  @click="updateStatus(notification.id, 'ACTIVE')"
                >
                  激活
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="deleteNotification(notification.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </template>
          <div class="notification-content">
            {{ notification.content }}
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <!-- 创建通知对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="发布新通知"
      width="50%"
    >
      <el-form :model="newNotification" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="newNotification.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="newNotification.content"
            type="textarea"
            :rows="4"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="newNotification.type">
            <el-option label="信息" value="INFO" />
            <el-option label="警告" value="WARNING" />
            <el-option label="错误" value="ERROR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createNotification">
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { Client } from '@stomp/stompjs'

const authStore = useAuthStore()
const notifications = ref([])
const createDialogVisible = ref(false)
const newNotification = ref({
  title: '',
  content: '',
  type: 'INFO'
})

const isAdmin = computed(() => authStore.isAdmin)
let stompClient: Client | null = null

// WebSocket连接
const connectWebSocket = () => {
  try {
    stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${authStore.token}`
      },
      debug: function (str) {
        console.log('STOMP: ' + str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    })

    stompClient.onConnect = function () {
      console.log('WebSocket Connected')
      
      // 订阅通知频道
      stompClient?.subscribe('/topic/notifications', message => {
        try {
          const notification = JSON.parse(message.body)
          notifications.value.unshift(notification)
        } catch (error) {
          console.error('Failed to parse notification:', error)
        }
      })
      
      // 订阅状态更新
      stompClient?.subscribe('/topic/notifications/status', message => {
        try {
          const updatedNotification = JSON.parse(message.body)
          const index = notifications.value.findIndex(n => n.id === updatedNotification.id)
          if (index !== -1) {
            notifications.value[index] = updatedNotification
          }
        } catch (error) {
          console.error('Failed to parse status update:', error)
        }
      })
      
      // 订阅删除事件
      stompClient?.subscribe('/topic/notifications/delete', message => {
        try {
          const deletedId = JSON.parse(message.body)
          notifications.value = notifications.value.filter(n => n.id !== deletedId)
        } catch (error) {
          console.error('Failed to parse delete event:', error)
        }
      })
    }

    stompClient.onStompError = function (frame) {
      console.error('STOMP error:', frame)
    }

    stompClient.activate()
  } catch (error) {
    console.error('Failed to initialize WebSocket:', error)
  }
}

// 获取所有通知
const fetchNotifications = async () => {
  try {
    const response = await fetch('/api/notifications', {
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    if (!response.ok) {
      throw new Error('获取通知失败')
    }
    const data = await response.json()
    if (data.code === 200) {
      notifications.value = data.data || []
    } else {
      throw new Error(data.message || '获取通知失败')
    }
  } catch (error) {
    ElMessage.error('获取通知列表失败')
    console.error('获取通知失败:', error)
  }
}

// 创建通知
const createNotification = async () => {
  try {
    const response = await fetch('/api/notifications', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify(newNotification.value)
    })
    if (!response.ok) {
      throw new Error('发布通知失败')
    }
    ElMessage.success('通知发布成功')
    createDialogVisible.value = false
    newNotification.value = { title: '', content: '', type: 'INFO' }
    await fetchNotifications() // 重新获取通知列表
  } catch (error) {
    ElMessage.error('发布通知失败')
    console.error('发布通知失败:', error)
  }
}

// 更新通知状态
const updateStatus = async (id: number, status: string) => {
  try {
    const response = await fetch(`/api/notifications/${id}/status?status=${status}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    if (!response.ok) {
      throw new Error('更新状态失败')
    }
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('更新状态失败')
    console.error('更新状态失败:', error)
  }
}

// 删除通知
const deleteNotification = async (id: number) => {
  try {
    const response = await fetch(`/api/notifications/${id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    if (!response.ok) {
      throw new Error('删除失败')
    }
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
    console.error('删除失败:', error)
  }
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 获取通知类型对应的样式
const getNotificationType = (type: string) => {
  const typeMap: Record<string, string> = {
    INFO: 'primary',
    WARNING: 'warning',
    ERROR: 'danger'
  }
  return typeMap[type] || 'info'
}

// 显示创建对话框
const showCreateDialog = () => {
  createDialogVisible.value = true
}

onMounted(() => {
  fetchNotifications()
  connectWebSocket()
})

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>

<style scoped>
.notifications-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.notification-card {
  margin-bottom: 10px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notification-header h3 {
  margin: 0;
}

.notification-actions {
  display: flex;
  gap: 8px;
}

.notification-content {
  white-space: pre-line;
}

.admin-controls {
  margin-bottom: 20px;
}
</style> 