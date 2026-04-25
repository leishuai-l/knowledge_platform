<template>
  <div class="admin-documents">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>文档管理</h2>
          <p>管理系统中的所有文档，进行审核、编辑和删除操作</p>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="search-section">
        <el-form :model="searchForm" :inline="true">
          <el-form-item label="文档状态">
            <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 150px">
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索标题、描述"
              clearable
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 文档列表 -->
      <div class="table-container">
        <el-table
          v-loading="loading"
          :data="documents"
          stripe
          @selection-change="handleSelectionChange"
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          :row-class-name="getRowClassName"
        >
          <el-table-column type="selection" width="55" />

          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="document-title">
                <el-icon :color="getFileTypeColor(row.fileExtension)" class="file-icon">
                  <component :is="getFileTypeIcon(row.fileExtension)" />
                </el-icon>
                <div class="title-content">
                  <div class="title-text">{{ row.title }}</div>
                  <div class="file-info">{{ row.fileName }}</div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="default" effect="dark">
                {{ getStatusText(row.status) }}
              </el-tag>
              <div v-if="row.status === 'PENDING'" style="font-size: 12px; color: #E6A23C; margin-top: 4px;">
                待审核
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="uploader" label="上传者" width="120">
            <template #default="{ row }">
              <div class="user-info">
                <div>{{ row.uploader?.username }}</div>
                <div class="user-role">{{ row.uploader?.role }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              <el-tag
                size="small"
                type="info"
                effect="light"
                style="background-color: #ecf5ff; color: #409eff; border-color: #b3d8ff;"
              >
                {{ row.category?.name || '未分类' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="fileSize" label="文件大小" width="100">
            <template #default="{ row }">
              <div class="file-size">{{ row.readableFileSize || formatFileSize(row.fileSize) }}</div>
            </template>
          </el-table-column>

          <el-table-column prop="downloadCount" label="下载次数" width="100">
            <template #default="{ row }">
              <span style="color: #000000;">{{ row.downloadCount || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="downloadPoints" label="下载积分" width="100" />

          <el-table-column prop="createdAt" label="上传时间" width="160">
            <template #default="{ row }">
              <div class="time-info">
                <div>{{ formatDate(row.createdAt) }}</div>
                <div v-if="row.approvedAt" class="approved-time">
                  审核: {{ formatDate(row.approvedAt) }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button
                  size="small"
                  type="primary"
                  link
                  @click="handlePreview(row)"
                  :disabled="!canPreview(row)"
                >
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="warning"
                  size="small"
                  link
                  @click="handleReviewClick(row)"
                >
                  <el-icon><Check /></el-icon>
                  审核
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  link
                  @click="handleDelete(row)"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 批量操作 -->
      <div class="batch-actions" v-if="selectedDocuments.length">
        <span>已选择 {{ selectedDocuments.length }} 项</span>
        <el-button type="success" @click="handleBatchApprove">批量通过</el-button>
        <el-button type="warning" @click="handleBatchReject">批量拒绝</el-button>
        <el-button type="danger" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 审核对话框 -->
    <DocumentReviewDialog
      v-model="showReviewDialog"
      :document="currentReviewDocument"
      @approve="onApprove"
      @reject="onReject"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document as DocumentIcon,
  Picture,
  VideoPlay,
  Headset,
  FolderOpened,
  View,
  Check,
  Delete
} from '@element-plus/icons-vue'
import { adminApi, normalizeAdminPageResponse } from '@/api/admin'
import { documentApi } from '@/api/document'
import { useUserStore } from '@/stores/user'
import type { Document } from '@/types'
import DocumentReviewDialog from '@/components/DocumentReviewDialog.vue'

const userStore = useUserStore()
const route = useRoute()
const loading = ref(false)
const showReviewDialog = ref(false)
const selectedDocuments = ref<Document[]>([])
const documents = ref<Document[]>([])
const currentReviewDocument = ref<Document | null>(null)

const searchForm = reactive({
  status: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
  pages: 0
})

const previewableDocumentIds = ref(new Set<number>())

// 根据文件扩展名获取图标
const getFileTypeIcon = (extension: string) => {
  const ext = extension.toLowerCase()
  if (['pdf', 'doc', 'docx', 'txt'].includes(ext)) return DocumentIcon
  if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) return Picture
  if (['mp4', 'avi', 'mov'].includes(ext)) return VideoPlay
  if (['mp3', 'wav', 'aac'].includes(ext)) return Headset
  if (['zip', 'rar', '7z'].includes(ext)) return FolderOpened
  return DocumentIcon
}

// 根据文件扩展名获取颜色
const getFileTypeColor = (extension: string) => {
  const ext = extension.toLowerCase()
  if (ext === 'pdf') return '#FF6B6B'
  if (['doc', 'docx'].includes(ext)) return '#4DABF7'
  if (['xls', 'xlsx'].includes(ext)) return '#51CF66'
  if (['ppt', 'pptx'].includes(ext)) return '#FF922B'
  if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) return '#9775FA'
  return '#409EFF'
}

// 状态类型
const getStatusType = (status: string) => {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    default: return 'info'
  }
}

// 状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '待审核'
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已拒绝'
    default: return status
  }
}

