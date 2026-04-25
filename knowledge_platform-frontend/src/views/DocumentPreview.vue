<template>
  <div class="document-preview-page">
    <!-- 顶部工具栏 -->
    <div class="preview-header">
      <div class="header-left">
        <el-button
          type="primary"
          :icon="ArrowLeft"
          @click="goBack"
          class="back-btn"
        >
          返回
        </el-button>
        <div class="document-info">
          <h2 class="document-title">{{ document?.title || '文档预览' }}</h2>
          <div class="document-meta" v-if="document">
            <span class="file-type">{{ getFileExtension(document.fileName).toUpperCase() }}</span>
            <span class="file-size">{{ formatFileSize(document.fileSize) }}</span>
            <span class="upload-time">{{ formatTime(document.createdAt) }}</span>
          </div>
        </div>
      </div>

      <div class="header-right">
        <!-- 预览控制工具 -->
        <div class="preview-controls" v-if="showControls">
          <el-button-group size="small">
            <el-button @click="zoomOut" :disabled="zoom <= 50">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <el-button disabled>{{ zoom }}%</el-button>
            <el-button @click="zoomIn" :disabled="zoom >= 200">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
          </el-button-group>
          <el-button @click="resetZoom" size="small">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <el-button @click="toggleFullscreen" size="small">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button
            v-if="!isAdminPreview && document?.status === 'APPROVED'"
            type="success"
            size="small"
            :loading="downloadLoading"
            @click="downloadDocument"
          >
            <el-icon><Download /></el-icon>
            下载文档
          </el-button>

          <!-- 举报按钮 -->
          <el-button
            v-if="!isAdminPreview && document?.status === 'APPROVED'"
            type="warning"
            size="small"
            @click="reportDocument"
          >
            <el-icon><Warning /></el-icon>
            举报反馈
          </el-button>

          <!-- 管理员审核按钮 -->
          <template v-if="isAdminPreview && userStore.isAdmin">
            <el-button
              v-if="document?.status === 'PENDING'"
              type="success"
              size="small"
              @click="approveDocument"
            >
              <el-icon><Check /></el-icon>
              批准
            </el-button>
            <el-button
              v-if="document?.status === 'PENDING'"
              type="danger"
              size="small"
              @click="rejectDocument"
            >
              <el-icon><Close /></el-icon>
              拒绝
            </el-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 预览内容区域 -->
    <div class="preview-content" ref="previewContainer">
      <div class="preview-wrapper" v-loading="loading">
        <!-- PDF预览 -->
        <div v-if="isPDF" class="pdf-preview">
          <iframe
            :src="previewUrl"
            class="pdf-iframe"
            :style="{ transform: `scale(${zoom / 100})` }"
            title="PDF 文档预览"
          ></iframe>
        </div>

        <div v-else-if="isTextLikePreview" class="text-preview" :class="{ 'office-text-preview': isOfficeText }">
          <div v-if="isOfficeText && previewInfo?.convertedFromOffice" class="preview-note">当前文档已尝试转换为 PDF 预览，但转换失败，现展示文本提取内容。</div>
          <div v-else-if="isOfficeText" class="preview-note">当前展示为提取内容预览，可能与原始排版不完全一致。</div>
          <div v-if="previewInfo?.encoding || previewInfo?.previewMessage" class="preview-meta-row">
            <span v-if="previewInfo?.encoding" class="meta-chip">编码：{{ previewInfo.encoding }}</span>
            <span v-if="previewInfo?.encodingSource" class="meta-chip">来源：{{ previewInfo.encodingSource }}</span>
            <span v-if="previewInfo?.contentLength != null" class="meta-chip">长度：{{ previewInfo.contentLength }}</span>
          </div>
          <div v-if="previewInfo?.previewMessage" class="preview-note">{{ previewInfo.previewMessage }}</div>
          <div v-if="previewInfo?.fallbackReason" class="preview-note warning-note">{{ previewInfo.fallbackReason }}</div>
          <div v-if="previewInfo?.truncated" class="preview-note warning-note">预览内容已截断，仅展示部分内容。</div>
          <TextPreviewPane :content="textContent || '未能提取到可预览的文档内容'" :extension="document?.fileExtension" />
        </div>

        <!-- 压缩包目录预览 -->
        <div v-else-if="isArchiveTree" class="archive-preview archive-explorer">
          <div class="archive-toolbar">
            <el-input v-model="archiveSearch" placeholder="搜索文件名或路径" clearable />
            <div class="archive-summary">
              <span v-if="archiveMeta?.archiveEntryCountReturned != null">已展示 {{ archiveMeta.archiveEntryCountReturned }} 项</span>
              <span v-if="archiveMeta?.archiveEntryCountTotal != null"> / 共扫描 {{ archiveMeta.archiveEntryCountTotal }} 项</span>
            </div>
          </div>
          <div v-if="archiveMeta?.archiveWarnings?.length" class="preview-note warning-note">
            {{ archiveMeta.archiveWarnings.join('；') }}
          </div>
          <div v-if="!archiveMeta?.archiveParseSucceeded" class="preview-note warning-note">压缩包目录解析不完整或为空。</div>
          <div class="archive-layout archive-layout-full">
            <div class="archive-sidebar">
              <el-tree
                :data="filteredArchiveTree"
                node-key="path"
                default-expand-all
                :expand-on-click-node="false"
                highlight-current
                class="archive-tree"
                @node-click="handleArchiveNodeClick"
              >
                <template #default="{ data }">
                  <span class="node-name">{{ data.name }}</span>
                </template>
              </el-tree>
            </div>
            <div class="archive-main">
              <div class="archive-breadcrumb">{{ selectedArchiveNode?.path || '根目录' }}</div>
              <template v-if="selectedArchiveNode && !selectedArchiveNode.directory">
                <div class="archive-entry-meta">
                  <span class="meta-chip">{{ selectedArchiveEntryInfo?.previewType || selectedArchiveNode.extension || '文件' }}</span>
                  <span class="meta-chip">{{ formatFileSize(selectedArchiveNode.size) }}</span>
                </div>
                <div v-if="selectedArchiveEntryInfo && !selectedArchiveEntryInfo.previewable" class="preview-note warning-note">
                  {{ selectedArchiveEntryInfo.reason || '该文件类型不支持预览' }}
                </div>
                <div v-else-if="selectedArchiveEntryInfo?.previewType === 'TEXT'" class="archive-entry-preview">
                  <TextPreviewPane :content="archiveEntryTextContent || '无可显示内容'" :extension="selectedArchiveEntryInfo?.extension || undefined" />
                </div>
                <div v-else-if="selectedArchiveEntryInfo?.previewType === 'IMAGE'" class="archive-entry-preview image-preview">
                  <img :src="archiveEntryPreviewUrl" :alt="selectedArchiveNode.name" class="preview-image" />
                </div>
                <div v-else-if="selectedArchiveEntryInfo?.previewType === 'PDF' || selectedArchiveEntryInfo?.previewType === 'OFFICE'" class="archive-entry-preview pdf-preview">
                  <iframe :src="archiveEntryPreviewUrl" class="pdf-iframe" title="压缩包内文档预览"></iframe>
                </div>
                <div v-else class="unsupported-preview">
                  <div class="unsupported-content">
                    <h3>该文件类型不支持预览</h3>
                  </div>
                </div>
              </template>
              <el-table v-else :data="selectedArchiveChildren" stripe style="width: 100%">
                <el-table-column prop="name" label="名称" min-width="260" />
                <el-table-column label="类型" width="120">
                  <template #default="{ row }">
                    {{ row.directory ? '目录' : (row.extension || '文件') }}
                  </template>
                </el-table-column>
                <el-table-column label="大小" width="140">
                  <template #default="{ row }">
                    {{ row.directory ? '-' : formatFileSize(row.size) }}
                  </template>
                </el-table-column>
                <el-table-column prop="path" label="路径" min-width="280" show-overflow-tooltip />
              </el-table>
            </div>
          </div>
        </div>

        <!-- 图片预览 -->
        <div v-else-if="isImage" class="image-preview">
          <img
            :src="previewUrl"
            :alt="document?.title"
            class="preview-image"
            :style="{ transform: `scale(${zoom / 100})` }"
            @load="handleImageLoad"
            @error="handleImageError"
          />
        </div>

        <div v-else class="unsupported-preview">
          <div class="unsupported-content">
            <el-icon :size="80" color="#909399">
              <Document />
            </el-icon>
            <h3>无法预览此文件类型</h3>
            <p>{{ getFileExtension(document?.fileName || '').toUpperCase() }} 文件暂不支持在线预览</p>
            <el-button
              v-if="!isAdminPreview && document?.status === 'APPROVED'"
              type="primary"
              @click="downloadDocument"
            >
              <el-icon><Download /></el-icon>
              下载查看
            </el-button>
          </div>
        </div>

        <!-- 错误状态 -->
        <div v-if="error" class="preview-error">
          <el-result
            icon="error"
            title="预览失败"
            :sub-title="error"
          >
            <template #extra>
              <el-button type="primary" @click="retryPreview">重试</el-button>
              <el-button @click="goBack">返回</el-button>
            </template>
          </el-result>
        </div>
      </div>
    </div>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="showRejectDialog"
      title="拒绝文档"
      width="500px"
    >
      <el-form ref="rejectForm" :model="rejectData" :rules="rejectRules">
        <el-form-item label="拒绝原因" prop="reason">
          <el-select v-model="rejectData.reason" placeholder="请选择拒绝原因">
            <el-option label="内容不符合规范" value="CONTENT_VIOLATION" />
            <el-option label="文件格式不正确" value="INVALID_FORMAT" />
            <el-option label="文件损坏或无法打开" value="FILE_CORRUPTED" />
            <el-option label="涉及版权问题" value="COPYRIGHT_ISSUE" />
            <el-option label="其他原因" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item
          label="详细说明"
          prop="description"
          :rules="rejectData.reason === 'OTHER' ? [{ required: true, message: '请输入详细说明' }] : []"
        >
          <el-input
            v-model="rejectData.description"
            type="textarea"
            :rows="4"
            placeholder="请详细说明拒绝原因，以便用户了解并改正"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
    <!-- 举报对话框 -->
    <el-dialog
      v-model="showReportDialog"
      title="举报文档"
      width="500px"
    >
      <el-form ref="reportForm" :model="reportData" :rules="reportRules">
        <el-form-item label="举报原因" prop="reason">
          <el-select v-model="reportData.reason" placeholder="请选择举报原因">
            <el-option label="侵犯版权" value="COPYRIGHT_INFRINGEMENT" />
            <el-option label="抄袭剽窃" value="PLAGIARISM" />
            <el-option label="违法内容" value="ILLEGAL_CONTENT" />
            <el-option label="不当内容" value="INAPPROPRIATE_CONTENT" />
            <el-option label="垃圾广告" value="SPAM" />
            <el-option label="其他原因" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item
          label="详细说明"
          prop="description"
        >
          <el-input
            v-model="reportData.description"
            type="textarea"
            :rows="4"
            placeholder="请详细说明举报原因，如有证据请提供链接或说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="danger" :loading="submittingReport" @click="confirmReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, ZoomOut, ZoomIn, Refresh, FullScreen,
  Download, Check, Close, Document, Warning
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { documentApi, type ArchiveEntryPreviewInfo, type ArchiveTreeNode, type DocumentArchivePreviewPayload, type DocumentPreviewInfo, type DocumentTextPreviewPayload } from '@/api/document'
import { adminApi } from '@/api/admin'
import { reportApi } from '@/api/report'
import TextPreviewPane from '@/components/preview/TextPreviewPane.vue'
import type { Document as DocumentType } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 路由参数
const documentId = computed(() => Number.parseInt(route.params.id as string))
const isAdminPreview = computed(() => route.query.admin === 'true')

