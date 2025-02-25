// Markdown 处理 Worker
// 用于将 Markdown 转换为 HTML 并处理语法高亮，在单独线程中执行以避免阻塞主线程

// 由于 Worker 不能直接使用 import 语法，我们使用 self 全局对象
// 这个文件将在构建时被转换为可用的 Worker

// 声明 Worker 上下文中可用的类型
declare const self: DedicatedWorkerGlobalScope;

// 当主线程发送消息时触发
self.addEventListener('message', (event) => {
  const { id, markdown, options } = event.data;
  
  // 如果没有提供 Markdown 内容，返回错误
  if (!markdown) {
    self.postMessage({
      id,
      error: 'No markdown content provided'
    });
    return;
  }
  
  try {
    // 处理 Markdown 内容
    const result = processMarkdown(markdown, options);
    
    // 将结果发送回主线程
    self.postMessage({
      id,
      html: result
    });
  } catch (error) {
    // 发送错误信息
    self.postMessage({
      id,
      error: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

/**
 * 处理 Markdown 并转换为 HTML
 * 
 * @param markdown Markdown 文本
 * @param options 选项
 * @returns 处理后的 HTML
 */
function processMarkdown(markdown: string, options: any = {}): string {
  // 这里我们将使用一个简单的 Markdown 解析实现
  // 在实际应用中，你可能会导入 marked 或其他库
  
  let html = parseMarkdown(markdown);
  
  // 应用语法高亮
  if (options.highlight !== false) {
    html = applyHighlight(html);
  }
  
  return html;
}

/**
 * 基本的 Markdown 解析函数
 * 注意：这是一个极其简化的版本，真实实现应使用专业库
 */
function parseMarkdown(markdown: string): string {
  // 转义 HTML 特殊字符
  let html = markdown
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
  
  // 处理标题 (# Heading)
  html = html.replace(/^(#{1,6})\s+(.+)$/gm, (_, level, text) => {
    const headingLevel = level.length;
    return `<h${headingLevel}>${text.trim()}</h${headingLevel}>`;
  });
  
  // 处理粗体 (**text**)
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  
  // 处理斜体 (*text*)
  html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
  
  // 处理代码块 (```code```)
  html = html.replace(/```(.*?)\n([\s\S]*?)```/g, (_, language, code) => {
    return `<pre><code class="language-${language || 'plaintext'}">${code}</code></pre>`;
  });
  
  // 处理行内代码 (`code`)
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  
  // 处理链接 ([text](url))
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
  
  // 处理列表 (- item)
  html = html.replace(/^-\s+(.+)$/gm, '<li>$1</li>');
  html = html.replace(/<li>.*?<\/li>/g, (match) => {
    if (match) {
      return '<ul>' + match + '</ul>';
    }
    return match;
  });
  
  // 处理段落
  html = html.replace(/^(?!<[a-z]).+$/gm, (line) => {
    if (line.trim()) {
      return `<p>${line}</p>`;
    }
    return line;
  });
  
  // 处理换行
  html = html.replace(/\n\n/g, '<br>');
  
  return html;
}

/**
 * 应用语法高亮
 */
function applyHighlight(html: string): string {
  // 查找所有代码块
  return html.replace(/<code class="language-(\w+)">([\s\S]*?)<\/code>/g, (_, language, code) => {
    // 这里应用简单的语法高亮
    // 真实实现应使用 highlight.js 或 Prism
    let highlighted = code;
    
    if (language === 'javascript' || language === 'js') {
      // 简单的 JS 高亮规则
      highlighted = highlighted
        // 关键字
        .replace(/(var|let|const|function|return|if|else|for|while|class|import|export|from|async|await)\b/g, '<span class="keyword">$1</span>')
        // 字符串
        .replace(/(["'])(.*?)\1/g, '<span class="string">$1$2$1</span>')
        // 注释
        .replace(/(\/\/.*)/g, '<span class="comment">$1</span>');
    }
    
    return `<code class="language-${language}">${highlighted}</code>`;
  });
}

// 通知主线程 Worker 已准备就绪
self.postMessage({ ready: true }); 