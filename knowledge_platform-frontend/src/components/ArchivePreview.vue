<template>
  <div class="archive-preview-container">
    <!-- 工具栏 -->
    <div class="archive-toolbar">
      <div class="toolbar-left">
        <span class="file-info">{{ fileExtension.toUpperCase() }} 压缩包 • {{ formatFileSize(fileSize) }}</span>
        <span v-if="loading" class="loading-hint">
          <el-icon class="is-loading"><Loading /></el-icon> 正在解析...
        </span>
      </div>
      <div class="toolbar-right">
        <el-button v-if="!treeData.length && !loading" size="small" @click="loadArchive">
          <el-icon><Refresh /></el-icon> 重试
        </el-button>
        <el-button size="small" @click="toggleCollapseAll">
          <el-icon><Folder /></el-icon>
          {{ allExpanded ? '全部折叠' : '全部展开' }}
        </el-button>
        <el-button size="small" type="primary" @click="downloadFile">
          <el-icon><Download /></el-icon> 下载
        </el-button>
      </div>
    </div>

    <!-- 解析警告 -->
    <div v-if="warnings.length" class="archive-warnings">
      <el-alert
        v-for="(warning, idx) in warnings"
        :key="idx"
        :title="warning"
        type="warning"
        :closable="false"
        show-icon
        class="warning-item"
      />
    </div>

    <!-- 解析成功：目录树 -->
    <div v-if="treeData.length" class="archive-tree-container">
      <div class="entry-count">
        共 {{ totalCount }} 项{{ truncated ? '（已限制显示）' : '' }}
      </div>
      <el-tree
        :data="treeData"
        :props="{ label: 'name', children: 'children' }"
        :default-expand-all="false"
        :expand-on-click-node="false"
        node-key="path"
        class="archive-tree"
      >
        <template #default="{ node, data }">
          <div class="tree-node" @click="handleNodeClick(data)">
            <div class="node-icon">
              <el-icon v-if="data.directory" :size="16" color="#f5a623">
                <FolderOpened v-if="node.expanded" />
                <Folder v-else />
              </el-icon>
              <el-icon v-else :size="16" :color="getFileIconColor(data.extension)">
                <Document />
              </el-icon>
            </div>
            <span class="node-name" :class="{ 'is-directory': data.directory }">
              {{ data.name }}
            </span>
            <span v-if="!data.directory" class="node-size">
              {{ formatFileSize(data.size) }}
            </span>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- 解析失败 -->
    <div v-else-if="!loading && parseSucceeded === false" class="archive-empty">
      <el-result
        icon="warning"
        title="压缩包解析失败"
        sub-title="无法读取压缩包内容结构"
      >
        <template #extra>
          <el-button type="primary" @click="downloadFile">
            <el-icon><Download /></el-icon> 下载查看
          </el-button>
        </template>
      </el-result>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="archive-loading">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 条目预览面板 -->
    <div v-if="previewEntry" class="entry-preview-panel">
      <div class="preview-header">
        <span class="preview-title">{{ previewEntry.entryName }}</span>
        <el-button size="small" text @click="closePreview">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="preview-body">
        <template v-if="previewEntry.loading">
          <div class="preview-loading">
            <el-icon class="is-loading"><Loading /></el-icon> 正在加载预览...
          </div>
        </template>
        <template v-else-if="previewEntry.error">
          <div class="preview-error">
            <p>{{ previewEntry.error }}</p>
            <el-button v-if="previewEntry.previewable === false" type="primary" size="small" @click="downloadEntryFile">
              <el-icon><Download /></el-icon> 下载该文件
            </el-button>
          </div>
        </template>
        <template v-else-if="previewEntry.previewType === 'TEXT'">
          <div class="text-preview-content" v-html="previewEntry.content"></div>
        </template>
        <template v-else-if="previewEntry.previewType === 'IMAGE'">
          <div class="image-preview-content">
            <img :src="previewEntry.contentUrl" :alt="previewEntry.entryName" />
          </div>
        </template>
        <template v-else-if="previewEntry.previewType === 'PDF'">
          <div class="pdf-preview-content">
            <iframe :src="previewEntry.contentUrl" class="entry-pdf-iframe" title="PDF预览" />
          </div>
        </template>
        <template v-else>
          <div class="unsupported-preview">
            <p>该文件类型暂不支持预览</p>
            <p class="entry-info">
              <span>{{ previewEntry.extension?.toUpperCase() || '未知' }} 文件</span>
              <span> • </span>
              <span>{{ formatFileSize(previewEntry.entrySize) }}</span>
            </p>
            <el-button type="primary" size="small" @click="downloadEntryFile">
              <el-icon><Download /></el-icon> 下载该文件
            </el-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Download, Folder, FolderOpened, Document, Loading, Refresh, Close
} from '@element-plus/icons-vue'
import {
  documentApi,
  type ArchiveTreeNode,
  type ArchiveEntryPreviewInfo
} from '@/api/document'