// 状态
const loading = ref(true)
const error = ref('')
const document = ref<DocumentType | null>(null)
const previewInfo = ref<DocumentPreviewInfo | null>(null)
const textPreview = ref<DocumentTextPreviewPayload | null>(null)
const archiveMeta = ref<DocumentArchivePreviewPayload | null>(null)
const selectedArchiveEntryInfo = ref<ArchiveEntryPreviewInfo | null>(null)
const archiveEntryPreviewUrl = ref('')
const archiveEntryTextContent = ref('')
const zoom = ref(100)
const textContent = ref('')
const archiveEntries = ref<DocumentPreviewInfo['archiveEntries']>([])
const archiveTree = ref<ArchiveTreeNode[]>([])
const archiveSearch = ref('')
const selectedArchiveNode = ref<ArchiveTreeNode | null>(null)
const previewObjectUrl = ref('')
const showRejectDialog = ref(false)
const showReportDialog = ref(false)
const downloadLoading = ref(false)

// 预览URL
const previewUrl = computed(() => previewObjectUrl.value)

// 文件类型判断
const previewType = computed(() => previewInfo.value?.previewType || 'NOT_SUPPORTED')
const isPDF = computed(() => previewType.value === 'PDF')

const isImage = computed(() => previewType.value === 'IMAGE')

