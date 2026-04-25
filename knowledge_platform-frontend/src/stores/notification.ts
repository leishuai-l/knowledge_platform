import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Notification, BatchUpdateNotification, AllReadNotification, PageResponse } from '@/types'
import * as notificationApi from '@/api/notification'
import webSocketService from '@/utils/websocket'
import { ElMessage, ElNotification } from 'element-plus'

export const useNotificationStore = defineStore('notification', () => {
  // 状态
  const notifications = ref<Notification[]>([])
  const notificationPage = ref<PageResponse<Notification>>({
    list: [],
    total: 0,
    page: 0,
    size: 10,
    pages: 0
  })
  const unreadCount = ref(0)
  const loading = ref(false)
  const isWebSocketConnected = ref(false)

  const mergeNotifications = (items: Notification[]) => {
    const notificationMap = new Map(notifications.value.map(item => [item.id, item]))

    items.forEach(item => {
      const existing = notificationMap.get(item.id)
      notificationMap.set(item.id, existing ? { ...existing, ...item } : item)
    })

    notifications.value = Array.from(notificationMap.values()).sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  }

  const syncNotificationPage = (pageData?: Partial<PageResponse<Notification>>) => {
    notificationPage.value = {
      ...notificationPage.value,
      ...pageData,
      list: notifications.value
    }
  }

  const recalculateUnreadCount = () => {
    unreadCount.value = notifications.value.filter(notification => !notification.isRead).length
  }

  const updateNotificationState = (notificationIds: number[], updates: Partial<Notification>) => {
    notifications.value = notifications.value.map(notification =>
      notificationIds.includes(notification.id)
        ? { ...notification, ...updates }
        : notification
    )
  }

  const removeNotificationFromState = (notificationId: number) => {
    notifications.value = notifications.value.filter(notification => notification.id !== notificationId)
  }

  // 计算属性
  const unreadNotifications = computed(() =>
    notifications.value.filter(n => !n.isRead)
  )

  const readNotifications = computed(() =>
    notifications.value.filter(n => n.isRead)
  )

  // 初始化WebSocket连接
  const initWebSocket = () => {
    webSocketService.setCallbacks({
      onConnect: () => {
        console.log('通知WebSocket连接成功')
        isWebSocketConnected.value = true
        // 连接成功后获取未读通知数量
        refreshUnreadCount()
      },
      onDisconnect: () => {
        console.log('通知WebSocket连接断开')
        isWebSocketConnected.value = false
      },
      onNotification: (notification: Notification) => {
        handleNewNotification(notification)
      },
      onBatchUpdate: (update: BatchUpdateNotification) => {
        handleBatchUpdate(update)
      },
      onAllRead: (update: AllReadNotification) => {
        handleAllRead(update)
      },
      onSystemAnnouncement: (announcement: Notification) => {
        handleSystemAnnouncement(announcement)
      },
      onError: (error) => {
        console.error('WebSocket错误:', error)
        ElMessage.error('实时通知连接异常')
      }
    })

    webSocketService.connect()
  }

  // 处理新通知
  const handleNewNotification = (notification: Notification) => {
    mergeNotifications([notification])
    syncNotificationPage({ total: notificationPage.value.total + 1 })
    recalculateUnreadCount()

    showNotificationToast(notification)
  }

  // 处理批量更新
  const handleBatchUpdate = (update: BatchUpdateNotification) => {
    updateNotificationState(update.notificationIds, {
      isRead: update.isRead,
      readAt: update.isRead ? new Date().toISOString() : undefined
    })

    syncNotificationPage()
    recalculateUnreadCount()
  }

  // 处理全部已读
  const handleAllRead = (update: AllReadNotification) => {
    if (update.allRead) {
      updateNotificationState(
        notifications.value.map(notification => notification.id),
        {
          isRead: true,
          readAt: new Date().toISOString()
        }
      )
      syncNotificationPage()
      recalculateUnreadCount()
    }
  }

  // 处理系统公告
  const handleSystemAnnouncement = (announcement: Notification) => {
    ElNotification({
      title: announcement.title,
      message: announcement.content,
      type: 'info',
      duration: 8000,
      showClose: true
    })
  }

  // 显示通知提示
  const showNotificationToast = (notification: Notification) => {
    const toastTypeMap: Partial<Record<Notification['type'], 'success' | 'warning' | 'error' | 'info'>> = {
      DOCUMENT_APPROVED: 'success',
      DOCUMENT_REJECTED: 'error',
      POINTS_EARNED: 'success'
    }
    const type = toastTypeMap[notification.type] ?? 'info'

    ElNotification({
      title: notification.title,
      message: notification.content,
      type,
      duration: 5000,
      showClose: true,
      onClick: () => {
        markAsRead(notification.id)
      }
    })
  }

  // 获取通知列表
  const fetchNotifications = async (page = 0, size = 10, isRead?: boolean): Promise<PageResponse<Notification>> => {
    try {
      loading.value = true
      const response = await notificationApi.getNotifications({ page, size, isRead })

      const springPage = response.data.data as any
      const pageData: PageResponse<Notification> = {
        list: Array.isArray(springPage?.content) ? springPage.content : [],
        total: springPage?.totalElements || 0,
        page: springPage?.number || 0,
        size: springPage?.size || size,
        pages: springPage?.totalPages || 0
      }

      notifications.value = pageData.list
      syncNotificationPage(pageData)
      recalculateUnreadCount()

      return notificationPage.value
    } catch (error) {
      console.error('获取通知列表失败:', error)
      ElMessage.error('获取通知列表失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  // 获取最近通知
  const fetchRecentNotifications = async (limit = 5): Promise<Notification[]> => {
    const response = await notificationApi.getRecentNotifications(limit)
    const recentItems = Array.isArray(response.data.data) ? response.data.data : []
    mergeNotifications(recentItems)
    syncNotificationPage({
      total: Math.max(notificationPage.value.total, notifications.value.length)
    })
    recalculateUnreadCount()
    return recentItems
  }

  // 刷新未读通知数量
  const refreshUnreadCount = async () => {
    try {
      const response = await notificationApi.getUnreadCount()
      unreadCount.value = response.data.data as unknown as number
      return unreadCount.value
    } catch (error) {
      console.error('获取未读通知数量失败:', error)
      return unreadCount.value
    }
  }

  // 标记通知为已读
  const markAsRead = async (notificationId: number) => {
    try {
      const response = await notificationApi.markNotificationAsRead(notificationId)

      if (response.data.code === 0) {
        const notification = notifications.value.find(n => n.id === notificationId)
        if (notification && !notification.isRead) {
          updateNotificationState([notificationId], {
            isRead: true,
            readAt: new Date().toISOString()
          })
          syncNotificationPage()
          recalculateUnreadCount()
        }

        webSocketService.markNotificationAsRead(notificationId)
        return true
      }

      throw new Error(response.data.message || '标记已读失败')
    } catch (error: any) {
      console.error('标记通知为已读失败:', error)
      console.error('Error details:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
        config: error.config?.url
      })
      ElMessage.error('标记通知为已读失败')
      throw error
    }
  }

  // 批量标记为已读
  const batchMarkAsRead = async (notificationIds: number[]) => {
    try {
      await notificationApi.batchMarkAsRead(notificationIds)

      const unreadIds = notifications.value
        .filter(notification => notificationIds.includes(notification.id) && !notification.isRead)
        .map(notification => notification.id)

      if (unreadIds.length > 0) {
        updateNotificationState(unreadIds, {
          isRead: true,
          readAt: new Date().toISOString()
        })
      }

      syncNotificationPage()
      recalculateUnreadCount()
      ElMessage.success(`已标记 ${unreadIds.length} 条通知为已读`)
    } catch (error) {
      console.error('批量标记通知为已读失败:', error)
      ElMessage.error('批量标记通知为已读失败')
    }
  }

  // 标记所有通知为已读
  const markAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead()

      updateNotificationState(
        notifications.value.filter(notification => !notification.isRead).map(notification => notification.id),
        {
          isRead: true,
          readAt: new Date().toISOString()
        }
      )

      syncNotificationPage()
      recalculateUnreadCount()
      ElMessage.success('已标记所有通知为已读')
    } catch (error) {
      console.error('标记所有通知为已读失败:', error)
      ElMessage.error('标记所有通知为已读失败')
    }
  }

  // 删除通知
  const deleteNotification = async (notificationId: number) => {
    try {
      const response = await notificationApi.deleteNotification(notificationId)

      if (response.data.code === 0) {
        removeNotificationFromState(notificationId)
        syncNotificationPage({ total: Math.max(0, notificationPage.value.total - 1) })
        recalculateUnreadCount()

        ElMessage.success('通知已删除')
        return true
      }

      throw new Error(response.data.message || '删除通知失败')
    } catch (error: any) {
      console.error('删除通知失败:', error)
      console.error('Error details:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
        config: error.config?.url
      })
      ElMessage.error('删除通知失败')
      throw error
    }
  }

  // 发送系统公告（管理员）
  const sendSystemAnnouncement = async (title: string, content: string) => {
    try {
      await notificationApi.sendSystemAnnouncement({ title, content })
      ElMessage.success('系统公告发送成功')
    } catch (error) {
      console.error('发送系统公告失败:', error)
      ElMessage.error('发送系统公告失败')
    }
  }

  // 清理通知状态
  const clearNotifications = () => {
    notifications.value = []
    syncNotificationPage({
      list: [],
      total: 0,
      page: 0,
      size: notificationPage.value.size,
      pages: 0
    })
    unreadCount.value = 0
  }

  // 断开WebSocket连接
  const disconnectWebSocket = () => {
    webSocketService.disconnect()
    isWebSocketConnected.value = false
  }

  return {
    // 状态
    notifications,
    notificationPage,
    unreadCount,
    loading,
    isWebSocketConnected,

    // 计算属性
    unreadNotifications,
    readNotifications,

    // 方法
    initWebSocket,
    fetchNotifications,
    fetchRecentNotifications,
    refreshUnreadCount,
    markAsRead,
    batchMarkAsRead,
    markAllAsRead,
    deleteNotification,
    sendSystemAnnouncement,
    clearNotifications,
    disconnectWebSocket
  }
})