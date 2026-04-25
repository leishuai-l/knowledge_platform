<template>
  <div class="admin-notifications">
    <div class="notifications-header">
      <h1>通知管理</h1>
      <p>管理系统通知和公告</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon total">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ statistics.totalCount || 0 }}</div>
              <div class="stats-label">通知总数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon unread">
              <el-icon><BellFilled /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ statistics.unreadCount || 0 }}</div>
              <div class="stats-label">未读通知</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon read">
              <el-icon><CircleCheckFilled /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ statistics.readCount || 0 }}</div>
              <div class="stats-label">已读通知</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon recent">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-number">{{ statistics.recentWeekCount || 0 }}</div>
              <div class="stats-label">近7天通知</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作区域 -->
    <div class="action-section">
      <div class="action-header">
        <h2>通知操作</h2>
        <div class="action-buttons">
          <el-button
            type="primary"
            @click="showAnnouncementDialog = true"
            :icon="Notification"
          >
            发布系统公告
          </el-button>
          <el-button
            type="success"
            @click="showTargetedDialog = true"
            :icon="Message"
          >
            发送定向通知
          </el-button>
        </div>
      </div>
    </div>

    <!-- 通知列表 -->
    <el-card class="notification-list-card">
      <template #header>
        <div class="list-header">
          <span class="title">通知列表</span>
          <div class="list-controls">
            <el-select
              v-model="filterType"
              placeholder="选择通知类型"
              clearable
              style="width: 200px; margin-right: 16px"
              @change="handleFilterChange"
            >
              <el-option label="系统公告" value="SYSTEM_ANNOUNCEMENT" />
              <el-option label="文档审核通过" value="DOCUMENT_APPROVED" />
              <el-option label="文档审核拒绝" value="DOCUMENT_REJECTED" />
              <el-option label="文档评论" value="DOCUMENT_COMMENTED" />
              <el-option label="文档评分" value="DOCUMENT_RATED" />
              <el-option label="积分获得" value="POINTS_EARNED" />
              <el-option label="积分消费" value="POINTS_SPENT" />
            </el-select>

            <el-select
              v-model="filterRead"
              placeholder="选择阅读状态"
              clearable
              style="width: 150px; margin-right: 16px"
              @change="handleFilterChange"
            >
              <el-option label="未读" :value="false" />
              <el-option label="已读" :value="true" />
            </el-select>

            <el-button @click="refreshList" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="notifications"
        v-loading="loading"
        stripe
        style="width: 100%"
        :default-sort="{ prop: 'createdAt', order: 'descending' }"
      >
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="内容" prop="content" min-width="300" show-overflow-tooltip />
        <el-table-column label="类型" prop="type" width="120">
          <template #default="{ row }">
            <el-tag :type="getNotificationTagType(row.type)" effect="dark">
              {{ getNotificationTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户ID" prop="userId" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'success' : 'danger'" effect="dark">
              {{ row.isRead ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="180" sortable>
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              text
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="totalPages > 1"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="totalNotifications"
        layout="total, prev, pager, next, jumper"
        @current-change="handlePageChange"
        style="margin-top: 20px; text-align: center"
      />
    </el-card>

    <!-- 系统公告对话框 -->
    <el-dialog
      v-model="showAnnouncementDialog"
      title="发布系统公告"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="announcementFormRef"
        :model="announcementForm"
        :rules="announcementRules"
        label-width="80px"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="announcementForm.title"
            placeholder="请输入公告标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            placeholder="请输入公告内容"
            :rows="6"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAnnouncementDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleSendAnnouncement"
          :loading="submitting"
        >
          发布公告
        </el-button>
      </template>
    </el-dialog>

    <!-- 定向通知对话框 -->
    <el-dialog
      v-model="showTargetedDialog"
      title="发送定向通知"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="targetedFormRef"
        :model="targetedForm"
        :rules="targetedRules"
        label-width="100px"
      >
        <el-form-item label="通知标题" prop="title">
          <el-input
            v-model="targetedForm.title"
            placeholder="请输入通知标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <el-input
            v-model="targetedForm.content"
            type="textarea"
            placeholder="请输入通知内容"
            :rows="4"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="targetedForm.type" placeholder="请选择通知类型">
            <el-option label="系统公告" value="SYSTEM_ANNOUNCEMENT" />
            <el-option label="文档审核通过" value="DOCUMENT_APPROVED" />
            <el-option label="文档审核拒绝" value="DOCUMENT_REJECTED" />
            <el-option label="积分获得" value="POINTS_EARNED" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标用户" prop="userIds">
          <el-input
            v-model="targetedForm.userIdsText"
            placeholder="请输入用户ID，多个用逗号分隔（如：1,2,3）"
            @blur="parseUserIds"
          />
          <div class="form-help-text">
            输入格式：用户ID用逗号分隔，例如：1,2,3,4
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTargetedDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleSendTargeted"
          :loading="submitting"
        >
          发送通知
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import {
  Bell,
  BellFilled,
  CircleCheckFilled,
  Clock,
  Notification,
  Message,
  Refresh
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as notificationApi from '@/api/notification'
import type { Notification as NotificationType } from '@/types'

// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const statistics = ref<any>({})
const notifications = ref<NotificationType[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const totalNotifications = ref(0)
const totalPages = ref(0)
const filterType = ref('')
const filterRead = ref<boolean | null>(null)

// 对话框状态
const showAnnouncementDialog = ref(false)
const showTargetedDialog = ref(false)

// 表单引用
const announcementFormRef = ref<FormInstance>()
const targetedFormRef = ref<FormInstance>()

// 系统公告表单
const announcementForm = reactive({
  title: '',
  content: ''
})

// 定向通知表单
const targetedForm = reactive({
  title: '',
  content: '',
  type: 'SYSTEM_ANNOUNCEMENT',
  userIds: [] as number[],
  userIdsText: ''
})

// 表单验证规则
const announcementRules: FormRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度应在 1 到 100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { min: 1, max: 1000, message: '内容长度应在 1 到 1000 个字符', trigger: 'blur' }
  ]
}

const targetedRules: FormRules = {
  title: [
    { required: true, message: '请输入通知标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度应在 1 到 100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入通知内容', trigger: 'blur' },
    { min: 1, max: 1000, message: '内容长度应在 1 到 1000 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择通知类型', trigger: 'change' }
  ],
  userIds: [
    {
      validator: (rule, value, callback) => {
        if (!targetedForm.userIds || targetedForm.userIds.length === 0) {
          callback(new Error('请输入目标用户ID'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 获取统计信息
const fetchStatistics = async () => {
  try {
    const response = await notificationApi.getNotificationStatistics()
    statistics.value = response.data.data
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

// 获取通知列表
const fetchNotifications = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      type: filterType.value || undefined,
      isRead: filterRead.value === null ? undefined : filterRead.value
    }

    const response = await notificationApi.getAllNotifications(params)
    const pageData = response.data.data as any

    if (pageData) {
      notifications.value = pageData.list || pageData.content || []
      totalNotifications.value = Number(pageData.total ?? pageData.totalElements ?? 0)
      totalPages.value = Number(pageData.pages ?? pageData.totalPages ?? 0)
      const rawPage = Number(pageData.page ?? pageData.number ?? 0)
      const normalizedPage = pageData.page == null ? rawPage + 1 : rawPage
      currentPage.value = Number.isFinite(normalizedPage) && normalizedPage > 0
        ? normalizedPage
        : currentPage.value
      pageSize.value = Number(pageData.size ?? pageSize.value) || pageSize.value
    } else {
      notifications.value = []
      totalNotifications.value = 0
      totalPages.value = 0
    }
  } catch (error) {
    console.error('获取通知列表失败:', error)
    ElMessage.error('获取通知列表失败')
    // 确保在错误情况下也有默认值
    notifications.value = []
    totalNotifications.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

// 刷新列表
const refreshList = () => {
  fetchNotifications()
  fetchStatistics()
}

// 筛选器变化处理
const handleFilterChange = () => {
  currentPage.value = 1
  fetchNotifications()
}

// 分页变化处理
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchNotifications()
}

// 解析用户ID
const parseUserIds = () => {
  if (!targetedForm.userIdsText.trim()) {
    targetedForm.userIds = []
    return
  }

  try {
    const ids = targetedForm.userIdsText
      .split(',')
      .map(id => parseInt(id.trim()))
      .filter(id => !isNaN(id) && id > 0)

    targetedForm.userIds = ids
  } catch (error) {
    ElMessage.error('用户ID格式错误')
    targetedForm.userIds = []
  }
}

// 发送系统公告
const handleSendAnnouncement = async () => {
  if (!announcementFormRef.value) return

  try {
    await announcementFormRef.value.validate()
    submitting.value = true

    await notificationApi.sendSystemAnnouncement({
      title: announcementForm.title.trim(),
      content: announcementForm.content.trim()
    })

    ElMessage.success('系统公告发布成功！')
    showAnnouncementDialog.value = false

    // 重置表单
    announcementForm.title = ''
    announcementForm.content = ''

    // 刷新数据
    refreshList()
  } catch (error) {
    console.error('发布系统公告失败:', error)
  } finally {
    submitting.value = false
  }
}

// 发送定向通知
const handleSendTargeted = async () => {
  if (!targetedFormRef.value) return

  try {
    // 先解析用户ID
    parseUserIds()

    await targetedFormRef.value.validate()
    submitting.value = true

    await notificationApi.sendTargetedNotification({
      title: targetedForm.title.trim(),
      content: targetedForm.content.trim(),
      type: targetedForm.type,
      userIds: targetedForm.userIds
    })

    ElMessage.success(`定向通知发送成功！已发送给 ${targetedForm.userIds.length} 个用户`)
    showTargetedDialog.value = false

    // 重置表单
    targetedForm.title = ''
    targetedForm.content = ''
    targetedForm.type = 'SYSTEM_ANNOUNCEMENT'
    targetedForm.userIds = []
    targetedForm.userIdsText = ''

    // 刷新数据
    refreshList()
  } catch (error) {
    console.error('发送定向通知失败:', error)
  } finally {
    submitting.value = false
  }
}

// 删除通知
const handleDelete = async (notification: NotificationType) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除通知"${notification.title}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await notificationApi.deleteNotification(notification.id)
    ElMessage.success('通知删除成功')
    refreshList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除通知失败:', error)
      ElMessage.error('删除通知失败')
    }
  }
}

// 获取通知类型标签样式
const getNotificationTagType = (type: string) => {
  switch (type) {
    case 'DOCUMENT_APPROVED': return 'success'
    case 'DOCUMENT_REJECTED': return 'danger'
    case 'DOCUMENT_COMMENTED': return 'primary'
    case 'DOCUMENT_RATED': return 'warning'
    case 'POINTS_EARNED': return 'success'
    case 'POINTS_SPENT': return 'danger'
    case 'SYSTEM_ANNOUNCEMENT': return 'info'
    default: return 'primary'
  }
}

// 获取通知类型文本
const getNotificationTypeText = (type: string) => {
  switch (type) {
    case 'DOCUMENT_APPROVED': return '审核通过'
    case 'DOCUMENT_REJECTED': return '审核拒绝'
    case 'DOCUMENT_COMMENTED': return '新评论'
    case 'DOCUMENT_RATED': return '新评分'
    case 'POINTS_EARNED': return '积分获得'
    case 'POINTS_SPENT': return '积分消费'
    case 'SYSTEM_ANNOUNCEMENT': return '系统公告'
    default: return '通知'
  }
}

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 组件挂载时获取数据
onMounted(() => {
  refreshList()
})
</script>

<style scoped>
.admin-notifications {
  padding: 0;
}

.notifications-header {
  margin-bottom: 30px;
}

.notifications-header h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #303133;
}

.notifications-header p {
  margin: 0;
  color: #909399;
  font-size: 16px;
}

.stats-row {
  margin-bottom: 30px;
}

.stats-card {
  transition: all 0.3s;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stats-content {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 28px;
}

.stats-icon.total {
  background: linear-gradient(135deg, #409EFF, #66B3FF);
  color: white;
}

.stats-icon.unread {
  background: linear-gradient(135deg, #E6A23C, #F0C678);
  color: white;
}

.stats-icon.read {
  background: linear-gradient(135deg, #67C23A, #85CE61);
  color: white;
}

.stats-icon.recent {
  background: linear-gradient(135deg, #909399, #B3B7BB);
  color: white;
}

.stats-info {
  flex: 1;
}

.stats-number {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stats-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.action-section {
  margin-bottom: 30px;
}

.action-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.action-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.notification-list-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header .title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.list-controls {
  display: flex;
  align-items: center;
}

.form-help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .stats-row .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .action-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .list-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .list-controls {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>