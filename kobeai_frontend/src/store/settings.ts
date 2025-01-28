import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  // 从 localStorage 加载初始设置
  const loadSavedSettings = () => {
    const saved = localStorage.getItem('app_settings')
    if (saved) {
      return JSON.parse(saved)
    }
    return {
      theme: 'system',
      primaryColor: '#409EFF',
      sidebarCollapsible: true,
      messageStyle: 'rounded',
      fontSize: 14,
      codeHighlight: true,
      timeFormat: 'relative',
      autoScroll: true,
      sendShortcut: 'ctrl+enter',
      newChatShortcut: 'ctrl+n'
    }
  }

  const settings = ref(loadSavedSettings())

  // 监听设置变化并保存到 localStorage
  watch(settings, (newSettings) => {
    localStorage.setItem('app_settings', JSON.stringify(newSettings))
    applySettings(newSettings)
  }, { deep: true })

  // 应用设置到全局样式
  const applySettings = (newSettings: any) => {
    // 应用主题
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    const isDark = newSettings.theme === 'dark' || 
      (newSettings.theme === 'system' && prefersDark)
    
    document.documentElement.classList.toggle('dark', isDark)

    // 应用主题色
    const style = document.documentElement.style
    style.setProperty('--el-color-primary', newSettings.primaryColor)
    
    // 计算主题色的不同亮度变体
    const color = newSettings.primaryColor
    const rgb = hexToRgb(color)
    if (rgb) {
      for (let i = 1; i <= 9; i++) {
        const lightness = i * 10
        const lightColor = adjustColorLightness(rgb, lightness)
        style.setProperty(`--el-color-primary-light-${i}`, lightColor)
      }
    }

    // 应用字体大小
    style.setProperty('--el-font-size-base', `${newSettings.fontSize}px`)
    
    // 应用消息样式
    document.body.setAttribute('data-message-style', newSettings.messageStyle)
  }

  // 辅助函数：将十六进制颜色转换为 RGB
  const hexToRgb = (hex: string) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : null
  }

  // 辅助函数：调整颜色亮度
  const adjustColorLightness = (rgb: { r: number, g: number, b: number }, lightness: number) => {
    const factor = (100 + lightness) / 100
    return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${1 - (factor - 1) * 0.8})`
  }

  // 重置设置
  const resetSettings = () => {
    settings.value = {
      theme: 'system',
      primaryColor: '#409EFF',
      sidebarCollapsible: true,
      messageStyle: 'rounded',
      fontSize: 14,
      codeHighlight: true,
      timeFormat: 'relative',
      autoScroll: true,
      sendShortcut: 'ctrl+enter',
      newChatShortcut: 'ctrl+n'
    }
  }

  // 初始化时应用设置
  applySettings(settings.value)

  // 监听系统主题变化
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (settings.value.theme === 'system') {
      applySettings(settings.value)
    }
  })

  return {
    settings,
    resetSettings,
    applySettings
  }
}) 