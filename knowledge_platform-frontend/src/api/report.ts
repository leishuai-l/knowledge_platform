import { http } from './request'

export type ReportType =
  | 'COPYRIGHT_INFRINGEMENT'
  | 'PLAGIARISM'
  | 'ILLEGAL_CONTENT'
  | 'INAPPROPRIATE_CONTENT'
  | 'SENSITIVE_CONTENT'
  | 'FALSE_INFORMATION'
  | 'SPAM'
  | 'OTHER'

export type ReportStatus = 'PENDING' | 'INVESTIGATING' | 'CONFIRMED' | 'REJECTED' | 'CLOSED'

export type ReportResolutionAction = 'DELETE_DOCUMENT' | 'TAKE_DOWN_DOCUMENT' | 'NO_DOCUMENT_CHANGE'

export interface CopyrightReport {
  id: number
  documentId: number
  reporterId: number
  reportType: ReportType
  status: ReportStatus
  description: string
  contactInfo?: string
  handlerId?: number
  handlerComment?: string
  actionTaken?: string
  createdAt: string
  updatedAt: string
  handledAt?: string
}

export interface ReportSubmitData {
  documentId: number
  reportType: ReportType
  description: string
  contactInfo?: string
}

export interface AdminReportHandleRequest {
  confirmed: boolean
  comment: string
  documentAction?: ReportResolutionAction
  actionNote?: string
}

export interface AdminReportInvestigateRequest {
  comment?: string
}

export interface AdminReportQuery {
  statuses?: ReportStatus[]
  reportType?: ReportType
  documentId?: number
  page?: number
  size?: number
}

export interface ReportStatistics {
  pending: number
  investigating: number
  confirmed: number
  rejected: number
}

export interface NormalizedPageResult<T> {
  list: T[]
  total: number
  page?: number
  size?: number
  pages?: number
}

export const normalizeReportPageResponse = <T>(payload: any): NormalizedPageResult<T> => {
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

export const reportApi = {
  submitReport: (data: ReportSubmitData) => {
    return http.post<CopyrightReport>('/api/copyright/reports/submit', data)
  },

  getMyReports: (page = 0, size = 20) => {
    return http.get<CopyrightReport[]>('/api/copyright/reports/my', {
      params: { page, size }
    })
  },

  getAdminReports: (query: AdminReportQuery = {}) => {
    const {
      statuses,
      reportType,
      documentId,
      page = 1,
      size = 20
    } = query

    const params = new URLSearchParams()
    params.set('page', String(page - 1))
    params.set('size', String(size))

    statuses?.forEach(status => {
      params.append('status', status)
    })

    if (reportType) {
      params.set('reportType', reportType)
    }

    if (documentId != null) {
      params.set('documentId', String(documentId))
    }

    return http.get<CopyrightReport[]>(`/api/admin/reports?${params.toString()}`)
  },

  investigateReport: (reportId: number, data: AdminReportInvestigateRequest) => {
    return http.post<CopyrightReport>(`/api/admin/reports/${reportId}/investigate`, data)
  },

  handleReport: (reportId: number, data: AdminReportHandleRequest) => {
    return http.post<CopyrightReport>(`/api/admin/reports/${reportId}/handle`, data)
  },

  getReportStatistics: () => {
    return http.get<ReportStatistics>('/api/admin/reports/statistics')
  }
}
