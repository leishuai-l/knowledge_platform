<template>
  <div class="admin-categories">
    <el-card>
      <template #header>
        <h2>分类管理</h2>
      </template>

      <div style="margin-bottom: 20px; display: flex; gap: 12px; align-items: center;">
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加分类
        </el-button>
        <el-button type="info" @click="showCategoryStatistics" :loading="loading">
          <el-icon><DataAnalysis /></el-icon>
          分类统计
        </el-button>
        <el-divider direction="vertical" />
        <span class="stats-text">共 {{ allCategories.length }} 个分类</span>
      </div>

      <!-- 批量操作 -->
      <div v-if="selectedCategories.length > 0" style="margin-bottom: 16px; padding: 12px; background-color: #f0f9ff; border-radius: 8px;">
        <span style="color: #606266; margin-right: 12px;">已选择 {{ selectedCategories.length }} 个分类：</span>
        <el-button type="danger" size="small" @click="batchDeleteCategories" :loading="loading">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button type="warning" size="small" @click="batchToggleCategories(false)" :loading="loading">
          <el-icon><Close /></el-icon>
          批量禁用
        </el-button>
        <el-button type="success" size="small" @click="batchToggleCategories(true)" :loading="loading">
          <el-icon><Check /></el-icon>
          批量启用
        </el-button>
      </div>

      <el-table :data="categories" stripe v-loading="loading" row-key="id" default-expand-all @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="分类名称" width="200" />
        <el-table-column prop="level" label="层级" width="80" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="isActive" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.isActive ? 'success' : 'danger'"
              size="small"
              effect="dark"
              style="font-weight: 600; opacity: 1;"
            >
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="editCategory(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑分类对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="editingCategory ? '编辑分类' : '添加分类'"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        label-width="100px"
        :rules="{
          name: [
            { required: true, message: '请输入分类名称', trigger: 'blur' },
            { min: 2, max: 20, message: '分类名称长度在 2 到 20 个字符', trigger: 'blur' }
          ]
        }"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="categoryForm.parentId" placeholder="选择父分类（留空为顶级分类）" clearable style="width: 100%">
            <el-option
              v-for="category in parentCategories"
              :key="category.id"
              :label="`${'　'.repeat(category.level - 1)}${category.name}`"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="categoryForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
        <el-form-item label="排序" v-if="!editingCategory">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="999" />
          <div class="form-tip">数字越小排序越靠前</div>
        </el-form-item>
        <el-form-item label="状态" v-if="editingCategory">
          <el-switch
            v-model="categoryForm.isActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleDialogClose">取消</el-button>
        <el-button type="primary" @click="saveCategory" :loading="loading">
          {{ editingCategory ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showStatsDialog"
      title="分类统计信息"
      width="860px"
      class="category-stats-dialog"
    >
      <div class="category-stats-wrapper">
        <div class="stats-summary">
          <div class="stats-summary-item">
            <span>总文档数</span>
            <strong>{{ statsSummary.totalDocuments }}</strong>
          </div>
          <div class="stats-summary-item">
            <span>分类总数</span>
            <strong>{{ statsSummary.totalCategories }}</strong>
          </div>
          <div class="stats-summary-item">
            <span>有文档的分类</span>
            <strong>{{ statsSummary.nonEmptyCategories }}</strong>
          </div>
          <div class="stats-summary-item">
            <span>平均每分类文档数</span>
            <strong>{{ averageDocuments }}</strong>
          </div>
        </div>
        <el-scrollbar max-height="480">
          <div class="category-stats-grid">
            <div
              class="category-stats-card"
              v-for="(item, index) in categoryStats"
              :key="item.id"
            >
              <div class="category-stats-rank">{{ index + 1 }}</div>
              <div class="category-stats-name">{{ item.name }}</div>
              <div class="category-stats-count">
                <span>文档数量</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <template #footer>
        <el-button @click="showStatsDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, DataAnalysis, Delete, Close, Check } from '@element-plus/icons-vue'
import { categoryApi, type CategoryCreateRequest, type CategoryUpdateRequest } from '@/api/category'
import { isUserCancelled, extractErrorMessage } from '@/utils/error'
import type { Category } from '@/types'

const loading = ref(false)
const showAddDialog = ref(false)
const editingCategory = ref<Category | null>(null)
const categories = ref<Category[]>([])
const allCategories = ref<Category[]>([])
const selectedCategories = ref<Category[]>([])

interface CategoryStat {
  id: number
  name: string
  count: number
}

const showStatsDialog = ref(false)
const categoryStats = ref<CategoryStat[]>([])
const statsSummary = reactive({
  totalDocuments: 0,
  totalCategories: 0,
  nonEmptyCategories: 0,
})
const averageDocuments = computed(() => {
  if (statsSummary.totalCategories === 0) {
    return '0'
  }
  return (statsSummary.totalDocuments / statsSummary.totalCategories).toFixed(1)
})


const categoryFormRef = ref<FormInstance>()
const categoryForm = reactive({
  name: '',
  parentId: undefined as number | undefined,
  description: '',
  sortOrder: 0,
  isActive: true
})

const parentCategories = computed(() => {
  if (!editingCategory.value) {
    return allCategories.value.filter(c => c.level <= 2)
  }

  const excludeIds = new Set<number>([editingCategory.value.id])
  const collectChildren = (parentId: number) => {
    allCategories.value.forEach(category => {
      if (category.parentId === parentId) {
        excludeIds.add(category.id)
        collectChildren(category.id)
      }
    })
  }

  collectChildren(editingCategory.value.id)

  return allCategories.value.filter(category => !excludeIds.has(category.id) && category.level <= 2)
})

const editCategory = (category: Category) => {
  editingCategory.value = category
  Object.assign(categoryForm, {
    name: category.name,
    parentId: category.parentId ?? undefined,
    description: category.description ?? '',
    sortOrder: category.sortOrder,
    isActive: category.isActive
  })
  showAddDialog.value = true
}

const deleteCategory = async (category: Category) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${category.name}」吗？删除后其下的文档将移动到未分类！`,
      '确认删除',
      { type: 'warning' }
    )

    loading.value = true
    await categoryApi.deleteCategory(category.id)

    ElMessage.success('分类已删除')
    await loadCategories()
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      return
    }

    console.error('删除分类失败:', error)

    const status = (error as any)?.response?.status
    let message = extractErrorMessage(error, '删除分类失败')
    if (status === 401) {
      message = '登录状态已过期，请重新登录'
    } else if (status === 403) {
      message = '没有权限删除该分类'
    }

    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const saveCategory = async () => {
  if (!categoryFormRef.value) return

  try {
    const valid = await categoryFormRef.value.validate()
    if (!valid) return
  } catch (validationError) {
    return
  }

  try {
    loading.value = true

    if (editingCategory.value) {
      const updateData: CategoryUpdateRequest = {
        name: categoryForm.name.trim(),
        description: categoryForm.description?.trim() || undefined,
        isActive: categoryForm.isActive
      }
      await categoryApi.updateCategory(editingCategory.value.id, updateData)
      ElMessage.success('分类已更新')
    } else {
      const createData: CategoryCreateRequest = {
        name: categoryForm.name.trim(),
        description: categoryForm.description?.trim() || undefined,
        parentId: categoryForm.parentId
      }
      await categoryApi.createCategory(createData)
      ElMessage.success('分类已创建')
    }

    showAddDialog.value = false
    resetForm()
    await loadCategories()
  } catch (error: unknown) {
    console.error('保存分类失败:', error)
    ElMessage.error(extractErrorMessage(error, '保存分类失败'))
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingCategory.value = null
  Object.assign(categoryForm, {
    name: '',
    parentId: undefined,
    description: '',
    sortOrder: 0,
    isActive: true
  })
  categoryFormRef.value?.clearValidate()
}

const handleDialogClose = () => {
  resetForm()
  showAddDialog.value = false
}

const handleSelectionChange = (selection: Category[]) => {
  selectedCategories.value = selection
}

const batchDeleteCategories = async () => {
  if (selectedCategories.value.length === 0) {
    ElMessage.warning('请先选择要删除的分类')
    return
  }

  try {
    const categoryNames = selectedCategories.value.map(cat => cat.name).join('、')
    const confirmMessage = `确定要删除选中的 ${selectedCategories.value.length} 个分类吗？\n\n${categoryNames}\n\n删除后其下的文档将移动到未分类！`

    await ElMessageBox.confirm(confirmMessage, '确认批量删除', { type: 'warning' })

    loading.value = true

    let successCount = 0
    let failCount = 0
    const failureReasons = new Set<string>()

    for (const category of selectedCategories.value) {
      try {
        await categoryApi.deleteCategory(category.id)
        successCount++
      } catch (error) {
        failCount++
        console.error(`删除分类 ${category.name} 失败:`, error)
        failureReasons.add(extractErrorMessage(error, `分类“${category.name}”删除失败`))
      }
    }

    if (failCount > 0) {
      const reasonText = Array.from(failureReasons).filter(Boolean).join('；')
      const message = reasonText
        ? `删除完成：成功 ${successCount} 个，失败 ${failCount} 个。原因：${reasonText}`
        : `删除完成：成功 ${successCount} 个，失败 ${failCount} 个。`
      ElMessage.warning(message)
    } else {
      ElMessage.success(`成功删除了 ${successCount} 个分类`)
    }

    selectedCategories.value = []
    await loadCategories()
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      return
    }

    console.error('批量删除分类失败:', error)
    ElMessage.error(extractErrorMessage(error, '批量删除失败'))
  } finally {
    loading.value = false
  }
}

const batchToggleCategories = async (active: boolean) => {
  if (selectedCategories.value.length === 0) {
    ElMessage.warning('请先选择要操作的分类')
    return
  }

  try {
    const action = active ? '启用' : '禁用'
    const categoryNames = selectedCategories.value.map(cat => cat.name).join('、')
    const confirmMessage = `确定要${action}选中的 ${selectedCategories.value.length} 个分类吗？\n\n${categoryNames}`

    await ElMessageBox.confirm(confirmMessage, `确认批量${action}`, { type: 'info' })

    loading.value = true

    const categoryIds = selectedCategories.value.map(cat => cat.id)
    await categoryApi.toggleCategoriesActive(categoryIds, active)

    ElMessage.success(`成功${action}了 ${selectedCategories.value.length} 个分类`)
    selectedCategories.value = []
    await loadCategories()
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      ElMessage.info('已取消批量操作')
      return
    }

    console.error(`批量${active ? '启用' : '禁用'}分类失败:`, error)
    ElMessage.error(extractErrorMessage(error, '批量操作失败'))
  } finally {
    loading.value = false
  }
}

const flattenCategories = (treeData: Category[]): Category[] => {
  const flattened: Category[] = []

  const flatten = (nodes?: Category[]) => {
    if (!Array.isArray(nodes)) {
      return
    }

    nodes.forEach(node => {
      if (!node) {
        return
      }

      const children = Array.isArray(node.children) ? node.children : []

      flattened.push({
        ...node,
        parentId: node.parentId ?? undefined,
        description: node.description,
        isDeleted: node.isDeleted ?? false,
        children
      })

      if (children.length > 0) {
        flatten(children)
      }
    })
  }

  flatten(treeData)
  return flattened
}

const buildCategoryTree = (categories: Category[]): Category[] => {
  const categoryMap = new Map<number, Category>()
  const rootCategories: Category[] = []

  categories.forEach(category => {
    categoryMap.set(category.id, { ...category, children: [] })
  })

  categories.forEach(category => {
    const node = categoryMap.get(category.id)!
    if (category.parentId === null || category.parentId === undefined) {
      rootCategories.push(node)
    } else {
      const parent = categoryMap.get(category.parentId)
      if (parent) {
        parent.children = parent.children || []
        parent.children.push(node)
      }
    }
  })

  return rootCategories
}

const showCategoryStatistics = async () => {
  try {
    loading.value = true
    const response = await categoryApi.getCategoryStatistics()

    if (response.data.code === 0) {
      const statistics = response.data.data || []

      if (statistics.length === 0) {
        ElMessage.info('暂无分类统计数据')
        return
      }

      const stats = statistics.map((stat: any[]) => ({
        id: Number(stat[0]),
        name: String(stat[1] ?? '未知分类'),
        count: Number(stat[2]) || 0
      }))

      const sortedStats = [...stats].sort((a, b) => {
        if (b.count !== a.count) {
          return b.count - a.count
        }
        return a.name.localeCompare(b.name)
      })

      categoryStats.value = sortedStats
      statsSummary.totalDocuments = sortedStats.reduce((sum, item) => sum + item.count, 0)
      statsSummary.totalCategories = sortedStats.length
      statsSummary.nonEmptyCategories = sortedStats.filter(item => item.count > 0).length

      showStatsDialog.value = true
    } else {
      throw new Error(response.data.message || '获取分类统计失败')
    }
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      return
    }

    console.error('获取分类统计失败:', error)
    ElMessage.error(extractErrorMessage(error, '获取分类统计失败'))
  } finally {
    loading.value = false
  }
}



const loadCategories = async () => {
  try {
    loading.value = true
    const response = await categoryApi.getAllCategoryTree()
    const treeData = response.data.data || []
    allCategories.value = flattenCategories(treeData)
    categories.value = buildCategoryTree(allCategories.value)
  } catch (error: unknown) {
    console.error('加载分类失败:', error)
    ElMessage.error(extractErrorMessage(error, '分类列表加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.admin-categories h2 {
  margin: 0;
  color: #2c3e50;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

:deep(.el-table .el-table__row td) {
  padding: 12px 0;
}

:deep(.el-table .el-table__row:hover) {
  background-color: #f5f7fa;
}

:deep(.el-table .el-table__header th) {
  background-color: #fafafa;
  color: #606266;
  font-weight: 500;
}

.stats-text {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
.category-stats-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 8px;
}

.category-stats-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.stats-summary-item {
  background: #f5f7fa;
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stats-summary-item span {
  color: #909399;
  font-size: 13px;
}

.stats-summary-item strong {
  font-size: 20px;
  color: #303133;
}

.category-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.category-stats-card {
  background: #f9fafc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.category-stats-card:hover {
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}

.category-stats-rank {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.category-stats-name {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
  line-height: 1.4;
}

.category-stats-count {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #606266;
  font-size: 13px;
}

.category-stats-count strong {
  font-size: 18px;
  color: #303133;
}

@media (max-width: 768px) {
  .stats-summary {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  }
}


</style>












