<template>
  <Layout>
    <div class="community-home">
      <div class="page-header">
        <h2>社区交流</h2>
        <el-button type="primary" @click="$router.push('/community/create')">发布帖子</el-button>
      </div>

      <div class="community-content">
        <!-- Sidebar: Categories & Hot Topics -->
        <div class="sidebar">
          <el-card class="category-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>板块列表</span>
              </div>
            </template>
            <el-menu
              :default-active="activeCategory"
              class="category-menu"
              @select="handleCategorySelect"
            >
              <el-menu-item index="">
                <el-icon><Menu /></el-icon>
                <span>全部话题</span>
              </el-menu-item>
              <el-menu-item 
                v-for="category in categories" 
                :key="category.id" 
                :index="category.id.toString()"
              >
                <el-icon>
                  <component :is="getIconComponent(category.icon)" />
                </el-icon>
                <span>{{ category.name }}</span>
              </el-menu-item>
            </el-menu>
          </el-card>

          <!-- Hot Topics -->
          <el-card class="sidebar-card hot-topics" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>热门帖子</span>
              </div>
            </template>
            <div class="hot-list">
              <div 
                v-for="(item, index) in hotTopics" 
                :key="item.id" 
                class="hot-item"
                @click="handleTopicClick(item.id)"
              >
                <span class="hot-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
                <span class="hot-title">{{ item.title }}</span>
              </div>
            </div>
          </el-card>

          <!-- Tags Cloud -->
          <el-card class="sidebar-card tags-cloud" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>热门标签</span>
              </div>
            </template>
            <div class="tag-cloud">
              <span
                v-for="(tag, index) in allTags"
                :key="tag.id"
                :class="['custom-tag', { active: activeTag === tag.name }]"
                :style="activeTag === tag.name ? {} : getTagStyle(index)"
                @click.stop="handleTagSelect(tag.name)"
              >
                {{ tag.name }} ({{ tag.topicCount }})
              </span>
            </div>
          </el-card>
        </div>

        <!-- Main: Topic List -->
        <div class="main-content">
          <el-card v-loading="loading" shadow="hover">
            <template #header>
              <div class="topic-list-header">
                <span>最新话题</span>
                <el-radio-group v-model="sortOrder" size="small" @change="fetchTopics">
                  <el-radio-button label="latest">最新</el-radio-button>
                  <el-radio-button label="hot">最热</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            
            <div v-if="topics.length === 0" class="empty-state">
              <el-empty description="暂无帖子，快来发布第一篇吧！" />
            </div>
            <div v-else class="topic-list">
              <div 
                v-for="topic in topics" 
                :key="topic.id" 
                class="topic-item"
                @click="handleTopicClick(topic.id)"
              >
                <div class="topic-main">
                  <div class="topic-title">
                    <el-tag v-if="topic.isPinned" type="danger" size="small" effect="dark" class="tag-item">置顶</el-tag>
                    <el-tag v-if="topic.isEssence" type="warning" size="small" effect="dark" class="tag-item">精华</el-tag>
                    <span class="title-text">{{ topic.title }}</span>
                  </div>
                  <div class="topic-content-preview">
                    {{ (topic.content || '').substring(0, 100) }}...
                  </div>
                  <div class="topic-meta">
                    <div class="meta-left">
                      <el-avatar :size="20" :src="getAvatarUrl(topic.author?.avatar)" class="author-avatar">
                        {{ topic.author?.username?.charAt(0) }}
                      </el-avatar>
                      <span class="author">{{ topic.author?.username || '未知用户' }}</span>
                      <span class="divider">•</span>
                      <span class="time">{{ formatDate(topic.createdAt) }}</span>
                      <span class="divider">•</span>
                      <el-tag 
                        size="small" 
                        effect="dark" 
                        :color="getCategoryColor(topic.category?.id)"
                        style="border: none; color: #fff;"
                      >
                        {{ topic.category?.name }}
                      </el-tag>
                    </div>
                  </div>
                </div>
                <div class="topic-stats">
                  <div class="stat-item" title="浏览量">
                    <el-icon><View /></el-icon>
                    <span>{{ topic.viewCount }}</span>
                  </div>
                  <div class="stat-item" title="回复数">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>{{ topic.replyCount }}</span>
                  </div>
                  <div class="stat-item" title="点赞数">
                    <el-icon><Pointer /></el-icon>
                    <span>{{ topic.likeCount || 0 }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="fetchTopics"
              />
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Menu, Folder, View, ChatDotRound, Reading, Files, QuestionFilled, Coffee, Pointer } from '@element-plus/icons-vue'
import { getCategories, getTopics, getHotTopics, getTags, type ForumCategory, type ForumTopic, type ForumTag } from '@/api/forum'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const categories = ref<ForumCategory[]>([])
const topics = ref<ForumTopic[]>([])
const hotTopics = ref<ForumTopic[]>([])
const allTags = ref<ForumTag[]>([])
const activeCategory = ref('')
const activeTag = ref('')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const sortOrder = ref('latest')
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || globalThis.location.origin).replace(/\/$/, '')

const iconMap: Record<string, any> = {
  Reading,
  Files,
  QuestionFilled,
  Coffee,
  Folder
}

const getIconComponent = (iconName: string) => {
  return iconMap[iconName] || Folder
}

