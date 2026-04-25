<template>
  <div class="admin-topics">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>帖子管理</h2>
          <p>管理社区中的所有帖子</p>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true">
        <el-form-item label="板块">
          <el-select v-model="searchForm.categoryId" placeholder="选择板块" clearable style="width: 150px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="隐藏" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索标题" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTopics">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="topics" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author.username" label="作者" width="120" />
        <el-table-column prop="category.name" label="板块" width="120" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="replyCount" label="回复" width="80" />
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isPinned" type="danger" size="small">置顶</el-tag>
            <el-tag v-else-if="row.isFeatured" type="warning" size="small">精华</el-tag>
            <el-tag v-else-if="row.status === 2" type="info" size="small">隐藏</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewTopic(row)">查看</el-button>
            <el-button size="small" type="warning" link @click="togglePin(row)">
              {{ row.isPinned ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button size="small" type="success" link @click="toggleFeature(row)">
              {{ row.isFeatured ? '取消精华' : '精华' }}
            </el-button>
            <el-button size="small" type="info" link @click="toggleHide(row)">
              {{ row.status === 2 ? '显示' : '隐藏' }}
            </el-button>
            <el-button size="small" type="danger" link @click="deleteTopic(row)">删除</el-button>
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
          @size-change="loadTopics"
          @current-change="loadTopics"
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
const topics = ref<any[]>([])
const categories = ref<any[]>([])

const searchForm = reactive({
  categoryId: null,
  status: null,
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const formatDate = (date: string) => new Date(date).toLocaleString('zh-CN')

const loadTopics = async () => {
  loading.value = true
  try {
    const res = await adminApi.getForumTopics({ ...searchForm, ...pagination })
    const pageData = res.data.data || {}
    topics.value = pageData.list || pageData.content || []
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
  searchForm.categoryId = null
  searchForm.status = null
  searchForm.keyword = ''
  pagination.page = 1
  loadTopics()
}

const viewTopic = (topic: any) => {
  window.open(`/community/topic/${topic.id}`, '_blank')
}

const togglePin = async (topic: any) => {
  try {
    await adminApi.toggleTopicPin(topic.id)
    ElMessage.success(topic.isPinned ? '已取消置顶' : '已置顶')
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const toggleFeature = async (topic: any) => {
  try {
    await adminApi.toggleTopicFeature(topic.id)
    ElMessage.success(topic.isFeatured ? '已取消精华' : '已设为精华')
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const toggleHide = async (topic: any) => {
  try {
    await adminApi.toggleTopicHide(topic.id)
    ElMessage.success(topic.status === 2 ? '已显示' : '已隐藏')
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const deleteTopic = async (topic: any) => {
  try {
    await ElMessageBox.confirm(`确定删除帖子「${topic.title}」吗？`, '确认删除', { type: 'warning' })
    await adminApi.deleteTopic(topic.id)
    ElMessage.success('已删除')
    loadTopics()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadTopics()
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
