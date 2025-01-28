<template>
  <div 
    class="theme-switch" 
    :class="{ dark: isDark }"
    @click="toggleTheme"
  >
    <el-icon class="icon sun-icon"><Sunny /></el-icon>
    <el-icon class="icon moon-icon"><Moon /></el-icon>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Sunny, Moon } from '@element-plus/icons-vue'

const isDark = ref(false)

const toggleTheme = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark')
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

onMounted(() => {
  const theme = localStorage.getItem('theme')
  isDark.value = theme === 'dark'
  if (isDark.value) {
    document.documentElement.classList.add('dark')
  }
})

// 添加默认导出
defineOptions({
  name: 'ThemeSwitch'
})
</script>

<style scoped>
.theme-switch {
  position: relative;
  width: 48px;
  height: 24px;
  border-radius: 12px;
  background: var(--el-color-primary);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.theme-switch::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  transform: translateX(var(--switch-translate, 0));
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 2;
}

.theme-switch.dark {
  background: #666;
  --switch-translate: 24px;
}

.theme-switch .icon {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 14px;
  height: 14px;
  transition: all 0.3s ease;
}

.theme-switch .sun-icon {
  left: 4px;
  opacity: 1;
  color: #fff;
}

.theme-switch .moon-icon {
  right: 4px;
  opacity: 0;
  color: #fff;
}

.theme-switch.dark .sun-icon {
  opacity: 0;
}

.theme-switch.dark .moon-icon {
  opacity: 1;
}

/* 动画效果 */
@keyframes rotate {
  from {
    transform: translateY(-50%) rotate(0deg);
  }
  to {
    transform: translateY(-50%) rotate(360deg);
  }
}

.theme-switch:not(.dark) .sun-icon {
  animation: rotate 4s linear infinite;
}

.theme-switch.dark .moon-icon {
  animation: rotate 4s linear infinite;
}

/* 悬停效果 */
.theme-switch:hover {
  filter: brightness(1.1);
}

.theme-switch:active::before {
  width: 24px;
}
</style> 