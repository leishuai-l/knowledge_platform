<template>
  <div class="admin-tags">
    <el-card>
      <template #header>
        <h2>标签管理</h2>
      </template>

      <div style="margin-bottom: 20px; display: flex; gap: 12px; align-items: center;">
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加标签
        </el-button>
        <el-button type="success" @click="initializeDefaultTags" :loading="loading">
          <el-icon><RefreshRight /></el-icon>
          初始化默认标签
        </el-button>
        <el-button type="warning" @click="showUnusedTags" :loading="loading">
          <el-icon><View /></el-icon>
          查看未使用标签
        </el-button>
        <el-button type="danger" @click="cleanUnusedTags" :loading="loading">
          <el-icon><Delete /></el-icon>
          清理未使用标签
        </el-button>
        <el-divider direction="vertical" />
        <span class="stats-text">
          {{ isShowingUnusedOnly ? `未使用标签 ${tags.length} 个` : `共 ${tags.length} 个标签` }}
        </span>
        <el-button v-if="isShowingUnusedOnly" type="info" size="small" @click="showAllTags">
          <el-icon><RefreshRight /></el-icon>
          显示所有标签
        </el-button>
      </div>

      <div
        v-if="selectedTags.length > 0"
        style="margin-bottom: 16px; padding: 12px; background-color: #f0f9ff; border-radius: 8px;"
      >
        <span style="color: #606266; margin-right: 12px;">已选择 {{ selectedTags.length }} 个标签：</span>
        <el-button type="danger" size="small" @click="batchDeleteTags" :loading="loading">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>

      <el-table :data="tags" stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="标签名称" width="150">
          <template #default="{ row }">
            <el-tag :color="row.color" effect="light">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="color" label="颜色" width="100">
          <template #default="{ row }">
            <div class="color-display" :style="{ backgroundColor: row.color }"></div>
          </template>
        </el-table-column>
        <el-table-column prop="usageCount" label="使用次数" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="color: #606266;">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="editTag(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTag(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="showAddDialog"
      :title="editingTag ? '编辑标签' : '添加标签'"
      width="450px"
      @close="handleDialogClose"
    >
      <el-form
        ref="tagFormRef"
        :model="tagForm"
        label-width="80px"
        :rules="{
          name: [
            { required: true, message: '请输入标签名称', trigger: 'blur' },
            { min: 1, max: 10, message: '标签名称长度在 1 到 10 个字符', trigger: 'blur' }
          ]
        }"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="tagForm.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签颜色">
          <div class="color-section">
            <el-color-picker v-model="tagForm.color" show-alpha />
            <div class="preset-colors">
              <div class="preset-label">预设颜色：</div>
              <div class="color-options">
                <div
                  v-for="color in presetColors"
                  :key="color"
                  class="color-option"
                  :style="{ backgroundColor: color }"
                  :class="{ active: tagForm.color === color }"
                  @click="tagForm.color = color"
                />
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="标签描述">
          <el-input
            v-model="tagForm.description"
            type="textarea"
            :rows="2"
            placeholder="请输入标签描述（可选）"
          />
        </el-form-item>
        <el-form-item label="预览效果">
          <el-tag :color="tagForm.color" effect="light" size="default">
            {{ tagForm.name || '标签预览' }}
          </el-tag>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleDialogClose">取消</el-button>
        <el-button type="primary" @click="saveTag" :loading="loading">
          {{ editingTag ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, RefreshRight, View, Delete } from '@element-plus/icons-vue'
import { tagApi, type TagCreateRequest, type TagUpdateRequest } from '@/api/tag'
import { isUserCancelled, extractErrorMessage } from '@/utils/error'
import type { Tag } from '@/types'

const loading = ref(false)
const showAddDialog = ref(false)
const editingTag = ref<Tag | null>(null)
const tags = ref<Tag[]>([])
const selectedTags = ref<Tag[]>([])
const isShowingUnusedOnly = ref(false)

const tagFormRef = ref<FormInstance>()
const tagForm = reactive({
  name: '',
  color: '#409EFF',
  description: ''
})

const presetColors = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FECA57',
  '#FF9FF3', '#54A0FF', '#5F27CD', '#00D2D3', '#FF9F43'
]

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadTags = async () => {
  try {
    loading.value = true
    const response = await tagApi.getAllTags()
    tags.value = response.data.data || []
  } catch (error: unknown) {
    console.error('加载标签失败:', error)
    ElMessage.error(extractErrorMessage(error, '标签列表加载失败'))
  } finally {
    loading.value = false
  }
}

const initializeDefaultTags = async () => {
  try {
    await ElMessageBox.confirm('确定要初始化默认标签吗？已有标签将保留。', '初始化默认标签', {
      type: 'warning'
    })

    loading.value = true
    await tagApi.initializeDefaultTags()
    ElMessage.success('默认标签已初始化')
    await loadTags()
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      ElMessage.info('已取消初始化')
      return
    }

    console.error('初始化默认标签失败:', error)
    ElMessage.error(extractErrorMessage(error, '初始化默认标签失败'))
  } finally {
    loading.value = false
  }
}

const editTag = (tag: Tag) => {
  editingTag.value = tag
  tagForm.name = tag.name
  tagForm.color = tag.color
  tagForm.description = tag.description || ''
  showAddDialog.value = true
}

