<template>
  <aside class="sidebar" :class="{ collapsed: !showSidebar }">
    <div class="sidebar-top">
      <router-link to="/" class="sidebar-logo">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" class="logo-icon">
          <rect width="24" height="24" rx="6" fill="url(#logo-grad)" />
          <path d="M7 9h10M7 12h7M7 15h4" stroke="#fff" stroke-width="1.5" stroke-linecap="round" />
          <defs>
            <linearGradient id="logo-grad" x1="0" y1="0" x2="24" y2="24">
              <stop stop-color="#6366f1" /><stop offset="1" stop-color="#8b5cf6" />
            </linearGradient>
          </defs>
        </svg>
        <span>KobeAI</span>
      </router-link>
      <el-button class="new-chat-btn" @click="emit('new-chat')">
        <el-icon><Plus /></el-icon>
        <span>新对话</span>
      </el-button>
    </div>

    <!-- 会话搜索 -->
    <div class="search-box" v-if="conversations.length > 3">
      <el-input v-model="searchText" size="small" placeholder="搜索会话..." clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- 会话列表 -->
    <div class="conversation-list">
      <div
        v-for="conv in filteredConversations"
        :key="conv.id"
        class="conv-item"
        :class="{ active: conv.id === currentConversationId }"
        @click="handleSelect(conv.id)"
      >
        <el-icon class="conv-icon"><ChatRound /></el-icon>
        <div class="conv-info">
          <div class="conv-title">{{ conv.title || '新对话' }}</div>
          <div class="conv-meta">
            <span class="conv-time">{{ formatTime(conv.createdAt) }}</span>
          </div>
        </div>
        <div class="conv-actions" @click.stop>
          <el-dropdown trigger="click" placement="bottom-start">
            <span class="conv-more"><el-icon><MoreFilled /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="startRename(conv)">
                  <el-icon><Edit /></el-icon>重命名
                </el-dropdown-item>
                <el-dropdown-item @click="handleDeleteConv(conv.id)" divided>
                  <el-icon><Delete /></el-icon>
                  <span class="danger-text">删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 分页加载更多 -->
      <div v-if="hasMoreConversations" class="load-more-convs">
        <el-button
          text
          size="small"
          :loading="loadingMoreConversations"
          @click="chatStore.loadMoreConversations()"
        >
          加载更多会话
        </el-button>
      </div>

      <div v-if="filteredConversations.length === 0 && conversations.length === 0" class="empty-list">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none" class="empty-icon">
          <rect x="6" y="8" width="36" height="32" rx="4" stroke="#d1d5db" stroke-width="2" />
          <line x1="12" y1="18" x2="36" y2="18" stroke="#e5e7eb" stroke-width="1.5" />
          <line x1="12" y1="24" x2="30" y2="24" stroke="#e5e7eb" stroke-width="1.5" />
          <line x1="12" y1="30" x2="24" y2="30" stroke="#e5e7eb" stroke-width="1.5" />
        </svg>
        <p>开始你的第一次对话</p>
      </div>
      <div v-else-if="filteredConversations.length === 0" class="empty-list">
        <p>没有匹配的会话</p>
      </div>
    </div>

    <!-- 底部状态区 -->
    <div class="sidebar-bottom">
      <div class="agent-status">
        <span class="status-dot"></span>
        <span>Agent 就绪</span>
        <span class="tool-count" v-if="toolCount > 0">{{ toolCount }} 个工具</span>
      </div>
      <div class="user-area" v-if="authStore.isAuthenticated">
        <el-avatar :size="28" :src="authStore.user?.avatar || '/ai-avatar.png'" />
        <span class="user-name">{{ authStore.user?.username }}</span>
        <el-icon class="logout-icon" @click="handleLogout" title="退出登录">
          <SwitchButton />
        </el-icon>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameVisible" title="重命名会话" width="360px" append-to-body>
      <el-input v-model="renameText" placeholder="输入新名称" @keydown.enter="confirmRename" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Plus, ChatRound, Search, MoreFilled, Edit, Delete, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import type { Conversation } from '../../api/chat'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

