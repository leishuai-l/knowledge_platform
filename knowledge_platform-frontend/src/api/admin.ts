import { http } from './request'
import type { Document, User, PageResponse } from '@/types'

export interface AdminUserSearchParams {
  page: number
  size: number
  keyword?: string
}

export interface AdminDocumentSearchParams {
  page: number
  size: number
  status?: string
  keyword?: string
}

export const normalizeAdminPageResponse = <T>(payload: any): PageResponse<T> => {
  const pageData = payload?.data ?? payload ?? {}
  const list = pageData.content ?? pageData.list ?? []
  const total = Number(pageData.totalElements ?? pageData.total ?? list.length)
  const rawPage = Number(pageData.number ?? pageData.page ?? 0)
  const page = pageData.number == null ? rawPage : rawPage + 1
  const size = Number(pageData.size ?? (Array.isArray(list) ? list.length : 0))
  const pages = Number(pageData.totalPages ?? pageData.pages ?? 0)

  return {
    list: Array.isArray(list) ? list : [],
    total: Number.isFinite(total) ? total : 0,
    page: Number.isFinite(page) && page > 0 ? page : 1,
    size: Number.isFinite(size) ? size : 0,
    pages: Number.isFinite(pages) ? pages : 0
  }
}

export const adminApi = {
  // 获取文档列表（支持筛选）
  getDocuments: (params: AdminDocumentSearchParams) =>
    http.get<PageResponse<Document>>('/api/admin/documents', {
      params: {
        page: params.page - 1, // 后端使用0基索引
        size: params.size,
        status: params.status,
        keyword: params.keyword
      }
    }),

  // 获取待审核文档
  getPendingDocuments: (page: number = 1, size: number = 10) =>
    http.get<PageResponse<Document>>('/api/admin/documents/pending', {
      params: {
        page: page - 1, // 后端使用0基索引
        size
      }
    }),

  // 审核通过文档
  approveDocument: (id: number) =>
    http.post(`/api/admin/documents/${id}/approve`),

  // 拒绝文档
  rejectDocument: (id: number, reason: string) =>
    http.post(`/api/admin/documents/${id}/reject`, null, {
      params: { reason }
    }),

  // 批量审核通过
  batchApproveDocuments: (ids: number[]) =>
    http.post('/api/admin/documents/batch-approve', ids),

  // 批量拒绝（暂时不支持）
  batchRejectDocuments: (ids: number[], reason: string) =>
    Promise.reject(new Error('批量拒绝功能暂不支持')),

  // 删除文档
  deleteDocument: (id: number) =>
    http.delete(`/api/admin/documents/${id}`),

  // 批量删除文档
  batchDeleteDocuments: async (ids: number[]) => {
    await Promise.all(ids.map(id => adminApi.deleteDocument(id)))
  },

  // 获取用户列表
  getUsers: (params: AdminUserSearchParams) =>
    http.get<PageResponse<User>>('/api/users/search', {
      params: {
        page: params.page - 1,
        size: params.size,
        keyword: params.keyword
      }
    }),

  // 锁定用户
  lockUser: (id: number) =>
    http.post(`/api/admin/users/${id}/lock`),

  // 解锁用户
  unlockUser: (id: number) =>
    http.post(`/api/admin/users/${id}/unlock`),

  // 切换用户状态（通过禁用/启用实现）
  toggleUserStatus: (user: User) => {
    if (user.status === 'ACTIVE') {
      return http.post(`/api/admin/users/${user.id}/disable`, null, {
        params: { reason: '管理员操作' }
      })
    }

    return http.post(`/api/admin/users/${user.id}/enable`)
  },

  // 获取管理员统计数据
  getAdminStats: () =>
    http.get('/api/admin/dashboard'),

  // 社区管理 - 帖子
  getForumTopics: (params: any) =>
    http.get('/api/admin/forum/topics', {
      params: {
        ...params,
        page: Math.max(1, params?.page ?? 1) - 1
      }
    }),
  toggleTopicPin: (id: number) =>
    http.post(`/api/admin/forum/topics/${id}/pin`),
  toggleTopicFeature: (id: number) =>
    http.post(`/api/admin/forum/topics/${id}/feature`),
  toggleTopicHide: (id: number) =>
    http.put(`/api/admin/forum/topics/${id}`),
  deleteTopic: (id: number) =>
    http.delete(`/api/admin/forum/topics/${id}`),

  // 社区管理 - 评论
  getForumReplies: (params: any) =>
    http.get('/api/admin/forum/replies', {
      params: {
        ...params,
        page: Math.max(1, params?.page ?? 1) - 1
      }
    }),
  deleteReply: (id: number) =>
    http.delete(`/api/admin/forum/replies/${id}`),

  // 社区管理 - 板块
  getForumCategories: () =>
    http.get('/api/admin/forum/categories'),
  createForumCategory: (data: any) =>
    http.post('/api/admin/forum/categories', data),
  updateForumCategory: (id: number, data: any) =>
    http.put(`/api/admin/forum/categories/${id}`, data),
  deleteForumCategory: (id: number) =>
    http.delete(`/api/admin/forum/categories/${id}`),

  // 社区管理 - 举报
  getReports: (params: any) =>
    http.get('/api/admin/reports', { params }),
  handleReport: (id: number, status: string) =>
    http.post(`/api/admin/reports/${id}/handle`, { status })
}
