<template>
  <div class="message-content" :class="{ 'loading': loading }">
    <!-- 使用 v-if/v-else 避免不必要的 DOM 操作 -->
    <div v-if="loading" class="message-loading">
      <div class="dot-flashing"></div>
    </div>
    <div v-else-if="error" class="message-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="retryRender" type="primary">重试</el-button>
    </div>
    <!-- 使用 v-html 渲染处理后的 HTML -->
    <div v-else ref="contentRef" class="message-html" v-html="renderedContent"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import { processMarkdown } from '../utils/markdownProcessor'

// 组件属性
const props = defineProps({
  content: {
    type: String,
    required: true
  },
  useMarkdown: {
    type: Boolean,
    default: true
  }
})

// 内部状态
const contentRef = ref<HTMLElement | null>(null)
const renderedContent = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

// 渲染内容
async function renderContent() {
  // 如果内容为空，直接返回
  if (!props.content.trim()) {
    renderedContent.value = ''
    return
  }
  
  loading.value = true
  error.value = null
  
  try {
    if (props.useMarkdown) {
      // 使用 Worker 处理 Markdown
      renderedContent.value = await processMarkdown(props.content)
    } else {
      // 简单的文本处理
      renderedContent.value = props.content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>')
    }
    
    // 渲染完成后处理代码块
    await nextTick()
    if (contentRef.value) {
      setupCodeBlocks(contentRef.value)
    }
  } catch (err: any) {
    error.value = err.message || '渲染内容失败'
    console.error('Failed to render message:', err)
  } finally {
    loading.value = false
  }
}

// 重试渲染
function retryRender() {
  renderContent()
}

// 设置代码块交互效果
function setupCodeBlocks(element: HTMLElement) {
  // 查找所有代码块
  const codeBlocks = element.querySelectorAll('pre code')
  
  codeBlocks.forEach((block) => {
    // 添加复制按钮
    const copyButton = document.createElement('button')
    copyButton.className = 'copy-button'
    copyButton.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>'
    copyButton.title = '复制代码'
    
    // 获取代码块的父级元素
    const preElement = block.parentElement
    if (preElement) {
      preElement.style.position = 'relative'
      preElement.appendChild(copyButton)
      
      // 添加点击事件监听器
      copyButton.addEventListener('click', () => {
        const code = block.textContent || ''
        copyToClipboard(code)
        
        // 显示复制成功反馈
        copyButton.classList.add('copied')
        copyButton.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>'
        
        // 2秒后恢复原状
        setTimeout(() => {
          copyButton.classList.remove('copied')
          copyButton.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>'
        }, 2000)
      })
    }
  })
}

// 复制文本到剪贴板
function copyToClipboard(text: string) {
  // 使用 Clipboard API (如果支持)
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text)
      .catch(err => console.error('复制到剪贴板失败:', err))
    return
  }
  
  // 回退方法
  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  } catch (err) {
    console.error('复制到剪贴板失败:', err)
  }
}

// 监听内容变化
watch(() => props.content, () => {
  renderContent()
}, { immediate: true })

// 组件挂载时渲染内容
onMounted(() => {
  renderContent()
})
</script>

<script lang="ts">
export default {
  name: 'MessageRenderer'
}
</script>

<style scoped>
.message-content {
  font-size: 15px;
  line-height: 1.6;
  overflow-wrap: break-word;
  transition: all 0.3s ease;
}

.message-html {
  transition: all 0.3s ease;
}

