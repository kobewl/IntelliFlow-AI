<template>
  <div class="user-info-wrapper">
  <el-dialog
    v-model="visible"
    title="个人信息"
    width="500px"
    :modal="true"
    :close-on-click-modal="false"
    :append-to-body="true"
    destroy-on-close
    class="user-info-dialog"
  >
    <div class="user-info-content">
      <!-- 头像和基本信息部分 -->
      <div class="info-header">
        <div class="avatar-wrapper">
          <div class="avatar-container">
            <div class="avatar-halo" v-if="isVIP || isSVIP" :class="{
              'vip-halo': isVIP,
              'svip-halo': isSVIP
            }"></div>
            <div class="user-avatar" @click="handleChangeAvatar" :class="{
              'vip-avatar': isVIP,
              'svip-avatar': isSVIP
            }">
              <el-icon v-if="!user?.username"><UserFilled /></el-icon>
              <template v-else>{{ user.username[0]?.toUpperCase() }}</template>
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
              </div>
            </div>
          </div>
        </div>
        <div class="basic-info">
          <div class="username-wrapper">
              <h3>{{ user?.username || '未登录' }}</h3>
            <el-tag 
              size="small" 
              :type="getRoleTagType" 
              :effect="getRoleTagEffect"
              :class="{'role-tag-vip': isVIP, 'role-tag-svip': isSVIP}"
            >
              {{ getRoleText }}
              <span v-if="isVIP || isSVIP" class="crown-icon">👑</span>
            </el-tag>
          </div>
          <p class="user-id">ID: {{ user?.id || '-' }}</p>
        </div>
      </div>

      <!-- 详细信息部分 -->
      <div class="info-section">
        <div class="info-item">
          <div class="info-label">
            <el-icon><User /></el-icon>
            <span>用户名</span>
          </div>
          <div class="info-value">
            <template v-if="!isEditingUsername">
              <span>{{ user?.username }}</span>
              <el-button type="primary" link @click="startEditUsername">
                <el-icon><EditPen /></el-icon>
              </el-button>
            </template>
            <template v-else>
              <el-input
                v-model="editingUsername"
                size="small"
                placeholder="请输入用户名"
                @keyup.enter="handleUsernameSubmit"
                ref="usernameInputRef"
              >
                <template #append>
                  <el-button @click="cancelEditUsername">取消</el-button>
                  <el-button type="primary" @click="handleUsernameSubmit">保存</el-button>
                </template>
              </el-input>
            </template>
          </div>
        </div>

        <div class="info-item">
          <div class="info-label">
            <el-icon><Message /></el-icon>
            <span>邮箱</span>
          </div>
          <div class="info-value">
            <template v-if="!isEditingEmail">
              <span>{{ user?.email || '未设置' }}</span>
              <el-button type="primary" link @click="startEditEmail">
                <el-icon><EditPen /></el-icon>
              </el-button>
            </template>
            <template v-else>
              <el-input
                v-model="editingEmail"
                size="small"
                placeholder="请输入邮箱"
                @keyup.enter="handleEmailSubmit"
                ref="emailInputRef"
              >
                <template #append>
                  <el-button @click="cancelEditEmail">取消</el-button>
                  <el-button type="primary" @click="handleEmailSubmit">保存</el-button>
                </template>
              </el-input>
            </template>
          </div>
        </div>

        <div class="info-item">
          <div class="info-label">
            <el-icon><Timer /></el-icon>
            <span>注册时间</span>
          </div>
          <div class="info-value">
            <span>{{ formatRegistrationTime(user?.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 账号安全部分 -->
      <div class="security-section">
        <div class="section-header">
          <el-icon><Lock /></el-icon>
          <h4>账号安全</h4>
        </div>
        <div class="security-options">
          <el-button type="primary" @click="showChangePassword">
            <el-icon><Key /></el-icon>
            修改密码
          </el-button>
          <el-button type="warning" @click="showBindEmail" v-if="!user?.email">
            <el-icon><Message /></el-icon>
            绑定邮箱
          </el-button>
        </div>
      </div>
    </div>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="changePasswordVisible"
      title="修改密码"
      width="400px"
      append-to-body
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changePasswordVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
export default {
  name: 'UserInfo'
}
</script>

<script setup lang="ts">
import { ref, nextTick, computed, watch } from 'vue'
import { 
  UserFilled, EditPen, Lock, User, Message, 
  Timer, Camera, Key
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '../stores/auth'
import { formatTime, formatRegistrationTime } from '../utils/time'
import type { FormInstance } from 'element-plus'
import { UserRole } from '../types/user'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = ref(false)
const authStore = useAuthStore()
const { user } = storeToRefs(authStore)

// 用户名编辑相关
const isEditingUsername = ref(false)
const editingUsername = ref('')
const usernameInputRef = ref<HTMLInputElement>()

// 邮箱编辑相关
const isEditingEmail = ref(false)
const editingEmail = ref('')
const emailInputRef = ref<HTMLInputElement>()

// 修改密码相关
const changePasswordVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 用户角色相关的计算属性
const isVIP = computed(() => user.value?.userRole === UserRole.VIP)
const isSVIP = computed(() => user.value?.userRole === UserRole.SVIP)
const isAdmin = computed(() => user.value?.userRole === UserRole.ADMIN)

// 角色标签类型
const getRoleTagType = computed(() => {
  if (isAdmin.value) return 'danger'
  if (isSVIP.value) return 'warning'
  if (isVIP.value) return 'success'
  return 'info'
})

// 角色标签效果
const getRoleTagEffect = computed(() => {
  if (isAdmin.value || isSVIP.value || isVIP.value) return 'light'
  return 'plain'
})

// 角色文本
const getRoleText = computed(() => {
  if (isAdmin.value) return '管理员'
  if (isSVIP.value) return 'SVIP'
  if (isVIP.value) return 'VIP'
  return '普通用户'
})

// 用户角色计算属性
const userRoleText = computed(() => {
  if (!user.value) return ''
  switch (user.value.userRole) {
    case UserRole.VIP:
      return 'VIP会员'
    case UserRole.SVIP:
      return 'SVIP会员'
    case UserRole.ADMIN:
      return '管理员'
    default:
      return '普通用户'
  }
})

// 会员状态计算属性
const membershipStatus = computed(() => {
  if (!user.value) return ''
  if (user.value.userRole === UserRole.NORMAL) {
    return '非会员'
  }
  if (user.value.userRole === UserRole.ADMIN) {
    return '永久'
  }
  if (user.value.membershipEndTime) {
    return `有效期至 ${formatTime(user.value.membershipEndTime)}`
  }
  return '未知'
})

// v-model 相关
watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 显示对话框
const show = () => {
  visible.value = true
}

// 用户名编辑相关方法
const startEditUsername = () => {
  editingUsername.value = user.value?.username || ''
  isEditingUsername.value = true
  nextTick(() => {
    usernameInputRef.value?.focus()
  })
}

const cancelEditUsername = () => {
  editingUsername.value = ''
  isEditingUsername.value = false
}

const handleUsernameSubmit = async () => {
  if (!editingUsername.value) {
    ElMessage.warning('用户名不能为空')
    return
  }
  try {
    await authStore.updateProfile({
      username: editingUsername.value
    })
    ElMessage.success('用户名修改成功')
    editingUsername.value = ''
    isEditingUsername.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '用户名修改失败')
  }
}

// 邮箱编辑相关方法
const startEditEmail = () => {
  editingEmail.value = user.value?.email || ''
  isEditingEmail.value = true
  nextTick(() => {
    emailInputRef.value?.focus()
  })
}

const cancelEditEmail = () => {
  editingEmail.value = ''
  isEditingEmail.value = false
}

const handleEmailSubmit = async () => {
  if (!editingEmail.value) {
    ElMessage.warning('邮箱不能为空')
    return
  }
  try {
    await authStore.updateProfile({
      email: editingEmail.value
    })
    ElMessage.success('邮箱修改成功')
    editingEmail.value = ''
    isEditingEmail.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '邮箱修改失败')
  }
}

// 修改密码相关方法
const showChangePassword = () => {
  changePasswordVisible.value = true
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await authStore.changePassword(
          passwordForm.value.currentPassword,
          passwordForm.value.newPassword
        )
        ElMessage.success('密码修改成功')
        changePasswordVisible.value = false
      } catch (error: any) {
        ElMessage.error(error.message || '密码修改失败')
      }
    }
  })
}

