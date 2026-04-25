<template>
  <div class="admin-report-page">
    <el-card class="header-card">
      <div class="header">
        <h2>举报处理</h2>
        <el-button type="primary" @click="reloadAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div v-if="statistics" class="statistics">
        <el-statistic title="待处理" :value="statistics.pending" />
        <el-statistic title="调查中" :value="statistics.investigating" />
        <el-statistic title="已确认" :value="statistics.confirmed" />
        <el-statistic title="已驳回" :value="statistics.rejected" />
      </div>
    </el-card>

    <el-card class="content-card">
      <div class="filters">
        <el-checkbox-group v-model="selectedStatuses">
          <el-checkbox value="PENDING">待处理</el-checkbox>
          <el-checkbox value="INVESTIGATING">调查中</el-checkbox>
          <el-checkbox value="CONFIRMED">已确认</el-checkbox>
          <el-checkbox value="REJECTED">已驳回</el-checkbox>
        </el-checkbox-group>

        <el-select v-model="selectedReportType" clearable placeholder="举报类型" class="filter-item">
          <el-option
            v-for="option in reportTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <el-input
          v-model="documentIdInput"
          placeholder="文档ID"
          clearable
          class="filter-item filter-input"
          @keyup.enter="applyFilters"
        />

        <el-button type="primary" @click="applyFilters">筛选</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="reports" v-loading="loading" stripe>
        <el-table-column prop="id" label="举报ID" width="90" />
        <el-table-column prop="documentId" label="文档ID" width="100">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDocument(row.documentId)">
              {{ row.documentId }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="举报类型" width="140">
          <template #default="{ row }">
            <el-tag :type="getReportTypeTag(row.reportType)">
              {{ getReportTypeText(row.reportType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报描述" min-width="260">
          <template #default="{ row }">
            <el-text line-clamp="2">{{ row.description }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="120">
          <template #default="{ row }">
            {{ row.handlerId ? `管理员 #${row.handlerId}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="warning"
              size="small"
              plain
              @click="markInvestigating(row)"
            >
              调查中
            </el-button>
            <el-button
              v-if="canHandle(row.status)"
              type="primary"
              size="small"
              @click="openHandleDialog(row)"
            >
              处理
            </el-button>
            <el-button type="info" size="small" link @click="viewReportDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="loadReports"
      />
    </el-card>

    <el-dialog
      v-model="handleDialogVisible"
      title="处理举报"
      width="700px"
      :close-on-click-modal="false"
    >
      <ReportDetailPanel
        v-if="currentReport"
        :report="currentReport"
        :document-context="documentContext"
        @view-document="viewDocument"
      />

      <el-divider />

      <el-form
        ref="handleFormRef"
        :model="handleForm"
        :rules="handleRules"
        label-width="100px"
      >
        <el-form-item label="处理结果" prop="confirmed">
          <el-radio-group v-model="handleForm.confirmed">
            <el-radio :label="true">确认举报</el-radio>
            <el-radio :label="false">驳回举报</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="currentReport?.status === 'PENDING'" label="附加操作">
          <el-checkbox v-model="handleForm.markInvestigatingFirst">先记录为调查后结案</el-checkbox>
        </el-form-item>

        <el-form-item label="处理意见" prop="comment">
          <el-input
            v-model="handleForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入处理意见"
          />
        </el-form-item>

        <el-form-item v-if="handleForm.confirmed" label="文档处置" prop="documentAction">
          <el-radio-group v-model="handleForm.documentAction">
            <el-radio value="DELETE_DOCUMENT">删除文档</el-radio>
            <el-radio value="TAKE_DOWN_DOCUMENT">下架文档</el-radio>
            <el-radio value="NO_DOCUMENT_CHANGE">仅记录举报</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="补充说明" prop="actionNote">
          <el-input
            v-model="handleForm.actionNote"
            type="textarea"
            :rows="3"
            placeholder="可选，补充本次处理说明"
          />
        </el-form-item>

        <el-alert
          v-if="handleForm.confirmed"
          :title="getActionHint(handleForm.documentAction, currentReport?.reportType)"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-alert
          v-else
          title="驳回举报后，文档将保持当前状态，仅记录处理结果。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>

      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle" :loading="submitting">
          提交处理
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="举报详情" width="700px">
      <ReportDetailPanel
        v-if="currentReport"
        :report="currentReport"
        :document-context="documentContext"
        @view-document="viewDocument"
      />

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  reportApi,
  normalizeReportPageResponse,
  type AdminReportHandleRequest,
  type CopyrightReport,
  type ReportResolutionAction,
  type ReportStatistics,
  type ReportStatus,
  type ReportType
} from '@/api/report'
import { documentApi } from '@/api/document'
import type { Document } from '@/types'
import { formatDate } from '@/utils/date'

const defaultStatuses: ReportStatus[] = ['PENDING', 'INVESTIGATING']

const reportTypeOptions: Array<{ label: string; value: ReportType }> = [
  { label: '版权侵权', value: 'COPYRIGHT_INFRINGEMENT' },
  { label: '抄袭剽窃', value: 'PLAGIARISM' },
  { label: '违法内容', value: 'ILLEGAL_CONTENT' },
  { label: '不当内容', value: 'INAPPROPRIATE_CONTENT' },
  { label: '敏感内容', value: 'SENSITIVE_CONTENT' },
  { label: '虚假信息', value: 'FALSE_INFORMATION' },
  { label: '垃圾广告', value: 'SPAM' },
  { label: '其他', value: 'OTHER' }
]

const resolutionActionOptions: Array<{ label: string; value: ReportResolutionAction }> = [
  { label: '删除文档', value: 'DELETE_DOCUMENT' },
  { label: '下架文档', value: 'TAKE_DOWN_DOCUMENT' },
  { label: '仅记录举报，不修改文档', value: 'NO_DOCUMENT_CHANGE' }
]

type DocumentContext = {
  loading: boolean
  missing: boolean
  error: string
  data: Document | null
}

type HandleFormModel = AdminReportHandleRequest & {
  markInvestigatingFirst: boolean
}

const loading = ref(false)
const reports = ref<CopyrightReport[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statistics = ref<ReportStatistics | null>(null)

const selectedStatuses = ref<ReportStatus[]>([...defaultStatuses])
const selectedReportType = ref<ReportType | undefined>()
const documentIdInput = ref('')

const handleDialogVisible = ref(false)
const handleFormRef = ref<FormInstance>()
const submitting = ref(false)
const currentReport = ref<CopyrightReport | null>(null)

const detailDialogVisible = ref(false)

const documentContext = ref<DocumentContext>({
  loading: false,
  missing: false,
  error: '',
  data: null
})

const createDefaultHandleForm = (): HandleFormModel => ({
  confirmed: true,
  comment: '',
  documentAction: undefined,
  actionNote: '',
  markInvestigatingFirst: false
})

const handleForm = ref<HandleFormModel>(createDefaultHandleForm())

const handleRules: FormRules<HandleFormModel> = {
  confirmed: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
  comment: [{ required: true, message: '请输入处理意见', trigger: 'blur' }],
  documentAction: [{
    validator: (_rule, value, callback) => {
      if (!handleForm.value.confirmed || value) {
        callback()
        return
      }
      callback(new Error('确认举报时必须选择文档处置动作'))
    },
    trigger: 'change'
  }]
}

const canHandle = (status: ReportStatus) => status === 'PENDING' || status === 'INVESTIGATING'

const getRecommendedAction = (type?: ReportType): ReportResolutionAction | undefined => {
  if (!type) return undefined
  if (['COPYRIGHT_INFRINGEMENT', 'PLAGIARISM', 'ILLEGAL_CONTENT'].includes(type)) {
    return 'DELETE_DOCUMENT'
  }
  return 'TAKE_DOWN_DOCUMENT'
}

const resetDocumentContext = () => {
  documentContext.value = {
    loading: false,
    missing: false,
    error: '',
    data: null
  }
}

const loadDocumentContext = async (documentId: number) => {
  resetDocumentContext()
  documentContext.value.loading = true

  try {
    const res = await documentApi.getDocument(documentId)
    documentContext.value.data = res.data.data
  } catch (error: any) {
    const message = error?.message || ''
    if (message.includes('不存在') || message.includes('删除') || message.includes('404')) {
      documentContext.value.missing = true
      documentContext.value.error = '目标文档已不存在'
    } else {
      documentContext.value.error = message || '文档信息加载失败'
    }
  } finally {
    documentContext.value.loading = false
  }
}

const loadReports = async () => {
  loading.value = true
  try {
    const documentId = documentIdInput.value.trim() ? Number(documentIdInput.value.trim()) : undefined
    if (documentIdInput.value.trim() && !Number.isInteger(documentId)) {
      throw new Error('文档ID必须是整数')
    }

    const res = await reportApi.getAdminReports({
      statuses: selectedStatuses.value.length ? selectedStatuses.value : undefined,
      reportType: selectedReportType.value,
      documentId,
      page: currentPage.value,
      size: pageSize.value
    })
    const pageData = normalizeReportPageResponse<CopyrightReport>(res.data)
    reports.value = pageData.list
    currentPage.value = Number(pageData.page ?? currentPage.value) || 1
    pageSize.value = Number(pageData.size ?? pageSize.value) || pageSize.value
    total.value = Number(pageData.total ?? 0)
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const res = await reportApi.getReportStatistics()
    statistics.value = res.data.data
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

const reloadAll = async () => {
  await Promise.all([loadReports(), loadStatistics()])
}

const applyFilters = () => {
  currentPage.value = 1
  loadReports()
}

const resetFilters = () => {
  selectedStatuses.value = [...defaultStatuses]
  selectedReportType.value = undefined
  documentIdInput.value = ''
  currentPage.value = 1
  loadReports()
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  loadReports()
}

const openHandleDialog = async (report: CopyrightReport) => {
  currentReport.value = report
  handleForm.value = {
    ...createDefaultHandleForm(),
    documentAction: getRecommendedAction(report.reportType)
  }
  handleDialogVisible.value = true
  await loadDocumentContext(report.documentId)
}

const viewReportDetail = async (report: CopyrightReport) => {
  currentReport.value = report
  detailDialogVisible.value = true
  await loadDocumentContext(report.documentId)
}

const markInvestigating = async (report: CopyrightReport) => {
  try {
    const { value } = await ElMessageBox.prompt('可选填写调查说明', '标记为调查中', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入调查说明'
    })
    await reportApi.investigateReport(report.id, { comment: value?.trim() || undefined })
    ElMessage.success('已标记为调查中')
    await reloadAll()
  } catch (error: any) {
    if (error === 'cancel' || error?.message === 'cancel') {
      return
    }
    ElMessage.error(error.message || '操作失败')
  }
}

const submitHandle = async () => {
  if (!handleFormRef.value || !currentReport.value) return

  try {
    await handleFormRef.value.validate()

    const actionText = handleForm.value.confirmed ? '确认并处理' : '驳回'
    await ElMessageBox.confirm(`确定要${actionText}该举报吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    submitting.value = true

    if (handleForm.value.markInvestigatingFirst && currentReport.value.status === 'PENDING') {
      await reportApi.investigateReport(currentReport.value.id, {
        comment: '进入调查流程：' + handleForm.value.comment
      })
    }

    await reportApi.handleReport(currentReport.value.id, {
      confirmed: handleForm.value.confirmed,
      comment: handleForm.value.comment,
      documentAction: handleForm.value.confirmed ? handleForm.value.documentAction : undefined,
      actionNote: handleForm.value.actionNote?.trim() || undefined
    })

    ElMessage.success('处理成功')
    handleDialogVisible.value = false
    await reloadAll()
  } catch (error: any) {
    if (error === 'cancel' || error?.message === 'cancel') {
      return
    }
    ElMessage.error(error.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

const viewDocument = async (documentId: number) => {
  try {
    await documentApi.getDocument(documentId)
    window.open(`/documents/${documentId}`, '_blank')
  } catch (error: any) {
    ElMessage.warning(error.message || '该文档不存在或已被删除')
  }
}

const getReportTypeText = (type: ReportType): string => {
  const typeMap: Record<ReportType, string> = {
    COPYRIGHT_INFRINGEMENT: '版权侵权',
    PLAGIARISM: '抄袭剽窃',
    ILLEGAL_CONTENT: '违法内容',
    INAPPROPRIATE_CONTENT: '不当内容',
    SENSITIVE_CONTENT: '敏感内容',
    FALSE_INFORMATION: '虚假信息',
    SPAM: '垃圾广告',
    OTHER: '其他'
  }
  return typeMap[type] || type
}

const getReportTypeTag = (type: ReportType): string => {
  const tagMap: Record<ReportType, string> = {
    COPYRIGHT_INFRINGEMENT: 'danger',
    PLAGIARISM: 'warning',
    ILLEGAL_CONTENT: 'danger',
    INAPPROPRIATE_CONTENT: 'warning',
    SENSITIVE_CONTENT: 'danger',
    FALSE_INFORMATION: 'warning',
    SPAM: 'info',
    OTHER: 'info'
  }
  return tagMap[type] || 'info'
}

const getStatusText = (status: ReportStatus) => {
  const statusMap: Record<ReportStatus, string> = {
    PENDING: '待处理',
    INVESTIGATING: '调查中',
    CONFIRMED: '已确认',
    REJECTED: '已驳回',
    CLOSED: '已关闭'
  }
  return statusMap[status] || status
}

const getStatusTag = (status: ReportStatus) => {
  const tagMap: Record<ReportStatus, string> = {
    PENDING: 'info',
    INVESTIGATING: 'warning',
    CONFIRMED: 'danger',
    REJECTED: 'success',
    CLOSED: 'info'
  }
  return tagMap[status] || 'info'
}

const getActionHint = (action?: ReportResolutionAction, type?: ReportType) => {
  if (action) {
    return resolutionActionOptions.find(item => item.value === action)?.label || '请确认文档处置方式'
  }
  const recommended = getRecommendedAction(type)
  return recommended
    ? `建议动作：${resolutionActionOptions.find(item => item.value === recommended)?.label}`
    : '请确认文档处置方式'
}

const ReportDetailPanel = defineComponent({
  name: 'ReportDetailPanel',
  props: {
    report: {
      type: Object as () => CopyrightReport,
      required: true
    },
    documentContext: {
      type: Object as () => DocumentContext,
      required: true
    }
  },
  emits: ['view-document'],
  setup(props, { emit }) {
    const metaItem = (label: string, value: string | ReturnType<typeof h>) => h('div', { class: 'meta-item' }, [
      h('div', { class: 'meta-label' }, label),
      typeof value === 'string'
        ? h('div', { class: 'meta-value' }, value)
        : h('div', { class: 'meta-value' }, [value])
    ])

    const textBlock = (label: string, value: string) => h('div', { class: 'detail-card detail-card-full' }, [
      h('div', { class: 'detail-card-label' }, label),
      h('div', { class: 'detail-card-content' }, value)
    ])

    return () => h('div', { class: 'report-panel' }, [
      h('div', { class: 'panel-section' }, [
        h('div', { class: 'panel-header' }, [
          h('div', { class: 'panel-title' }, '举报信息'),
          h('div', { class: ['status-pill', `status-pill--${props.report.status.toLowerCase()}`] }, getStatusText(props.report.status))
        ]),
        h('div', { class: 'meta-grid' }, [
          metaItem('举报ID', String(props.report.id)),
          metaItem('文档ID', h('a', {
            href: 'javascript:void(0)',
            class: 'inline-link',
            onClick: () => emit('view-document', props.report.documentId)
          }, String(props.report.documentId))),
          metaItem('举报人', `用户 #${props.report.reporterId}`),
          metaItem('举报类型', getReportTypeText(props.report.reportType)),
          metaItem('提交时间', formatDate(props.report.createdAt)),
          metaItem('处理人', props.report.handlerId ? `管理员 #${props.report.handlerId}` : '未处理'),
          metaItem('处理时间', props.report.handledAt ? formatDate(props.report.handledAt) : '未处理')
        ]),
        h('div', { class: 'detail-card-grid' }, [
          textBlock('举报描述', props.report.description),
          props.report.contactInfo ? textBlock('联系方式', props.report.contactInfo) : null,
          props.report.handlerComment ? textBlock('处理意见', props.report.handlerComment) : null,
          props.report.actionTaken ? textBlock('处理结果', props.report.actionTaken) : null
        ])
      ]),
      h('div', { class: 'panel-section' }, [
        h('div', { class: 'panel-title' }, '文档上下文'),
        props.documentContext.loading
          ? h('div', { class: 'state-card' }, '正在加载文档信息...')
          : props.documentContext.missing
            ? h('div', { class: 'state-card state-card--missing' }, '目标文档已不存在，但仍可处理该举报记录。')
            : props.documentContext.error
              ? h('div', { class: 'state-card state-card--warning' }, props.documentContext.error)
              : props.documentContext.data
                ? h('div', { class: 'meta-grid' }, [
                    metaItem('标题', props.documentContext.data.title),
                    metaItem('文档状态', props.documentContext.data.status),
                    metaItem('上传者', `用户 #${props.documentContext.data.uploaderId}`),
                    metaItem('文件类型', props.documentContext.data.fileType || '-')
                  ])
                : h('div', { class: 'state-card' }, '暂无文档信息')
      ])
    ])
  }
})

onMounted(() => {
  reloadAll()
})
</script>

<style lang="scss">
.admin-report-page {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
    }
  }

  .statistics {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .filters {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    margin-bottom: 16px;
  }

  .filter-item {
    width: 180px;
  }

  .filter-input {
    width: 140px;
  }

  .el-pagination {
    margin-top: 20px;
    justify-content: center;
  }
}

.report-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-section {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 18px;
  background: #fff;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-title {
  font-weight: 600;
  color: #303133;
}

.status-pill {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  background: #d9ecff;
  color: #1d4ed8;
}

.status-pill--pending {
  background: #e9e9eb;
  color: #4b5563;
}

.status-pill--investigating {
  background: #faecd8;
  color: #9a3412;
}

.status-pill--confirmed {
  background: #fde2e2;
  color: #b42318;
}

.status-pill--rejected {
  background: #e7f6e7;
  color: #166534;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f9fc;
  border: 1px solid #eef2f7;
}

.meta-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.meta-value {
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.detail-card-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.detail-card {
  padding: 14px 16px;
  border-radius: 10px;
  background: #fafbfd;
  border: 1px solid #eef2f7;
}

.detail-card-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.detail-card-content {
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.inline-link {
  color: #409eff;
  text-decoration: none;
}

:deep(.el-tag),
:deep(.el-tag span),
:deep(.el-tag__content),
:deep(.el-tag .el-tag__content) {
  color: #000000 !important;
}

/* 针对表格中的标签 */
:deep(.el-table .el-tag),
:deep(.el-table .el-tag.el-tag--danger),
:deep(.el-table .el-tag.el-tag--warning),
:deep(.el-table .el-tag.el-tag--success),
:deep(.el-table .el-tag.el-tag--info),
:deep(.el-table .el-tag.el-tag--primary) {
  color: #000000 !important;
}

:deep(.el-table .el-tag .el-tag__content) {
  color: #000000 !important;
}

.state-card {
  padding: 14px 16px;
  border-radius: 10px;
  background: #f7f9fc;
  border: 1px solid #eef2f7;
  color: #606266;
}

.state-card--warning {
  background: #faecd8;
  border-color: #f5d7b2;
  color: #9a3412;
}

.state-card--missing {
  background: #fde2e2;
  border-color: #f6caca;
  color: #b42318;
}
</style>
