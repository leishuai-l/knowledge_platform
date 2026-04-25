<template>
  <div class="layout">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-content">
        <div class="logo">
          <el-icon :size="28" color="#409EFF">
            <Document />
          </el-icon>
          <h2>知享校园知识库</h2>
        </div>

        <nav class="nav-menu">
          <div class="nav-links">
            <a
              href="#"
              @click.prevent="handleMenuSelect('home')"
              :class="['nav-link', { active: activeMenu === 'home' }]"
            >
              首页
            </a>
            <a
              href="#"
              @click.prevent="handleMenuSelect('ai')"
              :class="['nav-link', { active: activeMenu === 'ai' }]"
              v-if="userStore.isLoggedIn"
            >
              AI问答
            </a>
            <a
              href="#"
              @click.prevent="handleMenuSelect('community')"
              :class="['nav-link', { active: activeMenu === 'community' }]"
            >
              社区
            </a>
            <a
              href="#"
              @click.prevent="handleMenuSelect('documents')"
              :class="['nav-link', { active: activeMenu === 'documents' }]"
            >
              文档库
            </a>
            <a
              href="#"
              @click.prevent="handleMenuSelect('upload')"
              :class="['nav-link', { active: activeMenu === 'upload' }]"
              v-if="userStore.isLoggedIn"
            >
              上传
            </a>
            <a
              href="#"
              @click.prevent="handleMenuSelect('admin')"
              :class="['nav-link', { active: activeMenu === 'admin' }]"
              v-if="userStore.isAdmin"
            >
              管理后台
            </a>
          </div>
        </nav>

        <div class="user-actions">
          <template v-if="userStore.isLoggedIn">
            <!-- 积分显示 -->
            <div class="points-display">
              <el-icon><Coin /></el-icon>
              <span>{{ userStore.user?.points || 0 }} 积分</span>
            </div>

            <!-- 通知下拉菜单 -->
            <NotificationDropdown />

            <!-- 用户菜单 -->
            <el-dropdown trigger="click" placement="bottom-end" :hide-on-click="false">
              <span style="color: #fff; cursor: pointer;">
                <div class="user-info">
                  <el-avatar :size="32" :src="userAvatarUrl">
                    {{ userStore.user?.username?.[0] }}
                  </el-avatar>
                  <span class="username">{{ userStore.user?.username }}</span>
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </div>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <el-button type="primary" @click="$router.push('/login')">登录</el-button>
            <el-button @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主要内容区域 -->
    <el-main class="main">
      <slot />
    </el-main>

    <!-- 底部 -->
    <el-footer class="footer">
      <div class="footer-content">
        <p>&copy; 2025 知享校园知识库. All rights reserved. | 开发者: leishuai</p>
      </div>
    </el-footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Coin, User, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import NotificationDropdown from './NotificationDropdown.vue'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || globalThis.location.origin).replace(/\/$/, '')

// 组件挂载状态
const isMounted = ref(false)

// 计算属性：处理用户头像URL，添加时间戳防止缓存
const userAvatarUrl = computed(() => {
  const baseAvatar = userStore.user?.avatar
  if (!baseAvatar) return ''

  // 构建完整的头像URL
  let fullAvatarUrl = baseAvatar
  if (!baseAvatar.startsWith('http')) {
    // 相对路径，添加后端地址
    const path = baseAvatar.startsWith('/') ? baseAvatar : `/${baseAvatar}`
    fullAvatarUrl = `${apiBaseUrl}${path}`
  }

  // 强制缓存更新：添加时间戳
  const timestamp = userStore.avatarTimestamp || Date.now()
  const separator = fullAvatarUrl.includes('?') ? '&' : '?'
  return `${fullAvatarUrl}${separator}t=${timestamp}`
})

onMounted(() => {
  isMounted.value = true
})

