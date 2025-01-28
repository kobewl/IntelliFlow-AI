<!-- ErrorMessage.vue -->
<template>
  <Transition name="fade">
    <div v-if="show" class="error-message" :class="type">
      <el-icon class="error-icon">
        <component :is="icon" />
      </el-icon>
      <span class="error-text">{{ message }}</span>
      <el-icon class="close-icon" @click="$emit('close')">
        <Close />
      </el-icon>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Warning, InfoFilled, CircleCheckFilled, Close } from '@element-plus/icons-vue'

const props = defineProps<{
  show: boolean
  message: string
  type?: 'error' | 'warning' | 'success' | 'info'
}>()

defineEmits<{
  (e: 'close'): void
}>()

// 添加默认导出
defineOptions({
  name: 'ErrorMessage'
})

const icon = computed(() => {
  switch (props.type) {
    case 'error':
      return Warning
    case 'warning':
      return Warning
    case 'success':
      return CircleCheckFilled
    case 'info':
    default:
      return InfoFilled
  }
})
</script>

<style scoped>
.error-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 16px 24px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 9999;
  max-width: 400px;
  background: white;
}

.error-message.error {
  border-left: 4px solid var(--el-color-danger);
}

.error-message.warning {
  border-left: 4px solid var(--el-color-warning);
}

.error-message.success {
  border-left: 4px solid var(--el-color-success);
}

.error-message.info {
  border-left: 4px solid var(--el-color-info);
}

.error-icon {
  font-size: 20px;
}

.error-message.error .error-icon {
  color: var(--el-color-danger);
}

.error-message.warning .error-icon {
  color: var(--el-color-warning);
}

.error-message.success .error-icon {
  color: var(--el-color-success);
}

.error-message.info .error-icon {
  color: var(--el-color-info);
}

.error-text {
  flex: 1;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.close-icon {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: color 0.3s ease;
}

.close-icon:hover {
  color: var(--el-text-color-primary);
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style> 