<template>
  <div class="notifications-container">
    <div class="notifications-header">
      <div class="header-content">
        <div class="header-left">
          <el-button
            type="primary"
            plain
            :icon="ArrowLeft"
            @click="goBack"
            class="back-button"
          >
            返回
          </el-button>
          <h2>通知中心</h2>
        </div>
        <div class="header-stats">
          <el-tag type="info" effect="dark">
            总计 {{ totalNotifications }} 条
          </el-tag>
          <el-tag v-if="unreadCount > 0" type="warning" effect="dark">
            全部未读 {{ unreadCount }} 条
          </el-tag>
          <el-tag v-if="unreadCount === 0" type="success" effect="dark">
            全部已读
          </el-tag>
        </div>
      </div>

      <div class="header-actions">
        <el-button-group>
          <el-button
            :type="filterType === 'all' ? 'primary' : 'default'"
            @click="setFilter('all')"
          >
            全部
          </el-button>
          <el-button
            :type="filterType === 'unread' ? 'primary' : 'default'"
            @click="setFilter('unread')"
          >
            未读 ({{ unreadCount }})
          </el-button>
          <el-button
            :type="filterType === 'read' ? 'primary' : 'default'"
            @click="setFilter('read')"
          >
            已读
          </el-button>
        </el-button-group>

        <el-button-group>
          <el-button
            @click="fetchNotifications"
            :loading="loading"
            :icon="Refresh"
          >
            刷新
          </el-button>
          <el-button
            v-if="unreadCount > 0"
            @click="markAllAsRead"
            :loading="markingAllRead"
          >
            全部标记为已读
          </el-button>
          <el-button
            v-if="selectedNotifications.length > 0"
            @click="batchMarkAsRead"
            :loading="batchMarking"
          >
            批量标记已读 ({{ selectedNotifications.length }})
          </el-button>
          <el-button
            v-if="selectedNotifications.length > 0"
            type="danger"
            @click="batchDelete"
            :loading="batchDeleting"
          >
            批量删除 ({{ selectedNotifications.length }})
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div class="notifications-content">
      <div class="notifications-toolbar">
        <el-checkbox
          v-model="selectAll"
          :indeterminate="isIndeterminate"
          @change="handleSelectAllChange"
        >
          全选
        </el-checkbox>

        <div class="toolbar-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索通知..."
            prefix-icon="Search"
            clearable
            style="width: 250px"
            @input="handleSearch"
          />
        </div>
      </div>

      <div class="notifications-list" v-loading="loading">
        <div
          v-if="filteredNotifications.length === 0 && !loading"
          class="empty-state"
        >
          <el-icon size="64" color="#C0C4CC">
            <BellFilled />
          </el-icon>
          <h3>暂无通知</h3>
          <p>{{ getEmptyMessage() }}</p>
        </div>

        <div
          v-for="notification in filteredNotifications"
          :key="notification.id"
          class="notification-card"
          :class="{
            'unread': !notification.isRead,
            'selected': selectedNotifications.includes(notification.id)
          }"
        >
          <div class="notification-checkbox">
            <el-checkbox
              :model-value="selectedNotifications.includes(notification.id)"
              @change="handleNotificationSelect(notification.id, $event)"
            />
          </div>

          <div class="notification-icon">
            <el-icon
              size="24"
              :color="getNotificationIconColor(notification.type)"
            >
              <component :is="getNotificationIcon(notification.type)" />
            </el-icon>
          </div>

          <div class="notification-content" @click="handleNotificationClick(notification)">
            <div class="notification-header">
              <h4 class="notification-title">{{ notification.title }}</h4>
              <div class="notification-meta">
                <el-tag
                  :type="getNotificationTagType(notification.type)"
                  size="small"
                  effect="dark"
                >
                  {{ getNotificationTypeText(notification.type) }}
                </el-tag>
                <span class="notification-time">
                  {{ formatTime(notification.createdAt) }}
                </span>
              </div>
            </div>

            <p class="notification-text">{{ notification.content }}</p>

            <div class="notification-status">
              <el-tag
                v-if="!notification.isRead"
                type="danger"
                size="small"
                effect="dark"
              >
                未读
              </el-tag>
              <el-tag
                v-else
                type="success"
                size="small"
                effect="dark"
              >
                已读
              </el-tag>
              <span
                v-if="notification.isRead && notification.readAt"
                class="read-time"
              >
                {{ formatReadTime(notification.readAt) }}
              </span>
            </div>
          </div>

          <div class="notification-actions">
            <el-button
              v-if="!notification.isRead"
              text
              @click="markAsRead(notification.id)"
            >
              标记已读
            </el-button>
            <el-button
              text
              type="danger"
              @click="deleteNotification(notification.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="totalPages > 1" class="notifications-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalNotifications"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 通知详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="currentNotification?.title || '通知详情'"
      width="500px"
      class="notification-dialog"
    >
      <div v-if="currentNotification" class="notification-detail">
        <div class="detail-meta">
          <el-tag :type="getNotificationTagType(currentNotification.type)" size="small" effect="dark">
            {{ getNotificationTypeText(currentNotification.type) }}
          </el-tag>
          <span class="detail-time">{{ formatTime(currentNotification.createdAt) }}</span>
        </div>
        <div class="detail-content">
          <p>{{ currentNotification.content }}</p>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
          <el-button 
            v-if="currentNotification?.referenceId && ['DOCUMENT_APPROVED', 'DOCUMENT_COMMENTED', 'DOCUMENT_RATED'].includes(currentNotification.type)" 
            type="primary" 
            :loading="documentCheckLoading"
            @click="goToDocument(currentNotification.referenceId)"
          >
            查看相关文档
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  BellFilled,
  Document,
  CircleCheckFilled,
  CircleCloseFilled,
  ChatDotRound,
  Star,
  Coin,
  Refresh,
  Notification as NotificationIcon,
  ArrowLeft
} from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import type { Notification } from '@/types'

