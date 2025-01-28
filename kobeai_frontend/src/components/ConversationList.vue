<template>
  <div class="conversation-list">
    <div class="list-header">
      <div class="header-title">
        <el-icon><ChatRound /></el-icon>
        <h3>会话列表</h3>
      </div>
      <el-input
        v-model="searchQuery"
        placeholder="搜索会话..."
        clearable
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    
    <div class="list-content">
      <el-scrollbar>
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conv.id === currentId }"
          @click="handleSelect(conv.id)"
        >
          <div class="item-content">
            <div class="item-icon">
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <div class="item-main">
              <div v-if="editingId === conv.id" class="edit-title">
                <el-input
                  v-model="editTitle"
                  size="small"
                  placeholder="请输入标题"
                  @keyup.enter="handleEdit(conv.id)"
                  @blur="handleEdit(conv.id)"
                  ref="editInputRef"
                />
              </div>
              <div v-else class="item-info">
                <div class="item-title" :title="conv.title || '新会话'">
                  {{ conv.title || '新会话' }}
                </div>
                <div class="item-meta">
                  {{ formatTime(conv.updatedAt || conv.createdAt) }}
                </div>
              </div>
            </div>
            <div class="item-actions">
              <el-tooltip content="重命名" placement="top">
                <el-button
                  type="primary"
                  link
                  :icon="EditPen"
                  @click.stop="startEdit(conv.id, conv.title || '')"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  type="danger"
                  link
                  :icon="Delete"
                  @click.stop="handleDelete(conv.id)"
                />
              </el-tooltip>
            </div>
          </div>
        </div>
        <div v-if="filteredConversations.length === 0" class="empty-list">
          <el-empty description="暂无会话" :image-size="60" />
        </div>
      </el-scrollbar>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'ConversationList'
}
</script>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen, Search, ChatRound, ChatLineRound } from '@element-plus/icons-vue'
import type { Conversation } from '../api/chat'
import { formatTime } from '../utils/time'

interface Props {
  conversations: Conversation[]
  currentId?: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', id: number): void
  (e: 'rename', id: number, title: string): void
  (e: 'delete', id: number): void
}>()

const editingId = ref<number | null>(null)
const editTitle = ref('')
const editInputRef = ref<HTMLElement | null>(null)
const searchQuery = ref('')

// 过滤后的会话列表
const filteredConversations = computed(() => {
  if (!searchQuery.value) return props.conversations
  const query = searchQuery.value.toLowerCase()
  return props.conversations.filter(conv => 
    (conv.title || '新会话').toLowerCase().includes(query)
  )
})

const handleSelect = (id: number) => {
  emit('select', id)
}

const startEdit = async (id: number, title: string) => {
  editingId.value = id
  editTitle.value = title || ''
  await nextTick()
  editInputRef.value?.focus()
}

const handleEdit = (id: number) => {
  if (!editTitle.value.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  emit('rename', id, editTitle.value)
  editingId.value = null
  editTitle.value = ''
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该会话吗？此操作不可恢复。',
      '删除会话',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    emit('delete', id)
  } catch {
    // 用户取消删除
  }
}
</script>

<style scoped>
.conversation-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.list-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.header-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.search-input {
  margin-top: 8px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px var(--el-border-color) !important;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) !important;
}

.list-content {
  flex: 1;
  overflow: hidden;
  padding: 8px;
}

.conversation-item {
  margin-bottom: 4px;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid transparent;
}

.conversation-item:hover {
  background: var(--el-fill-color-light);
  transform: translateX(2px);
}

.conversation-item.active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.item-content {
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--el-color-primary-light-8);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.item-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.conversation-item:hover .item-actions {
  opacity: 1;
}

.edit-title {
  flex: 1;
}

.edit-title :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding: 0;
}

.edit-title :deep(.el-input__inner) {
  height: 24px;
  font-size: 14px;
  padding: 0;
}

.empty-list {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}

/* 滚动条样式 */
:deep(.el-scrollbar__bar.is-horizontal) {
  display: none;
}

:deep(.el-scrollbar__bar.is-vertical) {
  width: 4px;
}

:deep(.el-scrollbar__thumb) {
  background-color: var(--el-border-color-darker);
  opacity: 0.3;
}

:deep(.el-scrollbar__thumb:hover) {
  opacity: 0.5;
}
</style>