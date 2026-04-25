<template>
  <div class="admin-users">
    <el-card>
      <template #header>
        <div class="header-row">
          <h2>用户管理</h2>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索用户名或邮箱"
              clearable
              style="width: 240px"
              @clear="loadUsers"
              @keyup.enter="handleSearch"
            />
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
        </div>
      </template>

      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="220" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.role === 'ADMIN' ? 'danger' : 'primary'"
              size="small"
              effect="dark"
              style="font-weight: 600; opacity: 1;"
            >
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'ACTIVE' ? 'success' : 'danger'"
              size="small"
              effect="dark"
              style="font-weight: 600; opacity: 1;"
            >
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              @click="toggleUserStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, normalizeAdminPageResponse } from '@/api/admin'
import type { User } from '@/types'

const loading = ref(false)
const users = ref<User[]>([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const toggleUserStatus = async (user: User) => {
  try {
    await adminApi.toggleUserStatus(user)
    ElMessage.success('用户状态已更新')
    await loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadUsers()
}

const loadUsers = async () => {
  try {
    loading.value = true
    const response = await adminApi.getUsers({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined
    })
    const pageData = normalizeAdminPageResponse<User>(response.data.data)

    users.value = pageData.list || []
    currentPage.value = pageData.page || currentPage.value
    pageSize.value = pageData.size || pageSize.value
    total.value = pageData.total || 0
  } catch (error: any) {
    console.error('加载用户列表失败:', error)
    ElMessage.error(error.message || '加载用户列表失败')
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.admin-users h2 {
  margin: 0;
  color: #2c3e50;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
