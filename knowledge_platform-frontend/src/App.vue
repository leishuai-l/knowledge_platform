<template>
  <div id="app">
    <el-config-provider :locale="zhCn">
      <router-view />
      <AiAssistant />
    </el-config-provider>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import AiAssistant from '@/components/ai/AiAssistant.vue'

const userStore = useUserStore()
const notificationStore = useNotificationStore()

onMounted(async () => {
  // 恢复登录状态
  userStore.restoreAuth()

  // 如果有有效的token，获取最新的用户信息以确保角色等信息是最新的
  if (userStore.accessToken) {
    try {
      await userStore.getCurrentUser()
    } catch (error) {
      console.error('Failed to restore user info:', error)
      // 如果获取用户信息失败，可能token已过期，清除认证状态
      userStore.clearAuth()
    }
  }
})

// 监听用户登录状态，登录后初始化WebSocket
watch(
  () => userStore.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn) {
      // 用户登录后初始化WebSocket连接
      notificationStore.initWebSocket()
    } else {
      // 用户退出登录后断开WebSocket连接
      notificationStore.disconnectWebSocket()
      notificationStore.clearNotifications()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  // 组件销毁时断开WebSocket连接
  notificationStore.disconnectWebSocket()
})
</script>

<style>
#app {
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto',
    'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans',
    'Helvetica Neue', sans-serif;
}

/* 全局样式 */
.page-container {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 卡片样式 */
.el-card {
  margin-bottom: 20px;
}

/* 表格样式 */
.el-table {
  background: #fff;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 工具栏样式 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

/* 搜索表单样式 */
.search-form {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.search-form .el-form-item {
  margin-bottom: 10px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .content-wrapper {
    padding: 10px;
  }

  .toolbar {
    flex-direction: column;
    gap: 10px;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }
}
</style>