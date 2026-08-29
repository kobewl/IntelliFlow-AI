<template>
  <div class="welcome">
    <div class="welcome-bg"></div>
    <div class="welcome-content">
      <div class="welcome-icon">
        <svg width="72" height="72" viewBox="0 0 72 72" fill="none">
          <circle cx="36" cy="36" r="36" fill="url(#welcome-grad)" />
          <circle cx="36" cy="36" r="36" fill="url(#welcome-grad2)" opacity="0.5" />
          <path d="M24 28h24M24 36h18M24 44h12" stroke="#fff" stroke-width="3" stroke-linecap="round" />
          <defs>
            <linearGradient id="welcome-grad" x1="0" y1="0" x2="72" y2="72">
              <stop stop-color="#6366f1" /><stop offset="1" stop-color="#8b5cf6" />
            </linearGradient>
            <radialGradient id="welcome-grad2" cx="0.3" cy="0.3" r="1">
              <stop stop-color="#a5b4fc" /><stop offset="1" stop-color="transparent" />
            </radialGradient>
          </defs>
        </svg>
      </div>
      <h1>{{ greetingText }}</h1>
      <p class="welcome-sub">我可以帮你编程、计算、搜索知识库，试试下面的问题吧</p>

      <!-- 快捷提示 -->
      <div class="quick-prompts">
        <div
          v-for="prompt in quickPrompts"
          :key="prompt.text"
          class="quick-prompt"
          @click="emit('prompt', prompt.text)"
        >
          <span class="prompt-icon">{{ prompt.icon }}</span>
          <span class="prompt-text">{{ prompt.text }}</span>
        </div>
      </div>

      <!-- 工具能力展示 -->
      <div class="capability-bar" v-if="toolInfos.length > 0">
        <span class="cap-title">可用能力</span>
        <div class="cap-tags">
          <span v-for="t in topTools" :key="t.name" class="cap-tag">{{ t.description }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ToolInfo } from '../../api/chat'

const props = defineProps<{ toolInfos: ToolInfo[] }>()

const emit = defineEmits<{
  (e: 'prompt', text: string): void
}>()

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，早点休息'
  if (h < 12) return '早上好，有什么我可以帮你的？'
  if (h < 14) return '中午好，需要我帮你做些什么？'
  if (h < 18) return '下午好，一起开始高效工作吧'
  return '晚上好，有什么需要我帮忙的？'
})

const topTools = computed(() => props.toolInfos.slice(0, 4))

const quickPrompts = [
  { icon: '⏰', text: '现在几点了？帮我算一下 3^10' },
  { icon: '💻', text: '写一个 Python 快速排序算法' },
  { icon: '📊', text: '帮我分析一下这个项目的架构' },
  { icon: '🔍', text: '搜索知识库中关于 Spring Boot 的内容' }
]
</script>

<style lang="scss" scoped>
@use '../../styles/chat-vars' as *;

// ---- 欢迎页 ----
.welcome {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.welcome-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 50% at 50% 40%, rgba(99, 102, 241, 0.04), transparent),
    radial-gradient(ellipse 80% 60% at 50% 60%, rgba(139, 92, 246, 0.03), transparent);
  pointer-events: none;
}

.welcome-content {
  position: relative;
  text-align: center;
  max-width: 560px;
  padding: 40px 24px;
}

.welcome-icon {
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.welcome-content h1 {
  font-size: 26px;
  font-weight: 600;
  color: $text-1;
  margin: 0 0 8px;
}

.welcome-sub {
  color: $text-2;
  font-size: 15px;
  margin: 0 0 36px;
  line-height: 1.6;
}

// 快捷提示
.quick-prompts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  max-width: 520px;
  margin: 0 auto;
}

.quick-prompt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  font-size: 13px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  text-align: left;

  &:hover {
    border-color: $accent;
    color: $accent;
    box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);
    transform: translateY(-1px);
  }
}

.prompt-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.prompt-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 能力条
.capability-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.cap-title {
  font-size: 12px;
  color: $text-3;
}

.cap-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.cap-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  background: $accent-light;
  color: $accent;
}

// ---- 响应式 ----
@media (max-width: 768px) {
  .quick-prompts {
    grid-template-columns: 1fr;
  }
}
</style>
