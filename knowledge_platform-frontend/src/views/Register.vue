<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <el-icon :size="48" color="#409EFF">
            <Document />
          </el-icon>
          <h2>加入知享校园</h2>
          <p>创建账户，开始分享和学习之旅</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          size="large"
          @submit.prevent="handleSubmit"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
              @blur="checkUsernameAvailability"
            />
            <div v-if="usernameCheckResult" :class="['check-result', usernameCheckResult.type]">
              {{ usernameCheckResult.message }}
            </div>
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱地址"
              :prefix-icon="Message"
              clearable
              @blur="checkEmailAvailability"
            />
            <div v-if="emailCheckResult" :class="['check-result', emailCheckResult.type]">
              {{ emailCheckResult.message }}
            </div>
          </el-form-item>

          <el-form-item prop="verificationCode">
            <div class="verification-code-wrapper">
              <el-input
                v-model="registerForm.verificationCode"
                placeholder="请输入邮箱验证码"
                :prefix-icon="Key"
                clearable
                maxlength="6"
                style="flex: 1; margin-right: 12px;"
              />
              <el-button
                type="primary"
                :disabled="!registerForm.email || sendCodeLoading || countdown > 0"
                :loading="sendCodeLoading"
                @click="sendVerificationCode"
                style="width: 120px;"
              >
                {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              :prefix-icon="Lock"
              show-password
              clearable
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item>
            <div class="agreement">
              <el-checkbox v-model="agreeTerms">
                我已阅读并同意
                <el-link type="primary" :underline="false">服务条款</el-link>
                和
                <el-link type="primary" :underline="false">隐私政策</el-link>
              </el-checkbox>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              :disabled="!agreeTerms"
              @click="handleSubmit"
              class="register-button"
            >
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>
          </el-form-item>

          <el-form-item>
            <div class="login-link">
              已有账户？
              <el-link type="primary" :underline="false" @click="goToLogin">
                立即登录
              </el-link>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 背景装饰 -->
      <div class="background-decoration">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
        <div class="circle circle-4"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Document, User, Lock, Message, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { emailApi } from '@/api/email'
import { authApi } from '@/api/auth'
import type { RegisterRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const agreeTerms = ref(false)

const registerForm = reactive<RegisterRequest & { confirmPassword: string }>({
  username: '',
  email: '',
  password: '',
  verificationCode: '',
  confirmPassword: ''
})

const sendCodeLoading = ref(false)
const countdown = ref(0)
const usernameCheckResult = ref<{type: 'success' | 'error', message: string} | null>(null)
const emailCheckResult = ref<{type: 'success' | 'error', message: string} | null>(null)

// 自定义验证规则
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请确认密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateVerificationCode = (_rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入邮箱验证码'))
  } else if (!/^\d{6}$/.test(value)) {
    callback(new Error('验证码必须是6位数字'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, validator: validateVerificationCode, trigger: 'blur' }
  ]
}

// 检查用户名可用性
const checkUsernameAvailability = async () => {
  if (!registerForm.username || registerForm.username.length < 3) {
    usernameCheckResult.value = null
    return
  }

  try {
    const response = await authApi.checkUsernameAvailability(registerForm.username)
    if (response.data.data) {
      usernameCheckResult.value = {
        type: 'success',
        message: '✓ 用户名可用'
      }
    } else {
      usernameCheckResult.value = {
        type: 'error',
        message: '✗ 用户名已被占用'
      }
    }
  } catch (error) {
    usernameCheckResult.value = {
      type: 'error',
      message: '✗ 用户名已被占用'
    }
  }
}

// 检查邮箱可用性
const checkEmailAvailability = async () => {
  if (!registerForm.email || !/\S+@\S+\.\S+/.test(registerForm.email)) {
    emailCheckResult.value = null
    return
  }

  try {
    const response = await authApi.checkEmailAvailability(registerForm.email)
    if (response.data.data) {
      emailCheckResult.value = {
        type: 'success',
        message: '✓ 邮箱可用'
      }
    } else {
      emailCheckResult.value = {
        type: 'error',
        message: '✗ 邮箱已被注册'
      }
    }
  } catch (error) {
    emailCheckResult.value = {
      type: 'error',
      message: '✗ 邮箱已被注册'
    }
  }
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }

  if (!registerForm.username) {
    ElMessage.warning('请先输入用户名')
    return
  }

  try {
    sendCodeLoading.value = true
    await emailApi.sendRegistrationCode(registerForm.email, registerForm.username)

    ElMessage.success('验证码已发送到您的邮箱')
    startCountdown()
  } catch (error: any) {
    console.error('Send verification code error:', error)

    // 获取具体的错误消息
    let errorMessage = '验证码发送失败，请稍后重试'

    if (error.message) {
      // 根据具体错误提供友好提示
      if (error.message.includes('验证码尚未过期')) {
        errorMessage = error.message
      } else if (error.message.includes('邮箱')) {
        errorMessage = error.message
      } else if (error.message.includes('用户名')) {
        errorMessage = error.message
      } else {
        errorMessage = error.message
      }
    }

    ElMessage.error(errorMessage)
  } finally {
    sendCodeLoading.value = false
  }
}

// 倒计时
let countdownTimer: ReturnType<typeof setInterval> | null = null

const startCountdown = () => {
  // 如果已有定时器，先清除
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }
  }, 1000)
}

