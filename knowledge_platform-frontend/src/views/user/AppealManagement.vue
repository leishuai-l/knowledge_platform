<template>
  <div class="appeal-management-page">
    <el-card class="header-card">
      <div class="header">
        <h2>我的申诉</h2>
        <el-button type="primary" @click="showAppealDialog = true">
          <el-icon><Plus /></el-icon>
          提交申诉
        </el-button>
      </div>
    </el-card>

    <!-- 申诉列表 -->
    <el-card class="content-card">
      <el-table :data="appeals" v-loading="loading" stripe>
        <el-table-column prop="documentId" label="文档ID" width="100" />
        <el-table-column label="申诉理由" min-width="200">
          <template #default="{ row }">
            <el-text line-clamp="2">{{ row.appealReason }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
            <el-tag v-else-if="row.status === 'APPROVED'" type="success">已批准</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已驳回</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewAppealDetail(row)">
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
        @size-change="loadAppeals"
        @current-change="loadAppeals"
      />
    </el-card>

    <!-- 提交申诉对话框 -->
    <el-dialog
      v-model="showAppealDialog"
      title="提交申诉"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="appealFormRef"
        :model="appealForm"
        :rules="appealRules"
        label-width="100px"
      >
        <el-form-item label="文档ID" prop="documentId">
          <el-input-number
            v-model="appealForm.documentId"
            :min="1"
            placeholder="请输入被拒绝的文档ID"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="审核类型" prop="reviewType">
          <el-radio-group v-model="appealForm.reviewType">
            <el-radio label="INITIAL">初审</el-radio>
            <el-radio label="FINAL">复审</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="申诉理由" prop="appealReason">
          <el-input
            v-model="appealForm.appealReason"
            type="textarea"
            :rows="5"
            placeholder="请详细说明您认为审核结果不合理的原因"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="证据材料" prop="evidence">
          <el-input
            v-model="appealForm.evidence"
            type="textarea"
            :rows="3"
            placeholder="请提供支持您申诉的证据（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAppealDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAppeal" :loading="submitting">
          提交申诉
        </el-button>
      </template>
    </el-dialog>

    <!-- 申诉详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="申诉详情"
      width="700px"
    >
      <el-descriptions v-if="currentAppeal" :column="1" border>
        <el-descriptions-item label="文档ID">
          {{ currentAppeal.documentId }}
        </el-descriptions-item>
        <el-descriptions-item label="审核类型">
          {{ currentAppeal.reviewId ? '复审' : '初审' }}
        </el-descriptions-item>
        <el-descriptions-item label="申诉状态">
          <el-tag v-if="currentAppeal.status === 'PENDING'" type="warning">待处理</el-tag>
          <el-tag v-else-if="currentAppeal.status === 'APPROVED'" type="success">已批准</el-tag>
          <el-tag v-else-if="currentAppeal.status === 'REJECTED'" type="danger">已驳回</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申诉理由">
          {{ currentAppeal.appealReason }}
        </el-descriptions-item>
        <el-descriptions-item label="证据材料" v-if="currentAppeal.evidence">
          {{ currentAppeal.evidence }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDate(currentAppeal.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="处理意见" v-if="currentAppeal.handlerComment">
          {{ currentAppeal.handlerComment }}
        </el-descriptions-item>
        <el-descriptions-item label="最终决定" v-if="currentAppeal.finalDecision">
          {{ currentAppeal.finalDecision }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间" v-if="currentAppeal.handledAt">
          {{ formatDate(currentAppeal.handledAt) }}
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
import { Plus } from '@element-plus/icons-vue'
import {
  getMyAppeals,
  submitAppeal as submitAppealApi,
  normalizePageResponse,
  type DocumentAppeal,
  type AppealRequest
} from '@/api/review'
import { formatDate } from '@/utils/date'

// 数据
const loading = ref(false)
const appeals = ref<DocumentAppeal[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 提交申诉对话框
const showAppealDialog = ref(false)
const appealFormRef = ref<FormInstance>()
const submitting = ref(false)

const appealForm = ref<AppealRequest>({
  documentId: 0,
  reviewType: 'FINAL',
  appealReason: '',
  evidence: ''
})

const appealRules: FormRules = {
  documentId: [
    { required: true, message: '请输入文档ID', trigger: 'blur' },
    { type: 'number', min: 1, message: '文档ID必须大于0', trigger: 'blur' }
  ],
  reviewType: [{ required: true, message: '请选择审核类型', trigger: 'change' }],
  appealReason: [
    { required: true, message: '请输入申诉理由', trigger: 'blur' },
    { min: 10, message: '申诉理由至少10个字符', trigger: 'blur' }
  ]
}

// 申诉详情对话框
const showDetailDialog = ref(false)
const currentAppeal = ref<DocumentAppeal | null>(null)

// 加载申诉列表
const loadAppeals = async () => {
  loading.value = true
  try {
    const res = await getMyAppeals(currentPage.value - 1, pageSize.value)
    const pageData = normalizePageResponse<DocumentAppeal>(res.data)
    appeals.value = pageData.list
    currentPage.value = Number(pageData.page ?? currentPage.value) || 1
    pageSize.value = Number(pageData.size ?? pageSize.value) || pageSize.value
    total.value = Number(pageData.total ?? 0)
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 提交申诉
const submitAppeal = async () => {
  if (!appealFormRef.value) return

  await appealFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await submitAppealApi(appealForm.value)
      ElMessage.success('申诉提交成功，请等待管理员处理')
      showAppealDialog.value = false

      // 重置表单
      appealForm.value = {
        documentId: 0,
        reviewType: 'FINAL',
        appealReason: '',
        evidence: ''
      }

      loadAppeals()
    } catch (error: any) {
      ElMessage.error(error.message || '提交失败')
    } finally {
      submitting.value = false
    }
  })
}

// 查看申诉详情
const viewAppealDetail = (appeal: DocumentAppeal) => {
  currentAppeal.value = appeal
  showDetailDialog.value = true
}

// 初始化
onMounted(() => {
  loadAppeals()
})
</script>

<style scoped lang="scss">
.appeal-management-page {
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
}
</style>
