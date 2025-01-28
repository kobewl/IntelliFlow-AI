<template>
  <div class="settings-wrapper">
    <el-dialog
      v-model="visible"
      title="设置"
      width="460px"
      :close-on-click-modal="false"
      :modal="true"
      :append-to-body="true"
      class="settings-dialog"
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
          <el-button @click="resetSettings" plain size="default">恢复默认</el-button>
          <el-button type="primary" @click="saveSettings" size="default">保存设置</el-button>
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
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useSettingsStore } from '../store/settings'
import { 
  Brush, Sunny, Moon, Monitor, ChatDotRound, Operation
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const settingsStore = useSettingsStore()
const { settings } = storeToRefs(settingsStore)

const emit = defineEmits(['update:modelValue', 'open', 'close'])

function handleDialogOpen() {
  document.body.classList.add('settings-open')
  emit('open')
}

function handleDialogClose() {
  document.body.classList.remove('settings-open')
  emit('close')
}

function show() {
  visible.value = true
}

function resetSettings() {
  settingsStore.resetSettings()
  ElMessage.success('已恢复默认设置')
}

async function saveSettings() {
  try {
    settingsStore.applySettings(settings.value)
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
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1), 0 8px 16px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.el-dialog__header) {
  padding: 28px 32px;
  margin: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: linear-gradient(135deg, var(--el-color-primary-light-5), var(--el-color-primary-light-8));
  position: relative;
  overflow: hidden;
}

:deep(.el-dialog__header)::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at top right, rgba(255, 255, 255, 0.2), transparent 70%);
}

:deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-color-white);
  line-height: 24px;
  position: relative;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

:deep(.el-dialog__body) {
  padding: 0;
  max-height: calc(90vh - 150px);
  overflow-y: auto;
  background: var(--el-bg-color);
}

:deep(.el-dialog__footer) {
  padding: 20px 32px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.settings-content {
  padding: 32px;
}

.settings-section {
  padding: 24px;
  margin: 0 -32px;
  background: var(--el-bg-color);
  position: relative;
}

.settings-section:not(:last-child) {
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.settings-section:hover {
  background: var(--el-fill-color-light);
  transition: all 0.3s ease;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
}

.section-header .el-icon {
  font-size: 24px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 8px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.settings-section:hover .section-header .el-icon {
  transform: scale(1.1);
}

.section-content {
  padding: 0 16px;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 20px;
  margin: 12px 0;
  background: var(--el-fill-color-blank);
  border-radius: 16px;
  transition: all 0.3s ease;
  border: 1px solid var(--el-border-color-lighter);
}

.setting-item:hover {
  background: var(--el-fill-color);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  border-color: var(--el-color-primary-light-7);
}

.setting-item .label {
  width: 90px;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 500;
}

.setting-item .control {
  flex: 1;
  display: flex;
  justify-content: flex-start;
  gap: 16px;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 0;
}

:deep(.el-button) {
  min-width: 90px;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
  border: none;
}

:deep(.el-button--default) {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
}

:deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-radio-button__inner) {
  padding: 10px 20px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.el-switch) {
  --el-switch-on-color: var(--el-color-primary);
  border-radius: 24px;
  transition: all 0.3s ease;
}

:deep(.el-slider) {
  width: 300px;
}

:deep(.el-slider__runway) {
  height: 8px;
  border-radius: 4px;
}

:deep(.el-slider__bar) {
  height: 8px;
  border-radius: 4px;
  background: linear-gradient(to right, var(--el-color-primary), var(--el-color-primary-light-3));
}

:deep(.el-slider__button) {
  width: 20px;
  height: 20px;
  border: 3px solid var(--el-color-primary);
  background: var(--el-color-white);
  transition: all 0.3s ease;
}

:deep(.el-slider__button:hover) {
  transform: scale(1.2);
}

/* 深色模式适配 */
:deep(.dark) .el-dialog {
  background: var(--el-bg-color-overlay);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

:deep(.dark) .settings-section {
  background: var(--el-bg-color-overlay);
}

:deep(.dark) .settings-section:hover {
  background: var(--el-fill-color-dark);
}

:deep(.dark) .setting-item {
  background: var(--el-bg-color);
  border-color: var(--el-border-color-darker);
}

:deep(.dark) .setting-item:hover {
  background: var(--el-fill-color-darker);
  border-color: var(--el-color-primary-light-5);
}

:deep(.dark) .section-header .el-icon {
  background: var(--el-fill-color-darker);
}

:deep(.dark) .el-slider__button {
  border-color: var(--el-color-primary-light-3);
  background: var(--el-bg-color);
}
</style> 