// 获取当前激活的菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/admin')) return 'admin'
  if (path.startsWith('/documents')) return 'documents'
  if (path.startsWith('/upload')) return 'upload'
  if (path.startsWith('/community')) return 'community'
  if (path.startsWith('/ai')) return 'ai'
  if (path.startsWith('/home') || path === '/') return 'home'
  return ''
})

// 处理菜单选择
const handleMenuSelect = (index: string) => {
  switch (index) {
    case 'home':
      router.push('/home')
      break
    case 'documents':
      router.push('/documents')
      break
    case 'community':
      router.push('/community')
      break
    case 'ai':
      router.push('/ai')
      break
    case 'upload':
      router.push('/upload')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    await userStore.logout()
    ElMessage.success('退出登录成功')
    router.push('/login')
  } catch (error) {
    // 用户取消或其他错误
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: linear-gradient(90deg, #409EFF, #67C23A);
  padding: 0;
  height: 60px !important;
  line-height: 60px;
}

.header-content {
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

@media (max-width: 1024px) {
  .header-content {
    padding: 0 15px;
  }

  .logo {
    min-width: 150px;
  }

  .logo h2 {
    font-size: 16px;
  }

  .user-actions {
    min-width: 200px;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 10px;
  }

  .logo {
    min-width: 120px;
  }

  .logo h2 {
    font-size: 14px;
  }

  .user-actions {
    min-width: 150px;
  }

  .nav-link {
    padding: 0 8px;
    font-size: 14px;
  }
}

/* 响应式导航样式 */
@media (max-width: 1200px) {
  .nav-link {
    padding: 0 15px;
    font-size: 15px;
  }
}

@media (max-width: 992px) {
  .nav-link {
    padding: 0 12px;
    font-size: 14px;
  }
}

@media (max-width: 768px) {
  .nav-link {
    padding: 0 10px;
    font-size: 13px;
  }
}

@media (max-width: 576px) {
  .nav-link {
    padding: 0 8px;
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .nav-link {
    padding: 0 6px;
    font-size: 11px;
  }

  .header-content {
    padding: 0 8px;
  }

  .logo {
    min-width: 100px;
  }

  .logo h2 {
    font-size: 12px;
  }

  .user-actions {
    min-width: 120px;
  }
}

@media (max-width: 360px) {
  .nav-link {
    padding: 0 4px;
    font-size: 10px;
  }

  .header-content {
    padding: 0 5px;
  }

  .logo {
    min-width: 80px;
  }

  .logo h2 {
    font-size: 11px;
  }

  .user-actions {
    min-width: 100px;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  min-width: 200px;
}

.logo h2 {
  margin: 0;
  font-weight: 600;
}

.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 0;
}

.nav-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  flex-wrap: nowrap;
  min-width: 0;
  overflow: hidden;
  width: 100%;
}

.nav-link {
  color: #fff;
  text-decoration: none;
  padding: 0 20px;
  height: 60px;
  line-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 500;
  min-width: fit-content;
  flex-shrink: 0;
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.nav-link.active {
  border-bottom-color: #fff;
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 15px;
  min-width: 250px;
  justify-content: flex-end;
  flex-shrink: 0;
}

.points-display {
  display: flex;
  align-items: center;
  gap: 5px;
  background: transparent;
  color: #fff;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.points-display:hover {
  background: rgba(255, 255, 255, 0.1);
}

.points-display .el-icon {
  color: #FFD700;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #fff;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
  min-width: fit-content;
  white-space: nowrap;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.username {
  font-size: 14px;
}

.el-dropdown-link {
  cursor: pointer;
  color: #fff;
  text-decoration: none;
}

.el-dropdown-link:hover {
  color: #fff;
}

.main {
  flex: 1;
  padding: 0;
  background: #f5f7fa;
}

.footer {
  background: #2c3e50;
  color: #fff;
  text-align: center;
  height: 50px !important;
  line-height: 50px;
  padding: 0;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer p {
  margin: 0;
  font-size: 12px;
}

</style>