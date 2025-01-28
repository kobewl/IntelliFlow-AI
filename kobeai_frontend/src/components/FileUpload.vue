<template>
  <div 
    class="file-upload"
    @dragover.prevent="handleDragOver"
    @dragleave.prevent="handleDragLeave"
    @drop.prevent="handleDrop"
  >
    <el-upload
      ref="uploadRef"
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
    >
      <template #trigger>
        <div 
          class="upload-area"
          :class="{ 
            'is-dragover': isDragOver,
            'is-uploading': isUploading 
          }"
        >
          <template v-if="isUploading">
            <el-progress 
              type="circle"
              :percentage="uploadProgress"
              :width="80"
            />
            <div class="upload-text">
              正在上传 {{ currentFile?.name }}
            </div>
          </template>
          <template v-else>
            <el-icon :size="40">
              <Upload />
            </el-icon>
            <div class="upload-text">
              <div>拖拽文件到此处或点击上传</div>
              <div class="upload-hint">
                支持图片、文档、视频等文件格式
              </div>
            </div>
          </template>
        </div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import type { UploadInstance, UploadFile } from 'element-plus'

const props = defineProps<{
  maxSize?: number // 最大文件大小(MB)
}>()

const emit = defineEmits<{
  (e: 'select', file: File): void
}>()

const uploadRef = ref<UploadInstance>()
const isDragOver = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)
const currentFile = ref<File>()

// 处理文件拖拽
const handleDragOver = () => {
  isDragOver.value = true
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const handleDrop = (e: DragEvent) => {
  isDragOver.value = false
  const files = e.dataTransfer?.files
  if (files?.length) {
    handleFile(files[0])
  }
}

// 处理文件选择
const handleFileChange = (uploadFile: UploadFile) => {
  if (uploadFile.raw) {
    handleFile(uploadFile.raw)
  }
}

// 处理文件
const handleFile = (file: File) => {
  // 检查文件大小
  if (props.maxSize && file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return
  }

  currentFile.value = file
  emit('select', file)
}

// 开始上传
const startUpload = (progress: number) => {
  isUploading.value = true
  uploadProgress.value = progress
}

// 结束上传
const finishUpload = () => {
  isUploading.value = false
  uploadProgress.value = 0
  currentFile.value = undefined
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

defineExpose({
  startUpload,
  finishUpload
})

defineOptions({
  name: 'FileUpload'
})
</script>

<script lang="ts">
export default {
  name: 'FileUpload'
}
</script>

<style scoped>
.file-upload {
  width: 100%;
}

.upload-area {
  width: 100%;
  height: 200px;
  border: 2px dashed var(--el-border-color);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-area:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.upload-area.is-dragover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.upload-area.is-uploading {
  cursor: default;
}

.upload-text {
  margin-top: 16px;
  text-align: center;
}

.upload-hint {
  margin-top: 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

/* 深色模式适配 */
:deep(.dark) .upload-area {
  border-color: var(--el-border-color-darker);
}

:deep(.dark) .upload-area:hover,
:deep(.dark) .upload-area.is-dragover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .upload-area {
    height: 160px;
  }
  
  .upload-text {
    margin-top: 12px;
    font-size: 14px;
  }
  
  .upload-hint {
    font-size: 12px;
  }
}
</style> 