// 文件大小格式化
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 日期格式化
const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 判断是否可以预览
const canPreview = (document: Document): boolean => {
  // 管理员总是可以预览
  if (userStore.isAdmin) {
    return true
  }
  return previewableDocumentIds.value.has(document.id)
}

const loadPreviewableState = async (items: Document[]) => {
  const entries = await Promise.all(items.map(async (document) => {
    try {
      const response = await documentApi.getDocumentPreviewInfo(document.id)
      const info = response.data?.data
      const isPreviewable = info?.previewType !== 'NOT_SUPPORTED' && info?.previewable !== false
      console.log(`[Preview Check] Doc ${document.id} (${document.fileExtension}): previewType=${info?.previewType}, previewable=${isPreviewable}`)
      return [document.id, isPreviewable] as const
    } catch (error: any) {
      console.error(`[Preview Check] Failed for doc ${document.id}:`, error?.response?.status, error?.response?.data?.message || error.message)
      return [document.id, false] as const
    }
  }))

  previewableDocumentIds.value = new Set(entries.filter(([, previewable]) => previewable).map(([id]) => id))
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadDocuments()
}

// 重置搜索
const resetSearch = () => {
  searchForm.status = ''
  searchForm.keyword = ''
  pagination.page = 1
  loadDocuments()
}

// 选择变化
const handleSelectionChange = (selection: Document[]) => {
  selectedDocuments.value = selection
}

// 分页变化
const handlePageChange = (page: number) => {
  pagination.page = page
  loadDocuments()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadDocuments()
}

// 获取行类名（根据状态区分样式）
const getRowClassName = ({ row }: { row: Document }) => {
  switch (row.status) {
    case 'PENDING':
      return 'row-pending'
    case 'REJECTED':
      return 'row-rejected'
    case 'APPROVED':
      return 'row-approved'
    default:
      return ''
  }
}

// 预览文档
const handlePreview = (document: Document) => {
  window.open(`/documents/${document.id}/preview?admin=true`, '_blank')
}

// 打开审核对话框
const handleReviewClick = (document: Document) => {
  currentReviewDocument.value = document
  showReviewDialog.value = true
}

