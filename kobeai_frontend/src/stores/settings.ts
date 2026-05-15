import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
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

  watch(settings, (newSettings) => {
    localStorage.setItem('app_settings', JSON.stringify(newSettings))
    applySettings(newSettings)
  }, { deep: true })

  const applySettings = (newSettings: any) => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    const isDark = newSettings.theme === 'dark' ||
      (newSettings.theme === 'system' && prefersDark)

    document.documentElement.classList.toggle('dark', isDark)

    const style = document.documentElement.style
    style.setProperty('--el-color-primary', newSettings.primaryColor)

    const color = newSettings.primaryColor
    const rgb = hexToRgb(color)
    if (rgb) {
      for (let i = 1; i <= 9; i++) {
        const lightness = i * 10
        const lightColor = adjustColorLightness(rgb, lightness)
        style.setProperty(`--el-color-primary-light-${i}`, lightColor)
      }
    }

    style.setProperty('--el-font-size-base', `${newSettings.fontSize}px`)
    document.body.setAttribute('data-message-style', newSettings.messageStyle)
  }

  const hexToRgb = (hex: string) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : null
  }

  const adjustColorLightness = (rgb: { r: number, g: number, b: number }, lightness: number) => {
    const factor = (100 + lightness) / 100
    return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${1 - (factor - 1) * 0.8})`
  }

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

  applySettings(settings.value)

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
