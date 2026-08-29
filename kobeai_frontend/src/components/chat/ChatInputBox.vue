<template>
  <div class="input-area">
    <div class="input-box">
      <el-input
        ref="inputRef"
        v-model="inputText"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 5 }"
        :placeholder="inputPlaceholder"
        :disabled="loading"
        @keydown.enter.exact.prevent="emit('send')"
        @keydown.enter.shift.exact="inputText += '\n'"
        @keydown.escape="inputText = ''"
      />
      <div class="input-actions">
        <div class="input-hints">
          <span class="hint" v-if="inputText.startsWith('/')">
            <span class="hint-cmd" @click="emit('command', '/help')">/help</span>
            <span class="hint-cmd" @click="emit('command', '/clear')">/clear</span>
            <span class="hint-cmd" @click="emit('command', '/tools')">/tools</span>
          </span>
          <span class="hint" v-else>Enter 发送 · Shift+Enter 换行 · Esc 清空</span>
        </div>
        <div class="input-right">
          <span class="char-count" v-if="inputText.length > 500">
            {{ inputText.length }}
          </span>
          <el-button
            v-if="loading"
            type="danger"
            plain
            size="small"
            @click="emit('stop')"
          >
            <el-icon><CloseBold /></el-icon>
            停止
          </el-button>
          <el-button
            v-else
            type="primary"
            :disabled="!inputText.trim()"
            @click="emit('send')"
          >
            <el-icon><Position /></el-icon>
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Position, CloseBold } from '@element-plus/icons-vue'

const props = defineProps<{ loading: boolean }>()

const inputText = defineModel<string>({ required: true })

const emit = defineEmits<{
  (e: 'send'): void
  (e: 'stop'): void
  (e: 'command', cmd: string): void
}>()

const inputRef = ref<any>(null)

const inputPlaceholder = computed(() =>
  props.loading ? 'Agent 正在回复中...' : '输入消息，Enter 发送，Shift+Enter 换行'
)

/** 供父组件在新建会话后聚焦输入框 */
function focus() {
  inputRef.value?.focus()
}

defineExpose({ focus })
</script>

<style lang="scss" scoped>
@use '../../styles/chat-vars' as *;

// ---- 输入区 ----
.input-area {
  padding: 16px 24px 24px;
  flex-shrink: 0;
}

.input-box {
  max-width: 820px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid $border;
  border-radius: 16px;
  padding: 6px 6px 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  transition: border-color 0.2s, box-shadow 0.2s;

  &:focus-within {
    border-color: $accent;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
  }

  :deep(.el-textarea__inner) {
    border: none !important;
    box-shadow: none !important;
    padding: 8px 12px;
    font-size: 14px;
    line-height: 1.6;
    resize: none;
    background: transparent;
    min-height: 24px;
  }
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
}

.input-hints {
  font-size: 11px;
  color: $text-3;
}

.hint-cmd {
  display: inline-block;
  padding: 2px 6px;
  margin: 0 2px;
  border-radius: 4px;
  background: #f3f4f6;
  cursor: pointer;
  font-family: var(--font-mono, monospace);
  color: $accent;
  font-size: 11px;

  &:hover { background: $accent-light; }
}

.input-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.char-count {
  font-size: 11px;
  color: #f59e0b;
}

// ---- 响应式 ----
@media (max-width: 768px) {
  .input-area { padding: 12px 16px; }
}
</style>
