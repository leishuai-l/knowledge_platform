import { http } from './request'
import type { PageResponse, PointsRecord } from '@/types'

export const pointsApi = {
  // 获取用户当前积分
  getCurrentPoints: () =>
    http.get<number>('/api/points/current'),

  // 获取用户积分记录
  getPointsRecords: (page: number = 0, size: number = 10) =>
    http.get<PageResponse<PointsRecord>>('/api/points/records', {
      params: { page, size }
    }),

  // 根据类型获取积分记录
  getPointsRecordsByType: (type: 'EARN' | 'SPEND', page: number = 0, size: number = 10) =>
    http.get<PageResponse<PointsRecord>>(`/api/points/records/type/${type}`, {
      params: { page, size }
    }),

  // 获取今日积分统计
  getTodayStatistics: () =>
    http.get<any>('/api/points/today'),

  // 获取用户积分统计
  getUserStatistics: () =>
    http.get<any>('/api/points/statistics'),

  // 获取用户月度积分统计
  getMonthlyStatistics: () =>
    http.get<any[]>('/api/points/monthly'),

  // 获取积分排行榜
  getLeaderboard: (limit: number = 10) =>
    http.get<any[]>('/api/points/leaderboard', {
      params: { limit }
    }),

  // 获取最近积分记录
  getRecentRecords: (limit: number = 20) =>
    http.get<PointsRecord[]>('/api/points/recent', {
      params: { limit }
    })
}

