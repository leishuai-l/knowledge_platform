import { http } from './request'
import type { Category } from '@/types'

// 计算分类总文档数（包括子分类）
function calculateCategoryTotal(categoryId: number, statsMap: Map<number, number>, allStats: any[]): number {
  let total = statsMap.get(categoryId) || 0

  // 根据统计数据找到所有子分类
  allStats.forEach((stat: any[]) => {
    const [id, name, count] = stat
    // 根据ID范围判断是否为子分类
    if (categoryId === 1 && (id >= 7 && id <= 12)) { // 计算机科学子分类
      total += count
    } else if (categoryId === 2 && (id >= 13 && id <= 17)) { // 数学子分类
      total += count
    } else if (categoryId === 3 && (id >= 18 && id <= 22)) { // 英语子分类
      total += count
    } else if (categoryId === 4 && (id >= 23 && id <= 26)) { // 物理子分类
      total += count
    } else if (categoryId === 5 && (id >= 27 && id <= 30)) { // 化学子分类
      total += count
    } else if (categoryId === 6 && (id >= 31 && id <= 34)) { // 其他专业课程子分类
      total += count
    }
  })

  return total
}

export interface CategoryCreateRequest {
  name: string
  description?: string
  parentId?: number
}

export interface CategoryUpdateRequest {
  name?: string
  description?: string
  isActive?: boolean
}

export const categoryApi = {
  // 获取所有分类
  getCategories: () =>
    http.get<Category[]>('/api/categories/tree'),

  // 获取分类树
  getCategoryTree: (includeDisabled = false) =>
    http.get<Category[]>('/api/categories/tree', {
      params: { includeDisabled }
    }),

  // 获取所有分类树（包括禁用的，无权限限制）
  getAllCategoryTree: () =>
    http.get<Category[]>('/api/categories/all-tree'),

  // 获取分类详情
  getCategory: (id: number) =>
    http.get<Category>(`/api/categories/${id}`),

  // 获取子分类
  getSubCategories: (parentId: number) =>
    http.get<Category[]>(`/api/categories/${parentId}/children`),

  // 获取顶级分类
  getTopLevelCategories: () =>
    http.get<Category[]>('/api/categories/top-level'),

  // 根据名称查找分类
  getCategoryByName: (name: string) =>
    http.get<Category>(`/api/categories/name/${name}`),

  // 根据层级获取分类
  getCategoriesByLevel: (level: number) =>
    http.get<Category[]>(`/api/categories/level/${level}`),

  // 搜索分类
  searchCategories: (keyword?: string) =>
    http.get<Category[]>('/api/categories/search', {
      params: { keyword }
    }),

  // 获取有文档的分类
  getCategoriesWithDocuments: () =>
    http.get<Category[]>('/api/categories/with-documents'),

  // 获取分类路径
  getCategoryPath: (id: number) =>
    http.get<string>(`/api/categories/${id}/path`),

  // 创建分类
  createCategory: (data: CategoryCreateRequest) =>
    http.post<Category>('/api/categories', null, {
      params: {
        name: data.name,
        description: data.description,
        parentId: data.parentId
      }
    }),

  // 更新分类
  updateCategory: (id: number, data: CategoryUpdateRequest) =>
    http.put<Category>(`/api/categories/${id}`, null, {
      params: {
        name: data.name,
        description: data.description,
        isActive: data.isActive
      }
    }),

  // 删除分类
  deleteCategory: (id: number) =>
    http.delete(`/api/categories/${id}`),

  // 更新分类排序
  updateCategorySortOrder: (id: number, sortOrder: number) =>
    http.put(`/api/categories/${id}/sort-order`, null, {
      params: { sortOrder }
    }),

  // 移动分类
  moveCategory: (id: number, newParentId?: number) =>
    http.put(`/api/categories/${id}/move`, null, {
      params: { newParentId }
    }),

  // 批量启用/禁用分类
  toggleCategoriesActive: (categoryIds: number[], active: boolean) => {
    // 构建查询字符串参数
    const params = new URLSearchParams()
    categoryIds.forEach(id => params.append('categoryIds', id.toString()))
    params.append('active', active.toString())

    return http.put(`/api/categories/batch/toggle?${params.toString()}`)
  },

  // 检查分类是否存在
  categoryExists: (id: number) =>
    http.get<boolean>(`/api/categories/${id}/exists`),

  // 管理员：获取所有分类
  getAllCategoriesForAdmin: () =>
    http.get<Category[]>('/api/categories/admin/all'),

  // 管理员：获取分类树（包括禁用的分类）
  getCategoryTreeForAdmin: () =>
    http.get<Category[]>('/api/categories/admin/tree'),

  // 获取分类统计信息
  getCategoryStatistics: () =>
    http.get<any[]>('/api/categories/statistics'),

  // 获取分类统计信息（带文档数量的顶级分类）
  getCategoriesWithStats: async () => {
    const [categoriesResponse, statsResponse] = await Promise.all([
      http.get<Category[]>('/api/categories/top-level'),
      http.get<any[]>('/api/categories/statistics')
    ])

    const categories = categoriesResponse.data.data
    const stats = statsResponse.data.data

    // 创建统计信息映射
    const statsMap = new Map()
    stats.forEach((stat: any[]) => {
      const [id, name, count] = stat
      statsMap.set(id, count)
    })

    // 为每个顶级分类计算总文档数（包括子分类）
    return categories.map(category => ({
      ...category,
      documentCount: calculateCategoryTotal(category.id, statsMap, stats)
    }))
  }
}