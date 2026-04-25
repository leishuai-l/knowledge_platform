import { http } from './request'
import type { PageResponse, User } from '@/types'

export interface Comment {
  id: number
  documentId: number
  userId: number
  parentId?: number
  content: string
  createdAt: string
  updatedAt: string
  isDeleted: boolean
  user?: Pick<User, 'id' | 'username' | 'avatar'>
  document?: {
    id: number
    title: string
  }
  replies?: Comment[]
  replyCount?: number
}

export interface CommentCreateRequest {
  content: string
  parentId?: number
}

export const commentApi = {
  // 创建评论
  createComment: (data: { documentId: number; content: string; parentId?: number }) =>
    http.post<Comment>(`/api/comments/documents/${data.documentId}`, {
      documentId: data.documentId,  // 添加documentId到请求体
      content: data.content,
      parentId: data.parentId
    }),

  // 获取文档评论列表
  getDocumentComments: (documentId: number, page: number = 0, size: number = 20) =>
    http.get<PageResponse<Comment>>(`/api/comments/documents/${documentId}`, {
      params: { page, size }
    }),

  // 获取评论的回复列表
  getCommentReplies: (parentId: number) =>
    http.get<Comment[]>(`/api/comments/${parentId}/replies`),

  // 获取用户评论列表
  getUserComments: (page: number = 0, size: number = 20) =>
    http.get<PageResponse<Comment>>('/api/comments/user', {
      params: { page, size }
    }),

  // 获取评论详情
  getCommentById: (commentId: number) =>
    http.get<Comment>(`/api/comments/${commentId}`),

  // 更新评论内容
  updateComment: (commentId: number, content: string) =>
    http.put<Comment>(`/api/comments/${commentId}`, {
      content: content
    }),

  // 删除评论
  deleteComment: (commentId: number) =>
    http.delete<void>(`/api/comments/${commentId}`),

  // 获取文档评论统计
  getDocumentCommentStatistics: (documentId: number) =>
    http.get<{
      totalComments: number
      recentComments: Comment[]
      topCommentsByReplies: Comment[]
    }>(`/api/comments/documents/${documentId}/statistics`),

  // 获取用户评论统计
  getUserCommentStatistics: () =>
    http.get<{
      totalComments: number
      commentsByMonth: { [key: string]: number }
      topDocumentsByComments: any[]
    }>('/api/comments/user/statistics'),

  // 获取最新评论
  getRecentComments: (limit: number = 10) =>
    http.get<Comment[]>('/api/comments/recent', {
      params: { limit }
    }),

  // 获取热门评论
  getPopularComments: (limit: number = 10) =>
    http.get<Comment[]>('/api/comments/popular', {
      params: { limit }
    }),

  // 检查用户是否已评论过文档
  hasUserCommentedDocument: (documentId: number) =>
    http.get<boolean>(`/api/comments/documents/${documentId}/check`)
}