const router = useRouter()
const route = useRoute()
const notificationStore = useNotificationStore()

// 响应式状态
const loading = computed(() => notificationStore.loading)
const notifications = computed(() => notificationStore.notifications)
const filterType = ref<'all' | 'unread' | 'read'>('all')
const searchKeyword = ref('')
const selectedNotifications = ref<number[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalNotifications = ref(0)
const totalPages = ref(0)

// 详情对话框状态
const dialogVisible = ref(false)
const currentNotification = ref<Notification | null>(null)
const documentCheckLoading = ref(false)

// 操作状态
const markingAllRead = ref(false)
const batchMarking = ref(false)
const batchDeleting = ref(false)

// 计算属性
const unreadCount = computed(() => notificationStore.unreadCount)

// 当前页面的未读通知数量（用于页面显示）
const currentPageUnreadCount = computed(() => notifications.value.filter(n => !n.isRead).length)

const filteredNotifications = computed(() => {
  let filtered = notifications.value || []

  // 按状态筛选
  if (filterType.value === 'unread') {
    filtered = filtered.filter(n => !n.isRead)
  } else if (filterType.value === 'read') {
    filtered = filtered.filter(n => n.isRead)
  }

  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(n =>
      n.title.toLowerCase().includes(keyword) ||
      n.content.toLowerCase().includes(keyword)
    )
  }

  return filtered
})

const selectAll = computed({
  get: () => {
    return filteredNotifications.value.length > 0 &&
           selectedNotifications.value.length === filteredNotifications.value.length
  },
  set: (value: boolean) => {
    if (value) {
      selectedNotifications.value = filteredNotifications.value.map(n => n.id)
    } else {
      selectedNotifications.value = []
    }
  }
})

const isIndeterminate = computed(() => {
  return selectedNotifications.value.length > 0 &&
         selectedNotifications.value.length < filteredNotifications.value.length
})

// 获取通知列表
const fetchNotifications = async () => {
  try {
    let isRead: boolean | undefined
    if (filterType.value === 'read') {
      isRead = true
    } else if (filterType.value === 'unread') {
      isRead = false
    }

    const response = await notificationStore.fetchNotifications(
      currentPage.value - 1,
      pageSize.value,
      isRead
    )

    totalNotifications.value = response.total || 0
    totalPages.value = response.pages || 0
  } catch (error) {
    console.error('获取通知列表失败:', error)
    totalNotifications.value = 0
    totalPages.value = 0
  }
}

// 设置筛选条件
const setFilter = (type: 'all' | 'unread' | 'read') => {
  filterType.value = type
  currentPage.value = 1
  selectedNotifications.value = []
  fetchNotifications()
}

