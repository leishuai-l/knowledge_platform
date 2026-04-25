<template>
  <div class="copyright-report-page">
    <el-card class="header-card">
      <div class="header">
        <h2>侵权举报</h2>
        <el-button type="primary" @click="showReportDialog = true">
          <el-icon><Warning /></el-icon>
          提交举报
        </el-button>
      </div>
    </el-card>

    <!-- 举报列表 -->
    <el-card class="content-card">
      <el-table :data="reports" v-loading="loading" stripe>
        <el-table-column prop="id" label="举报ID" width="80" />
        <el-table-column prop="documentId" label="文档ID" width="100">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDocument(row.documentId)">
              {{ row.documentId }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="举报类型" width="150">
          <template #default="{ row }">
            <el-tag :type="getReportTypeTag(row.reportType)">
              {{ getReportTypeText(row.reportType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报描述" min-width="200">
          <template #default="{ row }">
            <el-text line-clamp="2">{{ row.description }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="info">待处理</el-tag>
            <el-tag v-else-if="row.status === 'INVESTIGATING'" type="warning">调查中</el-tag>
            <el-tag v-else-if="row.status === 'CONFIRMED'" type="danger">已确认</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="success">已驳回</el-tag>
            <el-tag v-else type="info">已关闭</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewReportDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadReports"
        @current-change="loadReports"
      />
    </el-card>

    <!-- 提交举报对话框 -->
    <el-dialog
      v-model="showReportDialog"
      title="提交侵权举报"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="reportFormRef"
        :model="reportForm"
        :rules="reportRules"
        label-width="100px"
      >
        <el-form-item label="文档ID" prop="documentId">
          <el-input-number
            v-model="reportForm.documentId"
            :min="1"
            placeholder="请输入要举报的文档ID"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="举报类型" prop="reportType">
          <el-select v-model="reportForm.reportType" placeholder="请选择举报类型" style="width: 100%">
            <el-option label="版权侵权" value="COPYRIGHT_INFRINGEMENT" />
            <el-option label="抄袭剽窃" value="PLAGIARISM" />
            <el-option label="违法内容" value="ILLEGAL_CONTENT" />
            <el-option label="不当内容" value="INAPPROPRIATE_CONTENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="举报描述" prop="description">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="5"
            placeholder="请详细描述侵权情况"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系方式" prop="contactInfo">
          <el-input
            v-model="reportForm.contactInfo"
            placeholder="请提供您的联系方式（邮箱或电话，方便管理员联系您）"
            maxlength="200"
          />
        </el-form-item>

        <el-alert
          title="提示：恶意举报将受到处罚"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>

      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReport" :loading="submitting">
          提交举报
        </el-button>
      </template>
    </el-dialog>

    <!-- 举报详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="举报详情"
      width="600px"
    >
      <el-descriptions v-if="currentReport" :column="1" border>
        <el-descriptions-item label="举报ID">
          {{ currentReport.id }}
        </el-descriptions-item>
        <el-descriptions-item label="文档ID">
          <el-link type="primary" @click="viewDocument(currentReport.documentId)">
            {{ currentReport.documentId }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="举报类型">
          <el-tag :type="getReportTypeTag(currentReport.reportType)">
            {{ getReportTypeText(currentReport.reportType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="举报状态">
          <el-tag v-if="currentReport.status === 'PENDING'" type="info">待处理</el-tag>
          <el-tag v-else-if="currentReport.status === 'INVESTIGATING'" type="warning">调查中</el-tag>
          <el-tag v-else-if="currentReport.status === 'CONFIRMED'" type="danger">已确认</el-tag>
          <el-tag v-else-if="currentReport.status === 'REJECTED'" type="success">已驳回</el-tag>
          <el-tag v-else type="info">已关闭</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="举报描述">
          {{ currentReport.description }}
        </el-descriptions-item>
        <el-descriptions-item label="联系方式" v-if="currentReport.contactInfo">
          {{ currentReport.contactInfo }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDate(currentReport.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="处理意见" v-if="currentReport.handlerComment">
          {{ currentReport.handlerComment }}
        </el-descriptions-item>
        <el-descriptions-item label="采取措施" v-if="currentReport.actionTaken">
          {{ currentReport.actionTaken }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间" v-if="currentReport.handledAt">
          {{ formatDate(currentReport.handledAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import {
  reportApi,
  normalizeReportPageResponse,
  type CopyrightReport,
  type ReportType
} from '@/api/report'
import { formatDate } from '@/utils/date'

// 数据
const loading = ref(false)
const reports = ref<CopyrightReport[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 提交举报对话框
const showReportDialog = ref(false)
const reportFormRef = ref<FormInstance>()
const submitting = ref(false)

type ReportFormState = {
  documentId: number
  reportType: ReportType
  description: string
  contactInfo: string
}

const createReportForm = (): ReportFormState => ({
  documentId: 0,
  reportType: 'COPYRIGHT_INFRINGEMENT',
  description: '',
  contactInfo: ''
})

const reportForm = ref<ReportFormState>(createReportForm())

const reportRules: FormRules = {
  documentId: [
    { required: true, message: '请输入文档ID', trigger: 'blur' },
    { type: 'number', min: 1, message: '文档ID必须大于0', trigger: 'blur' }
  ],
  reportType: [{ required: true, message: '请选择举报类型', trigger: 'change' }],
  description: [
    { required: true, message: '请输入举报描述', trigger: 'blur' },
    { min: 10, message: '举报描述至少10个字符', trigger: 'blur' }
  ]
}

// 举报详情对话框
const showDetailDialog = ref(false)
const currentReport = ref<CopyrightReport | null>(null)

// 加载举报列表
const loadReports = async () => {
  loading.value = true
  try {
    const res = await reportApi.getMyReports(currentPage.value - 1, pageSize.value)
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

// 提交举报
const submitReport = async () => {
  if (!reportFormRef.value) return

  await reportFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await reportApi.submitReport({
        documentId: reportForm.value.documentId,
        reportType: reportForm.value.reportType,
        description: reportForm.value.description,
        contactInfo: reportForm.value.contactInfo || undefined
      })
      ElMessage.success('举报提交成功，我们会尽快处理')
      showReportDialog.value = false

      // 重置表单
      reportForm.value = createReportForm()

      loadReports()
    } catch (error: any) {
      ElMessage.error(error.message || '提交失败')
    } finally {
      submitting.value = false
    }
  })
}

// 查看举报详情
const viewReportDetail = (report: CopyrightReport) => {
  currentReport.value = report
  showDetailDialog.value = true
}

// 查看文档
const viewDocument = (documentId: number) => {
  window.open(`/documents/${documentId}`, '_blank')
}

// 获取举报类型文本
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

// 获取举报类型标签颜色
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

// 初始化
onMounted(() => {
  loadReports()
})
</script>

<style scoped lang="scss">
.copyright-report-page {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h2 {
        margin: 0;
      }
    }
  }

  .content-card {
    .el-pagination {
      margin-top: 20px;
      justify-content: center;
    }
  }

  .el-alert {
    margin-top: 20px;
  }
}
</style>