// 通过审核
const onApprove = async (document: Document) => {
  try {
    await adminApi.approveDocument(document.id)
    ElMessage.success('文档已通过审核')
    loadDocuments()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 拒绝审核
const onReject = async (document: Document, comment: string) => {
  try {
    await adminApi.rejectDocument(document.id, comment)
    ElMessage.success('文档已拒绝')
    loadDocuments()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 删除文档
const handleDelete = async (document: Document) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${document.title}」吗？此操作不可恢复！`,
      '确认删除',
      { type: 'warning' }
    )

    await adminApi.deleteDocument(document.id)

    ElMessage.success('文档已删除')
    await loadDocuments()
  } catch (error: any) {
    if (error.message !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 批量通过
const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(`确定要通过选中的 ${selectedDocuments.value.length} 个文档吗？`)

    await adminApi.batchApproveDocuments(selectedDocuments.value.map(d => d.id))

    ElMessage.success('批量通过成功')
    loadDocuments()
  } catch (error: any) {
    if (error.message !== 'cancel') {
      ElMessage.error(error.message || '批量操作失败')
    }
  }
}

// 批量拒绝
const handleBatchReject = () => {
  ElMessage.info('批量拒绝功能开发中...')
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDocuments.value.length} 个文档吗？此操作不可恢复！`,
      '确认批量删除',
      { type: 'warning' }
    )

    await adminApi.batchDeleteDocuments(selectedDocuments.value.map(doc => doc.id))

    ElMessage.success('批量删除成功')
    selectedDocuments.value = []
    await loadDocuments()
  } catch (error: any) {
    if (error.message !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  }
}

// 加载文档列表
const loadDocuments = async () => {
  try {
    loading.value = true

    const params = {
      page: pagination.page,
      size: pagination.size,
      status: searchForm.status,
      keyword: searchForm.keyword
    }

    const response = await adminApi.getDocuments(params)
    const pageData = normalizeAdminPageResponse<Document>(response.data.data)
    documents.value = pageData.list || []
    await loadPreviewableState(documents.value)
    pagination.page = Number(pageData.page ?? pagination.page) || 1
    pagination.size = Number(pageData.size ?? pagination.size) || 10
    pagination.total = Number(pageData.total ?? 0)
    pagination.pages = Number(pageData.pages ?? 0)

    if (pagination.total > 0 && documents.value.length === 0 && pagination.page > 1) {
      pagination.page -= 1
      await loadDocuments()
    }

  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDocuments()
})

// 监听路由查询参数变化，当从预览页返回时刷新数据
watch(() => route.query.refresh, (newVal) => {
  if (newVal === 'true') {
    loadDocuments()
  }
})
</script>

<style scoped>
.admin-documents {
  height: 100%;
}

.card-header h2 {
  margin: 0 0 10px;
  color: #2c3e50;
}

.card-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.search-section {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.document-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-actions {
  margin: 20px 0;
  padding: 15px;
  background: #f0f9ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.table-container {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.file-icon {
  font-size: 18px;
}

.title-content {
  flex: 1;
  min-width: 0;
}

.title-text {
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-info {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.user-info {
  line-height: 1.4;
}

.user-role {
  font-size: 12px;
  color: #909399;
}

.file-size {
  color: #606266;
  font-family: monospace;
}

.time-info {
  line-height: 1.4;
}

.approved-time {
  font-size: 12px;
  color: #67C23A;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 行状态样式 */
:deep(.row-pending) {
  background-color: #fdf6ec !important;
}

:deep(.row-rejected) {
  background-color: #fef0f0 !important;
}

:deep(.row-approved) {
  background-color: #f0f9ff !important;
}

:deep(.row-deleted) {
  background-color: #f5f5f5 !important;
  opacity: 0.6;
}

:deep(.row-pending:hover) {
  background-color: #faecd8 !important;
}

:deep(.row-rejected:hover) {
  background-color: #fde2e2 !important;
}

:deep(.row-approved:hover) {
  background-color: #e1f5fe !important;
}

:deep(.row-deleted:hover) {
  background-color: #e0e0e0 !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-section :deep(.el-form) {
    display: block;
  }

  .search-section :deep(.el-form-item) {
    margin-bottom: 15px;
    margin-right: 0;
  }

  .batch-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .batch-actions span {
    text-align: center;
  }

  .action-buttons {
    flex-direction: column;
    align-items: stretch;
  }

  .action-buttons .el-button {
    justify-content: center;
  }
}
</style>