defineProps<{ showSidebar: boolean; toolCount: number }>()

const emit = defineEmits<{
  (e: 'new-chat'): void
  (e: 'select', id: number): void
}>()

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const { conversations, currentConversationId, hasMoreConversations, loadingMoreConversations } =
  storeToRefs(chatStore)

const searchText = ref('')
const renameVisible = ref(false)
const renameText = ref('')
const renameTarget = ref<Conversation | null>(null)

const filteredConversations = computed(() => {
  if (!searchText.value.trim()) return conversations.value
  const q = searchText.value.toLowerCase()
  return conversations.value.filter(c => (c.title || '').toLowerCase().includes(q))
})

function formatTime(t: string) {
  return dayjs(t).fromNow()
}

async function handleSelect(id: number) {
  await chatStore.switchConversation(id)
  emit('select', id)
}

async function handleDeleteConv(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这个会话吗？', '提示', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await chatStore.deleteConversation(id)
  } catch { /* 取消 */ }
}

function startRename(conv: Conversation) {
  renameTarget.value = conv
  renameText.value = conv.title || ''
  renameVisible.value = true
}

async function confirmRename() {
  if (renameTarget.value && renameText.value.trim()) {
    await chatStore.renameConversation(renameTarget.value.id, renameText.value.trim())
  }
  renameVisible.value = false
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
    chatStore.clearStore()
    await authStore.logout()
    router.push('/')
  } catch { /* 取消 */ }
}
</script>

<style lang="scss" scoped>
@use '../../styles/chat-vars' as *;

// ---- 左侧栏 ----
.sidebar {
  width: $sidebar-width;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid $border;
  transition: width 0.25s ease;

  &.collapsed {
    width: 0;
    overflow: hidden;
    border: none;
  }
}

.sidebar-top {
  padding: 16px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  margin-bottom: 14px;

  span {
    font-size: 20px;
    font-weight: 700;
    background: linear-gradient(135deg, $accent, #8b5cf6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.new-chat-btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  background: $accent;
  border-color: $accent;

  &:hover { background: #5558e6; border-color: #5558e6; }
}

// 搜索
.search-box {
  padding: 0 12px 8px;
}

// 会话列表
.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: #f3f4f6;

    .conv-actions { opacity: 1; }
  }

  &.active {
    background: $accent-light;

    .conv-title { color: $accent; font-weight: 500; }
  }
}

.conv-icon {
  font-size: 16px;
  color: $text-3;
  flex-shrink: 0;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-title {
  font-size: 13px;
  color: $text-1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-meta {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.conv-time {
  font-size: 11px;
  color: $text-3;
}

.conv-actions {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.conv-more {
  display: flex;
  padding: 3px;
  border-radius: 4px;
  color: $text-3;
  cursor: pointer;

  &:hover { background: #e5e7eb; color: $text-1; }
}

.danger-text { color: #ef4444; }

.load-more-convs {
  text-align: center;
  padding: 4px 0 8px;
}

// 空列表
.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  color: $text-3;
  text-align: center;

  p { font-size: 13px; margin: 12px 0 0; }

  .empty-icon { opacity: 0.4; }
}

// 底部
.sidebar-bottom {
  padding: 10px 16px;
  border-top: 1px solid #f0f1f5;
}

.agent-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-2;
  margin-bottom: 10px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.4);
}

.tool-count {
  margin-left: auto;
  color: $accent;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  flex: 1;
  font-size: 13px;
  color: $text-1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-icon {
  color: $text-3;
  cursor: pointer;
  font-size: 16px;

  &:hover { color: #ef4444; }
}

// ---- 响应式 ----
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    box-shadow: 4px 0 24px rgba(0,0,0,0.08);

    &.collapsed { display: none; }
  }
}
</style>
