import { http } from './request'

export interface EmailVerificationStatus {
  email: string
  type: string
  status: string
  description: string
  verified: boolean
}

export const emailApi = {
  // 发送注册验证码
  sendRegistrationCode: (email: string, username: string) =>
    http.post<void>('/api/email/send-registration-code', null, {
      params: { email, username }
    }),

  // 发送密码重置验证码
  sendPasswordResetCode: (email: string) =>
    http.post<void>('/api/email/send-reset-code', null, {
      params: { email }
    }),

  // 验证注册验证码
  verifyRegistrationCode: (email: string, code: string) =>
    http.post<void>('/api/email/verify-registration-code', null, {
      params: { email, code }
    }),

  // 验证密码重置验证码
  verifyPasswordResetCode: (email: string, code: string) =>
    http.post<void>('/api/email/verify-reset-code', null, {
      params: { email, code }
    }),

  // 检查邮箱验证状态
  getVerificationStatus: (email: string, type: string) =>
    http.get<EmailVerificationStatus>('/api/email/verification-status', {
      params: { email, type }
    }),

  // 检查邮箱是否已验证
  isEmailVerified: (email: string, type: string) =>
    http.get<boolean>('/api/email/is-verified', {
      params: { email, type }
    }),

  // 获取支持的验证类型列表
  getVerificationTypes: () =>
    http.get<Record<string, string>>('/api/email/verification-types')
}