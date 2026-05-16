<template>
  <div class="markdown-renderer">
    <MarkdownRender :content="preprocessMarkdown(content)" />
  </div>
</template>

<script setup lang="ts">
import MarkdownRender from 'markstream-vue'
import 'markstream-vue/index.css'

defineProps<{
  content: string
}>()

/**
 * 预处理 Markdown：修复 LLM 常见的格式问题
 * - 确保标题前有换行（处理 "text—##" 等情况）
 * - 确保引用 > 前有换行
 */
function preprocessMarkdown(content: string): string {
  let text = content
  // 标题前缺少换行
  text = text.replace(/([^\n])(#{1,6}\s)/g, '$1\n\n$2')
  // 引用标记前缺少换行
  text = text.replace(/([^\n>])(>\s)/g, '$1\n\n$2')
  return text
}
</script>

<style scoped>
.markdown-renderer {
  font-size: 14px;
  line-height: 1.75;
  color: inherit;
}

/* markstream-vue 容器样式微调 */
.markdown-renderer :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.markdown-renderer :deep(th),
.markdown-renderer :deep(td) {
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
}

.markdown-renderer :deep(blockquote) {
  margin: 6px 0;
  padding: 4px 12px;
  border-left: 3px solid #6366f1;
  background: rgba(99, 102, 241, 0.05);
  border-radius: 2px;
}

.markdown-renderer :deep(a) {
  color: #6366f1;
}

.markdown-renderer :deep(a:hover) {
  color: #8b5cf6;
}

.markdown-renderer :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}
</style>
