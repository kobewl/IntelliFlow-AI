<template>
  <div class="markdown-renderer">
    <MarkdownRender :content="preprocessMarkdown(content)" />
  </div>
</template>

<script setup lang="ts">
import MarkdownRender from 'markstream-vue'
import 'markstream-vue/index.css'
import { preprocessMarkdown } from '../utils/markdown-preprocessor'

defineProps<{
  content: string
}>()
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
