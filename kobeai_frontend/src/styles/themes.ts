import { useDark } from '@vueuse/core'

// 主题类型
export type Theme = 'light' | 'dark' | 'system'

// 主题配置
export const themes = {
  light: {
    '--el-color-primary': '#409EFF',
    '--el-bg-color': '#ffffff',
    '--el-bg-color-page': '#f5f7fa',
    '--el-text-color-primary': '#303133',
    '--el-text-color-regular': '#606266',
    '--el-text-color-secondary': '#909399',
    '--el-border-color': '#dcdfe6',
    '--el-border-color-light': '#e4e7ed',
    '--el-border-color-lighter': '#ebeef5',
    '--el-fill-color-light': '#f5f7fa',
    '--el-mask-color': 'rgba(255, 255, 255, 0.9)',
    '--el-mask-color-extra-light': 'rgba(255, 255, 255, 0.3)',
  },
  dark: {
    '--el-color-primary': '#409EFF',
    '--el-bg-color': '#141414',
    '--el-bg-color-page': '#0a0a0a',
    '--el-text-color-primary': '#ffffff',
    '--el-text-color-regular': '#d1d1d1',
    '--el-text-color-secondary': '#a8a8a8',
    '--el-border-color': '#434343',
    '--el-border-color-light': '#363636',
    '--el-border-color-lighter': '#262626',
    '--el-fill-color-light': '#1f1f1f',
    '--el-mask-color': 'rgba(0, 0, 0, 0.9)',
    '--el-mask-color-extra-light': 'rgba(0, 0, 0, 0.3)',
  }
}

// 使用系统主题
export const isDark = useDark()

// 应用主题
export function applyTheme(theme: Theme) {
  const root = document.documentElement
  const vars = themes[theme === 'system' ? (isDark.value ? 'dark' : 'light') : theme]
  
  Object.entries(vars).forEach(([key, value]) => {
    root.style.setProperty(key, value)
  })
  
  // 保存主题设置
  localStorage.setItem('theme', theme)
} 