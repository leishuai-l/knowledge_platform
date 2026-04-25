import { http } from './request'
import type { User } from '@/types'

export interface UserProfileUpdate {
  username?: string
  email?: string
}

export interface PasswordChange {
  oldPassword: string
  newPassword: string
}

export const userApi = {
  // 获取当前用户信息
  getCurrentUser: () =>
    http.get<User>('/api/users/profile'),

  // 发送邮箱验证码
  sendEmailVerification: (newEmail: string) =>
    http.post<void>('/api/users/send-email-verification', null, {
      params: { newEmail }
    }),

  // 验证邮箱并更新
  verifyAndUpdateEmail: (newEmail: string, verificationCode: string) =>
    http.put<User>('/api/users/verify-email', null, {
      params: { newEmail, verificationCode }
    }),

  // 修改密码
  changePassword: (data: PasswordChange) =>
    http.put<void>('/api/users/password', null, {
      params: data
    }),

  // 上传用户头像
  uploadAvatar: (formData: FormData) =>
    http.post<{ avatarUrl: string; message: string }>('/api/users/avatar', formData),
    // 注意：不要手动设置 Content-Type，让浏览器自动设置（包含 boundary）

  // 获取用户统计信息
  getUserStatistics: () =>
    http.get<{
      totalPoints: number
      totalUploads: number
      totalDownloads: number
      totalComments: number
      totalRatings: number
      memberSince: string
    }>('/api/users/statistics'),

  // 获取指定用户的公开信息
  getUserPublicInfo: (id: number) =>
    http.get<User>(`/api/users/${id}`),

  // 删除用户账户
  deleteAccount: (password: string) =>
    http.delete<void>('/api/users/profile', {
      params: { password }
    })
}