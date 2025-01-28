<template>
  <div class="file-message">
    <!-- 图片预览 -->
    <template v-if="isImage">
      <el-image
        :src="file.url"
        :preview-src-list="[file.url]"
        fit="cover"
        class="image-preview"
      >
        <template #placeholder>
          <div class="image-placeholder">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
        <template #error>
          <div class="image-error">
            <el-icon><PictureRounded /></el-icon>
            <span>加载失败</span>
          </div>
        </template>
      </el-image>
    </template>

    <!-- 文件信息 -->
    <div class="file-info">
      <div class="file-icon">
        <el-icon :size="32">
          <component :is="fileIcon" />
        </el-icon>
      </div>
      <div class="file-details">
        <div class="file-name" :title="file.name">{{ file.name }}</div>
        <div class="file-meta">
          <span>{{ formatFileSize(file.size) }}</span>
          <span class="dot">·</span>
          <span>{{ formatFileType(file.type) }}</span>
        </div>
      </div>
      <div class="file-actions">
        <el-button 
          type="primary" 
          link
          :icon="Download"
          @click="handleDownload"
        >
          下载
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Document,
  Download,
  Picture,
  PictureRounded,
  VideoPlay,
  Headset,
  Files,
} from '@element-plus/icons-vue'
import { chatApi } from '../api/chat'
import type { FileMessage } from '../api/chat'

const props = defineProps<{
  file: FileMessage['file']
}>()

// 判断是否为图片
const isImage = computed(() => {
  return props.file.type.startsWith('image/')
})

// 文件图标
const fileIcon = computed(() => {
  if (props.file.type.startsWith('image/')) {
    return Picture
  }
  if (props.file.type.startsWith('video/')) {
    return VideoPlay
  }
  if (props.file.type.startsWith('audio/')) {
    return Headset
  }
  if (props.file.type.startsWith('application/pdf')) {
    return Document
  }
  return Files
})

// 格式化文件大小
const formatFileSize = (size: number) => {
  if (size < 1024) {
    return size + ' B'
  }
  if (size < 1024 * 1024) {
    return (size / 1024).toFixed(1) + ' KB'
  }
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

// 格式化文件类型
const formatFileType = (type: string) => {
  return type.split('/')[1].toUpperCase()
}

// 下载文件
const handleDownload = async () => {
  try {
    const blob = await chatApi.downloadFile(props.file.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = props.file.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

defineOptions({
  name: 'FileMessage'
})

export default {}
</script>

<style scoped>
.file-message {
  width: 100%;
  max-width: 400px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.image-preview {
  width: 100%;
  height: 200px;
  background: var(--el-fill-color-light);
}

.image-placeholder,
.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  gap: 8px;
}

.file-info {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.file-icon {
  color: var(--el-text-color-secondary);
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.dot {
  margin: 0 4px;
}

.file-actions {
  flex-shrink: 0;
}
</style> 