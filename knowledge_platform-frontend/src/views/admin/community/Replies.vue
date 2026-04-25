<template>
  <div class="admin-replies">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>评论管理</h2>
          <p>管理社区中的所有评论</p>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索评论内容" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReplies">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="replies" v-loading="loading" stripe>
        <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="author.username" label="作者" width="120" />
        <el-table-column prop="topic.title" label="所属帖子" width="200" show-overflow-tooltip />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="createdAt" label="发布时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewTopic(row)">查看帖子</el-button>
            <el-button size="small" type="danger" link @click="deleteReply(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadReplies"
          @current-change="loadReplies"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const loading = ref(false)
const replies = ref<any[]>([])

const searchForm = reactive({
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const formatDate = (date: string) => new Date(date).toLocaleString('zh-CN')

const loadReplies = async () => {
  loading.value = true
  try {
    const res = await adminApi.getForumReplies({ ...searchForm, ...pagination })
    const pageData = res.data.data || {}
    replies.value = pageData.list || pageData.content || []
    const rawPage = Number(pageData.page ?? pageData.number ?? 0)
    const normalizedPage = pageData.page == null ? rawPage + 1 : rawPage
    pagination.page = Number.isFinite(normalizedPage) && normalizedPage > 0
      ? normalizedPage
      : pagination.page
    pagination.size = Number(pageData.size ?? pagination.size) || pagination.size
    pagination.total = Number(pageData.total ?? pageData.totalElements ?? 0)
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  pagination.page = 1
  loadReplies()
}

const viewTopic = (reply: any) => {
  window.open(`/community/topic/${reply.topic.id}`, '_blank')
}

const deleteReply = async (reply: any) => {
  try {
    await ElMessageBox.confirm('确定删除该评论吗？', '确认删除', { type: 'warning' })
    await adminApi.deleteReply(reply.id)
    ElMessage.success('已删除')
    loadReplies()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadReplies()
})
</script>

<style scoped>
.card-header h2 {
  margin: 0 0 10px;
  color: #2c3e50;
}

.card-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