/* 美化代码块 */
:deep(pre) {
  background-color: #f8f9fa;
  border-radius: 10px;
  padding: 16px;
  margin: 16px 0;
  overflow-x: auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

:deep(pre code) {
  font-family: 'JetBrains Mono', 'Fira Code', Monaco, Menlo, Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  tab-size: 2;
}

:deep(:not(pre) > code) {
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', Monaco, Menlo, Consolas, 'Courier New', monospace;
  font-size: 0.9em;
  color: var(--el-color-primary, #409eff);
}

/* 深色模式代码块 */
:global(.dark-mode) :deep(pre) {
  background-color: #1e1e2e;
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

:global(.dark-mode) :deep(:not(pre) > code) {
  background-color: rgba(255, 255, 255, 0.1);
  color: #7aa2f7;
}

/* 美化链接 */
:deep(a) {
  color: var(--el-color-primary, #409eff);
  text-decoration: none;
  border-bottom: 1px dashed var(--el-color-primary-light-5, #a0cfff);
  transition: all 0.2s ease;
}

:deep(a:hover) {
  color: var(--el-color-primary-light-3, #79bbff);
  border-bottom: 1px solid var(--el-color-primary-light-3, #79bbff);
}

/* 深色模式链接 */
:global(.dark-mode) :deep(a) {
  color: #7aa2f7;
  border-bottom-color: rgba(122, 162, 247, 0.4);
}

:global(.dark-mode) :deep(a:hover) {
  color: #9aadf7;
  border-bottom-color: rgba(154, 173, 247, 0.6);
}

/* 美化列表 */
:deep(ul), :deep(ol) {
  padding-left: 1.5em;
  margin: 1em 0;
}

:deep(li) {
  margin-bottom: 0.5em;
}

:deep(li::marker) {
  color: var(--el-color-primary, #409eff);
}

:global(.dark-mode) :deep(li::marker) {
  color: #7aa2f7;
}

/* 美化引用块 */
:deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid var(--el-color-primary-light-5, #a0cfff);
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  border-radius: 4px;
  color: var(--el-text-color-secondary, #909399);
  font-style: italic;
}

:global(.dark-mode) :deep(blockquote) {
  border-left-color: rgba(122, 162, 247, 0.5);
  background-color: rgba(122, 162, 247, 0.1);
  color: #9aa5ce;
}

/* 美化表格 */
:deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

:deep(th) {
  background-color: var(--el-color-primary-light-8, #d9ecff);
  color: var(--el-color-primary-dark-2, #337ecc);
  font-weight: 600;
  padding: 12px 16px;
  text-align: left;
}

:deep(td) {
  padding: 10px 16px;
  border-top: 1px solid var(--el-border-color-lighter, #ebeef5);
}

:deep(tr:nth-child(2n)) {
  background-color: var(--el-fill-color-lighter, #f5f7fa);
}

:global(.dark-mode) :deep(table) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

:global(.dark-mode) :deep(th) {
  background-color: rgba(122, 162, 247, 0.2);
  color: #7aa2f7;
}

:global(.dark-mode) :deep(td) {
  border-top-color: rgba(255, 255, 255, 0.1);
}

:global(.dark-mode) :deep(tr:nth-child(2n)) {
  background-color: rgba(255, 255, 255, 0.03);
}

/* 加载动画 */
.message-loading {
  display: flex;
  padding: 12px 0;
  align-items: center;
}

.dot-flashing {
  position: relative;
  width: 10px;
  height: 10px;
  border-radius: 5px;
  background-color: var(--el-color-primary, #409eff);
  color: var(--el-color-primary, #409eff);
  animation: dot-flashing 1s infinite linear alternate;
  animation-delay: 0.5s;
}

.dot-flashing::before, .dot-flashing::after {
  content: '';
  display: inline-block;
  position: absolute;
  top: 0;
}

.dot-flashing::before {
  left: -15px;
  width: 10px;
  height: 10px;
  border-radius: 5px;
  background-color: var(--el-color-primary, #409eff);
  color: var(--el-color-primary, #409eff);
  animation: dot-flashing 1s infinite alternate;
  animation-delay: 0s;
}

.dot-flashing::after {
  left: 15px;
  width: 10px;
  height: 10px;
  border-radius: 5px;
  background-color: var(--el-color-primary, #409eff);
  color: var(--el-color-primary, #409eff);
  animation: dot-flashing 1s infinite alternate;
  animation-delay: 1s;
}

@keyframes dot-flashing {
  0% {
    background-color: var(--el-color-primary, #409eff);
  }
  50%, 100% {
    background-color: rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.2);
  }
}

/* 错误消息 */
.message-error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-danger, #f56c6c);
  padding: 12px 16px;
  background-color: var(--el-color-danger-light-9, #fef0f0);
  border-radius: 8px;
  margin: 8px 0;
}

:global(.dark-mode) .message-error {
  background-color: rgba(245, 108, 108, 0.1);
}
</style> 