// 头像相关方法
const handleChangeAvatar = () => {
  // TODO: 实现头像上传功能
  ElMessage.info('头像上传功能开发中')
}

// 绑定邮箱
const showBindEmail = () => {
  isEditingEmail.value = true
  nextTick(() => {
    emailInputRef.value?.focus()
  })
}

// 导出方法供外部使用
defineExpose({
  show
})
</script>

<style lang="scss" scoped>
.info-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 24px;
  margin: -24px -24px 24px -24px;
  background: linear-gradient(135deg, var(--el-color-primary-light-8) 0%, var(--el-color-primary-light-9) 100%);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.avatar-container {
  position: relative;
  width: 88px;
  height: 88px;
}

.avatar-halo {
  position: absolute;
  top: -6px;
  left: -6px;
  right: -6px;
  bottom: -6px;
  border-radius: 50%;
  z-index: 0;
  animation: rotate 3s linear infinite;
  opacity: 0.8;
}

.vip-halo {
  background: conic-gradient(
    from 0deg,
    transparent,
    #ffd700,
    #ffa500,
    transparent
  );
}

.svip-halo {
  background: conic-gradient(
    from 0deg,
    transparent,
    #ff00ff,
    #ff0000,
    transparent
  );
}

.user-avatar {
  position: relative;
  z-index: 1;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--el-color-success) 0%, var(--el-color-success-dark-2) 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.user-avatar:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-overlay .el-icon {
  font-size: 24px;
  color: white;
}

