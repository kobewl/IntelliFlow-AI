<template>
  <div class="register-view">
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <div class="logo-container">
            <div class="logo-circle">
              <el-icon :size="32" color="var(--el-color-primary)">
                <ChatRound />
              </el-icon>
            </div>
            <h1>KobeAI</h1>
          </div>
          <h2>创建账号</h2>
          <p class="subtitle">加入我们，开启智能对话之旅</p>
        </div>
        
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              :prefix-icon="Message"
              size="large"
            >
              <template #append>
                <el-button 
                  :disabled="!!timer || !form.email"
                  @click="handleSendCode"
                >
                  {{ timer ? `${countdown}s后重试` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item label="验证码" prop="code">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              size="large"
            />
          </el-form-item>
          
          <div class="form-actions">
            <el-button
              type="primary"
              native-type="submit"
              :loading="loading"
              class="submit-btn"
              size="large"
            >
              注册
              <template #icon>
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </template>
            </el-button>
            
            <div class="form-links">
              <router-link to="/auth/login" class="login-link">
                已有账号？立即登录
                <el-icon><ArrowRight /></el-icon>
              </router-link>
            </div>
          </div>
        </el-form>
      </div>

      <!-- 装饰元素 -->
      <div class="decoration-container">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
        <div class="line line-1"></div>
        <div class="line line-2"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowRight, ChatRound, Message } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import * as authApi from '../../api/auth'
import { ApiResponse, EmailCodeResponse, RegisterResponse } from '../../types/api'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const timer = ref<number | null>(null)
const countdown = ref(60)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  code: '',
  phone: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (loading.value) return
  
  try {
    await formRef.value?.validate()
    loading.value = true
    
    // 先验证验证码
    const verifyRes = await authApi.verifyEmailCode(form.email, form.code) as ApiResponse<EmailCodeResponse>
    if (verifyRes.code !== 200) {
      throw new Error(verifyRes.message || '验证码验证失败')
    }
    
    // 提交注册
    const res = await authApi.register(form) as ApiResponse<RegisterResponse>
    
    if (res.code === 200) {
      ElMessage.success('注册成功')
      router.push('/auth/login')
    } else {
      throw new Error(res.message || '注册失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}

// 发送验证码
const handleSendCode = async () => {
  try {
    await formRef.value?.validateField('email')
    const res = await authApi.sendEmailCode(form.email) as ApiResponse<EmailCodeResponse>
    
    if (res.code === 200) {
      ElMessage.success('验证码已发送，请注意查收')
      
      // 开始倒计时
      countdown.value = 60
      timer.value = window.setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          if (timer.value) {
            clearInterval(timer.value)
            timer.value = null
          }
        }
      }, 1000)
    } else {
      throw new Error(res.message || '发送验证码失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  }
}
</script>

<style scoped>
.register-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 48px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 
    0 4px 6px rgba(0, 0, 0, 0.02),
    0 10px 15px rgba(0, 0, 0, 0.03),
    0 20px 30px rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  position: relative;
  z-index: 2;
  transform: translateY(0);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.auth-card:hover {
  transform: translateY(-5px);
  box-shadow: 
    0 6px 8px rgba(0, 0, 0, 0.03),
    0 12px 18px rgba(0, 0, 0, 0.04),
    0 24px 36px rgba(0, 0, 0, 0.05);
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}

.logo-container h1 {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, #409eff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
}

.logo-circle {
  width: 56px;
  height: 56px;
  border-radius: 28px;
  background: linear-gradient(135deg, var(--el-color-primary-light-8) 0%, var(--el-color-primary-light-9) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px rgba(var(--el-color-primary-rgb), 0.15);
  transition: transform 0.3s ease;
}

.logo-circle:hover {
  transform: scale(1.05);
}

.auth-header {
  text-align: center;
  margin-bottom: 40px;
}

.auth-header h2 {
  font-size: 32px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 12px;
}

.subtitle {
  font-size: 16px;
  color: var(--el-text-color-regular);
  margin: 0;
}

:deep(.el-form-item__label) {
  font-size: 16px;
  font-weight: 500;
  padding-bottom: 8px;
}

:deep(.el-input__wrapper) {
  padding: 6px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.95);
  border-color: var(--el-color-primary-light-5);
}

:deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 4px var(--el-color-primary-light-8);
}

:deep(.el-input__inner) {
  height: 42px;
  font-size: 16px;
}

.form-actions {
  margin-top: 32px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, #409eff 100%);
  border: none;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(var(--el-color-primary-rgb), 0.3);
}

.form-links {
  margin-top: 24px;
  text-align: center;
}

.login-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  color: var(--el-color-primary);
  text-decoration: none;
  transition: all 0.3s ease;
}

.login-link:hover {
  gap: 8px;
  opacity: 0.8;
}

.login-link .el-icon {
  transition: transform 0.3s ease;
}

.login-link:hover .el-icon {
  transform: translateX(4px);
}

/* 装饰元素 */
.decoration-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(var(--el-color-primary-rgb), 0.1) 0%, rgba(var(--el-color-primary-rgb), 0.05) 100%);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  left: -50px;
  animation: float 6s ease-in-out infinite;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  right: 15%;
  animation: float 10s ease-in-out infinite;
}

.line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(var(--el-color-primary-rgb), 0.1), transparent);
  height: 2px;
  filter: blur(1px);
}

.line-1 {
  width: 200px;
  top: 20%;
  left: 10%;
  transform: rotate(-45deg);
  animation: glow 4s ease-in-out infinite;
}

.line-2 {
  width: 300px;
  bottom: 30%;
  right: 5%;
  transform: rotate(45deg);
  animation: glow 6s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

@keyframes glow {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.6;
  }
}

@media (max-width: 768px) {
  .auth-card {
    padding: 32px 24px;
  }

  .auth-header h2 {
    font-size: 28px;
  }

  .circle-1 {
    width: 200px;
    height: 200px;
  }

  .circle-2 {
    width: 150px;
    height: 150px;
  }

  .circle-3 {
    width: 100px;
    height: 100px;
  }
}
</style> 