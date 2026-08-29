<template>
  <div class="messages-container" ref="msgContainerRef">
    <div v-if="loadingMore" class="load-more">
      <el-button text size="small" :loading="loadingMore" @click="chatStore.loadMoreMessages()">
        加载更早的消息
      </el-button>
    </div>

    <div
      v-for="(msg, index) in messages"
      :key="msg.id"
      class="message-row"
      :class="msg.role"
    >
      <!-- 用户消息 -->
      <template v-if="msg.role === 'user'">
        <div class="user-bubble">
          <div class="bubble-text">{{ msg.content }}</div>
          <span class="bubble-time">{{ formatMsgTime(msg.createdAt) }}</span>
        </div>
        <div class="user-msg-actions">
          <el-tooltip content="从这里分叉对话" placement="left">
            <el-button
              class="fork-btn"
              size="small"
              circle
              text
              @click="emit('fork', index)"
              :disabled="loading"
            >
              <el-icon><Share /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        <el-avatar :size="30" :src="authStore.user?.avatar || '/ai-avatar.png'" class="msg-avatar" />
      </template>

      <!-- Agent 消息 -->
      <template v-else>
        <div class="agent-avatar">
          <svg width="30" height="30" viewBox="0 0 30 30" fill="none">
            <rect width="30" height="30" rx="8" fill="url(#ai-grad)" />
            <path d="M8 10h14M8 15h10M8 20h7" stroke="#fff" stroke-width="1.5" stroke-linecap="round" />
            <defs>
              <linearGradient id="ai-grad" x1="0" y1="0" x2="30" y2="30">
                <stop stop-color="#6366f1" /><stop offset="1" stop-color="#8b5cf6" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="agent-content">
          <!-- 思考过程卡片 -->
          <div
            v-if="msg.reasoning.length > 0 || msg.isThinking"
            class="thinking-card"
            :class="{ collapsed: !msg._thinkingOpen }"
          >
            <div class="thinking-header" @click="msg._thinkingOpen = !msg._thinkingOpen">
              <div class="thinking-header-left">
                <span class="thinking-dot" :class="{ active: msg.isThinking }"></span>
                <span class="thinking-label">
                  {{ msg.isThinking ? '深度思考中...' : '思考过程' }}
                </span>
              </div>
              <el-icon class="expand-icon" :class="{ expanded: msg._thinkingOpen }">
                <ArrowDown />
              </el-icon>
            </div>
            <transition name="collapse">
              <div class="thinking-body" v-show="msg._thinkingOpen">
                <div class="thinking-text">{{ msg.reasoning }}</div>
                <div v-if="msg.isThinking" class="thinking-pending">
                  <span class="step-loading"></span>
                  <span class="step-text dim">思考中...</span>
                </div>
              </div>
            </transition>
          </div>

          <!-- 工作台时间线 -->
          <AgentWorkbench :steps="msg.steps" />

          <!-- 工具执行中状态指示器 -->
          <div v-if="hasRunningTool(msg)" class="tool-running-bar">
            <span class="running-spinner"></span>
            <span class="running-text">正在执行 {{ runningToolName(msg) }}...</span>
          </div>

          <!-- 工具调用结果卡片 -->
          <div
            v-for="(tool, ti) in msg.toolCalls"
            :key="'tool-' + ti"
            class="tool-card"
            :class="{ 'is-error': tool.status === 'error' }"
          >
            <div class="tool-header" @click="toggleToolResult(tool)">
              <el-icon class="tool-icon" :class="{ spinning: tool.status === 'running' }">
                <Loading v-if="tool.status === 'running'" />
                <Tools v-else />
              </el-icon>
              <span class="tool-name">{{ tool.name }}</span>
              <span class="tool-badge" :class="tool.status">
                {{ toolStatusText(tool) }}
              </span>
              <el-icon class="tool-expand" :class="{ open: isToolExpanded(tool) }">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="tool-body" v-show="isToolExpanded(tool)">
              <div v-if="tool.input" class="tool-section">
                <span class="tool-label">输入参数</span>
                <code class="tool-code">{{ tool.input }}</code>
              </div>
              <div v-if="tool.output" class="tool-section">
                <span class="tool-label">返回结果</span>
                <code class="tool-code" :class="{ 'collapsed': isLongOutput(tool.output) && !expandedOutputs.has(tool.toolCallId || '') }">
                  {{ tool.output }}
                </code>
                <span
                  v-if="isLongOutput(tool.output)"
                  class="expand-output-btn"
                  @click="toggleOutputExpand(tool.toolCallId || '')"
                >
                  {{ expandedOutputs.has(tool.toolCallId || '') ? '收起结果' : '展开完整结果' }}
                  <el-icon><ArrowDown /></el-icon>
                </span>
              </div>
            </div>
          </div>

          <!-- 最终回复 -->
          <div v-if="msg.content" class="agent-bubble" :class="{ streaming: msg.isStreaming }">
            <MarkdownRenderer :content="msg.content" />
            <span class="bubble-time agent-time">{{ formatMsgTime(msg.createdAt) }}</span>
          </div>

          <!-- 错误 -->
          <div v-if="msg.error" class="agent-error">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ msg.error }}</span>
          </div>
        </div>
      </template>
    </div>

    <div ref="scrollAnchorRef"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { Loading, ArrowDown, WarningFilled, Tools, Share } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import MarkdownRenderer from '../MarkdownRenderer.vue'
