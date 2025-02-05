<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h2>个人信息</h2>
          <el-button type="primary" @click="isEditing = true" v-if="!isEditing">
            编辑资料
          </el-button>
        </div>
      </template>

      <el-form 
        ref="formRef"
        :model="userForm"
        :rules="rules"
        label-width="100px"
        :disabled="!isEditing"
      >
        <div class="avatar-section">
          <el-avatar 
            :size="100"
            :src="userForm.avatar || ''"
            @error="() => true"
          >
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <el-upload
            v-if="isEditing"
            class="avatar-uploader"
            :action="`${import.meta.env.VITE_API_BASE_URL.replace('/api', '')}/api/file/upload`"
            :headers="{
              Authorization: `Bearer ${authStore.token}`
            }"
            name="data"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :on-error="handleAvatarError"
            :before-upload="beforeAvatarUpload"
            accept="image/jpeg,image/png"
          >
            <el-button type="primary" size="small">更换头像</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 jpg/png 格式，大小不超过 10MB
              </div>
            </template>
          </el-upload>
        </div>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="userForm.gender" placeholder="请选择性别">
            <el-option label="男" :value="Gender.MALE" />
            <el-option label="女" :value="Gender.FEMALE" />
            <el-option label="未知" :value="Gender.UNKNOWN" />
          </el-select>
        </el-form-item>

        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="userForm.bio"
            type="textarea"
            :rows="4"
            placeholder="请输入个人简介"
          />
        </el-form-item>

        <el-form-item label="注册时间">
          <span>{{ formatDate(userForm.createdAt) }}</span>
        </el-form-item>

        <el-form-item label="会员类型">
          <el-tag :type="roleTagType">{{ roleText }}</el-tag>
        </el-form-item>

        <el-form-item v-if="isUserVIP || isUserSVIP" label="会员到期">
          <span>{{ formatDate(userForm.membershipEndTime) }}</span>
        </el-form-item>

        <el-form-item>
          <template v-if="isEditing">
            <el-button type="primary" @click="handleSubmit">保存</el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="isEditing = true">编辑资料</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 会员信息卡片 -->
    <el-card class="membership-card" v-if="!authStore.isAdmin">
      <template #header>
        <div class="card-header">
          <h2>会员信息</h2>
          <el-button 
            type="warning" 
            @click="router.push('/vip-plans')"
            v-if="!isVIP && !isSVIP"
          >
            升级会员
          </el-button>
        </div>
      </template>

      <div class="membership-info">
        <div class="membership-status">
          <h3>当前状态</h3>
          <el-tag :type="membershipTagType" effect="dark">
            {{ membershipText }}
          </el-tag>
        </div>
        
        <template v-if="isVIP || isSVIP">
          <div class="membership-period">
            <h3>会员有效期</h3>
            <p>{{ formatDate(userForm.membershipStartTime) }} 至 {{ formatDate(userForm.membershipEndTime) }}</p>
          </div>
          <div class="membership-benefits">
            <h3>会员权益</h3>
            <ul>
              <li>无限次数AI对话</li>
              <li>优先响应服务</li>
              <li>专属客服支持</li>
              <li v-if="isSVIP">自定义AI模型</li>
            </ul>
          </div>
        </template>
      </div>
    </el-card>

    <!-- 管理员功能卡片 -->
    <el-card class="admin-card" v-if="authStore.isAdmin">
      <template #header>
        <div class="card-header">
          <h2>管理员功能</h2>
        </div>
      </template>

      <div class="admin-functions">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="hover" @click="router.push('/admin/chats')">
              <el-icon><ChatDotRound /></el-icon>
              <h3>对话管理</h3>
              <p>查看和管理所有用户的对话记录</p>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" @click="router.push('/admin/users')">
              <el-icon><Avatar /></el-icon>
              <h3>用户管理</h3>
              <p>管理用户账号和权限设置</p>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" @click="router.push('/admin/notifications')">
              <el-icon><Bell /></el-icon>
              <h3>通知管理</h3>
              <p>发布和管理系统通知</p>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { ChatDotRound, Avatar, Bell, UserFilled } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { UserRole, Gender, type User, isAdmin, isVIP, isSVIP } from '../types/user'

const router = useRouter()
const authStore = useAuthStore()
const { user, isUserAdmin, isUserVIP, isUserSVIP } = storeToRefs(authStore)
const formRef = ref<FormInstance>()
const isEditing = ref(false)

interface UserForm {
  username: string;
  email: string;
  phone: string;
  bio: string;
  avatar: string;
  gender: Gender;
  createdAt: string;
  membershipStartTime: string;
  membershipEndTime: string;
  userRole: UserRole;
  id: number;
}