const deleteTag = async (tag: Tag) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除标签「${tag.name}」吗？删除后已关联的文档标签也会被移除。`,
      '确认删除',
      { type: 'warning' }
    )

    loading.value = true
    await tagApi.deleteTag(tag.id)

    ElMessage.success('标签已删除')
    if (isShowingUnusedOnly.value) {
      await showUnusedTags()
    } else {
      await loadTags()
    }
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      return
    }

    console.error('删除标签失败:', error)
    ElMessage.error(extractErrorMessage(error, '删除标签失败'))
  } finally {
    loading.value = false
  }
}

const saveTag = async () => {
  if (!tagFormRef.value) return

  try {
    const valid = await tagFormRef.value.validate()
    if (!valid) return
  } catch (_validationError) {
    return
  }

  try {
    loading.value = true

    if (editingTag.value) {
      const updateData: TagUpdateRequest = {
        name: tagForm.name.trim(),
        color: tagForm.color,
        description: tagForm.description?.trim() || undefined
      }
      await tagApi.updateTag(editingTag.value.id, updateData)
      ElMessage.success('标签已更新')
    } else {
      const createData: TagCreateRequest = {
        name: tagForm.name.trim(),
        color: tagForm.color,
        description: tagForm.description?.trim() || undefined
      }
      await tagApi.createTag(createData)
      ElMessage.success('标签已创建')
    }

    showAddDialog.value = false
    resetForm()
    if (isShowingUnusedOnly.value) {
      await showUnusedTags()
    } else {
      await loadTags()
    }
  } catch (error: unknown) {
    console.error('保存标签失败:', error)
    ElMessage.error(extractErrorMessage(error, '保存标签失败'))
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingTag.value = null
  Object.assign(tagForm, {
    name: '',
    color: '#409EFF',
    description: ''
  })
  tagFormRef.value?.clearValidate()
}

const handleDialogClose = () => {
  resetForm()
  showAddDialog.value = false
}

const handleSelectionChange = (selection: Tag[]) => {
  selectedTags.value = selection
}

const batchDeleteTags = async () => {
  if (selectedTags.value.length === 0) {
    ElMessage.warning('请先选择要删除的标签')
    return
  }

  try {
    const tagNames = selectedTags.value.map(tag => tag.name).join('、')
    const confirmMessage = `确定要删除选中的 ${selectedTags.value.length} 个标签吗？\n\n${tagNames}\n\n删除后关联的文档标签也会被移除。`

    await ElMessageBox.confirm(confirmMessage, '确认批量删除', { type: 'warning' })

    loading.value = true

    let successCount = 0
    let failCount = 0
    const failureReasons = new Set<string>()

    for (const tag of selectedTags.value) {
      try {
        await tagApi.deleteTag(tag.id)
        successCount++
      } catch (error: unknown) {
        failCount++
        console.error(`删除标签 ${tag.name} 失败:`, error)
        failureReasons.add(extractErrorMessage(error, `标签“${tag.name}”删除失败`))
      }
    }

    if (failCount > 0) {
      const reasonText = Array.from(failureReasons).filter(Boolean).join('；')
      const message = reasonText
        ? `删除完成：成功 ${successCount} 个，失败 ${failCount} 个。原因：${reasonText}`
        : `删除完成：成功 ${successCount} 个，失败 ${failCount} 个。`
      ElMessage.warning(message)
    } else {
      ElMessage.success(`成功删除了 ${successCount} 个标签`)
    }

    selectedTags.value = []
    if (isShowingUnusedOnly.value) {
      await showUnusedTags()
    } else {
      await loadTags()
    }
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      return
    }

    console.error('批量删除标签失败:', error)
    ElMessage.error(extractErrorMessage(error, '批量删除标签失败'))
  } finally {
    loading.value = false
  }
}

const showAllTags = async () => {
  isShowingUnusedOnly.value = false
  selectedTags.value = []
  await loadTags()
}

const showUnusedTags = async () => {
  try {
    loading.value = true
    const response = await tagApi.getUnusedTags()
    const unusedTags = response.data.data || []

    if (unusedTags.length === 0) {
      ElMessage.info('没有未使用的标签')
      return
    }

    tags.value = unusedTags
    isShowingUnusedOnly.value = true
    selectedTags.value = []
    ElMessage.success(`找到 ${unusedTags.length} 个未使用的标签`)
  } catch (error: unknown) {
    console.error('获取未使用标签失败:', error)
    ElMessage.error(extractErrorMessage(error, '获取未使用标签失败'))
  } finally {
    loading.value = false
  }
}

const cleanUnusedTags = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清理所有未使用的标签吗？此操作不可恢复！',
      '确认清理',
      { type: 'warning' }
    )

    loading.value = true
    const response = await tagApi.cleanUnusedTags()
    const deletedCount = response.data.data || 0

    if (deletedCount > 0) {
      ElMessage.success(`成功清理了 ${deletedCount} 个未使用的标签`)
      if (isShowingUnusedOnly.value) {
        await showUnusedTags()
      } else {
        await loadTags()
      }
    } else {
      ElMessage.info('没有找到需要清理的标签')
    }
  } catch (error: unknown) {
    if (isUserCancelled(error)) {
      ElMessage.info('已取消清理')
      return
    }

    console.error('清理未使用标签失败:', error)
    ElMessage.error(extractErrorMessage(error, '清理未使用标签失败'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTags()
})
</script>

<style scoped>
.admin-tags h2 {
  margin: 0;
  color: #2c3e50;
}

.color-display {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.color-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.preset-colors {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preset-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.color-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.color-option:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.color-option.active {
  border-color: #409EFF;
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
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
</style>



