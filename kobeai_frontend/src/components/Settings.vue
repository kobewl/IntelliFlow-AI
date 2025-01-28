<template>
  <div class="settings-wrapper">
    <el-dialog
      v-model="visible"
      title="设置"
      width="460px"
      :close-on-click-modal="false"
      :modal="true"
      :modal-class="'settings-modal'"
      destroy-on-close
      @open="handleDialogOpen"
      @close="handleDialogClose"
    >
      <div class="settings-content">
        <!-- 主题设置 -->
        <div class="settings-section">
          <div class="section-header">
            <el-icon><Brush /></el-icon>
            <h4>主题设置</h4>
          </div>
          <div class="section-content">
            <div class="setting-item">
              <span class="label">主题模式</span>
              <div class="control">
                <el-radio-group v-model="settings.theme" size="large">
                  <el-radio-button label="light">
                    <el-icon><Sunny /></el-icon>
                    浅色
                  </el-radio-button>
                  <el-radio-button label="dark">
                    <el-icon><Moon /></el-icon>
                    深色
                  </el-radio-button>
                  <el-radio-button label="system">
                    <el-icon><Monitor /></el-icon>
                    跟随系统
                  </el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div class="setting-item">
              <span class="label">主题色</span>
              <div class="control">
                <el-color-picker v-model="settings.primaryColor" show-alpha />
              </div>
            </div>
          </div>
        </div>

        <!-- 界面设置 -->
        <div class="settings-section">
          <div class="section-header">
            <el-icon><Monitor /></el-icon>
            <span>界面设置</span>
          </div>
          <div class="section-content">
            <div class="setting-item">
              <span class="label">侧边栏</span>
              <div class="control">
                <el-switch
                  v-model="settings.sidebarCollapsible"
                  active-text="可折叠"
                  inactive-text="固定"
                />
              </div>
            </div>
            <div class="setting-item">
              <span class="label">消息气泡</span>
              <div class="control">
                <el-radio-group v-model="settings.messageStyle" size="small">
                  <el-radio-button label="rounded">圆角</el-radio-button>
                  <el-radio-button label="square">直角</el-radio-button>
                  <el-radio-button label="minimal">简约</el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div class="setting-item">
              <span class="label">字体大小</span>
              <div class="control">
                <el-slider
                  v-model="settings.fontSize"
                  :min="12"
                  :max="18"
                  :step="1"
                  :marks="{
                    12: '小',
                    14: '中',
                    16: '大',
                    18: '特大'
                  }"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 消息设置 -->
        <div class="settings-section">
          <div class="section-header">
            <el-icon><ChatDotRound /></el-icon>
            <span>消息设置</span>
          </div>
          <div class="section-content">
            <div class="setting-item">
              <span class="label">代码高亮</span>
              <div class="control">
                <el-switch
                  v-model="settings.codeHighlight"
                  inline-prompt
                  :active-text="'启用'"
                  :inactive-text="'禁用'"
                />
              </div>
            </div>
            <div class="setting-item">
              <span class="label">消息时间</span>
              <div class="control">
                <el-radio-group v-model="settings.timeFormat" size="small">
                  <el-radio-button label="relative">相对</el-radio-button>
                  <el-radio-button label="absolute">绝对</el-radio-button>
                  <el-radio-button label="detailed">详细</el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div class="setting-item">
              <span class="label">自动滚动</span>
              <div class="control">
                <el-switch
                  v-model="settings.autoScroll"
                  inline-prompt
                  :active-text="'启用'"
                  :inactive-text="'禁用'"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷键设置 -->
        <div class="settings-section">
          <div class="section-header">
            <el-icon><Operation /></el-icon>
            <span>快捷键设置</span>
          </div>
          <div class="section-content">
            <div class="setting-item">
              <span class="label">发送消息</span>
              <div class="control">
                <el-radio-group v-model="settings.sendShortcut" size="small">
                  <el-radio-button label="enter">Enter</el-radio-button>
                  <el-radio-button label="ctrl+enter">Ctrl + Enter</el-radio-button>
                  <el-radio-button label="shift+enter">Shift + Enter</el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div class="setting-item">
              <span class="label">新建会话</span>
              <div class="control">
                <el-radio-group v-model="settings.newChatShortcut" size="small">
                  <el-radio-button label="ctrl+n">Ctrl + N</el-radio-button>
                  <el-radio-button label="alt+n">Alt + N</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resetSettings" size="small">恢复默认</el-button>
          <el-button type="primary" @click="saveSettings" size="small">保存设置</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
