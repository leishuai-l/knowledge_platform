import { http } from './request'
import type { Tag, PageResponse } from '@/types'

export interface TagCreateRequest {
  name: string
  color?: string
  description?: string
}

export interface TagUpdateRequest {
  name?: string
  color?: string
  description?: string
}

export const tagApi = {
  // 获取所有标签
  getAllTags: () =>
    http.get<Tag[]>('/api/tags'),

  // 分页获取标签
  getTags: (page: number = 0, size: number = 20, sortBy: string = 'usage') =>
    http.get<PageResponse<Tag>>('/api/tags/page', {
      params: { page, size, sortBy }
    }),

  // 获取热门标签
  getPopularTags: (limit: number = 20) =>
    http.get<Tag[]>('/api/tags/popular', {
      params: { limit }
    }),

  // 搜索标签
  searchTags: (keyword: string) =>
    http.get<Tag[]>('/api/tags/search', {
      params: { keyword }
    }),

  // 根据ID获取标签
  getTagById: (id: number) =>
    http.get<Tag>(`/api/tags/${id}`),

  // 创建标签
  createTag: (data: TagCreateRequest) =>
    http.post<Tag>('/api/tags', null, {
      params: {
        name: data.name,
        color: data.color,
        description: data.description
      }
    }),

  // 更新标签
  updateTag: (id: number, data: TagUpdateRequest) =>
    http.put<Tag>(`/api/tags/${id}`, null, {
      params: {
        name: data.name,
        color: data.color,
        description: data.description
      }
    }),

  // 删除标签
  deleteTag: (id: number) =>
    http.delete(`/api/tags/${id}`),

  // 批量创建标签
  createTags: (tagNames: string[]) =>
    http.post<Tag[]>('/api/tags/batch', tagNames),

  // 获取未使用的标签
  getUnusedTags: () =>
    http.get<Tag[]>('/api/tags/unused'),

  // 清理未使用的标签
  cleanUnusedTags: () =>
    http.delete<number>('/api/tags/unused'),

  // 获取标签统计信息
  getTagStatistics: () =>
    http.get<any>('/api/tags/statistics'),

  // 根据使用次数范围查询标签
  getTagsByUsageRange: (minUsage: number, maxUsage: number) =>
    http.get<Tag[]>('/api/tags/usage-range', {
      params: { minUsage, maxUsage }
    }),

  // 获取推荐标签
  getRecommendedTags: (limit: number = 10) =>
    http.get<Tag[]>('/api/tags/recommended', {
      params: { limit }
    }),

  // 验证标签名称
  validateTagName: (name: string) =>
    http.get<boolean>('/api/tags/validate', {
      params: { name }
    }),

  // 标准化标签名称
  normalizeTagName: (name: string) =>
    http.get<string>('/api/tags/normalize', {
      params: { name }
    }),

  // 初始化默认标签
  initializeDefaultTags: () =>
    http.post('/api/tags/initialize')
}