const isOfficeText = computed(() => previewType.value === 'OFFICE_TEXT')

const isArchiveTree = computed(() => previewType.value === 'ARCHIVE_TREE')

const isTextFile = computed(() => previewType.value === 'TEXT')
const isTextLikePreview = computed(() => isTextFile.value || isOfficeText.value)

const showControls = computed(() => isPDF.value || isImage.value || isTextLikePreview.value)

// 审核表单
const rejectData = ref({
  reason: '',
  description: ''
})

const rejectRules = {
  reason: [{ required: true, message: '请选择拒绝原因' }]
}

const reportData = ref({
  reason: '',
  description: ''
})
const submittingReport = ref(false)

const reportRules = {
  reason: [{ required: true, message: '请选择举报原因' }],
  description: [{ required: true, message: '请输入详细说明' }]
}

const resetPreviewState = () => {
  textContent.value = ''
  archiveEntries.value = []
  archiveTree.value = []
  archiveSearch.value = ''
  textPreview.value = null
  archiveMeta.value = null
  selectedArchiveNode.value = null
  selectedArchiveEntryInfo.value = null
  archiveEntryTextContent.value = ''
  if (archiveEntryPreviewUrl.value?.startsWith('blob:')) {
    globalThis.URL.revokeObjectURL(archiveEntryPreviewUrl.value)
  }
  archiveEntryPreviewUrl.value = ''
  if (previewObjectUrl.value?.startsWith('blob:')) {
    globalThis.URL.revokeObjectURL(previewObjectUrl.value)
  }
  previewObjectUrl.value = ''
}

