<template>
  <div class="admin-review-page">
    <el-card class="header-card">
      <div class="header">
        <h2>文档审核管理</h2>
        <el-button type="primary" @click="loadReviews">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 统计信息 -->
      <div class="statistics" v-if="statistics">
        <el-statistic title="待初审" :value="statistics.pendingInitialReviews" />
        <el-statistic title="待复审" :value="statistics.pendingFinalReviews" />
      </div>
    </el-card>

    <!-- 待审核列表 -->
    <el-card class="content-card">
      <el-table :data="reviews" v-loading="loading" stripe>
        <el-table-column prop="documentId" label="文档ID" width="80" />
        <el-table-column label="文档标题" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDocument(row.documentId)">
              {{ getDocumentTitle(row) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="初审结果" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.formatCheckPassed" type="success" size="small">格式通过</el-tag>
            <el-tag v-else type="danger" size="small">格式未通过</el-tag>
            <br />
            <el-tag v-if="row.contentCompliancePassed" type="success" size="small">内容合规</el-tag>
            <el-tag v-else type="danger" size="small">内容不合规</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="相似度" width="100">
          <template #default="{ row }">
            <span v-if="row.similarityScore">
              {{ row.similarityScore.toFixed(1) }}%
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openReviewDialog(row)">
              复审
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadReviews"
        @current-change="loadReviews"
      />
    </el-card>

    <!-- 复审对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="文档复审"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="reviewFormRef"
        :model="reviewForm"
        :rules="reviewRules"
        label-width="120px"
      >
        <el-form-item label="学术性评分" prop="academicScore">
          <el-rate
            v-model="reviewForm.academicScore"
            :max="5"
            show-score
            text-color="#ff9900"
          />
          <span class="score-hint">（1-5分，3分及以上为通过）</span>
        </el-form-item>

        <el-form-item label="原创性评分" prop="originalityScore">
          <el-rate
            v-model="reviewForm.originalityScore"
            :max="5"
            show-score
            text-color="#ff9900"
          />
          <span class="score-hint">（1-5分，3分及以上为通过）</span>
        </el-form-item>

        <el-form-item label="实用性评分" prop="practicalityScore">
          <el-rate
            v-model="reviewForm.practicalityScore"
            :max="5"
            show-score
            text-color="#ff9900"
          />
          <span class="score-hint">（1-5分，3分及以上为通过）</span>
        </el-form-item>

        <el-form-item label="版权合规性" prop="copyrightCompliance">
          <el-radio-group v-model="reviewForm.copyrightCompliance">
            <el-radio :label="true">合规</el-radio>
            <el-radio :label="false">不合规</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="审核意见" prop="comment">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见"
          />
        </el-form-item>

        <el-form-item label="拒绝原因" v-if="!willPass">
          <el-input
            v-model="reviewForm.rejectionReason"
            type="textarea"
            :rows="3"
            placeholder="如果不通过，请说明原因"
          />
        </el-form-item>

        <el-form-item label="修改建议" v-if="!willPass">
          <el-input
            v-model="reviewForm.suggestions"
            type="textarea"
            :rows="3"
            placeholder="给用户的修改建议"
          />
        </el-form-item>

        <el-alert
          v-if="willPass"
          title="该文档将通过审核"
          type="success"
          :closable="false"
          show-icon
        />
        <el-alert
          v-else
          title="该文档将被拒绝"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="submitting">
          提交审核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getPendingReviews,
  performFinalReview,
  getReviewStatistics,
  normalizePageResponse,
  type DocumentReview,
  type FinalReviewRequest,
  type ReviewStatistics
} from '@/api/review'
import { formatDate } from '@/utils/date'
import { documentApi } from '@/api/document'

// 数据
const loading = ref(false)
const reviews = ref<DocumentReview[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statistics = ref<ReviewStatistics | null>(null)
const documentTitleCache = ref<Map<number, string>>(new Map())

// 复审对话框
const reviewDialogVisible = ref(false)
const reviewFormRef = ref<FormInstance>()
const submitting = ref(false)
const currentReview = ref<DocumentReview | null>(null)

const reviewForm = ref<FinalReviewRequest>({
  academicScore: 3,
  originalityScore: 3,
  practicalityScore: 3,
  copyrightCompliance: true,
  comment: '',
  rejectionReason: '',
  suggestions: ''
})

const reviewRules: FormRules = {
  academicScore: [{ required: true, message: '请评分', trigger: 'change' }],
  originalityScore: [{ required: true, message: '请评分', trigger: 'change' }],
  practicalityScore: [{ required: true, message: '请评分', trigger: 'change' }],
  copyrightCompliance: [{ required: true, message: '请选择', trigger: 'change' }],
  comment: [{ required: true, message: '请输入审核意见', trigger: 'blur' }]
}

// 计算是否通过
const willPass = computed(() => {
  return (
    reviewForm.value.academicScore >= 3 &&
    reviewForm.value.originalityScore >= 3 &&
    reviewForm.value.practicalityScore >= 3 &&
    reviewForm.value.copyrightCompliance
  )
})

// 加载待审核列表
const loadReviews = async () => {
  loading.value = true
  try {
    const res = await getPendingReviews(currentPage.value - 1, pageSize.value)
    const pageData = normalizePageResponse<DocumentReview>(res.data)
    reviews.value = pageData.list
    currentPage.value = Number(pageData.page ?? currentPage.value) || 1
    pageSize.value = Number(pageData.size ?? pageSize.value) || pageSize.value
    total.value = Number(pageData.total ?? 0)
    // 预加载文档标题
    preloadDocumentTitles(pageData.list)
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    const res = await getReviewStatistics()
    statistics.value = res.data
  } catch (error: any) {
    console.error('加载统计失败:', error)
  }
}

// 打开复审对话框
const openReviewDialog = (review: DocumentReview) => {
  currentReview.value = review
  reviewForm.value = {
    academicScore: 3,
    originalityScore: 3,
    practicalityScore: 3,
    copyrightCompliance: true,
    comment: '',
    rejectionReason: '',
    suggestions: ''
  }
  reviewDialogVisible.value = true
}

// 提交复审
const submitReview = async () => {
  if (!reviewFormRef.value) return

  await reviewFormRef.value.validate(async (valid) => {
    if (!valid) return

    const action = willPass.value ? '通过' : '拒绝'
    try {
      await ElMessageBox.confirm(
        `确定要${action}该文档吗？`,
        '确认操作',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      submitting.value = true
      await performFinalReview(currentReview.value!.documentId, reviewForm.value)

      ElMessage.success('审核成功')
      reviewDialogVisible.value = false
      loadReviews()
      loadStatistics()
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.message || '审核失败')
      }
    } finally {
      submitting.value = false
    }
  })
}

// 查看文档
const viewDocument = (documentId: number) => {
  window.open(`/documents/${documentId}`, '_blank')
}

// 获取文档标题（从缓存获取，页面加载时会预加载）
const getDocumentTitle = (review: DocumentReview) => {
  const docId = review.documentId
  if (documentTitleCache.value.has(docId)) {
    return documentTitleCache.value.get(docId) || `文档 #${docId}`
  }
  return `文档 #${docId}`
}

// 预加载文档标题
const preloadDocumentTitles = async (reviewList: DocumentReview[]) => {
  const docIds = reviewList.map(r => r.documentId).filter(id => !documentTitleCache.value.has(id))
  if (docIds.length === 0) return
  try {
    await Promise.all(docIds.map(async (docId) => {
      try {
        const res = await documentApi.getDocument(docId)
        documentTitleCache.value.set(docId, res.data?.title || `文档 #${docId}`)
      } catch {
        documentTitleCache.value.set(docId, `文档 #${docId}`)
      }
    }))
  } catch (e) {
    console.error('预加载文档标题失败', e)
  }
}

// 初始化
onMounted(() => {
  loadReviews()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.admin-review-page {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;

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
      display: flex;
      gap: 40px;
    }
  }

  .content-card {
    .el-pagination {
      margin-top: 20px;
      justify-content: center;
    }
  }

  .score-hint {
    margin-left: 10px;
    font-size: 12px;
    color: #909399;
  }

  .el-alert {
    margin-top: 20px;
  }
}
</style>