// 处理搜索
const handleSearch = () => {
  // 搜索是在前端进行的，所以不需要重新请求数据
  selectedNotifications.value = []
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  selectedNotifications.value = []
  fetchNotifications()
}

// 处理全选变化
const handleSelectAllChange = (value: boolean) => {
  selectAll.value = value
}

// 处理单个通知选择
const handleNotificationSelect = (notificationId: number, selected: boolean) => {
  if (selected) {
    selectedNotifications.value.push(notificationId)
  } else {
    const index = selectedNotifications.value.indexOf(notificationId)
    if (index !== -1) {
      selectedNotifications.value.splice(index, 1)
    }
  }
}

// 处理通知点击
const handleNotificationClick = async (notification: Notification) => {
  // 如果未读，标记为已读
  if (!notification.isRead) {
    await markAsRead(notification.id)
  }

  // 显示详情对话框
  currentNotification.value = notification
  dialogVisible.value = true
}

// 跳转到相关文档
const goToDocument = async (documentId: number | string) => {
  documentCheckLoading.value = true
  try {
    // 先检查文档是否存在/是否被删除
    await documentApi.getDocument(Number(documentId))
    dialogVisible.value = false
    router.push(`/documents/${documentId}`)
  } catch (error: any) {
    // 捕获异常，说明文档不存在或已删除
    ElMessage.warning(error.message || '该文档不存在或已被删除')
  } finally {
    documentCheckLoading.value = false
  }
}