const loadDocument = async () => {
  try {
    loading.value = true
    error.value = ''
    resetPreviewState()

    console.log('[Preview] Loading document:', documentId.value, 'isAdmin:', isAdminPreview.value)

    let documentResponse, previewResponse
    try {
      documentResponse = await documentApi.getDocument(documentId.value)
      console.log('[Preview] Document loaded:', documentResponse.data)
    } catch (e) {
      console.error('[Preview] Failed to load document:', e)
      throw e
    }

    try {
      previewResponse = await documentApi.getDocumentPreviewInfo(documentId.value)
      console.log('[Preview] Preview info loaded:', previewResponse.data)
    } catch (e) {
      console.error('[Preview] Failed to load preview info:', e)
      throw e
    }

    document.value = documentResponse.data.data
    previewInfo.value = previewResponse.data.data

    console.log('[Preview] Raw previewType from response:', previewResponse.data.data?.previewType)
    console.log('[Preview] Computed previewType:', previewType.value)
    console.log('[Preview] Is PDF:', isPDF.value)
    console.log('[Preview] Is Image:', isImage.value)
    console.log('[Preview] Is Text:', isTextFile.value)
    console.log('[Preview] Is Office Text:', isOfficeText.value)
    console.log('[Preview] Is Archive:', isArchiveTree.value)

    if (isTextFile.value || isOfficeText.value) {
      const textResponse = await documentApi.getDocumentTextPreview(documentId.value)
      textPreview.value = textResponse.data.data
      textContent.value = textPreview.value.content || '无法加载文本内容'
      if (previewInfo.value) {
        previewInfo.value.encoding = textPreview.value.encoding
        previewInfo.value.encodingSource = textPreview.value.encodingSource
        previewInfo.value.previewMessage = textPreview.value.previewMessage
        previewInfo.value.contentLength = textPreview.value.contentLength
        previewInfo.value.truncated = textPreview.value.truncated
      }
    } else if (isPDF.value || isImage.value) {
      await loadBinaryPreview()
    } else if (isArchiveTree.value) {
      const archiveResponse = await documentApi.getDocumentArchivePreview(documentId.value)
      archiveMeta.value = archiveResponse.data.data
      archiveEntries.value = archiveMeta.value.archiveEntries || []
      archiveTree.value = archiveMeta.value.archiveTree || buildArchiveTree(archiveEntries.value)
      selectedArchiveNode.value = archiveTree.value[0] || null
      if (selectedArchiveNode.value && !selectedArchiveNode.value.directory) {
        await handleArchiveNodeClick(selectedArchiveNode.value)
      }
    } else {
      console.error('[Preview] Unsupported preview type:', previewInfo.value?.previewType)
      error.value = '文件暂不支持在线预览'
    }

  } catch (err: any) {
    console.error('加载文档失败:', err)
    error.value = err.response?.data?.message || '加载文档失败'

    if (err.response?.status === 404) {
      error.value = '文档不存在或已被删除'
    } else if (err.response?.status === 403) {
      error.value = '您没有权限访问此文档'
    }

    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const getPreviewMimeType = (): string | undefined => {
  const extension = (document.value?.fileExtension || '').toLowerCase()

  if (isImage.value) {
    const imageMimeMap: Record<string, string> = {
      jpg: 'image/jpeg',
      jpeg: 'image/jpeg',
      png: 'image/png',
      gif: 'image/gif',
      bmp: 'image/bmp',
      webp: 'image/webp'
    }
    return imageMimeMap[extension]
  }

  if (isPDF.value) {
    return 'application/pdf'
  }

  return undefined
}

const loadBinaryPreview = async () => {
  try {
    const response = await documentApi.getDocumentPreview(documentId.value)

    // 检查是否是有效的 blob
    if (!(response.data instanceof Blob)) {
      error.value = '预览数据格式错误'
      loading.value = false
      return
    }

    const responseType = response.headers?.['content-type'] as string | undefined
    const normalizedResponseType = responseType?.split(';')[0]?.trim()
    const inferredType = getPreviewMimeType()
    const finalType = normalizedResponseType && normalizedResponseType !== 'application/octet-stream'
      ? normalizedResponseType
      : inferredType

    let blob: Blob = response.data

    // 如果 blob 没有类型且我们有确定的类型，重新创建 blob
    if (!blob.type && finalType) {
      blob = new Blob([await blob.arrayBuffer()], { type: finalType })
    }

    // 确保 blob 有正确的 MIME 类型
    if (finalType && blob.type !== finalType) {
      blob = new Blob([blob], { type: finalType })
    }

    // 检查 blob 是否为空
    if (blob.size === 0) {
      error.value = '预览数据为空'
      loading.value = false
      return
    }

    if (previewObjectUrl.value?.startsWith('blob:')) {
      globalThis.URL.revokeObjectURL(previewObjectUrl.value)
    }

    previewObjectUrl.value = globalThis.URL.createObjectURL(blob)
  } catch (err: any) {
    error.value = err?.message || '加载预览内容失败'
    loading.value = false
  }
}

const selectedArchiveChildren = computed(() => {
  if (!selectedArchiveNode.value) {
    return archiveTree.value
  }
  if (!selectedArchiveNode.value.directory) {
    return []
  }
  return selectedArchiveNode.value.children || []
})

const handleArchiveNodeClick = async (node: ArchiveTreeNode) => {
  selectedArchiveNode.value = node
  selectedArchiveEntryInfo.value = null
  archiveEntryTextContent.value = ''
  if (archiveEntryPreviewUrl.value?.startsWith('blob:')) {
    globalThis.URL.revokeObjectURL(archiveEntryPreviewUrl.value)
    archiveEntryPreviewUrl.value = ''
  }

  if (node.directory) {
    return
  }

  try {
    const infoResponse = await documentApi.getDocumentArchiveEntryPreviewInfo(documentId.value, node.path)
    selectedArchiveEntryInfo.value = infoResponse.data.data
    if (!selectedArchiveEntryInfo.value?.previewable) {
      return
    }

    if (selectedArchiveEntryInfo.value.previewType === 'TEXT') {
      archiveEntryTextContent.value = selectedArchiveEntryInfo.value.content || ''
      return
    }

    const contentResponse = await documentApi.getDocumentArchiveEntryPreviewContent(documentId.value, node.path)
    const responseType = contentResponse.headers?.['content-type'] as string | undefined
    const normalizedType = responseType?.split(';')[0]?.trim()

    let archiveBlob: Blob
    if (contentResponse.data instanceof Blob) {
      archiveBlob = contentResponse.data
      if (!archiveBlob.type && normalizedType) {
        archiveBlob = new Blob([await archiveBlob.arrayBuffer()], { type: normalizedType })
      }
    } else if (contentResponse.data instanceof ArrayBuffer) {
      archiveBlob = new Blob([contentResponse.data], normalizedType ? { type: normalizedType } : undefined)
    } else if (contentResponse.data instanceof Uint8Array) {
      archiveBlob = new Blob([contentResponse.data], normalizedType ? { type: normalizedType } : undefined)
    } else {
      archiveBlob = new Blob([contentResponse.data as unknown as BlobPart], normalizedType ? { type: normalizedType } : undefined)
    }
    archiveEntryPreviewUrl.value = globalThis.URL.createObjectURL(archiveBlob)
  } catch (err: any) {
    selectedArchiveEntryInfo.value = {
      previewable: false,
      previewType: 'NOT_SUPPORTED',
      entryPath: node.path,
      entryName: node.name,
      entrySize: node.size,
      extension: node.extension,
      reason: err?.response?.data?.message || err?.message || '该文件类型不支持预览',
      truncated: false,
      encoding: null,
      encodingSource: null,
      content: null
    }
  }
}

const filteredArchiveTree = computed(() => {
  const keyword = archiveSearch.value.trim().toLowerCase()
  if (!keyword) {
    return archiveTree.value
  }

  const filterNodes = (nodes: ArchiveTreeNode[]): ArchiveTreeNode[] => {
    const result: ArchiveTreeNode[] = []

    for (const node of nodes) {
      const children = node.children ? filterNodes(node.children) : undefined
      const matched = node.name.toLowerCase().includes(keyword) || node.path.toLowerCase().includes(keyword)
      if (matched || (children && children.length > 0)) {
        result.push({ ...node, children })
      }
    }

    return result
  }

  return filterNodes(archiveTree.value)
})

const buildArchiveTree = (entries: NonNullable<DocumentPreviewInfo['archiveEntries']>): ArchiveTreeNode[] => {
  const nodeMap = new Map<string, ArchiveTreeNode>()
  const roots: ArchiveTreeNode[] = []

  const ensureNode = (path: string, name: string, directory: boolean, size: number, extension: string) => {
    const existing = nodeMap.get(path)
    if (existing) return existing
    const created: ArchiveTreeNode = { path, name, directory, size, extension, children: directory ? [] : undefined }
    nodeMap.set(path, created)
    return created
  }

  for (const entry of entries) {
    const segments = entry.path.split('/').filter(Boolean)
    let currentPath = ''
    let parent: ArchiveTreeNode | null = null

    segments.forEach((segment, index) => {
      currentPath = currentPath ? `${currentPath}/${segment}` : segment
      const isLeaf = index === segments.length - 1
      const isDirectory = isLeaf ? entry.directory : true
      const node = ensureNode(currentPath, segment, isDirectory, isLeaf ? entry.size : 0, isLeaf ? entry.extension : '')

      if (parent) {
        parent.children = parent.children || []
        if (!parent.children.some(child => child.path === node.path)) {
          parent.children.push(node)
        }
      } else if (!roots.some(root => root.path === node.path)) {
        roots.push(node)
      }

      parent = node
    })
  }

  const sortNodes = (nodes: ArchiveTreeNode[]) => {
    nodes.sort((a, b) => {
      if (a.directory !== b.directory) {
        return a.directory ? -1 : 1
      }
      return a.name.localeCompare(b.name)
    })
    nodes.forEach(node => {
      if (node.children?.length) sortNodes(node.children)
    })
  }

  sortNodes(roots)
  return roots
}

const goBack = () => {
  // 检查是否为管理员预览模式
  if (isAdminPreview.value) {
    // 管理员预览时，回到文档管理页面
    router.push('/admin/documents')
  } else {
    // 普通用户预览时，尝试返回上一页，如果失败则回到文档库
    try {
      if (window.history.length > 1) {
        router.go(-1)
      } else {
        router.push('/')
      }
    } catch (error) {
      router.push('/')
    }
  }
}

const goToLogin = () => {
  router.push({
    name: 'Login',
    query: { redirect: route.fullPath }
  })
}

const zoomIn = () => {
  if (zoom.value < 200) {
    zoom.value += 25
  }
}

const zoomOut = () => {
  if (zoom.value > 50) {
    zoom.value -= 25
  }
}

const resetZoom = () => {
  zoom.value = 100
}

const toggleFullscreen = () => {
  if (!window.document.fullscreenElement) {
    window.document.documentElement.requestFullscreen()
  } else {
    window.document.exitFullscreen()
  }
}

const downloadDocument = async () => {
  if (!userStore.isLoggedIn) {
    goToLogin()
    return
  }

  if (document.value && userStore.user && userStore.user.points < document.value.downloadPoints) {
    ElMessage.error('积分不足，无法下载')
    return
  }

  try {
    downloadLoading.value = true

    await ElMessageBox.confirm(
      `下载此文档需要消费 ${document.value?.downloadPoints || 0} 积分，确定要下载吗？`,
      '确认下载',
      { type: 'warning' }
    )

    const response = await documentApi.downloadDocument(documentId.value)

    const blob = response.data instanceof Blob ? response.data : new Blob([response.data as unknown as BlobPart])
    const url = window.URL.createObjectURL(blob)
    const link = window.document.createElement('a')
    link.href = url
    link.download = document.value?.fileName || 'document'
    window.document.body.appendChild(link)
    link.click()
    window.document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    try {
      await userStore.getCurrentUser()
    } catch {
      if (userStore.user && document.value) {
        userStore.updateUserPoints(userStore.user.points - document.value.downloadPoints)
      }
    }

    if (document.value) {
      document.value.downloadCount = (document.value.downloadCount || 0) + 1
    }

    ElMessage.success('下载成功')
  } catch (err: any) {
    if (err !== 'cancel' && err?.message !== 'cancel') {
      ElMessage.error(err.response?.data?.message || err.message || '下载失败')
    }
  } finally {
    downloadLoading.value = false
  }
}

const approveDocument = async () => {
  try {
    await ElMessageBox.confirm('确认批准这个文档吗？', '确认操作', {
      type: 'success'
    })

    await adminApi.approveDocument(documentId.value)
    ElMessage.success('文档已批准')

    // 更新文档状态
    if (document.value) {
      document.value.status = 'APPROVED'
    }

    // 延迟返回，让用户看到状态变化
    setTimeout(() => {
      if (isAdminPreview.value) {
        router.push('/admin/documents?refresh=true')
      }
    }, 1000)
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || '批准失败')
    }
  }
}

