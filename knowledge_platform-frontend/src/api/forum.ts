import { http } from './request'

// Types
export interface ForumCategory {
  id: number
  name: string
  description: string
  icon: string
  sortOrder: number
  status: boolean
}

export interface ForumTag {
  id: number
  name: string
  topicCount: number
}

export interface ForumTopic {
  id: number
  title: string
  content: string
  author: {
    id: number
    username: string
    avatar: string
    role: string
  }
  category: ForumCategory
  viewCount: number
  replyCount: number
  likeCount: number
  collectionCount: number
  tags: ForumTag[]
  isLiked: boolean
  isCollected: boolean
  isPinned: boolean
  isEssence: boolean
  status: number
  createdAt: string
  updatedAt: string
}

export interface ForumReply {
  id: number
  content: string
  author: {
    id: number
    username: string
    avatar: string
    role: string
  }
  topicId: number
  parentId: number | null
  likeCount: number
  isLiked: boolean
  createdAt: string
}

export interface CreateTopicRequest {
  title: string
  content: string
  categoryId: number
  tags?: string[]
  status?: number // 0: normal, 3: draft
}

export interface CreateReplyRequest {
  content: string
  topicId: number
  parentId?: number
}

// API methods
export const getCategories = () => {
  return http.get<ForumCategory[]>('/api/forum/categories')
}

export const getTopics = (params: { categoryId?: number; page?: number; size?: number; tag?: string; status?: number; sort?: string }) => {
  return http.get<{
    list: ForumTopic[]
    total: number
    page: number
    size: number
    pages: number
  }>('/api/forum/topics', { params })
}

export const getTopicDetail = (id: number) => {
  return http.get<ForumTopic>(`/api/forum/topics/${id}`)
}

export const createTopic = (data: CreateTopicRequest) => {
  return http.post<ForumTopic>('/api/forum/topics', data)
}

export const createReply = (data: CreateReplyRequest) => {
  return http.post<ForumReply>('/api/forum/replies', data)
}

export const getReplies = (topicId: number, params: { page?: number; size?: number }) => {
  return http.get<{
    list: ForumReply[]
    total: number
    page: number
    size: number
    pages: number
  }>(`/api/forum/topics/${topicId}/replies`, { params })
}

export const toggleTopicLike = (id: number) => {
  return http.post<boolean>(`/api/forum/topics/${id}/like`)
}

export const toggleReplyLike = (id: number) => {
  return http.post<boolean>(`/api/forum/replies/${id}/like`)
}

export const getTags = () => {
  return http.get<ForumTag[]>('/api/forum/tags')
}

export const getHotTopics = (limit: number = 5) => {
  return http.get<ForumTopic[]>('/api/forum/hot-topics', { params: { limit } })
}

export const toggleTopicCollection = (id: number) => {
  return http.post<boolean>(`/api/forum/topics/${id}/collect`)
}

export const getUserCollections = (params: { page?: number; size?: number }) => {
  return http.get<{
    list: ForumTopic[]
    total: number
    page: number
    size: number
    pages: number
  }>('/api/forum/users/collections', { params })
}

export const getUserDrafts = (params: { page?: number; size?: number }) => {
  return http.get<{
    list: ForumTopic[]
    total: number
    page: number
    size: number
    pages: number
  }>('/api/forum/users/drafts', { params })
}

export const toggleUserFollow = (userId: number) => {
  return http.post<boolean>(`/api/forum/users/${userId}/follow`)
}

export const toggleTopicPin = (id: number) => {
  return http.post<boolean>(`/api/forum/topics/${id}/pin`)
}

export const toggleTopicEssence = (id: number) => {
  return http.post<boolean>(`/api/forum/topics/${id}/essence`)
}
