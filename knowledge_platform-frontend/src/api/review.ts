/**
 * 文档审核、申诉和知识产权保护 API 接口
 */

import request from './request'

// ==================== 类型定义 ====================

/**
 * 审核类型
 */
export type ReviewType = 'INITIAL' | 'FINAL'

/**
 * 审核状态
 */
export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/**
 * 申诉状态
 */
export type AppealStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/**
 * 举报类型
 */
export type ReportType =
  | 'COPYRIGHT_INFRINGEMENT'
  | 'PLAGIARISM'
  | 'ILLEGAL_CONTENT'
  | 'INAPPROPRIATE_CONTENT'
  | 'SENSITIVE_CONTENT'
  | 'FALSE_INFORMATION'
  | 'SPAM'
  | 'OTHER'

/**
 * 举报状态
 */
export type ReportStatus = 'PENDING' | 'INVESTIGATING' | 'CONFIRMED' | 'REJECTED' | 'CLOSED'

/**
 * 文档审核记录
 */
export interface DocumentReview {
  id: number
  documentId: number
  reviewType: ReviewType
  status: ReviewStatus
  reviewerId?: number
  reviewComment?: string

  // 初审结果
  formatCheckPassed?: boolean
  contentCompliancePassed?: boolean
  similarityScore?: number
  similarDocumentId?: number

  // 复审评估
  academicScore?: number
  originalityScore?: number
  practicalityScore?: number
  copyrightCompliance?: boolean

  rejectionReason?: string
  suggestions?: string

  createdAt: string
  reviewedAt?: string
}

/**
 * 文档申诉
 */
export interface DocumentAppeal {
  id: number
  documentId: number
  reviewId: number
  userId: number
  status: AppealStatus

  appealReason: string
  evidence?: string

  handlerId?: number
  handlerComment?: string
  finalDecision?: string

  createdAt: string
  updatedAt: string
  handledAt?: string
}

/**
 * 侵权举报
 */
export interface CopyrightReport {
  id: number
  documentId: number
  reporterId: number
  reportType: ReportType
  status: ReportStatus

  description: string
  evidenceUrls?: string
  contactInfo?: string

  handlerId?: number
  handlerComment?: string
  actionTaken?: string

  createdAt: string
  updatedAt: string
  handledAt?: string
}

/**
 * 复审请求
 */
export interface FinalReviewRequest {
  academicScore: number // 1-5
  originalityScore: number // 1-5
  practicalityScore: number // 1-5
  copyrightCompliance: boolean
  comment: string
  rejectionReason?: string
  suggestions?: string
}

/**
 * 申诉请求
 */
export interface AppealRequest {
  documentId: number
  reviewType: ReviewType
  appealReason: string
  evidence?: string
}

/**
 * 申诉处理请求
 */
export interface AppealHandleRequest {
  approved: boolean
  comment: string
  decision: string
}

/**
 * 举报请求
 */
export interface ReportRequest {
  documentId: number
  reportType: ReportType
  description: string
  evidenceUrls?: string
  contactInfo?: string
}

/**
 * 举报处理请求
 */
export interface ReportHandleRequest {
  confirmed: boolean
  comment: string
  action: string
}

/**
 * 审核统计
 */
export interface ReviewStatistics {
  pendingInitialReviews: number
  pendingFinalReviews: number
}

/**
 * 申诉统计
 */
export interface AppealStatistics {
  pending: number
  approved: number
  rejected: number
}

/**
 * 举报统计
 */
export interface ReportStatistics {
  pending: number
  investigating: number
  confirmed: number
  rejected: number
}

// ==================== 审核管理 API ====================

/**
 * 获取待复审的文档列表
 */
export function getPendingReviews(page = 0, size = 20) {
  return request.get<Page<DocumentReview>>('/api/admin/reviews/pending', {
    params: { page, size }
  })
}

/**
 * 执行文档复审
 */
export function performFinalReview(documentId: number, data: FinalReviewRequest) {
  return request.post<DocumentReview>(`/api/admin/reviews/${documentId}/final-review`, data)
}

/**
 * 获取审核统计
 */
export function getReviewStatistics() {
  return request.get<ReviewStatistics>('/api/admin/reviews/statistics')
}

// ==================== 申诉管理 API ====================

/**
 * 提交申诉
 */
export function submitAppeal(data: AppealRequest) {
  return request.post<DocumentAppeal>('/api/appeals', data)
}

/**
 * 获取我的申诉记录
 */
export function getMyAppeals(page = 0, size = 20) {
  return request.get<Page<DocumentAppeal>>('/api/appeals/my', {
    params: { page, size }
  })
}

/**
 * 获取待处理的申诉列表（管理员）
 */
export function getPendingAppeals(page = 0, size = 20) {
  return request.get<Page<DocumentAppeal>>('/api/appeals/pending', {
    params: { page, size }
  })
}

/**
 * 处理申诉（管理员）
 */
export function handleAppeal(appealId: number, data: AppealHandleRequest) {
  return request.post<DocumentAppeal>(`/api/appeals/${appealId}/handle`, data)
}

/**
 * 获取申诉统计（管理员）
 */
export function getAppealStatistics() {
  return request.get<AppealStatistics>('/api/appeals/statistics')
}

// ==================== 知识产权保护 API ====================

/**
 * 提交侵权举报
 */
export function submitReport(data: ReportRequest) {
  return request.post<CopyrightReport>('/api/copyright/reports', data)
}

/**
 * 获取我的举报记录
 */
export function getMyReports(page = 0, size = 20) {
  return request.get<Page<CopyrightReport>>('/api/copyright/reports/my', {
    params: { page, size }
  })
}

/**
 * 获取文档的举报记录（管理员）
 */
export function getDocumentReports(documentId: number) {
  return request.get<CopyrightReport[]>(`/api/copyright/reports/document/${documentId}`)
}

/**
 * 获取待处理的举报列表（管理员）
 */
export function getPendingReports(page = 0, size = 20) {
  return request.get<Page<CopyrightReport>>('/api/copyright/reports/pending', {
    params: { page, size }
  })
}

/**
 * 处理举报（管理员）
 */
export function handleReport(reportId: number, data: ReportHandleRequest) {
  return request.post<CopyrightReport>(`/api/copyright/reports/${reportId}/handle`, data)
}

/**
 * 获取举报统计（管理员）
 */
export function getReportStatistics() {
  return request.get<ReportStatistics>('/api/copyright/reports/statistics')
}

// ==================== 辅助类型 ====================

/**
 * 分页响应
 */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface NormalizedPageResult<T> {
  list: T[]
  total: number
  page?: number
  size?: number
  pages?: number
}

export function normalizePageResponse<T>(payload: any): NormalizedPageResult<T> {
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