const rejectDocument = () => {
  showRejectDialog.value = true
  rejectData.value = { reason: '', description: '' }
}

const confirmReject = async () => {
  try {
    await adminApi.rejectDocument(documentId.value, rejectData.value.reason)
    ElMessage.success('文档已拒绝')
    showRejectDialog.value = false

    // 更新文档状态
    if (document.value) {
      document.value.status = 'REJECTED'
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '拒绝失败')
  }
}

const reportDocument = () => {
  showReportDialog.value = true
  reportData.value = { reason: '', description: '' }
}

const confirmReport = async () => {
  if (!reportData.value.reason) {
    ElMessage.warning('请选择举报原因')
    return
  }

  const description = reportData.value.description.trim()
  if (description.length < 20) {
    ElMessage.warning('详细说明至少需要20个字')
    return
  }

  try {
    submittingReport.value = true
    const response = await reportApi.submitReport({
      documentId: documentId.value,
      reportType: reportData.value.reason as any,
      description
    })

    if (response.data?.code !== 0) {
      throw new Error(response.data?.message || '举报提交失败')
    }

    ElMessage.success(response.data?.message || '举报已提交，我们会尽快处理')
    showReportDialog.value = false
    reportData.value = { reason: '', description: '' }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '举报提交失败')
  } finally {
    submittingReport.value = false
  }
}

