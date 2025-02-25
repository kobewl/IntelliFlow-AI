/**
 * Markdown 处理工具类
 * 使用 Web Worker 在后台线程中处理 Markdown，避免阻塞主线程
 */

// 为每个请求生成唯一ID
let requestId = 0;

// 记录待处理的请求
const pendingRequests = new Map<number, { resolve: (html: string) => void, reject: (error: Error) => void }>();

// Worker 实例
let worker: Worker | null = null;

// Worker 是否准备就绪
let isWorkerReady = false;

/**
 * 初始化 Worker
 */
function initWorker() {
  if (worker) return;
  
  try {
    // 创建 Worker
    worker = new Worker(new URL('./markdownWorker.ts', import.meta.url), { type: 'module' });
    
    // 处理来自 Worker 的消息
    worker.addEventListener('message', (event) => {
      const { id, html, error, ready } = event.data;
      
      // 处理 Worker 就绪消息
      if (ready) {
        isWorkerReady = true;
        return;
      }
      
      // 获取待处理的请求
      const request = pendingRequests.get(id);
      if (!request) return;
      
      // 从待处理列表中移除
      pendingRequests.delete(id);
      
      // 处理结果或错误
      if (error) {
        request.reject(new Error(error));
      } else {
        request.resolve(html);
      }
    });
    
    // 处理 Worker 错误
    worker.addEventListener('error', (event) => {
      console.error('Markdown Worker error:', event);
      // 如果 Worker 发生错误，处理所有待处理请求
      for (const [id, request] of pendingRequests.entries()) {
        request.reject(new Error('Worker encountered an error'));
        pendingRequests.delete(id);
      }
      
      // 重置 Worker
      worker = null;
      isWorkerReady = false;
    });
  } catch (error) {
    console.error('Failed to initialize Markdown Worker:', error);
    // 如果创建 Worker 失败，所有请求都会使用回退方法
  }
}

/**
 * 使用 Worker 处理 Markdown
 * 
 * @param markdown Markdown 文本
 * @param options 配置选项
 * @returns 处理后的 HTML
 */
export async function processMarkdown(markdown: string, options: any = {}): Promise<string> {
  // 如果没有 Worker 支持，使用回退方法
  if (typeof Worker === 'undefined') {
    console.warn('Web Workers not supported, using fallback method');
    return processFallback(markdown, options);
  }
  
  // 确保 Worker 已初始化
  if (!worker) {
    initWorker();
  }
  
  // 如果初始化失败，使用回退方法
  if (!worker) {
    return processFallback(markdown, options);
  }
  
  // 创建请求
  const id = requestId++;
  
  // 创建 Promise
  return new Promise<string>((resolve, reject) => {
    // 将请求添加到待处理列表
    pendingRequests.set(id, { resolve, reject });
    
    // 发送消息给 Worker
    worker!.postMessage({ id, markdown, options });
    
    // 设置超时处理
    setTimeout(() => {
      if (pendingRequests.has(id)) {
        pendingRequests.delete(id);
        reject(new Error('Markdown processing timed out'));
      }
    }, 5000); // 5秒超时
  });
}

/**
 * 回退处理方法，在 Worker 不可用时使用
 * 
 * @param markdown Markdown 文本
 * @param options 配置选项
 * @returns 处理后的 HTML
 */
function processFallback(markdown: string, options: any = {}): string {
  // 这里使用简单的处理方式，需要更完整的实现请使用专业库
  return markdown
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>');
}

// 预先初始化 Worker
if (typeof window !== 'undefined') {
  // 在客户端环境中初始化
  if (typeof Worker !== 'undefined') {
    initWorker();
  }
} 