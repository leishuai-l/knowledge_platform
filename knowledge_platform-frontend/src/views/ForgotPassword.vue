<template>
  <div class="forgot-password-page">
    <div class="forgot-password-container">
      <div class="forgot-password-card">
        <div class="header">
          <el-icon :size="48" color="#409EFF">
            <Lock />
          </el-icon>
          <h2>忘记密码</h2>
          <p>请按照步骤重置您的密码</p>
        </div>

        <!-- 步骤1: 输入邮箱 -->
        <div v-if="currentStep === 1" class="step-container">
          <h3>第1步：验证邮箱</h3>
          <p>请输入您注册时使用的邮箱地址</p>

          <el-form
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            size="large"
          >
            <el-form-item prop="email">
              <el-input
                v-model="emailForm.email"
                placeholder="请输入邮箱地址"
                :prefix-icon="Message"
                clearable
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="sendingCode"
                @click="sendVerificationCode"
                class="full-width-btn"
              >
                {{ sendingCode ? '发送中...' : '发送验证码' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤2: 输入验证码和新密码 -->
        <div v-if="currentStep === 2" class="step-container">
          <h3>第2步：重置密码</h3>
          <p>验证码已发送到 {{ maskEmail(emailForm.email) }}</p>

          <el-form
            ref="resetFormRef"
            :model="resetForm"
            :rules="resetRules"
            size="large"
          >
            <el-form-item prop="verificationCode">
              <el-input
                v-model="resetForm.verificationCode"
                placeholder="请输入6位验证码"
                :prefix-icon="Key"
                maxlength="6"
                clearable
              >
                <template #suffix>
                  <el-button
                    v-if="countdown === 0"
                    link
                    type="primary"
                    @click="resendCode"
                    :loading="sendingCode"
                  >
                    重新发送
                  </el-button>
                  <span v-else class="countdown">{{ countdown }}s</span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="newPassword">
              <el-input
                v-model="resetForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="resetForm.confirmPassword"
                type="password"
                placeholder="请确认新密码"
                :prefix-icon="Lock"
                show-password
                clearable
                @keyup.enter="resetPassword"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="resetting"
                @click="resetPassword"
                class="full-width-btn"
              >
                {{ resetting ? '重置中...' : '重置密码' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤3: 完成 -->
        <div v-if="currentStep === 3" class="step-container success-container">
          <el-icon :size="64" color="#67C23A">
            <SuccessFilled />
          </el-icon>
          <h3>密码重置成功！</h3>
          <p>您可以使用新密码登录系统了</p>

          <el-button
            type="primary"
            size="large"
            @click="goToLogin"
            class="full-width-btn"
          >
            返回登录
          </el-button>
        </div>

        <!-- 返回登录链接 -->
        <div v-if="currentStep < 3" class="back-link">
          <el-link type="primary" :underline="false" @click="goToLogin">
            <el-icon><ArrowLeft /></el-icon>
            返回登录
          </el-link>
        </div>
      </div>

      <!-- 背景装饰 -->
      <div class="background-decoration">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, Message, Key, SuccessFilled, ArrowLeft } from '@element-plus/icons-vue'
import { forgotPassword, resetPassword as apiResetPassword } from '@/api/auth'

const router = useRouter()

// 表单引用
const emailFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()

// 当前步骤
const currentStep = ref(1)

// 加载状态
const sendingCode = ref(false)
const resetting = ref(false)

// 倒计时
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 邮箱表单
const emailForm = reactive({
  email: ''
})

// 重置密码表单
const resetForm = reactive({
  verificationCode: '',
  newPassword: '',
  confirmPassword: ''
})

// 邮箱验证规则
const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 重置密码验证规则
const resetRules: FormRules = {
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须是6位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!emailFormRef.value) return

  try {
    await emailFormRef.value.validate()
    sendingCode.value = true

    await forgotPassword({ email: emailForm.email })

    ElMessage({
      type: 'success',
      message: '验证码已发送到您的邮箱'
    })

    // 进入下一步
    currentStep.value = 2
    startCountdown()

  } catch (error: any) {
    console.error('Send verification code error:', error)
    ElMessage({
      type: 'error',
      message: error.message || '发送验证码失败，请稍后重试'
    })
  } finally {
    sendingCode.value = false
  }
}

// 重新发送验证码
const resendCode = async () => {
  try {
    sendingCode.value = true

    await forgotPassword({ email: emailForm.email })

    ElMessage({
      type: 'success',
      message: '验证码已重新发送'
    })

    startCountdown()

  } catch (error: any) {
    console.error('Resend verification code error:', error)
    ElMessage({
      type: 'error',
      message: error.message || '重新发送失败，请稍后重试'
    })
  } finally {
    sendingCode.value = false
  }
}

// 重置密码
const resetPassword = async () => {
  if (!resetFormRef.value) return

  try {
    await resetFormRef.value.validate()
    resetting.value = true

    await apiResetPassword({
      email: emailForm.email,
      verificationCode: resetForm.verificationCode,
      newPassword: resetForm.newPassword
    })

    ElMessage({
      type: 'success',
      message: '密码重置成功！'
    })

    // 进入完成步骤
    currentStep.value = 3

  } catch (error: any) {
    console.error('Reset password error:', error)
    ElMessage({
      type: 'error',
      message: error.message || '密码重置失败，请检查验证码'
    })
  } finally {
    resetting.value = false
  }
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer !== null) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }
  }, 1000)
}

// 清理倒计时
const clearCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 掩码邮箱
const maskEmail = (email: string) => {
  const [username, domain] = email.split('@')
  if (username.length <= 2) {
    return `${username}***@${domain}`
  }
  return `${username.substring(0, 2)}***@${domain}`
}

// 返回登录
const goToLogin = () => {
  router.push('/login')
}

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  clearCountdown()
})
</script>

<style scoped>
.forgot-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.forgot-password-container {
  position: relative;
  z-index: 2;
}

.forgot-password-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.header {
  text-align: center;
  margin-bottom: 30px;
}

.header h2 {
  margin: 15px 0 10px;
  color: #2c3e50;
  font-weight: 600;
}

.header p {
  color: #7f8c8d;
  margin: 0;
  font-size: 14px;
}

.step-container {
  margin-bottom: 20px;
}

.step-container h3 {
  color: #2c3e50;
  margin-bottom: 10px;
  font-size: 18px;
}

.step-container p {
  color: #7f8c8d;
  margin-bottom: 20px;
  font-size: 14px;
}

.success-container {
  text-align: center;
}

.success-container h3 {
  color: #67C23A;
  margin: 20px 0 10px;
}

.full-width-btn {
  width: 100%;
  height: 44px;
}

.countdown {
  color: #909399;
  font-size: 14px;
}

.back-link {
  text-align: center;
  margin-top: 20px;
}

.back-link .el-icon {
  margin-right: 5px;
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
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 120px;
  height: 120px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 80px;
  height: 80px;
  top: 70%;
  right: 10%;
  animation-delay: 2s;
}

.circle-3 {
  width: 60px;
  height: 60px;
  top: 30%;
  right: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .forgot-password-card {
    width: 90%;
    max-width: 360px;
    padding: 30px 20px;
  }
}
</style>