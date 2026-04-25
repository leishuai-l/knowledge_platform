import { http } from './request'
import type { LoginRequest, RegisterRequest, JwtResponse, User, ApiResponse } from '@/types'

export const authApi = {
  // 登录
  login: (data: LoginRequest) =>
    http.post<JwtResponse>('/api/auth/login', data),

  // 注册
  register: (data: RegisterRequest) =>
    http.post<JwtResponse>('/api/auth/register', data),

  // 登出
  logout: (refreshToken: string) =>
    http.post('/api/auth/logout', { refreshToken }),

  // 刷新令牌
  refreshToken: (refreshToken: string) =>
    http.post<JwtResponse>('/api/auth/refresh', { refreshToken }),

  // 获取当前用户信息
  getCurrentUser: () =>
    http.get<User>('/api/auth/validate'),

  // 验证令牌有效性
  validateToken: () =>
    http.get('/api/auth/validate'),

  // 发送注册验证码
  sendRegistrationCode: (email: string, username: string) =>
    http.post('/api/email/send-registration-code', null, {
      params: { email, username }
    }),

  // 验证注册验证码
  verifyRegistrationCode: (email: string, code: string) =>
    http.post('/api/email/verify-registration-code', null, {
      params: { email, code }
    }),

  // 检查邮箱是否已验证
  isEmailVerified: (email: string, type: string = 'REGISTRATION') =>
    http.get<boolean>('/api/email/is-verified', {
      params: { email, type }
    }),

  // 检查用户名是否可用
  checkUsernameAvailability: (username: string) =>
    http.get<boolean>('/api/auth/check-username', {
      params: { username }
    }),

  // 检查邮箱是否可用
  checkEmailAvailability: (email: string) =>
    http.get<boolean>('/api/auth/check-email', {
      params: { email }
    }),

  // 忘记密码 - 发送验证码
  sendPasswordResetCode: (email: string) =>
    http.post('/api/email/send-reset-code', null, {
      params: { email }
    }),

  // 重置密码
  resetPasswordWithCode: (email: string, verificationCode: string, newPassword: string) =>
    http.post('/api/auth/reset-password', {
      email,
      verificationCode,
      newPassword
    })
}

// 导出方便使用的方法
export const { login, register, logout, refreshToken: refresh } = authApi

// 忘记密码相关方法导出
export const forgotPassword = (data: { email: string }) =>
  authApi.sendPasswordResetCode(data.email)

export const resetPassword = (data: { email: string; verificationCode: string; newPassword: string }) =>
  authApi.resetPasswordWithCode(data.email, data.verificationCode, data.newPassword)