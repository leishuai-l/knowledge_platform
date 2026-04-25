<template>
  <div class="admin-categories">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>板块管理</h2>
          <el-button type="primary" @click="showAddDialog = true">新增板块</el-button>
        </div>
      </template>

      <el-table :data="categories" v-loading="loading">
        <el-table-column prop="name" label="板块名称" width="200" />
        <el-table-column prop="description" label="描述" min-width="300" />
        <el-table-column prop="topicCount" label="帖子数" width="100" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="editCategory(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showAddDialog" title="新增板块" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="板块名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const loading = ref(false)
const categories = ref<any[]>([])
const showAddDialog = ref(false)
const form = reactive({ id: null, name: '', description: '', sortOrder: 0 })

const loadCategories = async () => {
  loading.value = true
  try {
    const res = await adminApi.getForumCategories()
    categories.value = res.data.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const editCategory = (cat: any) => {
  Object.assign(form, cat)
  showAddDialog.value = true
}

const saveCategory = async () => {
  try {
    if (form.id) {
      await adminApi.updateForumCategory(form.id, form)
    } else {
      await adminApi.createForumCategory(form)
    }
    ElMessage.success('保存成功')
    showAddDialog.value = false
    loadCategories()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const deleteCategory = async (cat: any) => {
  try {
    await ElMessageBox.confirm(`确定删除板块「${cat.name}」吗？`, '确认删除', { type: 'warning' })
    await adminApi.deleteForumCategory(cat.id)
    ElMessage.success('已删除')
    loadCategories()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: #2c3e50;
}
</style>