interface Props {
  documentId: number
  fileName: string
  fileExtension: string
  fileSize: number
}

const props = defineProps<Props>()

interface PreviewEntry {
  entryName: string
  entryPath: string
  entrySize: number
  extension: string
  previewType: string
  previewable: boolean
  content?: string
  contentUrl?: string
  error?: string
  loading: boolean
}

const loading = ref(true)
const treeData = ref<ArchiveTreeNode[]>([])
const warnings = ref<string[]>([])
const truncated = ref(false)
const depthLimited = ref(false)
const parseSucceeded = ref<boolean | null>(null)
const totalCount = ref(0)
const allExpanded = ref(false)
const previewEntry = ref<PreviewEntry | null>(null)

const loadArchive = async () => {
  try {
    loading.value = true
    const response = await documentApi.getDocumentArchivePreview(props.documentId)
    const data = response.data

    if (data.code === 0 || data.code === 200) {
      const payload = data.data
      treeData.value = payload.archiveTree || []
      warnings.value = payload.archiveWarnings || []
      truncated.value = payload.archiveTruncated
      depthLimited.value = payload.archiveDepthLimited
      parseSucceeded.value = payload.archiveParseSucceeded
      totalCount.value = payload.archiveEntryCountTotal || 0
    } else {
      parseSucceeded.value = false
      ElMessage.error(data.message || '加载压缩包预览失败')
    }
  } catch (err: any) {
    parseSucceeded.value = false
    ElMessage.error(err.message || '加载压缩包预览失败')
  } finally {
    loading.value = false
  }
}

const handleNodeClick = async (node: ArchiveTreeNode) => {
  if (node.directory) return

  // 如果是图片/PDF，可以直接预览
  if (['jpg', 'jpeg', 'png', 'gif', 'pdf'].includes(node.extension?.toLowerCase() || '')) {
    try {
      previewEntry.value = {
        entryName: node.name,
        entryPath: node.path,
        entrySize: node.size,
        extension: node.extension || '',
        previewType: node.extension?.toLowerCase() === 'pdf' ? 'PDF' : 'IMAGE',
        previewable: true,
        loading: true
      }

      const response = await documentApi.getDocumentArchiveEntryPreviewContent(props.documentId, node.path)
      const responseType = response.headers?.['content-type'] as string | undefined
      const normalizedType = responseType?.split(';')[0]?.trim()

      let blob: Blob
      if (response.data instanceof Blob) {
        blob = response.data
        if (!blob.type && normalizedType) {
          blob = new Blob([await blob.arrayBuffer()], { type: normalizedType })
        }
      } else if (response.data instanceof ArrayBuffer) {
        blob = new Blob([response.data], normalizedType ? { type: normalizedType } : undefined)
      } else if (response.data instanceof Uint8Array) {
        blob = new Blob([response.data], normalizedType ? { type: normalizedType } : undefined)
      } else {
        blob = new Blob([response.data as unknown as BlobPart], normalizedType ? { type: normalizedType } : undefined)
      }
      const contentUrl = URL.createObjectURL(blob)

      previewEntry.value.contentUrl = contentUrl
      previewEntry.value.loading = false
    } catch (err: any) {
      previewEntry.value = {
        entryName: node.name,
        entryPath: node.path,
        entrySize: node.size,
        extension: node.extension || '',
        previewType: 'NOT_SUPPORTED',
        previewable: false,
        error: '文件预览加载失败',
        loading: false
      }
    }
    return
  }

  // 其他文件：先获取预览信息
  try {
    previewEntry.value = {
      entryName: node.name,
      entryPath: node.path,
      entrySize: node.size,
      extension: node.extension || '',
      previewType: 'NOT_SUPPORTED',
      previewable: false,
      loading: true
    }

    const infoResponse = await documentApi.getDocumentArchiveEntryPreviewInfo(props.documentId, node.path)
    const info: ArchiveEntryPreviewInfo = infoResponse.data.data

    if (info.previewable && info.content) {
      // 文本内容直接嵌入
      previewEntry.value.previewType = 'TEXT'
      previewEntry.value.content = info.content
      previewEntry.value.previewable = true
      previewEntry.value.loading = false
    } else if (info.previewable) {
      previewEntry.value.previewType = info.previewType || 'NOT_SUPPORTED'
      previewEntry.value.previewable = true
      previewEntry.value.error = info.reason || '该文件类型暂不支持预览'
      previewEntry.value.loading = false
    } else {
      previewEntry.value.previewType = info.previewType || 'NOT_SUPPORTED'
      previewEntry.value.previewable = false
      previewEntry.value.error = info.reason || '该文件类型暂不支持预览'
      previewEntry.value.loading = false
    }
  } catch (err: any) {
    previewEntry.value = {
      entryName: node.name,
      entryPath: node.path,
      entrySize: node.size,
      extension: node.extension || '',
      previewType: 'NOT_SUPPORTED',
      previewable: false,
      error: '预览信息加载失败',
      loading: false
    }
  }
}