const retryPreview = () => {
  loadDocument()
}

const handleImageLoad = () => {
  // 图片加载成功，清除可能存在的错误状态
  error.value = ''
  loading.value = false
}

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  error.value = '图片加载失败'
  loading.value = false
}

// 工具函数
const getFileExtension = (fileName: string): string => {
  return fileName.split('.').pop() || ''
}

const formatFileSize = (size: number): string => {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

const formatTime = (time: string): string => {
  return new Date(time).toLocaleString()
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    goBack()
  } else if (e.key === 'F11') {
    e.preventDefault()
    toggleFullscreen()
  }
}

// 生命周期
onMounted(() => {
  loadDocument()
  globalThis.document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (previewObjectUrl.value?.startsWith('blob:')) {
    globalThis.URL.revokeObjectURL(previewObjectUrl.value)
  }
  globalThis.document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.document-preview-page {
  min-height: 100vh;
  height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  --preview-header-height: 56px;
}

.preview-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: var(--preview-header-height);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.back-btn {
  flex-shrink: 0;
  padding: 8px 12px;
  font-size: 14px;
}

.document-info {
  min-width: 0;
  flex: 1;
}

.document-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
}

.document-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.document-meta span {
  display: flex;
  align-items: center;
}

.file-type {
  background: #e1f3ff;
  color: #409eff;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 500;
  font-size: 11px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.preview-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.preview-content {
  flex: 1;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 0;
  min-height: 0;
  width: 100%;
  height: calc(100vh - var(--preview-header-height));
  overflow: hidden;
}

.preview-wrapper {
  width: 100%;
  height: 100%;
  background: #fff;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  box-shadow: none;
}

.pdf-preview,
.office-preview {
  width: 100%;
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pdf-iframe,
.office-iframe {
  width: 100%;
  height: 100%;
  flex: 1;
  border: none;
  transform-origin: center top;
}

.image-preview {
  width: 100%;
  height: 100%;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  overflow: auto;
  background: #fafafa;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform-origin: center;
}

.text-preview {
  width: 100%;
  height: 100%;
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #fafafa;
}

.preview-note {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.warning-note {
  color: #e6a23c;
}

.text-content {
  font-family: 'Courier New', monospace;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  height: 100%;
}

.archive-preview {
  width: 100%;
  height: 100%;
  flex: 1;
  padding: 20px;
  overflow: auto;
  background: #fafafa;
}

.preview-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef5ff;
  color: #2f5ea8;
  font-size: 12px;
}

.archive-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.archive-summary {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.archive-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: calc(100vh - 220px);
  height: calc(100vh - 220px);
}

.archive-layout-full {
  align-items: stretch;
}

.archive-sidebar,
.archive-main {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  overflow: auto;
  min-height: 0;
  height: 100%;
}

.archive-breadcrumb {
  margin-bottom: 12px;
  color: #909399;
  font-size: 13px;
}

.archive-entry-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.archive-entry-preview {
  min-height: calc(100% - 48px);
  height: calc(100% - 48px);
}

.archive-main .pdf-preview,
.archive-main .image-preview {
  height: calc(100% - 12px);
  min-height: 520px;
}

.archive-tree {
  background: transparent;
}

.unsupported-preview {
  width: 100%;
  height: 100%;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.unsupported-content {
  text-align: center;
  max-width: 400px;
}

.unsupported-content h3 {
  margin: 16px 0 8px;
  color: #303133;
}

.unsupported-content p {
  color: #909399;
  margin-bottom: 24px;
}

.preview-fallback {
  width: 100%;
  height: 100%;
}

.preview-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .document-preview-page {
    --preview-header-height: auto;
  }

  .archive-layout {
    grid-template-columns: 1fr;
  }

  .preview-header {
    height: auto;
    padding: 12px 16px;
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .header-left,
  .header-right {
    justify-content: space-between;
  }

  .document-meta {
    flex-direction: column;
    gap: 4px;
  }

  .preview-controls {
    order: -1;
  }

  .action-buttons {
    flex-wrap: wrap;
  }

  .preview-content {
    height: calc(100vh - 120px);
  }
}

@media (max-width: 480px) {
  .document-title {
    font-size: 16px;
  }

  .preview-controls .el-button {
    padding: 8px 12px;
  }

  .action-buttons .el-button {
    font-size: 12px;
    padding: 8px 12px;
  }
}
</style>