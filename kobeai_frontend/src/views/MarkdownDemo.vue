<template>
  <div class="markdown-demo">
    <div class="demo-header">
      <h2>Markdown 渲染示例</h2>
      <el-switch
        v-model="isDarkMode"
        active-text="暗色模式"
        inactive-text="亮色模式"
      />
    </div>
    <div class="demo-container" :class="{ 'dark-mode': isDarkMode }">
      <div class="editor-section">
        <el-input
          v-model="markdownContent"
          type="textarea"
          :rows="20"
          placeholder="请输入 Markdown 内容..."
        />
      </div>
      <div class="preview-section">
        <MarkdownRenderer :content="markdownContent" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'

const isDarkMode = ref(false)
const markdownContent = ref(`# Markdown 渲染示例

## 基本语法

### 1. 标题

# 一级标题
## 二级标题
### 三级标题

### 2. 文本样式

**粗体文本** 和 *斜体文本*

### 3. 列表

无序列表：
- 项目 1
- 项目 2
  - 子项目 2.1
  - 子项目 2.2

有序列表：
1. 第一项
2. 第二项
3. 第三项

### 4. 引用

> 这是一段引用文本
> 可以有多行

### 5. 代码

行内代码：\`const example = "hello world"\`

代码块：
\`\`\`javascript
function greeting(name) {
  console.log(\`Hello, \${name}!\`);
}

greeting("World");
\`\`\`

### 6. 表格

| 表头 1 | 表头 2 | 表头 3 |
|--------|--------|--------|
| 单元格 1 | 单元格 2 | 单元格 3 |
| 示例 4 | 示例 5 | 示例 6 |

### 7. 链接和图片

[链接文本](https://example.com)

![图片描述](https://example.com/image.jpg)

### 8. 任务列表

- [x] 已完成任务
- [ ] 未完成任务
- [ ] 待办事项

### 9. 数学公式

行内公式：$E = mc^2$

独立公式：
$$
\\sum_{i=1}^n x_i = x_1 + x_2 + \\cdots + x_n
$$
`)
</script>

<style scoped>
.markdown-demo {
  padding: 20px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.demo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.demo-container {
  flex: 1;
  display: flex;
  gap: 20px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
  padding: 20px;
  height: calc(100vh - 100px);
}

.editor-section,
.preview-section {
  flex: 1;
  overflow: auto;
  border-radius: 8px;
}

.editor-section {
  background-color: var(--el-bg-color-page);
  padding: 20px;
}

.preview-section {
  background-color: var(--el-bg-color);
}

.dark-mode {
  background-color: #1a1a1a;
}

:deep(.el-textarea__inner) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .demo-container {
    flex-direction: column;
  }
  
  .editor-section,
  .preview-section {
    height: 50%;
  }
}
</style> 