import AgentWorkbench from '../AgentWorkbench.vue'
import dayjs from 'dayjs'
import type { ToolCall } from '../../api/chat'

dayjs.locale('zh-cn')

const authStore = useAuthStore()
const chatStore = useChatStore()

const { messages, loading, loadingMore } = storeToRefs(chatStore)

const msgContainerRef = ref<HTMLElement | null>(null)
const scrollAnchorRef = ref<HTMLElement | null>(null)
const expandedToolIds = ref(new Set<string>())
const expandedOutputs = ref(new Set<string>())

function formatMsgTime(t: string) {
  return dayjs(t).format('HH:mm')
}

// ---- 自动滚动 ----
function scrollToBottom() {
  nextTick(() => {
    scrollAnchorRef.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

watch(() => messages.value.length, scrollToBottom)
watch(() => {
  const last = messages.value[messages.value.length - 1]
  return last?.content?.length ?? 0
}, scrollToBottom)

// ---- 工具状态辅助 ----

function hasRunningTool(msg: { steps?: Array<{ type?: string; status?: string }> }): boolean {
  return msg.steps?.some((s) => s.type === 'tool_call' && s.status === 'running') || false
}

function runningToolName(msg: { steps?: Array<{ type?: string; status?: string; name?: string }> }): string {
  const step = msg.steps?.find((s) => s.type === 'tool_call' && s.status === 'running')
  return step?.name || '工具'
}

function toolStatusText(tool: ToolCall): string {
  if (tool.status === 'running') return '执行中'
  if (tool.status === 'error') return '失败'
  return '完成'
}

function toggleToolResult(tool: ToolCall) {
  const id = tool.toolCallId || tool.name
  if (expandedToolIds.value.has(id)) {
    expandedToolIds.value.delete(id)
  } else {
    expandedToolIds.value.add(id)
  }
}

function isToolExpanded(tool: ToolCall): boolean {
  const id = tool.toolCallId || tool.name
  return expandedToolIds.value.has(id)
}

function isLongOutput(output: string): boolean {
  return output.length > 300
}

function toggleOutputExpand(toolCallId: string) {
  if (expandedOutputs.value.has(toolCallId)) {
    expandedOutputs.value.delete(toolCallId)
  } else {
    expandedOutputs.value.add(toolCallId)
  }
}

const emit = defineEmits<{
  (e: 'fork', index: number): void
}>()
</script>

<style lang="scss" scoped>
@use '../../styles/chat-vars' as *;

// ---- 消息区域 ----
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

.load-more {
  text-align: center;
  padding: 8px 0 16px;
}

// 消息行
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 28px;
  max-width: 820px;
  animation: fadeInUp 0.3s ease;

  &.user {
    margin-left: auto;
    flex-direction: row-reverse;
  }

  &.agent {
    margin-right: auto;
  }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

// 用户消息
.user-bubble {
  background: linear-gradient(135deg, $accent, #7c3aed);
  color: #fff;
  padding: 10px 16px;
  border-radius: 14px 4px 14px 14px;
  font-size: 14px;
  line-height: 1.6;
  max-width: 70%;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

.bubble-time {
  display: block;
  font-size: 10px;
  opacity: 0.7;
  margin-top: 4px;
  text-align: right;
}

.msg-avatar {
  flex-shrink: 0;
  align-self: flex-end;
}

// 用户消息操作按钮
.user-msg-actions {
  display: flex;
  align-items: center;
  opacity: 0;
  transition: opacity 0.15s;

  .message-row:hover & {
    opacity: 1;
  }
}

.fork-btn {
  color: #9ca3af;
  font-size: 15px;
  padding: 4px;

  &:hover { color: $accent; background: $accent-light; }
}

// Agent 消息
.agent-avatar {
  flex-shrink: 0;
}

.agent-content {
  flex: 1;
  min-width: 0;
}

// 思考卡片
.thinking-card {
  background: #fff;
  border: 1px solid $border;
  border-left: 3px solid $accent;
  border-radius: 0 10px 10px 0;
  margin-bottom: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
}

.thinking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
}

.thinking-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thinking-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $accent;
  flex-shrink: 0;

  &.active {
    animation: pulse 1.2s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.3); }
}

.thinking-label {
  font-size: 13px;
  color: $text-2;
  font-weight: 500;
}

.expand-icon {
  font-size: 12px;
  color: $text-3;
  transition: transform 0.2s;

  &.expanded { transform: rotate(180deg); }
}

.thinking-body {
  padding: 12px 14px;
  border-top: 1px solid #f3f4f6;
  max-height: 240px;
  overflow-y: auto;
}

.thinking-text {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.thinking-pending {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}

.step-loading {
  width: 14px;
  height: 14px;
  border: 2px solid #e5e7eb;
  border-top-color: $accent;
  border-radius: 50%;
  flex-shrink: 0;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.step-text {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;

  &.dim { color: $text-3; font-style: italic; }
}

// 工具调用卡片
.tool-card {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;

  &.is-error {
    background: #fef2f2;
    border-color: #fecaca;
  }
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid #dcfce7;
  cursor: pointer;
  user-select: none;

  .tool-card.is-error & {
    border-bottom-color: #fecaca;
  }
}

.tool-icon {
  color: #16a34a;
  font-size: 16px;
  flex-shrink: 0;

  &.spinning {
    animation: spin 0.8s linear infinite;
  }

  .tool-card.is-error & {
    color: #dc2626;
  }
}

.tool-name {
  font-size: 13px;
  font-weight: 500;
  color: #166534;

  .tool-card.is-error & {
    color: #991b1b;
  }
}

.tool-expand {
  font-size: 12px;
  color: #9ca3af;
  margin-left: auto;
  transition: transform 0.2s;

  &.open { transform: rotate(180deg); }
}

.tool-badge {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  background: #dcfce7;
  color: #16a34a;

  &.running {
    background: #fef3c7;
    color: #d97706;
    animation: badgePulse 1.5s ease-in-out infinite;
  }

  &.error {
    background: #fecaca;
    color: #dc2626;
  }
}

@keyframes badgePulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.tool-body {
  padding: 10px 14px;
}

.tool-section {
  & + & { margin-top: 8px; }
}

.tool-label {
  display: block;
  font-size: 10px;
  color: #15803d;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

.tool-code {
  display: block;
  background: #fff;
  border: 1px solid #dcfce7;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: #14532d;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 120px;
  overflow-y: auto;
  font-family: var(--font-mono, monospace);

  &.collapsed {
    max-height: 72px;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 36px;
      background: linear-gradient(transparent, #fff);
      pointer-events: none;
    }
  }
}

// 展开结果按钮
.expand-output-btn {
  font-size: 11px;
  color: $accent;
  margin-top: 4px;
  padding: 2px 4px;

  .el-icon {
    font-size: 10px;
    margin-left: 2px;
    transition: transform 0.2s;
  }

  &:hover {
    color: #5558e6;
    background: $accent-light;
  }
}

// 工具执行中状态栏
.tool-running-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  margin-bottom: 8px;
}

.running-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #fde68a;
  border-top-color: #f59e0b;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

.running-text {
  font-size: 13px;
  color: #92400e;
}

// Agent 最终回复
.agent-bubble {
  background: #fff;
  border: 1px solid $border;
  border-radius: 4px 14px 14px 14px;
  padding: 16px 20px;
  font-size: 14px;
  line-height: 1.75;
  color: $text-1;
  transition: border-color 0.2s;

  &.streaming { border-color: $accent; }
}

.agent-time {
  display: block;
  margin-top: 8px;
  text-align: left;
}

// 错误
.agent-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #dc2626;
  font-size: 13px;
}

// ---- 过渡 ----
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.2s ease;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

// ---- 响应式 ----
@media (max-width: 768px) {
  .messages-container { padding: 16px; }

  .message-row {
    max-width: 100%;

    &.user .user-bubble { max-width: 85%; }
  }
}
</style>