export default {
  name: 'Settings'
}
</script>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { 
  Brush, Sunny, Moon, Monitor, ChatDotRound, Operation
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const visible = ref(false)

// 设置选项
const settings = reactive({
  // 主题设置
  theme: 'system',
  primaryColor: '#409EFF',
  
  // 界面设置
  sidebarCollapsible: true,
  messageStyle: 'rounded',
  fontSize: 14,
  
  // 消息设置
  codeHighlight: true,
  timeFormat: 'relative',
  autoScroll: true,
  
  // 快捷键设置
  sendShortcut: 'ctrl+enter',
  newChatShortcut: 'ctrl+n'
})

const emit = defineEmits(['update:modelValue', 'open', 'close'])

function handleDialogOpen() {
  // 添加遮罩样式
  document.body.classList.add('settings-open')
  emit('open')
}

function handleDialogClose() {
  // 移除遮罩样式
  document.body.classList.remove('settings-open')
  emit('close')
}

// 显示设置对话框
function show() {
  visible.value = true
}

// 重置设置
function resetSettings() {
  Object.assign(settings, {
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
  })
  ElMessage.success('已恢复默认设置')
}

// 保存设置
async function saveSettings() {
  try {
    // TODO: 调用接口保存设置
    ElMessage.success('设置保存成功')
    visible.value = false
  } catch (error) {
    ElMessage.error('设置保存失败')
  }
}

defineExpose({
  show
})
</script>

<style>
/* 遮罩层样式 */
:deep(.el-overlay) {
  background-color: rgba(0, 0, 0, 0.5) !important;
  z-index: 2999;
}

/* 设置对话框样式 */
:deep(.el-dialog) {
  margin-top: 5vh !important;
  border-radius: 24px;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);
  background: var(--el-bg-color);
  width: 460px !important;
  max-width: 90%;
  z-index: 3000;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  margin: 0;
  padding: 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  border-radius: 24px 24px 0 0;
  background: var(--el-bg-color);
}

:deep(.el-dialog__body) {
  padding: 0;
  background: var(--el-bg-color);
}

:deep(.el-dialog__footer) {
  margin: 0;
  padding: 20px 24px;
  border-top: 1px solid var(--el-border-color-lighter);
  border-radius: 0 0 24px 24px;
  background: var(--el-bg-color);
}

/* 深色模式适配 */
:deep(.dark) .el-dialog {
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-darker);
}

:deep(.dark) .el-dialog__header,
:deep(.dark) .el-dialog__body,
:deep(.dark) .el-dialog__footer {
  background: var(--el-bg-color-overlay);
}

:deep(.dark) .el-dialog__header {
  border-bottom-color: var(--el-border-color-darker);
}

:deep(.dark) .el-dialog__footer {
  border-top-color: var(--el-border-color-darker);
}
</style>

<style scoped>
.settings-content {
  padding: 24px;
  max-height: 65vh;
  overflow-y: auto;
}

.settings-section {
  padding: 20px;
  margin: 0 -24px;
  background: var(--el-fill-color-blank);
}

.settings-section:not(:last-child) {
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.section-content {
  padding: 0 12px;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 16px;
  margin: 8px 0;
  background: var(--el-fill-color-light);
  border-radius: 16px;
  transition: all 0.3s ease;
}

.setting-item:hover {
  background: var(--el-fill-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.setting-item .label {
  width: 80px;
  color: var(--el-text-color-regular);
  font-size: 14px;
  font-weight: 500;
}

.setting-item .control {
  flex: 1;
  display: flex;
  justify-content: flex-start;
  gap: 12px;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-slider) {
  width: 280px;
}

:deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.el-radio-button__inner) {
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 12px;
}

:deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 12px;
}

:deep(.el-switch) {
  --el-switch-on-color: var(--el-color-primary);
  border-radius: 24px;
}

:deep(.el-button) {
  border-radius: 12px;
  padding: 10px 20px;
  height: auto;
  transition: all 0.3s ease;
}

/* 深色模式适配 */
:deep(.dark) .el-dialog {
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-darker);
}

:deep(.dark) .settings-section {
  background: var(--el-bg-color);
}

:deep(.dark) .setting-item {
  background: var(--el-fill-color-dark);
}

:deep(.dark) .setting-item:hover {
  background: var(--el-fill-color-darker);
}

:deep(.dark) .section-header {
  color: var(--el-text-color-primary);
}

:deep(.dark) .setting-item .label {
  color: var(--el-text-color-regular);
}

:deep(.dark) .settings-section:not(:last-child) {
  border-color: var(--el-border-color-darker);
}
</style> 