// 组件卸载时清除定时器
onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})

const goToLogin = () => {
  router.push({
    path: '/login',
    query: route.query.redirect ? { redirect: route.query.redirect as string } : undefined
  })
}

const handleSubmit = async () => {
  if (!registerFormRef.value) return

  if (!agreeTerms.value) {
    ElMessage.warning('请先同意服务条款和隐私政策')
    return
  }

  try {
    await registerFormRef.value.validate()

    loading.value = true

    // 提交注册数据，包含验证码
    const registerData = {
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      verificationCode: registerForm.verificationCode
    }
    await userStore.register(registerData)

    ElMessage({
      type: 'success',
      message: '注册成功，欢迎加入知享校园！'
    })

    const redirect = route.query.redirect as string | undefined
    setTimeout(() => {
      router.push(redirect || '/home')
    }, 1500)
  } catch (error: any) {
    console.error('Register error:', error)

    // 获取具体的错误消息
    let errorMessage = '注册失败，请检查网络连接或稍后重试'

    if (error.message) {
      // 根据具体错误提供友好提示
      if (error.message.includes('用户名已存在')) {
        errorMessage = '用户名已被占用，请选择其他用户名'
      } else if (error.message.includes('邮箱已存在')) {
        errorMessage = '邮箱已被注册，请使用其他邮箱或直接登录'
      } else if (error.message.includes('用户名')) {
        errorMessage = error.message
      } else if (error.message.includes('邮箱')) {
        errorMessage = error.message
      } else if (error.message.includes('密码')) {
        errorMessage = error.message
      } else if (error.message.includes('验证码')) {
        errorMessage = error.message
      } else {
        errorMessage = error.message
      }
    }

    ElMessage({
      type: 'error',
      message: errorMessage
    })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.register-container {
  position: relative;
  z-index: 2;
}

.register-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  max-height: 90vh;
  overflow-y: auto;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h2 {
  margin: 15px 0 10px;
  color: #2c3e50;
  font-weight: 600;
}

.register-header p {
  color: #7f8c8d;
  margin: 0;
  font-size: 14px;
}

.agreement {
  font-size: 14px;
  color: #7f8c8d;
  line-height: 1.5;
}

.register-button {
  width: 100%;
  height: 44px;
}

.login-link {
  text-align: center;
  color: #7f8c8d;
  font-size: 14px;
}

.verification-code-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
}

.check-result {
  margin-top: 5px;
  font-size: 12px;
  padding: 2px 0;
}

.check-result.success {
  color: #67C23A;
}

.check-result.error {
  color: #F56C6C;
}

/* 背景装饰 */
.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 8s ease-in-out infinite;
}

.circle-1 {
  width: 100px;
  height: 100px;
  top: 20%;
  left: 15%;
  animation-delay: 0s;
}

.circle-2 {
  width: 60px;
  height: 60px;
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.circle-3 {
  width: 80px;
  height: 80px;
  top: 10%;
  right: 25%;
  animation-delay: 4s;
}

.circle-4 {
  width: 40px;
  height: 40px;
  top: 80%;
  left: 20%;
  animation-delay: 6s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-card {
    width: 90%;
    max-width: 380px;
    padding: 30px 20px;
    max-height: 95vh;
  }

  .register-header h2 {
    font-size: 20px;
  }
}
</style>