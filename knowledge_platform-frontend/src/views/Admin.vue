<template>
  <Layout>
    <div class="admin-page">
      <div class="content-wrapper">
        <el-container>
          <!-- 侧边栏导航 -->
          <el-aside width="250px" class="admin-sidebar">
            <el-menu
              :default-active="$route.path"
              router
              background-color="#304156"
              text-color="#fff"
              active-text-color="#409EFF"
            >
              <el-menu-item index="/admin/dashboard">
                <el-icon><DataBoard /></el-icon>
                <span>管理总览</span>
              </el-menu-item>
              <el-menu-item index="/admin/documents">
                <el-icon><Document /></el-icon>
                <span>文档管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/users">
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </el-menu-item>
              <el-sub-menu index="community">
                <template #title>
                  <el-icon><ChatDotRound /></el-icon>
                  <span>社区管理</span>
                </template>
                <el-menu-item index="/admin/community/topics">帖子管理</el-menu-item>
                <el-menu-item index="/admin/community/replies">评论管理</el-menu-item>
                <el-menu-item index="/admin/community/categories">板块管理</el-menu-item>
              </el-sub-menu>
              <el-menu-item index="/admin/categories">
                <el-icon><Folder /></el-icon>
                <span>分类管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/tags">
                <el-icon><Collection /></el-icon>
                <span>标签管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/reports">
                <el-icon><Warning /></el-icon>
                <span>举报处理</span>
              </el-menu-item>
              <el-menu-item index="/admin/notifications">
                <el-icon><Bell /></el-icon>
                <span>通知管理</span>
              </el-menu-item>
            </el-menu>
          </el-aside>

          <!-- 主要内容区域 -->
          <el-main class="admin-main">
            <router-view />
          </el-main>
        </el-container>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { Document, User, Folder, Collection, DataBoard, Bell, ChatDotRound, Warning } from '@element-plus/icons-vue'
import Layout from '@/components/Layout.vue'
</script>

<style scoped>
.admin-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.admin-sidebar {
  background: #304156;
  min-height: calc(100vh - 60px);
}

.admin-sidebar :deep(.el-menu) {
  border: none;
}

.admin-main {
  padding: 20px;
  background: #f5f7fa;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .el-aside {
    width: 200px !important;
  }

  .admin-main {
    padding: 10px;
  }
}
</style>
