<template>
  <div class="markdown-body" :class="{ 'dark-theme': isDarkMode }" v-html="renderedContent" />
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import 'github-markdown-css/github-markdown.css'

const props = defineProps<{
  content: string
}>()

// 检测暗色模式
const isDarkMode = computed(() => {
  return document.documentElement.classList.contains('dark')
})

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<div class="code-block-wrapper">
                  <div class="code-block-header">
                    <span class="code-lang">${lang}</span>
                  </div>
                  <pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>
                </div>`
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const renderedContent = ref('')

onMounted(() => {
  renderedContent.value = md.render(props.content)
  initializeCodeBlocks()
})

function initializeCodeBlocks() {
  const codeBlocks = document.querySelectorAll('.code-block-wrapper')
  codeBlocks.forEach(block => {
    const copyButton = document.createElement('button')
    copyButton.className = 'copy-button'
    copyButton.innerHTML = '<i class="far fa-copy"></i>'
    
    copyButton.addEventListener('click', async () => {
      const code = block.querySelector('code')?.textContent || ''
      try {
        await navigator.clipboard.writeText(code)
        copyButton.innerHTML = '<i class="fas fa-check"></i>'
        setTimeout(() => {
          copyButton.innerHTML = '<i class="far fa-copy"></i>'
        }, 2000)
      } catch (err) {
        console.error('Failed to copy:', err)
        copyButton.innerHTML = '<i class="fas fa-times"></i>'
        setTimeout(() => {
          copyButton.innerHTML = '<i class="far fa-copy"></i>'
        }, 2000)
      }
    })
    
    const header = block.querySelector('.code-block-header')
    if (header) {
      header.appendChild(copyButton)
    }
  })
}

defineOptions({
  name: 'MarkdownRenderer'
})
</script>

<script lang="ts">
export default {
  name: 'MarkdownRenderer'
}
</script>

<style>
.markdown-body {
  box-sizing: border-box;
  min-width: 200px;
  max-width: 980px;
  margin: 0;
  padding: 0;
  background: transparent;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  font-size: 14px;
  line-height: 1.5;
  color: inherit;
}

/* 标题样式 */
.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin: 0.5em 0 0.25em;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: -0.01em;
}

.markdown-body h1 { font-size: 1.75em; }
.markdown-body h2 { font-size: 1.5em; }
.markdown-body h3 { font-size: 1.25em; }
.markdown-body h4 { font-size: 1.1em; }
.markdown-body h5, .markdown-body h6 { font-size: 1em; }

/* 段落和列表样式 */
.markdown-body p {
  margin: 0.25em 0;
  line-height: 1.5;
}

.markdown-body ul,
.markdown-body ol {
  margin: 0.25em 0;
  padding-left: 1.5em;
}

.markdown-body li {
  margin: 0.125em 0;
  line-height: 1.5;
}

.markdown-body li > p {
  margin: 0;
}

/* 链接样式 */
.markdown-body a {
  color: #0085FF;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s ease;
}

.markdown-body a:hover {
  color: #00B2FF;
  border-bottom-color: currentColor;
}

/* 代码块样式 */
.code-block-wrapper {
  margin: 0.5em 0;
  border-radius: 8px;
  overflow: hidden;
  background: #1E1E1E;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.code-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #2C2C2C;
  border-bottom: 1px solid #3C3C3C;
}

.code-lang {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #888;
}

.copy-button {
  padding: 4px 8px;
  font-size: 12px;
  color: #888;
  background: transparent;
  border: 1px solid #3C3C3C;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-button:hover {
  color: #fff;
  border-color: #888;
  background: rgba(255, 255, 255, 0.1);
}

.markdown-body pre {
  margin: 0;
  padding: 12px;
  overflow-x: auto;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #D4D4D4;
}

.markdown-body code {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.9em;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.05);
}

/* 引用块样式 */
.markdown-body blockquote {
  margin: 0.5em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #0085FF;
  background: rgba(0, 133, 255, 0.05);
  border-radius: 4px;
}

.markdown-body blockquote p {
  margin: 0;
}

/* 表格样式 */
.markdown-body table {
  width: 100%;
  margin: 0.5em 0;
  border-collapse: separate;
  border-spacing: 0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.markdown-body th,
.markdown-body td {
  padding: 8px 12px;
  border: 1px solid #eee;
}

.markdown-body th {
  background: #f5f5f5;
  font-weight: 600;
}

.markdown-body tr:nth-child(even) {
  background: #fafafa;
}

/* 图片样式 */
.markdown-body img {
  max-width: 100%;
  margin: 0.5em 0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.markdown-body img:hover {
  transform: scale(1.02);
}

/* 深色模式适配 */
.dark-theme {
  color: #fff;
}

.dark-theme a {
  color: #00B2FF;
}

.dark-theme a:hover {
  color: #40C9FF;
}

.dark-theme code {
  background: rgba(255, 255, 255, 0.1);
}

.dark-theme blockquote {
  background: rgba(0, 133, 255, 0.1);
}

.dark-theme table {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dark-theme th {
  background: #2C2C2C;
  border-color: #3C3C3C;
}

.dark-theme td {
  border-color: #3C3C3C;
}

.dark-theme tr:nth-child(even) {
  background: #2C2C2C;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .markdown-body {
    font-size: 13px;
  }

  .markdown-body pre,
  .markdown-body code {
    font-size: 12px;
  }

  .markdown-body blockquote {
    padding: 0.4em 0.8em;
  }

  .markdown-body th,
  .markdown-body td {
    padding: 6px 8px;
  }
}

/* 滚动条美化 */
.markdown-body ::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.markdown-body ::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.markdown-body ::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.dark-theme ::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
}

.dark-theme ::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}
</style> 