const closePreview = () => {
  if (previewEntry.value?.contentUrl) {
    URL.revokeObjectURL(previewEntry.value.contentUrl)
  }
  previewEntry.value = null
}

const downloadEntryFile = async () => {
  if (!previewEntry.value) return
  try {
    const response = await documentApi.getDocumentArchiveEntryPreviewContent(props.documentId, previewEntry.value.entryPath)
    const blob = response.data as unknown as Blob
    const url = globalThis.URL.createObjectURL(blob)
    const link = globalThis.document.createElement('a')
    link.href = url
    link.download = previewEntry.value.entryName
    link.click()
    globalThis.URL.revokeObjectURL(url)
  } catch (err: any) {
    ElMessage.error('下载失败: ' + (err.message || '未知错误'))
  }
}

const toggleCollapseAll = () => {
  allExpanded.value = !allExpanded.value
  // Note: el-tree doesn't expose a direct collapse-all API,
  // this is handled via the tree's expand-on-click-node behavior
}

const downloadFile = () => {
  documentApi.downloadDocument(props.documentId)
}

const getFileIconColor = (extension?: string): string => {
  const ext = (extension || '').toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) return '#67c23a'
  if (['pdf'].includes(ext)) return '#e6a23c'
  if (['txt', 'md', 'json', 'xml', 'csv', 'log'].includes(ext)) return '#909399'
  if (['doc', 'docx'].includes(ext)) return '#4DABF7'
  if (['xls', 'xlsx'].includes(ext)) return '#51CF66'
  if (['ppt', 'pptx'].includes(ext)) return '#FF922B'
  if (['zip', 'rar', '7z'].includes(ext)) return '#f5a623'
  return '#409EFF'
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

onMounted(() => {
  loadArchive()
})
</script>

<style scoped>
.archive-preview-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  overflow: hidden;
}

.archive-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.file-info {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.loading-hint {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.archive-warnings {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
}

.warning-item {
  margin: 0;
}

.archive-tree-container {
  flex: 1;
  overflow: auto;
  padding: 8px;
}

.entry-count {
  font-size: 12px;
  color: #909399;
  padding: 0 4px 8px;
}

.archive-tree {
  background: transparent;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 2px 0;
  cursor: pointer;
}

.tree-node:hover {
  background: #f5f7fa;
  border-radius: 4px;
}

.node-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.node-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-name.is-directory {
  font-weight: 500;
}

.node-size {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
  flex-shrink: 0;
}

.archive-loading,
.archive-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* 条目预览面板 */
.entry-preview-panel {
  border-top: 1px solid #e4e7ed;
  background: #fff;
  display: flex;
  flex-direction: column;
  max-height: 300px;
  flex-shrink: 0;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.preview-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-body {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.preview-loading,
.preview-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: #909399;
  font-size: 13px;
}

.text-preview-content {
  font-family: 'Courier New', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #303133;
  max-height: 240px;
  overflow: auto;
}

.image-preview-content {
  display: flex;
  justify-content: center;
  padding: 8px;
  max-height: 240px;
  overflow: auto;
}

.image-preview-content img {
  max-width: 100%;
  max-height: 220px;
  object-fit: contain;
  border-radius: 4px;
}

.pdf-preview-content {
  height: 240px;
}

.entry-pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.unsupported-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: #909399;
  font-size: 13px;
}

.entry-info {
  color: #c0c4cc;
  font-size: 12px;
}

@media (max-width: 768px) {
  .archive-toolbar {
    flex-direction: column;
    gap: 6px;
    height: auto;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }
}
</style>
