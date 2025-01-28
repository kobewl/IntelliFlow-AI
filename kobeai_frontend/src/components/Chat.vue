<template>
  <div class="chat-container">
    <!-- 消息列表 -->
    <div class="message-list">
      <VirtualMessageList
        :messages="messages"
        :has-more="hasMore"
        :loading-more="loadingMore"
        @load-more="loadMoreMessages"
      />
    </div>

    <!-- 消息输入区域 -->
    <div class="message-input-wrapper">
      <div class="message-input">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="输入消息，Enter发送，Shift+Enter换行"
          resize="none"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="input-actions">
          <el-button type="primary" @click="sendMessage" :loading="sending">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.3s ease;
  border-radius: 24px;
  overflow: hidden;
  margin: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

body.settings-open .chat-container {
  filter: blur(1px);
  opacity: 0.85;
  transform: scale(0.98);
}

.message-list {
  flex: 1;
  overflow: hidden;
  padding: 20px;
  background: var(--el-fill-color-light);
  border-radius: 24px 24px 0 0;
}

.message-input-wrapper {
  position: relative;
  padding: 20px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  transition: all 0.3s ease;
  border-radius: 0 0 24px 24px;
}

body.settings-open .message-input-wrapper {
  opacity: 0.15;
  filter: grayscale(100%);
  transform: translateY(8px);
  pointer-events: none;
}

.message-input {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  transition: all 0.3s ease;
  background: var(--el-fill-color-blank);
  border-radius: 16px;
  padding: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.message-input :deep(.el-textarea__inner) {
  min-height: 60px !important;
  resize: none;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  background: var(--el-fill-color-light);
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.input-actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
}

.input-actions :deep(.el-button) {
  border-radius: 12px;
  padding: 12px 24px;
  height: auto;
}

/* 深色模式适配 */
:deep(.dark) .message-input-wrapper {
  border-top-color: var(--el-border-color-darker);
  background: var(--el-bg-color-overlay);
}

:deep(.dark) .message-list {
  background: var(--el-fill-color-dark);
}

:deep(.dark) .message-input {
  background: var(--el-bg-color);
}

:deep(.dark) .message-input :deep(.el-textarea__inner) {
  background: var(--el-fill-color-darker);
}
</style> 