import { http } from './request'
import type { PageResponse, User } from '@/types'

export interface Rating {
  id: number
  documentId: number
  userId: number
  rating: number
  createdAt: string
  updatedAt: string
  user?: Pick<User, 'id' | 'username' | 'avatar'>
  document?: {
    id: number
    title: string
    description?: string
  }
}

export interface RatingCreateRequest {
  score: number
  comment?: string
}

export const ratingApi = {
  // 创建或更新评分
  createRating: (data: { documentId: number; score: number; comment?: string }) =>
    http.post<Rating>(`/api/ratings/documents/${data.documentId}`, {
      documentId: data.documentId,  // 添加documentId到请求体
      score: data.score,
      comment: data.comment
    }),

  // 获取文档评分列表
  getDocumentRatings: (documentId: number) =>
    http.get<Rating[]>(`/api/ratings/documents/${documentId}`),

  // 获取用户对文档的评分
  getUserRatingForDocument: (documentId: number) =>
    http.get<Rating>(`/api/ratings/documents/${documentId}/user`),

  // 获取用户的评分记录
  getUserRatings: () =>
    http.get<Rating[]>('/api/ratings/user'),

  // 删除评分
  deleteRating: (ratingId: number) =>
    http.delete<void>(`/api/ratings/${ratingId}`),

  // 获取文档评分统计
  getDocumentRatingStatistics: (documentId: number) =>
    http.get<{
      averageRating: number
      totalRatings: number
      ratingDistribution: { [key: string]: number }
    }>(`/api/ratings/documents/${documentId}/statistics`),

  // 获取用户评分统计
  getUserRatingStatistics: () =>
    http.get<{
      totalRatings: number
      averageRating: number
      ratingsByMonth: { [key: string]: number }
    }>('/api/ratings/user/statistics'),

  // 获取最新评分列表
  getRecentRatings: (limit: number = 10) =>
    http.get<Rating[]>('/api/ratings/recent', {
      params: { limit }
    }),

  // 获取高分文档排行
  getTopRatedDocuments: (limit: number = 10) =>
    http.get<any[]>('/api/ratings/top-documents', {
      params: { limit }
    }),

  // 检查用户是否已评分
  hasUserRatedDocument: (documentId: number) =>
    http.get<boolean>(`/api/ratings/documents/${documentId}/check`)
}