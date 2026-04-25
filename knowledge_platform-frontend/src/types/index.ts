// API 响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应类型
export interface PageResponse<T = any> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
}

// 用户相关类型
export interface User {
  id: number
  username: string
  email: string
  avatar?: string
  role: 'USER' | 'ADMIN'
  points: number
  totalPoints: number
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED' | 'DELETED'
  lastLoginTime?: string
  lockedUntil?: string
  failedLoginAttempts?: number
  createdAt: string
  updatedAt: string
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

// 注册请求
export interface RegisterRequest {
  username: string
  email: string
  password: string
  verificationCode: string
}

// JWT响应
export interface JwtResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

// 文档相关类型
export interface Document {
  id: number
  title: string
  description?: string
  fileName: string
  filePath: string
  fileSize: number
  fileType: string
  fileExtension: string
  categoryId: number
  uploaderId: number
  uploader?: User
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'DELETED'
  rejectionReason?: string
  downloadCount: number
  viewCount: number
  ratingAverage: number
  ratingCount: number
  downloadPoints: number
  approvedAt?: string
  approvedBy?: number
  aiSummary?: string
  aiAnalysisStatus?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'
  createdAt: string
  updatedAt: string
  // 关联数据
  category?: Category
  tags?: Tag[]
}

export interface DocumentUploadResponse {
  document: Document
  initialReviewOutcome: 'PASSED' | 'REJECTED' | 'SYSTEM_REJECTED'
  rejectionReason?: string
  suggestions?: string
  shouldRedirectToMyUploads: boolean
  canRetryUpload: boolean
}

// 分类类型
export interface Category {
  id: number
  name: string
  parentId?: number
  level: number
  sortOrder: number
  description?: string
  isActive: boolean
  isDeleted: boolean
  createdAt: string
  updatedAt: string
  children?: Category[]
}

// 标签类型
export interface Tag {
  id: number
  name: string
  color: string
  description?: string
  usageCount: number
  createdAt: string
  updatedAt?: string
}

// 评分类型
export interface Rating {
  id: number
  documentId: number
  userId: number
  score: number
  comment?: string
  createdAt: string
  updatedAt: string
  user?: User
  document?: Document
  rating?: number
}

// 评论类型
export interface Comment {
  id: number
  documentId: number
  userId: number
  parentId?: number
  content: string
  isDeleted: boolean
  createdAt: string
  updatedAt: string
  user?: User
  replies?: Comment[]
}

// 积分记录类型
export interface PointsRecord {
  id: number
  userId: number
  type: 'EARN' | 'SPEND'
  points: number
  source: 'REGISTER' | 'UPLOAD' | 'APPROVED' | 'DOWNLOAD_REWARD' | 'RATING_REWARD' | 'ADMIN_ADJUST' | 'DOWNLOAD_COST'
  referenceId?: number
  description?: string
  createdAt: string
}

// 搜索请求类型
export interface DocumentSearchRequest {
  keyword?: string
  categoryId?: number
  categoryPath?: number[]
  tags?: string[]
  fileType?: string
  minRating?: number
  sortBy?: 'createdAt' | 'downloadCount' | 'ratingAverage' | 'title'
  sortOrder?: 'asc' | 'desc'
  page?: number
  size?: number
}

// 上传文档请求
export interface DocumentUploadRequest {
  title: string
  description?: string
  categoryId: number
  tags?: string[]
  downloadPoints?: number
}

// 创建评论请求
export interface CommentCreateRequest {
  documentId: number
  parentId?: number
  content: string
}

// 创建评分请求
export interface RatingCreateRequest {
  documentId: number
  rating: number
}

// 通知相关类型
export interface Notification {
  id: number
  userId: number
  type: 'DOCUMENT_APPROVED' | 'DOCUMENT_REJECTED' | 'DOCUMENT_COMMENTED' | 'DOCUMENT_RATED' | 'POINTS_EARNED' | 'POINTS_SPENT' | 'SYSTEM_ANNOUNCEMENT'
  title: string
  content: string
  referenceId?: number
  isRead: boolean
  createdAt: string
  readAt?: string
}

// WebSocket消息类型
export interface WebSocketMessage {
  type: string
  data: any
  timestamp: number
}

// 批量更新通知
export interface BatchUpdateNotification {
  notificationIds: number[]
  isRead: boolean
}

// 全部已读通知
export interface AllReadNotification {
  allRead: boolean
}