const userForm = ref<UserForm>({
  username: '',
  email: '',
  phone: '',
  bio: '',
  avatar: '',
  gender: Gender.UNKNOWN,
  createdAt: '',
  membershipStartTime: '',
  membershipEndTime: '',
  userRole: UserRole.NORMAL,
  id: 0,
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  bio: [
    { max: 500, message: '个人简介不能超过500个字符', trigger: 'blur' }
  ]
}

const roleText = computed(() => {
  if (isUserAdmin.value) return '管理员'
  if (isUserSVIP.value) return 'SVIP会员'
  if (isUserVIP.value) return 'VIP会员'
  return '普通用户'
})

const roleTagType = computed(() => {
  if (isUserAdmin.value) return 'danger'
  if (isUserSVIP.value) return 'success'
  if (isUserVIP.value) return 'warning'
  return 'info'
})

const membershipText = computed(() => {
  if (isUserSVIP.value) return 'SVIP会员'
  if (isUserVIP.value) return 'VIP会员'
  return '普通用户'
})

const membershipTagType = computed(() => {
  if (isUserSVIP.value) return 'success'
  if (isUserVIP.value) return 'warning'
  return 'info'
})

const formatDate = (date: string) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const handleAvatarSuccess = (response: any) => {
  if (response) {
    // 后端直接返回完整的URL
    userForm.value.avatar = response
    ElMessage.success('头像上传成功')
    // 更新全局用户信息
    if (user.value) {
      authStore.updateProfile({
        ...user.value,
        avatar: response
      })
    }
  } else {
    ElMessage.error('头像上传失败')
  }
}

const handleAvatarError = (error: any) => {
  console.error('Avatar upload failed:', error)
  const errorMessage = error?.response?.data || error?.message || '头像上传失败，请稍后重试'
  ElMessage.error(errorMessage)
}

const beforeAvatarUpload = (file: File) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isJPG && !isPNG) {
    ElMessage.error('上传头像只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('上传头像大小不能超过 10MB!')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    const submitData = {
      id: userForm.value.id,
      username: userForm.value.username,
      email: userForm.value.email,
      phone: userForm.value.phone,
      bio: userForm.value.bio,
      avatar: userForm.value.avatar,
      gender: userForm.value.gender,
      userRole: userForm.value.userRole
    }
    
    const result = await authStore.updateProfile(submitData)
    if (result.code === 200) {
      ElMessage.success('个人信息更新成功')
      isEditing.value = false
      // 更新本地表单数据
      if (result.data) {
        userForm.value = {
          ...userForm.value,
          ...result.data
        }
      }
    } else {
      throw new Error(result.message || '更新失败')
    }
  } catch (error: any) {
    console.error('Profile update error:', error)
    ElMessage.error(error.message || '更新个人信息失败')
  }
}

const cancelEdit = () => {
  isEditing.value = false
  // 重置表单为当前用户信息
  if (user.value) {
    userForm.value = {
      username: user.value.username || '',
      email: user.value.email || '',
      phone: user.value.phone || '',
      bio: user.value.bio || '',
      avatar: user.value.avatar || '',
      gender: user.value.gender || Gender.UNKNOWN,
      createdAt: user.value.createdAt || '',
      membershipStartTime: user.value.membershipStartTime || '',
      membershipEndTime: user.value.membershipEndTime || '',
      userRole: user.value.userRole || UserRole.NORMAL,
      id: user.value.id || 0,
    }
  }
}

onMounted(async () => {
  // 初始化表单数据
  if (user.value) {
    userForm.value = {
      username: user.value.username || '',
      email: user.value.email || '',
      phone: user.value.phone || '',
      bio: user.value.bio || '',
      avatar: user.value.avatar || '',
      gender: user.value.gender || Gender.UNKNOWN,
      createdAt: user.value.createdAt || '',
      membershipStartTime: user.value.membershipStartTime || '',
      membershipEndTime: user.value.membershipEndTime || '',
      userRole: user.value.userRole || UserRole.NORMAL,
      id: user.value.id || 0,
    }
  }
})
</script>

<style scoped>
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  display: grid;
  gap: 20px;
}

.profile-card, .membership-card, .admin-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 1.5em;
  color: var(--el-text-color-primary);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.avatar-uploader {
  text-align: center;
}

.membership-info {
  display: grid;
  gap: 20px;
}

.membership-status, .membership-period {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.membership-benefits h3 {
  margin-bottom: 12px;
}

.membership-benefits ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.membership-benefits li {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
  display: flex;
  align-items: center;
  gap: 8px;
}

.membership-benefits li::before {
  content: "✓";
  color: var(--el-color-success);
}

.admin-functions :deep(.el-card) {
  cursor: pointer;
  text-align: center;
  transition: all 0.3s ease;
  height: 100%;
}

.admin-functions :deep(.el-card:hover) {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.admin-functions :deep(.el-icon) {
  font-size: 32px;
  color: var(--el-color-primary);
  margin-bottom: 12px;
}

.admin-functions :deep(h3) {
  margin: 12px 0;
  color: var(--el-text-color-primary);
}

.admin-functions :deep(p) {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 10px;
  }

  .admin-functions :deep(.el-col) {
    margin-bottom: 16px;
  }
}
</style> 