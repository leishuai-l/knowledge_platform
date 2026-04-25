import { http } from './request'
import type { PageResponse, Notification } from '@/types'

// 获取通知列表
export const getNotifications = (params: {
  page?: number
  size?: number
  isRead?: boolean
}) => {
  return http.get<PageResponse<Notification>>('/api/notifications', { params })
}

// 获取未读通知数量
export const getUnreadCount = () => {
  return http.get<number>('/api/notifications/unread-count')
}

// 获取最近通知
export const getRecentNotifications = (limit: number = 5) => {
  return http.get<Notification[]>('/api/notifications/recent', {
    params: { limit }
  })
}

// 标记通知为已读
export const markNotificationAsRead = (id: number) => {
  return http.put<void>(`/api/notifications/${id}/read`)
}

// 批量标记通知为已读
export const batchMarkAsRead = (notificationIds: number[]) => {
  return http.put<void>('/api/notifications/batch-read', notificationIds)
}

// 标记所有通知为已读
export const markAllAsRead = () => {
  return http.put<void>('/api/notifications/mark-all-read')
}

// 删除通知
export const deleteNotification = (id: number) => {
  return http.delete<void>(`/api/notifications/${id}`)
}

// 发送系统公告（管理员）
export const sendSystemAnnouncement = (data: { title: string; content: string }) => {
  return http.post<void>('/api/notifications/system-announcement', data)
}

// 发送定向通知（管理员）
export const sendTargetedNotification = (data: {
  title: string;
  content: string;
  userIds: number[];
  type?: string;
}) => {
  return http.post<void>('/api/notifications/targeted-notification', data)
}

// 获取通知统计信息（管理员）
export const getNotificationStatistics = () => {
  return http.get<any>('/api/notifications/admin/statistics')
}

// 获取所有通知列表（管理员）
export const getAllNotifications = (params: {
  page?: number
  size?: number
  type?: string
  isRead?: boolean
}) => {
  return http.get<PageResponse<Notification>>('/api/notifications/admin/all', { params })
}