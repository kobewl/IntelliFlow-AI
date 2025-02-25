<template>
  <div class="chat-input">
    <div class="input-wrapper">
      <el-input
        v-model="inputValue"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        :placeholder="loading ? '正在思考中...' : '输入消息，Enter发送，Shift+Enter换行'"
        @keydown.enter.exact.prevent="handleSend"
        @keydown.enter.shift.exact="appendNewLine"
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
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Position, Paperclip } from '@element-plus/icons-vue'

// 定义组件接收的属性
const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

// 定义向父组件发出的事件
const emit = defineEmits(['send', 'file-select'])

// 内部状态
const inputValue = ref('')
const fileUploadRef = ref()

// 计算属性：是否可以发送消息
const canSendMessage = computed(() => {
  return inputValue.value.trim().length > 0 && !props.loading
})

// 方法: 添加新行
function appendNewLine() {
  inputValue.value += '\n'
}

// 方法: 处理发送消息
function handleSend() {
  if (!canSendMessage.value) return
  
  emit('send', inputValue.value)
  inputValue.value = '' // 清空输入框
}

// 方法: 处理文件选择
function handleFileSelect(file: any) {
  emit('file-select', file)
}
</script>

<script lang="ts">
export default {
  name: 'ChatInput'
}
</script>

<style scoped>
.chat-input {
  width: 100%;
  padding: 16px 24px;
  background-color: var(--el-bg-color, #fff);
  transition: all 0.3s ease;
  border-top: 1px solid var(--el-border-color-light, #e6e6e6);
}

:global(.dark-mode) .chat-input {
  background-color: #1e1f2c;
  border-color: #2f3241;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  background-color: var(--el-bg-color, #fff);
  border-radius: 12px;
  position: relative;
  transition: all 0.3s ease;
}

:global(.dark-mode) .input-wrapper {
  background-color: #24273a;
}

.input-wrapper :deep(.el-input__wrapper) {
  background-color: var(--el-bg-color-page, #f5f7fa);
  border-radius: 12px;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border: 1px solid var(--el-border-color, #dcdfe6);
}

.input-wrapper :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 1px var(--el-color-primary, #409eff), 0 4px 12px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.2);
  border-color: var(--el-color-primary, #409eff);
}

:global(.dark-mode) .input-wrapper :deep(.el-input__wrapper) {
  background-color: #24273a;
  border-color: #2f3241;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:global(.dark-mode) .input-wrapper :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 1px #7aa2f7, 0 4px 12px rgba(122, 162, 247, 0.2);
  border-color: #7aa2f7;
}

.input-wrapper :deep(.el-textarea__inner) {
  background-color: transparent;
  padding: 8px;
  font-size: 15px;
  transition: all 0.3s ease;
  color: var(--el-text-color-primary, #303133);
  line-height: 1.5;
}

:global(.dark-mode) .input-wrapper :deep(.el-textarea__inner) {
  color: #c0caf5;
}

.input-wrapper :deep(.el-textarea__inner::placeholder) {
  color: var(--el-text-color-placeholder, #a8abb2);
}

:global(.dark-mode) .input-wrapper :deep(.el-textarea__inner::placeholder) {
  color: rgba(192, 202, 245, 0.5);
}

.action-buttons {
  display: flex;
  margin-left: 12px;
  gap: 8px;
}

.action-buttons :deep(.el-button) {
  height: 40px;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  border: none;
}

.action-buttons :deep(.el-button:not(.is-disabled):hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.action-buttons :deep(.el-button--primary) {
  background-color: var(--el-color-primary, #409eff);
}

.action-buttons :deep(.el-button--primary:not(.is-disabled):hover) {
  background-color: var(--el-color-primary-light-3, #79bbff);
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.3);
}

.action-buttons :deep(.el-icon) {
  font-size: 18px;
  line-height: 1;
}

:global(.dark-mode) .action-buttons :deep(.el-button) {
  background-color: #2f3241;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

:global(.dark-mode) .action-buttons :deep(.el-button--primary) {
  background-color: #7aa2f7;
}

:global(.dark-mode) .action-buttons :deep(.el-button--primary:not(.is-disabled):hover) {
  background-color: #9aadf7;
  box-shadow: 0 4px 12px rgba(122, 162, 247, 0.3);
}

@media (max-width: 768px) {
  .chat-input {
    padding: 12px 16px;
  }
  
  .action-buttons :deep(.el-button) {
    height: 36px;
    width: 36px;
  }
}
</style> 