/**
 * Markdown 预处理管道
 * 参照 Open WebUI 的多层预处理架构，修复 LLM 输出的常见格式问题
 *
 * 管道顺序有讲究，不可随意调换：
 *   1. sanitize     — 先消毒，去掉异常 token
 *   2. chineseFix   — 修复中文标点与 Markdown 标记的冲突
 *   3. tables       — 修复表格分隔行（全角破折号等）
 *   4. headings     — 修复标题格式（缺空格、缺换行）
 *   5. blockquotes  — 修复引用格式（缺换行）
 */

// ---- 1. 消毒：去掉 LLM 可能输出的异常 token ----
function sanitize(content: string): string {
  let text = content
  // 去掉行末的 <| 开头控制 token（如 <|im_start|>, <|assistant|> 等）
  text = text.replace(/<\|[^|]*\|>/g, '')
  // 去掉零宽字符
  text = text.replace(/[​-‍﻿]/g, '')
  return text
}

// ---- 2. 中文排版修复（参照 Open WebUI processChineseContent） ----
function chineseFix(content: string): string {
  let text = content

  // 中文引号与粗体标记冲突：「**文字**」→ 在 ** 两侧加空格
  // 模式：中文字符后紧跟 ** 或 ** 前紧跟中文字符
  text = text.replace(/([一-鿿　-〿＀-￯])(\*\*)/g, '$1 $2')
  text = text.replace(/(\*\*)([一-鿿　-〿＀-￯])/g, '$1 $2')

  // 中文标点（。！？，、：；）「」『』【】（）《》后紧跟 Markdown 标记
  const cjkPunct = '。！？，、：；」』】）》》」』'
  text = text.replace(new RegExp(`([${cjkPunct}])(\\*\\*|__|\\*|_)`, 'g'), '$1 $2')
  text = text.replace(new RegExp(`(\\*\\*|__|\\*|_)([${cjkPunct}])`, 'g'), '$1 $2')

  // 中文引号内侧的 Markdown 标记：「**文字**」已处理但「**文字」这种情况也处理
  text = text.replace(/(“|『|「)(\*\*|__)([^\*_]+)(\*\*|__)(”|』|」)/g, '$1 $2$3$4 $5')

  return text
}

// ---- 3. 表格修复 ----
function tables(content: string): string {
  const lines = content.split('\n')
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    // 只处理表格分隔行：|:—:| → |:---:|
    if (/^\|?[\s:—]+\|/.test(line) && /—/.test(line)) {
      lines[i] = line.replace(/—/g, '-')
    }
  }
  return lines.join('\n')
}

// ---- 4. 标题修复 ----
function headings(content: string): string {
  let text = content

  // 4a. 标题 # 后缺空格：##text → ## text
  // [^\s#] 确保不匹配 "######" 这类全 # 行
  text = text.replace(/^(\s*#{1,6})([^\s#])/gm, '$1 $2')

  // 4b. 标题前缺换行：非换行符后紧跟 # heading  → 添加换行
  // 但要保护代码块（``` 内的内容）、URL 中的 #、列表项中的 #
  text = text.replace(/([^\n`])(#{1,6}\s)/g, (match, before, hashPart, offset) => {
    // 检查是否在代码块内（简单判断：前面最近的 ``` 是否未闭合）
    const beforeText = text.substring(0, offset)
    const fenceCount = (beforeText.match(/```/g) || []).length
    if (fenceCount % 2 !== 0) return match // 在代码块内，不处理
    return before + '\n\n' + hashPart
  })

  return text
}

// ---- 5. 引用修复 ----
function blockquotes(content: string): string {
  // 引用 > 前缺换行
  return content.replace(/([^\n>])(>\s)/g, '$1\n\n$2')
}

// ---- 主入口：管道式处理 ----
export function preprocessMarkdown(content: string): string {
  if (!content) return ''

  const pipeline = [sanitize, chineseFix, tables, headings, blockquotes]

  let result = content
  for (const fn of pipeline) {
    result = fn(result)
  }
  return result
}
