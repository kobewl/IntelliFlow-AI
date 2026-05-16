<template>
  <div class="workbench" v-if="steps.length > 0">
    <div class="workbench-header" @click="expanded = !expanded">
      <div class="header-left">
        <el-icon class="header-icon"><Clock /></el-icon>
        <span class="header-title">Agent 工作台</span>
        <span class="step-count">{{ steps.length }} 个步骤</span>
        <span class="total-duration" v-if="totalDuration">{{ totalDuration }}</span>
      </div>
      <el-icon class="expand-icon" :class="{ expanded }">
        <ArrowDown />
      </el-icon>
    </div>

    <transition name="collapse">
      <div class="workbench-body" v-show="expanded">
        <div class="timeline">
          <div
            v-for="(step, i) in steps"
            :key="i"
            class="step"
            :class="step.type"
          >
            <!-- 时间线节点 -->
            <div class="step-node">
              <div class="node-dot" :class="step.type">
                <el-icon v-if="step.type === 'thinking'" class="dot-icon"><Loading /></el-icon>
                <el-icon v-else class="dot-icon"><Tools /></el-icon>
              </div>
              <div class="node-line" v-if="i < steps.length - 1"></div>
            </div>

            <!-- 步骤内容 -->
            <div class="step-body">
              <div class="step-header" @click="toggleStep(i)">
                <span class="step-type-badge" :class="step.type">
                  {{ step.type === 'thinking' ? '思考' : '工具调用' }}
                </span>
                <span class="step-name" v-if="step.name">{{ step.name }}</span>
                <span class="step-duration" v-if="stepDuration(step)">
                  {{ stepDuration(step) }}
                </span>
                <el-icon class="step-expand" :class="{ open: openSteps.has(i) }">
                  <ArrowDown />
                </el-icon>
              </div>

              <!-- 思考内容 -->
              <div class="step-content" v-if="step.type === 'thinking' && openSteps.has(i)">
                <div class="thinking-content">{{ step.content }}</div>
              </div>

              <!-- 工具调用详情 -->
              <div class="step-content" v-if="step.type === 'tool_call' && openSteps.has(i)">
                <div class="tool-detail" v-if="step.input">
                  <span class="detail-label">输入参数</span>
                  <code class="detail-code">{{ step.input }}</code>
                </div>
                <div class="tool-detail" v-if="step.output">
                  <span class="detail-label">返回结果</span>
                  <code class="detail-code">{{ step.output }}</code>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Clock, Loading, Tools, ArrowDown } from '@element-plus/icons-vue'
import type { WorkbenchStep } from '../stores/chat'

const props = defineProps<{
  steps: WorkbenchStep[]
}>()

const expanded = ref(true)
const openSteps = ref(new Set<number>())

function toggleStep(i: number) {
  if (openSteps.value.has(i)) {
    openSteps.value.delete(i)
  } else {
    openSteps.value.add(i)
  }
}

function stepDuration(step: WorkbenchStep): string {
  if (!step.endTime) return ''
  const ms = step.endTime - step.startTime
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

const totalDuration = computed(() => {
  if (props.steps.length === 0) return ''
  const first = props.steps[0].startTime
  const last = props.steps[props.steps.length - 1]
  const end = last.endTime || Date.now()
  const ms = end - first
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
})
</script>

<style lang="scss" scoped>
$accent: #6366f1;
$green: #16a34a;

.workbench {
  background: #fff;
  border: 1px solid #e8eaef;
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;
}

// ---- Header ----
.workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  background: #fafbfc;

  &:hover { background: #f3f4f6; }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 15px;
  color: $accent;
}

.header-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.step-count {
  font-size: 11px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 1px 8px;
  border-radius: 10px;
}

.total-duration {
  font-size: 11px;
  color: $accent;
  font-weight: 500;
}

.expand-icon {
  font-size: 12px;
  color: #9ca3af;
  transition: transform 0.2s;

  &.expanded { transform: rotate(180deg); }
}

// ---- Body ----
.workbench-body {
  border-top: 1px solid #f3f4f6;
  max-height: 360px;
  overflow-y: auto;
}

// ---- Timeline ----
.timeline {
  padding: 12px 14px;
}

.step {
  display: flex;
  gap: 10px;
}

// ---- Node ----
.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 20px;
  flex-shrink: 0;
}

.node-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.thinking {
    background: #eef0ff;
    color: $accent;
  }

  &.tool_call {
    background: #dcfce7;
    color: $green;
  }

  .dot-icon { font-size: 11px; }
}

.node-line {
  width: 2px;
  flex: 1;
  min-height: 14px;
  background: #e5e7eb;
  margin: 3px 0;
}

// ---- Step body ----
.step-body {
  flex: 1;
  min-width: 0;
  padding-bottom: 10px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 3px 0;
}

.step-type-badge {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 8px;
  font-weight: 500;

  &.thinking {
    background: #eef0ff;
    color: $accent;
  }

  &.tool_call {
    background: #dcfce7;
    color: $green;
  }
}

.step-name {
  font-size: 12px;
  color: #374151;
  font-weight: 500;
  font-family: var(--font-mono, monospace);
}

.step-duration {
  font-size: 11px;
  color: #9ca3af;
  margin-left: auto;
}

.step-expand {
  font-size: 10px;
  color: #d1d5db;
  transition: transform 0.15s;

  &.open { transform: rotate(180deg); }
}

// ---- Content ----
.step-content {
  margin-top: 6px;
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #f3f4f6;
}

.thinking-content {
  font-size: 12px;
  color: #4b5563;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 120px;
  overflow-y: auto;
}

.tool-detail {
  & + & { margin-top: 8px; }
}

.detail-label {
  display: block;
  font-size: 10px;
  color: #6b7280;
  margin-bottom: 3px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 500;
}

.detail-code {
  display: block;
  font-size: 11px;
  color: #1f2937;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: var(--font-mono, monospace);
  max-height: 80px;
  overflow-y: auto;
}

// ---- Transitions ----
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.2s ease;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
