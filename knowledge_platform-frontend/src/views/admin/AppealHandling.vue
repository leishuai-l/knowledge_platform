<template>
  <div class="admin-appeal-page">
    <el-card class="header-card">
      <div class="header">
        <h2>申诉处理</h2>
        <el-button type="primary" @click="loadAppeals">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 统计信息 -->
      <div class="statistics" v-if="statistics">
        <el-statistic title="待处理" :value="statistics.pending" />
        <el-statistic title="已批准" :value="statistics.approved" />
        <el-statistic title="已驳回" :value="statistics.rejected" />
      </div>
    </el-card>

    <!-- 待处理申诉列表 -->
    <el-card class="content-card">
      <el-table :data="appeals" v-loading="loading" stripe>
        <el-table-column prop="id" label="申诉ID" width="80" />
        <el-table-column prop="documentId" label="文档ID" width="100" />
        <el-table-column label="申诉人" width="120">
          <template #default="{ row }">
            用户 #{{ row.userId }}
          </template>
        </el-table-column>
        <el-table-column label="申诉理由" min-width="200">
          <template #default="{ row }">
            <el-text line-clamp="2">{{ row.appealReason }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              size="small"
              @click="openHandleDialog(row)"
            >
              处理
            </el-button>
            <el-button
              type="info"
              size="small"
              link
              @click="viewAppealDetail(row)"
            >
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
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadAppeals"
        @current-change="loadAppeals"
      />
    </el-card>

    <!-- 处理申诉对话框 -->
    <el-dialog
      v-model="handleDialogVisible"
      title="处理申诉"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-descriptions v-if="currentAppeal" :column="1" border class="appeal-info">
        <el-descriptions-item label="文档ID">
          <el-link type="primary" @click="viewDocument(currentAppeal.documentId)">
            {{ currentAppeal.documentId }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="申诉人">
          用户 #{{ currentAppeal.userId }}
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
      </el-descriptions>

      <el-divider />

      <el-form
        ref="handleFormRef"
        :model="handleForm"
        :rules="handleRules"
        label-width="100px"
      >
        <el-form-item label="处理结果" prop="approved">
          <el-radio-group v-model="handleForm.approved">
            <el-radio :label="true">批准申诉</el-radio>
            <el-radio :label="false">驳回申诉</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理意见" prop="comment">
          <el-input
            v-model="handleForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入处理意见"
          />
        </el-form-item>

        <el-form-item label="最终决定" prop="decision">
          <el-input
            v-model="handleForm.decision"
            type="textarea"
            :rows="3"
            placeholder="请说明最终决定"
          />
        </el-form-item>

        <el-alert
          v-if="handleForm.approved"
          title="批准申诉后，文档将重新进入审核流程"
          type="success"
          :closable="false"
          show-icon
        />
        <el-alert
          v-else
          title="驳回申诉后，维持原审核结果"
          type="warning"
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

    <!-- 申诉详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="申诉详情"
      width="700px"
    >
      <el-descriptions v-if="currentAppeal" :column="1" border>
        <el-descriptions-item label="申诉ID">
          {{ currentAppeal.id }}
        </el-descriptions-item>
        <el-descriptions-item label="文档ID">
          <el-link type="primary" @click="viewDocument(currentAppeal.documentId)">
            {{ currentAppeal.documentId }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="申诉人">
          用户 #{{ currentAppeal.userId }}
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
        <el-descriptions-item label="处理人" v-if="currentAppeal.handlerId">
          管理员 #{{ currentAppeal.handlerId }}
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
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getPendingAppeals,
  handleAppeal,
  getAppealStatistics,
  normalizePageResponse,
  type DocumentAppeal,
  type AppealHandleRequest,
  type AppealStatistics
} from '@/api/review'
import { formatDate } from '@/utils/date'

// 数据
const loading = ref(false)
const appeals = ref<DocumentAppeal[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statistics = ref<AppealStatistics | null>(null)

// 处理对话框
const handleDialogVisible = ref(false)
const handleFormRef = ref<FormInstance>()
const submitting = ref(false)
const currentAppeal = ref<DocumentAppeal | null>(null)

const handleForm = ref<AppealHandleRequest>({
  approved: true,
  comment: '',
  decision: ''
})

const handleRules: FormRules = {
  approved: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
  comment: [{ required: true, message: '请输入处理意见', trigger: 'blur' }],
  decision: [{ required: true, message: '请说明最终决定', trigger: 'blur' }]
}

// 详情对话框
const detailDialogVisible = ref(false)

// 加载申诉列表
const loadAppeals = async () => {
  loading.value = true
  try {
    const res = await getPendingAppeals(currentPage.value - 1, pageSize.value)
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

// 加载统计信息
const loadStatistics = async () => {
  try {
    const res = await getAppealStatistics()
    statistics.value = (res.data as any)?.data ?? res.data ?? null
  } catch (error: any) {
    console.error('加载统计失败:', error)
  }
}

// 打开处理对话框
const openHandleDialog = (appeal: DocumentAppeal) => {
  currentAppeal.value = appeal
  handleForm.value = {
    approved: true,
    comment: '',
    decision: ''
  }
  handleDialogVisible.value = true
}

// 提交处理
const submitHandle = async () => {
  if (!handleFormRef.value || !currentAppeal.value) return

  await handleFormRef.value.validate(async (valid) => {
    if (!valid) return

    const action = handleForm.value.approved ? '批准' : '驳回'
    try {
      await ElMessageBox.confirm(
        `确定要${action}该申诉吗？`,
        '确认操作',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      submitting.value = true
      await handleAppeal(currentAppeal.value!.id, handleForm.value)

      ElMessage.success('处理成功')
      handleDialogVisible.value = false
      loadAppeals()
      loadStatistics()
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.message || '处理失败')
      }
    } finally {
      submitting.value = false
    }
  })
}

// 查看申诉详情
const viewAppealDetail = (appeal: DocumentAppeal) => {
  currentAppeal.value = appeal
  detailDialogVisible.value = true
}

// 查看文档
const viewDocument = (documentId: number) => {
  window.open(`/documents/${documentId}`, '_blank')
}

// 初始化
onMounted(() => {
  loadAppeals()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.admin-appeal-page {
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

  .appeal-info {
    margin-bottom: 20px;
  }

  .el-alert {
    margin-top: 20px;
  }
}
</style>
