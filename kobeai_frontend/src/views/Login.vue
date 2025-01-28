<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo">
        <el-icon :size="40" class="logo-icon">
          <ChatRound />
        </el-icon>
        <h1>KobeAI</h1>
      </div>

      <h2>{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
      <p class="subtitle">{{ isLogin ? '登录你的账号继续使用' : '注册新账号开始使用' }}</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        @submit.prevent="handleSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="submit-btn"
            @click="handleSubmit"
          >
            {{ isLogin ? '登录' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="switch-mode">
        {{ isLogin ? '没有账号？' : '已有账号？' }}
        <el-button
          link
          type="primary"
          @click="isLogin = !isLogin"
        >
          {{ isLogin ? '立即注册' : '立即登录' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatRound, User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLogin = ref(true)
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = ref({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    if (isLogin.value) {
      await authStore.login(form.value.username, form.value.password)
      ElMessage.success('登录成功')
    } else {
      await authStore.register(form.value.username, form.value.password)
      ElMessage.success('注册成功')
    }
    
    // 跳转到之前的页面或首页
    const redirect = route.query.redirect as string
    router.replace(redirect || '/chat')
  } catch (error: any) {
    ElMessage.error(error)
  } finally {
    loading.value = false
  }
}

defineOptions({
  name: 'Login'
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}

.logo-icon {
  color: var(--el-color-primary);
}

.logo h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 500;
  text-align: center;
  color: var(--el-text-color-primary);
}

.subtitle {
  margin: 0 0 32px;
  font-size: 16px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.submit-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
}

.switch-mode {
  margin-top: 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

:deep(.el-input__wrapper) {
  padding: 12px;
}

:deep(.el-input__inner) {
  font-size: 16px;
}

:deep(.el-input__prefix-inner) {
  font-size: 20px;
}
</style> 