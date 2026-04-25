import request, { http } from './request'
import type { Document, DocumentSearchRequest, DocumentUploadResponse, PageResponse } from '@/types'

// 平台统计数据类型
export interface PlatformStats {
  documentCount: number
  userCount: number
  downloadCount: number
  averageRating: number
}

export interface FileTypeCapabilityItem {
  code: string
  group: string
  searchGroup: string
  displayName: string
  extensions: string[]
  mimeTypes: string[]
  uploadSupported: boolean
  initialReviewSupported: boolean
  previewEntrySupported: boolean
  previewType: 'TEXT' | 'IMAGE' | 'PDF' | 'OFFICE_TEXT' | 'ARCHIVE_TREE' | 'NOT_SUPPORTED'
  inlinePreviewSupported: boolean
  downloadOnlyPreview: boolean
  thumbnailSupported: boolean
  aiSupported: boolean
  searchSupported: boolean
  notes: string
}

export interface FileTypeSearchGroup {
  value: string
  label: string
  extensions: string[]
}

export interface FileTypeCapabilitiesResponse {
  types: FileTypeCapabilityItem[]
  uploadExtensions: string[]
  aiSupportedExtensions: string[]
  uploadAccept: string
  maxFileSize: number
  searchGroups: FileTypeSearchGroup[]
  uploadTipText: string
}

export interface ArchiveEntryInfo {
  path: string
  name: string
  directory: boolean
  size: number
  extension: string
}

export interface ArchiveTreeNode {
  name: string
  path: string
  directory: boolean
  size: number
  extension: string
  children?: ArchiveTreeNode[]
}

export interface DocumentPreviewInfo {
  filename: string
  extension: string
  fileSize: number
  previewable: boolean
  previewType: 'TEXT' | 'IMAGE' | 'PDF' | 'OFFICE_TEXT' | 'ARCHIVE_TREE' | 'NOT_SUPPORTED'
  content?: string
  truncated?: boolean
  imageInfo?: Record<string, unknown>
  pdfInfo?: Record<string, unknown>
  archiveEntries?: ArchiveEntryInfo[]
  archiveTree?: ArchiveTreeNode[]
  archiveParseSucceeded?: boolean
  archiveWarnings?: string[]
  archiveTruncated?: boolean
  archiveDepthLimited?: boolean
  archiveEntryCountReturned?: number
  archiveEntryCountTotal?: number
  fallbackReason?: string
  convertedFromOffice?: boolean
  encoding?: string
  encodingSource?: string
  previewMessage?: string
  contentLength?: number
}

export interface DocumentTextPreviewPayload {
  content: string
  encoding: string
  encodingSource: string
  previewMessage: string
  contentLength: number
  truncated: boolean
  contentSource: string
}

export interface DocumentArchivePreviewPayload {
  archiveTree: ArchiveTreeNode[]
  archiveEntries: ArchiveEntryInfo[]
  archiveParseSucceeded: boolean
  archiveWarnings: string[]
  archiveTruncated: boolean
  archiveDepthLimited: boolean
  archiveEntryCountReturned: number
  archiveEntryCountTotal: number
}

export interface ArchiveEntryPreviewInfo {
  previewable: boolean
  previewType: 'TEXT' | 'IMAGE' | 'PDF' | 'OFFICE' | 'NOT_SUPPORTED'
  entryPath: string
  entryName: string
  entrySize: number
  extension?: string | null
  reason?: string | null
  truncated: boolean
  encoding?: string | null
  encodingSource?: string | null
  content?: string | null
}

export const documentApi = {
  // 搜索文档
  searchDocuments: (params: DocumentSearchRequest) =>
    http.get<PageResponse<Document>>('/api/documents/search', { params }),

  // 获取文档详情
  getDocument: (id: number) =>
    http.get<Document>(`/api/documents/${id}`),

  // 获取平台统计数据
  getPlatformStats: () =>
    http.get<PlatformStats>('/api/documents/stats'),

  // 获取最新文档
  getRecentDocuments: (limit: number = 8) =>
    http.get<PageResponse<Document>>('/api/documents/latest', { params: { size: limit, page: 1 } }),

  // 获取热门文档
  getPopularDocuments: (limit: number = 8) =>
    http.get<PageResponse<Document>>('/api/documents/popular', { params: { size: limit, page: 1 } }),

  // 上传文档
  uploadDocument: (data: FormData) =>
    http.post<DocumentUploadResponse>('/api/documents/upload', data),

  // 下载文档
  downloadDocument: (id: number) =>
    request.get(`/api/documents/${id}/download`, { responseType: 'blob' }),

  // 获取我上传的文档
  getMyDocuments: (page: number = 1, size: number = 10) =>
    http.get<PageResponse<Document>>('/api/documents/my', { params: { page, size } }),

  // 获取我下载的文档
  getMyDownloads: (page: number = 1, size: number = 10) =>
    http.get<PageResponse<Document>>('/api/downloads/my', { params: { page, size } }),

  // 删除文档
  deleteDocument: (id: number) =>
    http.delete(`/api/documents/${id}`),

  // 获取文件类型能力矩阵
  getFileTypeCapabilities: () =>
    http.get<FileTypeCapabilitiesResponse>('/api/documents/file-types/capabilities'),

  // 获取文档预览信息
  getDocumentPreviewInfo: (id: number) =>
    http.get<DocumentPreviewInfo>(`/api/documents/${id}/preview-info`),

  // 获取文本预览负载
  getDocumentTextPreview: (id: number) =>
    http.get<DocumentTextPreviewPayload>(`/api/documents/${id}/preview-text`),

  // 获取压缩包预览负载
  getDocumentArchivePreview: (id: number) =>
    http.get<DocumentArchivePreviewPayload>(`/api/documents/${id}/preview-archive`),

  // 获取压缩包内文件预览信息
  getDocumentArchiveEntryPreviewInfo: (id: number, path: string) =>
    http.get<ArchiveEntryPreviewInfo>(`/api/documents/${id}/preview-archive-entry-info`, { params: { path } }),

  // 获取压缩包内文件预览内容
  getDocumentArchiveEntryPreviewContent: (id: number, path: string) =>
    request.get(`/api/documents/${id}/preview-archive-entry-content`, { params: { path }, responseType: 'blob' }),

  // 获取文档预览
  getDocumentPreview: (id: number) =>
    request.get(`/api/documents/${id}/preview-content`, { responseType: 'blob' }),

  // 检查文档下载状态
  checkDownloadStatus: (id: number) =>
    http.get<{hasDownloaded: boolean}>(`/api/downloads/check/${id}`),

  // AI 生成文档信息
  generateAiInfo: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<{summary: string, tags: string[]}>('/api/ai/generate-info', formData)
  },

  // AI 仅生成摘要
  generateAiSummary: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<{summary: string}>('/api/ai/generate-summary', formData)
  },

  // AI 仅生成标签
  generateAiTags: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<{tags: string[]}>('/api/ai/generate-tags', formData)
  }
}