.vip-avatar {
  background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
  box-shadow: 0 6px 16px rgba(255, 165, 0, 0.3);
}

.svip-avatar {
  background: linear-gradient(135deg, #ff00ff 0%, #ff0000 100%);
  box-shadow: 0 6px 16px rgba(255, 0, 255, 0.3);
}

.basic-info {
  flex: 1;
}

.username-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.username-wrapper h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.user-id {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-section {
  display: grid;
  gap: 16px;
  padding: 24px 0;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.info-item:hover {
  background: var(--el-fill-color);
  transform: translateY(-1px);
}

.info-label {
  min-width: 120px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.info-label .el-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.info-value {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.info-value .el-button {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.info-item:hover .info-value .el-button {
  opacity: 1;
}

.security-section {
  padding: 24px;
  margin: 24px -24px -24px -24px;
  background: var(--el-fill-color-light);
  border-top: 1px solid var(--el-border-color-lighter);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.section-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section-header .el-icon {
  font-size: 18px;
  color: var(--el-color-danger);
}

.security-options {
  display: flex;
  gap: 12px;
}

.security-options .el-button {
  border-radius: 8px;
  padding: 12px 20px;
}

.role-tag-vip {
  background: linear-gradient(135deg, #ffd700, #ffa500) !important;
  border: none !important;
  color: white !important;
  padding: 4px 12px !important;
  border-radius: 16px !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(255, 165, 0, 0.3);
}

.role-tag-svip {
  background: linear-gradient(135deg, #ff00ff, #ff0000) !important;
  border: none !important;
  color: white !important;
  padding: 4px 12px !important;
  border-radius: 16px !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(255, 0, 255, 0.3);
}

.crown-icon {
  margin-left: 4px;
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 暗黑模式适配 */
:deep(html.dark) {
  .info-header {
    background: linear-gradient(135deg, var(--el-bg-color-overlay) 0%, var(--el-bg-color) 100%);
  }
  
  .info-item {
    background: var(--el-bg-color-overlay);
  }
  
  .info-item:hover {
    background: var(--el-bg-color);
  }
  
  .security-section {
    background: var(--el-bg-color-overlay);
  }
}

:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 32px 4px rgba(0, 0, 0, .04), 0 8px 20px rgba(0, 0, 0, .08);
}

:deep(.el-dialog__header) {
  padding: 24px;
  margin: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: linear-gradient(to right, var(--el-color-primary-light-7), var(--el-color-primary-light-9));
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 24px;
}

:deep(.el-dialog__body) {
  padding: 0;
  max-height: calc(90vh - 150px);
  overflow-y: auto;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color-page);
}

.user-info-content {
  padding: 24px;
}
</style> 