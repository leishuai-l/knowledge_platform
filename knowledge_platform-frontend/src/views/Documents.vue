<template>
  <Layout>
    <div class="documents-page">
      <div class="content-wrapper">
        <!-- 搜索区域 -->
        <el-card class="search-card">
          <el-form :model="searchForm" :inline="true" @submit.prevent="handleSearch">
            <el-form-item label="关键词">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索文档标题、描述..."
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
                @input="handleKeywordChange"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="分类">
              <el-cascader
                v-model="searchForm.categoryPath"
                :options="categoryTree"
                :props="cascaderProps"
                placeholder="选择分类"
                clearable
                style="width: 200px"
                @change="handleCategoryChange"
              />
            </el-form-item>

            <el-form-item label="文件类型">
              <el-select v-model="searchForm.fileType" placeholder="文件类型" clearable style="width: 150px" @change="handleSearch">
                <el-option
                  v-for="group in searchGroups"
                  :key="group.value"
                  :label="group.label"
                  :value="group.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="评分">
              <el-select v-model="searchForm.minRating" placeholder="最低评分" clearable style="width: 100px">
                <el-option label="5星" :value="5" />
                <el-option label="4星+" :value="4" />
                <el-option label="3星+" :value="3" />
                <el-option label="2星+" :value="2" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSearch" :loading="loading">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <span class="result-count">共找到 {{ pagination.total }} 个文档</span>
          </div>
          <div class="toolbar-right">
            <el-select v-model="searchForm.sortBy" placeholder="排序方式" style="width: 120px" @change="handleSearch">
              <el-option label="最新" value="createdAt" />
              <el-option label="下载量" value="downloadCount" />
              <el-option label="评分" value="ratingAverage" />
              <el-option label="标题" value="title" />
            </el-select>
            <el-select v-model="searchForm.sortOrder" style="width: 80px" @change="handleSearch">
              <el-option label="降序" value="desc" />
              <el-option label="升序" value="asc" />
            </el-select>
            <el-radio-group v-model="viewMode" @change="handleViewModeChange">
              <el-radio-button value="grid">
                <el-icon><Menu /></el-icon>
              </el-radio-button>
              <el-radio-button value="list">
                <el-icon><List /></el-icon>
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 文档列表 -->
        <div v-loading="loading" class="documents-container">
          <!-- 网格视图 -->
          <div v-if="viewMode === 'grid'" class="grid-view">
            <el-row :gutter="20">
              <el-col
                v-for="doc in documents"
                :key="doc.id"
                :xs="24" :sm="12" :md="8" :lg="6"
                class="document-col"
              >
                <DocumentCard :document="doc" @click="goToDetail(doc.id)" />
              </el-col>
            </el-row>
          </div>

          <!-- 列表视图 -->
          <div v-else class="list-view">
            <DocumentListItem
              v-for="doc in documents"
              :key="doc.id"
              :document="doc"
              @click="goToDetail(doc.id)"
            />
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper pagination-wrapper-bottom">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[12, 24, 48, 96]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Menu, List } from '@element-plus/icons-vue'
import Layout from '@/components/Layout.vue'
import DocumentCard from '@/components/DocumentCard.vue'
import DocumentListItem from '@/components/DocumentListItem.vue'
import { documentApi } from '@/api/document'
import { categoryApi } from '@/api/category'
import type { FileTypeSearchGroup } from '@/api/document'
import type { Document, Category, DocumentSearchRequest } from '@/types'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const documents = ref<Document[]>([])
const categoryTree = ref<Category[]>([])
const searchGroups = ref<FileTypeSearchGroup[]>([])

const searchForm = reactive<DocumentSearchRequest>({
  keyword: '',
  categoryId: undefined,
  categoryPath: [],
  fileType: '',
  minRating: undefined,
  sortBy: 'createdAt',
  sortOrder: 'desc',
  page: 1,
  size: 12
})

const pagination = reactive({
  page: 1,
  size: 12,
  total: 0,
  pages: 0
})

const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

// 防抖定时器
let searchTimeout: NodeJS.Timeout | null = null

// 关键词输入防抖
const handleKeywordChange = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    handleSearch()
  }, 500) // 500ms防抖
}

// 搜索文档
const handleSearch = async () => {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      page: pagination.page,
      size: pagination.size
    }

    // 调试日志
    console.log('搜索参数:', params)

    const response = await documentApi.searchDocuments(params)
    const data = response.data.data

    console.log('搜索结果:', data)

    documents.value = Array.isArray(data.list) ? data.list : []
    pagination.page = Number(data.page ?? pagination.page) || 1
    pagination.size = Number(data.size ?? pagination.size) || 12
    pagination.total = Number(data.total ?? 0)
    pagination.pages = Number(data.pages ?? 0)
  } catch (error: any) {
    console.error('搜索错误:', error)
    ElMessage.error(error.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  Object.assign(searchForm, {
    keyword: '',
    categoryId: undefined,
    categoryPath: [],
    fileType: '',
    minRating: undefined,
    sortBy: 'createdAt',
    sortOrder: 'desc'
  })
  pagination.page = 1
  handleSearch()
}

// 分类变化
const handleCategoryChange = (value: number | number[]) => {
  if (Array.isArray(value)) {
    searchForm.categoryId = value[value.length - 1]
  } else {
    searchForm.categoryId = value
  }
  handleSearch()
}

// 视图模式变化
const handleViewModeChange = () => {
  localStorage.setItem('documentsViewMode', viewMode.value)
}

// 分页变化
const handlePageChange = (page: number) => {
  pagination.page = page
  handleSearch()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  handleSearch()
}

// 跳转到详情页
const goToDetail = (id: number) => {
  router.push(`/documents/${id}`)
}

const loadSearchGroups = async () => {
  try {
    const response = await documentApi.getFileTypeCapabilities()
    searchGroups.value = response.data.data.searchGroups || []
  } catch (error) {
    console.error('Failed to load file type capabilities:', error)
  }
}

// 加载分类树
const loadCategories = async () => {
  try {
    const response = await categoryApi.getCategoryTree()
    categoryTree.value = response.data.data
  } catch (error) {
    console.error('Failed to load categories:', error)
  }
}

onMounted(async () => {
  // 恢复视图模式
  viewMode.value = (localStorage.getItem('documentsViewMode') as 'grid' | 'list') || 'grid'

  // 从路由参数设置搜索条件
  if (route.query.category) {
    searchForm.categoryId = Number(route.query.category)
  }
  if (route.query.keyword) {
    searchForm.keyword = route.query.keyword as string
  }

  await Promise.all([
    loadCategories(),
    loadSearchGroups()
  ])
  await handleSearch()
})
</script>

<style scoped>
.documents-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.search-card {
  margin-bottom: 20px;
}

.search-card :deep(.el-card__body) {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
}

.toolbar-left {
  color: #666;
  font-size: 14px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.documents-container {
  min-height: 400px;
}

.grid-view .document-col {
  margin-bottom: 20px;
}

.list-view .document-item {
  margin-bottom: 15px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-card :deep(.el-form) {
    display: block;
  }

  .search-card :deep(.el-form-item) {
    margin-bottom: 15px;
    margin-right: 0;
  }

  .search-card :deep(.el-input),
  .search-card :deep(.el-select),
  .search-card :deep(.el-cascader) {
    width: 100% !important;
  }

  .toolbar {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: center;
  }
}
</style>