const getTagStyle = (index: number) => {
  const colors = [
    { bg: '#c6e2ff', border: '#409eff' }, // Darker Blue
    { bg: '#d1edc4', border: '#67c23a' }, // Darker Green
    { bg: '#f8e3c5', border: '#e6a23c' }, // Darker Orange
    { bg: '#fcd3d3', border: '#f56c6c' }, // Darker Red
    { bg: '#d9d9d9', border: '#909399' }, // Darker Grey
  ]
  const color = colors[index % colors.length]
  return {
    backgroundColor: color.bg,
    color: '#000000',
    borderColor: color.border,
    fontWeight: 'bold',
    '--el-tag-text-color': '#000000'
  }
}

const getAvatarUrl = (avatar?: string) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  const path = avatar.startsWith('/') ? avatar : `/${avatar}`
  return `${apiBaseUrl}${path}`
}

const categoryColors: Record<number, string> = {
  1: '#409EFF', // 学习交流 - Blue
  2: '#67C23A', // 资源分享 - Green
  3: '#E6A23C', // 技术问答 - Orange
  4: '#F56C6C', // 闲聊灌水 - Red
  // Add more mappings as needed
}

const getCategoryColor = (categoryId?: number) => {
  return (categoryId && categoryColors[categoryId]) || '#909399' // Default gray
}

const fetchCategories = async () => {
  try {
    const res = await getCategories()
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      categories.value = res.data.data
    }
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

const fetchHotTopics = async () => {
  try {
    const res = await getHotTopics(3)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      hotTopics.value = res.data.data
    }
  } catch (error) {
    console.error('Failed to fetch hot topics:', error)
  }
}

const fetchAllTags = async () => {
  try {
    const res = await getTags()
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      allTags.value = res.data.data
    }
  } catch (error) {
    console.error('Failed to fetch tags:', error)
  }
}

const fetchTopics = async () => {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value - 1, // Backend uses 0-based index
      size: pageSize.value
    }
    
    // Only add categoryId if it's not empty string (which means "All Topics")
    if (activeCategory.value) {
      params.categoryId = Number(activeCategory.value)
    }

    if (activeTag.value) {
      params.tag = activeTag.value
    }

    // Pass params as the first argument, NOT inside an object property named 'params'
    // The api/forum.ts definition expects: getTopics(params: { ... })
    // And it passes it to request.get('/...', { params }) internally
    params.sort = sortOrder.value // Add sort parameter
    const res = await getTopics(params)
    console.log('Fetched topics response:', res) // Debug log
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      // Handle the case where the backend returns PageResponse structure directly in data
      // or wrapped in data.data depending on the ApiResponse structure
      const pageData = res.data.data
      
      console.log('Page data structure:', pageData) // Debug log

      // Try to extract list from common page structures
      if (pageData && Array.isArray(pageData.list)) {
         topics.value = pageData.list
         total.value = pageData.total || pageData.list.length
      } else if (pageData && Array.isArray((pageData as any).content)) { // Spring Data Page default
         const contentData = (pageData as any)
         topics.value = contentData.content
         total.value = contentData.totalElements || contentData.content.length
      } else if (Array.isArray(pageData)) {
         topics.value = pageData
         total.value = pageData.length
      } else {
         console.warn('Unknown data structure for topics list', pageData)
         topics.value = []
         total.value = 0
      }
    } else {
      console.error('API Error:', res.data?.message || 'Unknown error')
      topics.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('Failed to fetch topics:', error)
  } finally {
    loading.value = false
  }
}

const handleCategorySelect = (index: string) => {
  activeCategory.value = index
  activeTag.value = '' // Clear tag filter when category changes
  currentPage.value = 1
  fetchTopics()
}

const handleTagSelect = (tagName: string) => {
  if (activeTag.value === tagName) {
    activeTag.value = '' // Toggle off
  } else {
    activeTag.value = tagName
  }
  currentPage.value = 1
  fetchTopics()
}

const handleTopicClick = (id: number) => {
  console.log('Topic clicked:', id)
  if (id) {
    router.push({ name: 'TopicDetail', params: { id } })
  } else {
    console.error('Topic ID is missing')
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchCategories()
  fetchTopics()
  fetchHotTopics()
  fetchAllTags()
})
</script>

<style scoped>
.community-home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.community-content {
  display: flex;
  gap: 20px;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category-card {
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.sidebar-card {
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.hot-item:hover {
  color: #409EFF;
}

.hot-rank {
  font-size: 12px;
  font-weight: bold;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: #f5f7fa;
  color: #909399;
}

.rank-1 { background: #f56c6c; color: #fff; }
.rank-2 { background: #e6a23c; color: #fff; }
.rank-3 { background: #f2c07d; color: #fff; }

.hot-title {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-cloud .custom-tag {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  font-size: 12px;
  line-height: 1;
  border-width: 1px;
  border-style: solid;
  border-radius: 4px;
  box-sizing: border-box;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-cloud .custom-tag:hover {
  transform: scale(1.05);
}

.tag-cloud .custom-tag.active {
  background-color: #409eff;
  color: #fff !important;
  border-color: #409eff;
}

.card-header {
  font-weight: bold;
  color: #303133;
}

.category-menu {
  border-right: none;
}

.main-content {
  flex: 1;
}

.topic-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.topic-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-radius: 8px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.topic-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #dcdfe6;
}

.topic-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.topic-content-preview {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
  line-height: 1.6;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author {
  font-weight: 500;
  color: #606266;
}

.divider {
  color: #dcdfe6;
  margin: 0 4px;
}

.topic-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 4px;
  background-color: transparent;
  transition: all 0.2s;
}

.stat-item:hover {
  background-color: #f5f7fa;
  color: #409EFF;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
