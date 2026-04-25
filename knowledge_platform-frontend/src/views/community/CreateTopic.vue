<template>
  <Layout>
    <div class="create-topic-page">
      <div class="page-header">
        <el-button link @click="$router.push('/community')">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
        <h2>发布新帖子</h2>
      </div>

      <el-card class="form-card">
        <el-form 
          ref="formRef" 
          :model="form" 
          :rules="rules" 
          label-width="80px"
          label-position="top"
        >
          <el-form-item label="标题" prop="title">
            <el-input 
              v-model="form.title" 
              placeholder="请输入帖子标题" 
              maxlength="100" 
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="板块" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择发布板块" style="width: 100%">
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="标签" prop="tags">
            <el-select
              v-model="form.tags"
              multiple
              filterable
              allow-create
              default-first-option
              :reserve-keyword="false"
              placeholder="请输入或选择标签（按回车确认）"
              style="width: 100%"
            >
              <el-option
                v-for="item in existingTags"
                :key="item.id"
                :label="item.name"
                :value="item.name"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="15"
              placeholder="请输入帖子内容，支持 Markdown 语法..."
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitForm">发布</el-button>
            <el-button @click="$router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getCategories, createTopic, getTags, type ForumCategory, type ForumTag } from '@/api/forum'
import { ElMessage, type FormInstance } from 'element-plus'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const categories = ref<ForumCategory[]>([])
const existingTags = ref<ForumTag[]>([])

const form = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  content: '',
  tags: [] as string[]
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度在 5 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择板块', trigger: 'change' }
  ],
  tags: [
    { type: 'array', max: 5, message: '最多添加 5 个标签', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 10, message: '内容不能少于 10 个字符', trigger: 'blur' }
  ]
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

const fetchTags = async () => {
  try {
    const res = await getTags()
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      existingTags.value = res.data.data
    }
  } catch (error) {
    console.error('Failed to fetch tags:', error)
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const res = await createTopic({
          title: form.title,
          content: form.content,
          categoryId: form.categoryId!,
          tags: form.tags
        })
        
        if (res.data && (res.data.code === 200 || res.data.code === 0)) {
            ElMessage.success('发布成功')
            router.push({ name: 'TopicDetail', params: { id: res.data.data.id } })
          } else {
            ElMessage.error(res.data?.message || '发布失败')
          }
      } catch (error) {
        ElMessage.error('发布失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

onMounted(() => {
  fetchCategories()
  fetchTags()
})
</script>

<style scoped>
.create-topic-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.form-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  background: #fff;
  padding: 24px;
}
</style>