// 标记通知为已读
const markAsRead = async (notificationId: number) => {
  try {
    await notificationStore.markAsRead(notificationId)
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 标记所有通知为已读
const markAllAsRead = async () => {
  try {
    markingAllRead.value = true
    await notificationStore.markAllAsRead()
    totalNotifications.value = notificationStore.notificationPage.total
  } finally {
    markingAllRead.value = false
  }
}

// 批量标记为已读
const batchMarkAsRead = async () => {
  try {
    batchMarking.value = true
    await notificationStore.batchMarkAsRead(selectedNotifications.value)
    totalNotifications.value = notificationStore.notificationPage.total
    selectedNotifications.value = []
  } finally {
    batchMarking.value = false
  }
}

// 删除通知
const deleteNotification = async (notificationId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await notificationStore.deleteNotification(notificationId)
    totalNotifications.value = notificationStore.notificationPage.total
    const selectedIndex = selectedNotifications.value.indexOf(notificationId)
    if (selectedIndex !== -1) {
      selectedNotifications.value.splice(selectedIndex, 1)
    }
  } catch (error) {
    console.error('删除通知失败:', error)
  }
}

// 批量删除
const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedNotifications.value.length} 条通知吗？`,
      '批量删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    batchDeleting.value = true
    for (const notificationId of selectedNotifications.value) {
      await notificationStore.deleteNotification(notificationId)
    }
    totalNotifications.value = notificationStore.notificationPage.total
    selectedNotifications.value = []
  } catch (error) {
    console.error('批量删除通知失败:', error)
  } finally {
    batchDeleting.value = false
  }
}

// 获取通知图标
const getNotificationIcon = (type: string) => {
  switch (type) {
    case 'DOCUMENT_APPROVED': return CircleCheckFilled
    case 'DOCUMENT_REJECTED': return CircleCloseFilled
    case 'DOCUMENT_COMMENTED': return ChatDotRound
    case 'DOCUMENT_RATED': return Star
    case 'POINTS_EARNED':
    case 'POINTS_SPENT': return Coin
    case 'SYSTEM_ANNOUNCEMENT': return NotificationIcon
    default: return Document
  }
}

// 获取通知图标颜色
const getNotificationIconColor = (type: string) => {
  switch (type) {
    case 'DOCUMENT_APPROVED': return '#67c23a'
    case 'DOCUMENT_REJECTED': return '#f56c6c'
    case 'DOCUMENT_COMMENTED': return '#409eff'
    case 'DOCUMENT_RATED': return '#e6a23c'
    case 'POINTS_EARNED': return '#67c23a'
    case 'POINTS_SPENT': return '#f56c6c'
    case 'SYSTEM_ANNOUNCEMENT': return '#909399'
    default: return '#409eff'
  }
}

// 获取通知标签类型
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

// 获取空状态消息
const getEmptyMessage = () => {
  switch (filterType.value) {
    case 'unread': return '没有未读通知'
    case 'read': return '没有已读通知'
    default: return '暂时没有任何通知'
  }
}

// 格式化时间
const formatTime = (timeStr: string) => {
  const time = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - time.getTime()

  if (diff < 60 * 1000) {
    return '刚刚'
  } else if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 1000))} 分钟前`
  } else if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 60 * 1000))} 小时前`
  } else {
    return time.toLocaleString('zh-CN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }
}

// 格式化已读时间（专门用于已读状态显示）
const formatReadTime = (timeStr: string) => {
  const time = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - time.getTime()

  if (diff < 60 * 1000) {
    return '刚才已读'
  } else if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 1000))} 分钟前已读`
  } else if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 60 * 1000))} 小时前已读`
  } else {
    const timeStr = time.toLocaleString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
    return `${timeStr} 已读`
  }
}

// 返回功能
const goBack = () => {
  router.back()
}

const checkRouteQuery = () => {
  const idStr = route.query.id as string
  if (idStr) {
    const id = Number.parseInt(idStr, 10)
    const notification = notifications.value.find(n => n.id === id)
    if (notification) {
      handleNotificationClick(notification)
      // 清除 url 上的参数，避免刷新时再次打开
      router.replace({ path: '/notifications' })
    }
  }
}

onMounted(async () => {
  await notificationStore.refreshUnreadCount()
  await fetchNotifications()
  checkRouteQuery()
})

// 监听路由参数变化，实现从下拉菜单点击后打开特定通知
watch(() => route.query.id, () => {
  checkRouteQuery()
})

// 监听筛选条件变化
watch(filterType, () => {
  selectedNotifications.value = []
})
</script>

<style scoped>
.notifications-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.notifications-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-button {
  height: 36px;
  padding: 8px 16px;
}

.header-content h2 {
  margin: 0;
  color: var(--el-text-color-primary, #303133);
}

.header-stats {
  display: flex;
  gap: 8px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.notifications-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.notifications-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter, #e4e7ed);
  background: var(--el-fill-color-lighter, #f5f7fa);
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.notifications-list {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--el-text-color-secondary, #909399);
}

.empty-state h3 {
  margin: 16px 0 8px;
  color: var(--el-text-color-regular, #606266);
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.notification-card {
  display: flex;
  align-items: flex-start;
  padding: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter, #e4e7ed);
  transition: all 0.3s;
  position: relative;
  background-color: #fafafa; /* 默认浅灰背景，确保文字可见 */
}

.notification-card:hover {
  background-color: var(--el-fill-color-light, #f5f7fa);
}

.notification-card.unread {
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  border-left: 4px solid var(--el-color-primary, #409eff);
}

.notification-card.selected {
  background-color: var(--el-color-primary-light-8, #d9ecff);
}

/* 已读通知的特殊样式 */
.notification-card:not(.unread) {
  background-color: #f8f9fa; /* 已读通知使用更浅的灰色背景 */
}

.notification-card:not(.unread):hover {
  background-color: #f0f2f5; /* 已读通知悬停时的背景色 */
}

.notification-checkbox {
  margin-right: 16px;
  margin-top: 4px;
}

.notification-icon {
  margin-right: 16px;
  margin-top: 4px;
}

.notification-content {
  flex: 1;
  cursor: pointer;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.notification-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
  flex: 1;
}

.notification-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 16px;
}

.notification-time {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  white-space: nowrap;
}

.notification-text {
  margin: 0 0 12px;
  line-height: 1.6;
  color: var(--el-text-color-regular, #606266);
}

.notification-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.read-time {
  font-size: 12px;
  color: var(--el-text-color-secondary, #606266);
  margin-left: 8px;
  font-weight: normal;
  opacity: 0.8;
}

.notification-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: 16px;
  margin-top: 4px;
}

.notifications-pagination {
  padding: 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

/* 通知详情对话框样式 */
.notification-detail {
  padding: 10px 0;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.detail-time {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.detail-content {
  color: var(--el-text-color-primary);
  line-height: 1.6;
  font-size: 15px;
  white-space: pre-wrap;
  word-break: break-word;
  background-color: var(--el-fill-color-light);
  padding: 16px;
  border-radius: 8px;
}
</style>