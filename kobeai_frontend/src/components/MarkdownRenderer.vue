<template>
  <div class="markdown-body" :class="{ 'dark-theme': isDarkMode }" v-html="renderedContent" />
</template>

<script setup lang="ts">
import { onMounted, ref, computed, nextTick, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import Prism from 'prismjs'
// 导入 Prism 核心样式
import 'prismjs/themes/prism-tomorrow.css'
// 导入额外的语言支持
import 'prismjs/components/prism-python'
import 'prismjs/components/prism-javascript'
import 'prismjs/components/prism-typescript'
import 'prismjs/components/prism-java'
import 'prismjs/components/prism-c'
import 'prismjs/components/prism-cpp'
import 'prismjs/components/prism-csharp'
import 'prismjs/components/prism-go'
import 'prismjs/components/prism-rust'
import 'prismjs/components/prism-sql'
import 'prismjs/components/prism-markup'
import 'prismjs/components/prism-css'
import 'prismjs/components/prism-scss'
import 'prismjs/components/prism-json'
import 'prismjs/components/prism-yaml'
import 'prismjs/components/prism-markdown'
// 导入插件
import 'prismjs/plugins/line-numbers/prism-line-numbers'
import 'prismjs/plugins/line-numbers/prism-line-numbers.css'
import 'prismjs/plugins/toolbar/prism-toolbar'
import 'prismjs/plugins/toolbar/prism-toolbar.css'
import 'prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard'
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
    if (lang && Prism.languages[lang]) {
      try {
        // 确保语言小写
        lang = lang.toLowerCase()
        const code = Prism.highlight(str, Prism.languages[lang], lang)
        return `<div class="code-block-wrapper ${lang}">
                  <div class="code-block-header">
                    <span class="code-lang">${lang}</span>
                  </div>
                  <pre class="line-numbers"><code class="language-${lang}">${code}</code></pre>
                </div>`
      } catch (err) {
        console.error('Prism highlight error:', err)
      }
    }
    // 如果没有指定语言或者发生错误，返回未格式化的代码
    return `<pre class="line-numbers"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

// 配置额外的 markdown-it 选项
md.configure({
  breaks: true,      // 转换换行符为 <br>
  linkify: true,     // 自动转换URL为链接
  html: true,        // 允许HTML标签
  typographer: true, // 启用一些语言中性的替换和引号美化
  quotes: '""'''     // 设置引号样式
})

const renderedContent = ref('')

// 监听内容变化
watch(
  () => props.content,
  (newContent: string) => {
    renderedContent.value = md.render(newContent)
    nextTick(() => {
      Prism.highlightAll()
    })
  },
  { immediate: true }
)

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
/* 基础样式 */
.markdown-body {
  box-sizing: border-box;
  min-width: 200px;
  max-width: none;
  margin: 0;
  padding: 0;
  background: transparent;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  font-size: 14px;
  line-height: 1.4;
  color: inherit;
  word-break: break-word;
  white-space: pre-wrap;
}

/* 标题样式 */
.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin: 8px 0 4px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: -0.01em;
}

.markdown-body h1 { font-size: 1.5em; }
.markdown-body h2 { font-size: 1.3em; }
.markdown-body h3 { font-size: 1.2em; }
.markdown-body h4 { font-size: 1.1em; }
.markdown-body h5, .markdown-body h6 { font-size: 1em; }

/* 段落和列表样式 */
.markdown-body p {
  margin: 4px 0;
  line-height: 1.4;
}

/* 列表样式 */
.markdown-body ol,
.markdown-body ul {
  margin: 4px 0;
  padding-left: 1.5em;
  list-style-position: inside;
}

.markdown-body ol {
  list-style: decimal;
  margin-left: 0;
}

.markdown-body ul {
  list-style: disc;
  margin-left: 0;
}

.markdown-body li {
  margin: 2px 0;
  line-height: 1.6;
  text-indent: -1.5em;
  padding-left: 1.5em;
}

.markdown-body li > p {
  margin: 0;
  display: inline-block;
  text-indent: 0;
}

.markdown-body li::marker {
  margin-right: 0.5em;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: inherit;
}

/* 嵌套列表的缩进 */
.markdown-body li ol,
.markdown-body li ul {
  margin-top: 2px;
  margin-bottom: 2px;
  padding-left: 1.2em;
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
  margin: 6px 0;
  border-radius: 6px;
  overflow: hidden;
  background: var(--prism-background, #2d2d2d);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  max-width: 100%;
}

.code-block-wrapper pre {
  margin: 0;
  padding: 12px;
  overflow-x: auto;
  white-space: pre;
}

.code-block-wrapper code {
  display: inline-block;
  min-width: 100%;
  font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.4;
  tab-size: 2;
}

.code-block-header {
  padding: 4px 8px;
  background: var(--prism-header-background, #21252b);
  border-bottom: 1px solid var(--prism-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code-lang {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8em;
  color: var(--prism-lang-color);
  text-transform: uppercase;
}

/* Prism 主题自定义 */
:root {
  --prism-background: #282c34;
  --prism-header-background: #21252b;
  --prism-border-color: #3e4451;
  --prism-lang-color: #abb2bf;
}

.token.comment,
.token.prolog,
.token.doctype,
.token.cdata {
  color: #5c6370;
  font-style: italic;
}

.token.function {
  color: #61afef;
}

.token.keyword {
  color: #c678dd;
}

.token.string {
  color: #98c379;
}

.token.number {
  color: #d19a66;
}

.token.operator {
  color: #56b6c2;
}

.token.class-name {
  color: #e5c07b;
}

.token.variable {
  color: #e06c75;
}

/* 行号样式 */
.line-numbers .line-numbers-rows {
  border-right: 1px solid #3e4451;
  padding: 1em 0;
}

.line-numbers .line-numbers-rows > span:before {
  color: #495162;
}

/* 亮色主题 */
.markdown-body:not(.dark-theme) {
  --prism-background: #f6f8fa;
  --prism-header-background: #f0f2f5;
  --prism-border-color: #e1e4e8;
  --prism-lang-color: #666666;
  --prism-line-number-color: #bbb;
}

.markdown-body:not(.dark-theme) .token.comment,
.markdown-body:not(.dark-theme) .token.prolog,
.markdown-body:not(.dark-theme) .token.doctype,
.markdown-body:not(.dark-theme) .token.cdata {
  color: #6a737d;
}

.markdown-body:not(.dark-theme) .token.function {
  color: #005cc5;
}

.markdown-body:not(.dark-theme) .token.keyword {
  color: #d73a49;
}

.markdown-body:not(.dark-theme) .token.string {
  color: #22863a;
}

.markdown-body:not(.dark-theme) .token.number {
  color: #e36209;
}

.markdown-body:not(.dark-theme) .token.operator {
  color: #d73a49;
}

.markdown-body:not(.dark-theme) .token.class-name {
  color: #6f42c1;
}

.markdown-body:not(.dark-theme) .token.variable {
  color: #005cc5;
}

.markdown-body:not(.dark-theme) .line-numbers .line-numbers-rows {
  border-right: 1px solid #e1e4e8;
}

.markdown-body:not(.dark-theme) .line-numbers .line-numbers-rows > span:before {
  color: #bbb;
}

/* 代码块工具栏 */
div.code-toolbar {
  position: relative;
}

div.code-toolbar > .toolbar {
  position: absolute;
  top: 0.3em;
  right: 0.2em;
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
}

div.code-toolbar:hover > .toolbar {
  opacity: 1;
}

div.code-toolbar > .toolbar > .toolbar-item > button {
  background: none;
  border: 1px solid var(--prism-border-color);
  color: var(--prism-lang-color);
  font-size: 0.8em;
  padding: 0.3em 0.7em;
  margin: 0 0.2em;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

div.code-toolbar > .toolbar > .toolbar-item > button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.markdown-body:not(.dark-theme) div.code-toolbar > .toolbar > .toolbar-item > button:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #000;
}

/* 引用块样式 */
.markdown-body blockquote {
  margin: 6px 0;
  padding: 4px 10px;
  border-left: 3px solid #0085FF;
  background: rgba(0, 133, 255, 0.05);
  border-radius: 2px;
}

.markdown-body blockquote p {
  margin: 0;
}

/* 表格样式 */
.markdown-body table {
  margin: 6px 0;
  border-collapse: collapse;
  border-spacing: 0;
  width: 100%;
}

.markdown-body th,
.markdown-body td {
  padding: 6px 10px;
  border: 1px solid #eee;
  text-align: left;
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
  --prism-background: #282c34;
  --prism-header-background: #21252b;
  --prism-border-color: #404859;
  --prism-lang-color: #abb2bf;
  --prism-line-number-color: #636d83;
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
    padding: 3px 8px;
  }

  .markdown-body th,
  .markdown-body td {
    padding: 4px 8px;
  }
  
  .markdown-body ul,
  .markdown-body ol {
    padding-left: 1em;
  }
  
  .markdown-body li {
    text-indent: -1.2em;
    padding-left: 1.2em;
  }
}

/* 滚动条样式 */
.markdown-body pre::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}

.markdown-body pre::-webkit-scrollbar-thumb {
  background: #404040;
  border-radius: 4px;
}

.markdown-body pre::-webkit-scrollbar-track {
  background: #1a1a1a;
  border-radius: 4px;
}

/* 修改代码块样式 */
pre[class*="language-"] {
  margin: 0;
  padding: 1em;
  overflow: auto;
  background: var(--prism-background, #282c34) !important;
}

code[class*="language-"] {
  font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.5;
  direction: ltr;
  text-align: left;
  white-space: pre;
  word-spacing: normal;
  word-break: normal;
  tab-size: 4;
  hyphens: none;
  background: none !important;
}

/* 行号样式优化 */
pre[class*="language-"].line-numbers {
  position: relative;
  padding-left: 3.8em;
  counter-reset: linenumber;
}

pre[class*="language-"].line-numbers > code {
  position: relative;
  white-space: inherit;
}

.line-numbers .line-numbers-rows {
  position: absolute;
  pointer-events: none;
  top: 1em;
  font-size: 100%;
  left: 0;
  width: 3em;
  letter-spacing: -1px;
  border-right: 1px solid var(--prism-border-color, #404859);
  user-select: none;
}

.line-numbers-rows > span {
  display: block;
  counter-increment: linenumber;
}

.line-numbers-rows > span:before {
  content: counter(linenumber);
  color: var(--prism-line-number-color, #636d83);
  display: block;
  padding-right: 0.8em;
  text-align: right;
}

/* 工具栏样式优化 */
div.code-toolbar {
  position: relative;
}

div.code-toolbar > .toolbar {
  position: absolute;
  top: 0.3em;
  right: 0.2em;
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
}

div.code-toolbar:hover > .toolbar {
  opacity: 1;
}

div.code-toolbar > .toolbar > .toolbar-item > button {
  background: none;
  border: 1px solid var(--prism-border-color);
  color: var(--prism-lang-color);
  font-size: 0.8em;
  padding: 0.3em 0.7em;
  margin: 0 0.2em;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

div.code-toolbar > .toolbar > .toolbar-item > button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

/* 语言标签样式 */
.code-block-header {
  padding: 0.5em 1em;
  background: var(--prism-header-background, #21252b);
  border-bottom: 1px solid var(--prism-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code-lang {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8em;
  color: var(--prism-lang-color);
  text-transform: uppercase;
}

/* 亮色主题变量 */
.markdown-body:not(.dark-theme) {
  --prism-background: #f6f8fa;
  --prism-header-background: #f0f2f5;
  --prism-border-color: #e1e4e8;
  --prism-lang-color: #666666;
  --prism-line-number-color: #bbb;
}

/* 暗色主题变量 */
.dark-theme {
  --prism-background: #282c34;
  --prism-header-background: #21252b;
  --prism-border-color: #404859;
  --prism-lang-color: #abb2bf;
  --prism-line-number-color: #636d83;
}
</style> 