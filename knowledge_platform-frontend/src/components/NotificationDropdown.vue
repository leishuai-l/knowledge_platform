<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="notification-trigger">
      <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99">
        <el-icon size="20">
          <Bell />
        </el-icon>
      </el-badge>
      <el-icon v-else size="20">
        <Bell />
      </el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu class="notification-dropdown">
        <div class="notification-header">
          <span class="title">通知</span>
          <div class="actions">
            <el-button
              v-if="unreadCount > 0"
              text
              size="small"
              @click="markAllAsRead"
            >
              全部已读
            </el-button>
            <el-button
              text
              size="small"
              @click="goToNotificationPage"
            >
              查看全部
            </el-button>
          </div>
        </div>

        <el-divider style="margin: 8px 0" />

        <div class="notification-list" v-loading="loading">
          <div
            v-if="recentNotifications.length === 0"
            class="empty-state"
          >
            <el-icon size="48" color="#C0C4CC">
              <BellFilled />
            </el-icon>
            <p>暂无通知</p>
          </div>

          <div
            v-for="notification in recentNotifications"
            :key="notification.id"
            class="notification-item"
            :class="{ 'unread': !notification.isRead }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-content">
              <div class="notification-title">
                {{ notification.title }}
              </div>
              <div class="notification-text">
                {{ notification.content }}
              </div>
              <div class="notification-time">
                {{ formatTime(notification.createdAt) }}
              </div>
            </div>
            <div class="notification-actions">
              <el-button
                v-if="!notification.isRead"
                text
                size="small"
                @click.stop="markAsRead(notification.id)"
              >
                标记已读
              </el-button>
              <el-button
                text
                size="small"
                @click.stop="deleteNotification(notification.id)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>

        <el-divider style="margin: 8px 0" />

        <div class="notification-footer">
          <el-button
            text
            size="small"
            @click="goToNotificationPage"
            style="width: 100%"
          >
            查看所有通知
          </el-button>
        </div>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, BellFilled } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import type { Notification } from '@/types'

const router = useRouter()
const notificationStore = useNotificationStore()

const loading = computed(() => notificationStore.loading)
const unreadCount = computed(() => notificationStore.unreadCount)
const recentNotifications = computed(() => notificationStore.notifications.slice(0, 6))

const fetchRecentNotifications = async () => {
  if (notificationStore.notifications.length === 0) {
    await notificationStore.fetchNotifications(0, 6)
  }
}

const handleNotificationClick = async (notification: Notification) => {
  if (!notification.isRead) {
    await notificationStore.markAsRead(notification.id)
  }

  router.push(`/notifications?id=${notification.id}`)
}

const markAsRead = async (notificationId: number) => {
  await notificationStore.markAsRead(notificationId)
}

const markAllAsRead = async () => {
  await notificationStore.markAllAsRead()
}

const deleteNotification = async (notificationId: number) => {
  await notificationStore.deleteNotification(notificationId)
}

const goToNotificationPage = () => {
  router.push('/notifications')
}

const handleCommand = (command: string) => {
  console.log('Dropdown command:', command)
}

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
    return time.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }
}

fetchRecentNotifications()
</script>

<style scoped>
.notification-trigger {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.notification-trigger:hover {
  background-color: var(--el-fill-color-light);
}

.notification-dropdown {
  width: 320px;
  max-height: 500px;
  overflow: hidden;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 0;
}

.notification-header .title {
  font-weight: 600;
  font-size: 16px;
}

.notification-header .actions {
  display: flex;
  gap: 8px;
}

.notification-list {
  max-height: 350px;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--el-text-color-secondary);
}

.empty-state p {
  margin: 8px 0 0;
  font-size: 14px;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background-color 0.3s;
  position: relative;
}

.notification-item:hover {
  background-color: var(--el-fill-color-light);
}

.notification-item.unread {
  background-color: var(--el-color-primary-light-9);
}

.notification-item.unread::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 20px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--el-color-primary);
}

.notification-content {
  padding-left: 8px;
}

.notification-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.notification-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.notification-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.notification-item:hover .notification-actions {
  opacity: 1;
}

.notification-footer {
  padding: 8px 16px;
}
</style>