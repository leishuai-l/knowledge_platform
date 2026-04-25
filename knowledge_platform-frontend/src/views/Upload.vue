<template>
  <Layout>
    <div class="upload-page">
      <div class="content-wrapper">
        <el-card class="upload-card">
          <template #header>
            <div class="card-header">
              <h2>上传文档</h2>
              <p>分享您的学习资料，帮助更多同学学习进步</p>
            </div>
          </template>

          <el-form
            ref="uploadFormRef"
            :model="uploadForm"
            :rules="uploadRules"
            label-width="100px"
            size="large"
          >
            <!-- 文件上传 -->
            <el-form-item label="选择文件">
              <el-upload
                ref="uploadRef"
                class="upload-dragger"
                drag
                :auto-upload="false"
                :multiple="false"
                :show-file-list="true"
                :accept="acceptedTypes"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :before-upload="beforeUpload"
              >
                <div class="upload-area">
                  <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                  <div class="el-upload__text">
                    将文件拖到此处，或 <em>点击上传</em>
                  </div>
                  <div class="el-upload__tip">
                    {{ uploadTipText }}
                  </div>
                </div>
              </el-upload>
            </el-form-item>

            <!-- 文档标题 -->
            <el-form-item label="文档标题" prop="title">
              <el-input
                v-model="uploadForm.title"
                placeholder="请输入文档标题"
                clearable
                maxlength="100"
                show-word-limit
              />
            </el-form-item>

            <!-- 文档描述 -->
            <el-form-item label="文档描述" prop="description">
              <div class="description-container">
                <el-input
                  v-model="uploadForm.description"
                  type="textarea"
                  :rows="4"
                  placeholder="请简要描述文档内容、适用场景等"
                  maxlength="500"
                  show-word-limit
                  style="width: 100%"
                />
                <el-button
                  type="primary"
                  link
                  class="ai-summary-btn"
                  :loading="generatingSummary"
                  :disabled="!fileInfo || !isAiSupportedFile()"
                  @click="handleAiSummary"
                >
                  <el-icon><MagicStick /></el-icon>
                  AI 一键填写
                </el-button>
              </div>
            </el-form-item>

            <!-- 选择分类 -->
            <el-form-item label="文档分类" prop="categoryId">
              <el-cascader
                v-model="uploadForm.categoryId"
                :options="categoryTree"
                :props="cascaderProps"
                placeholder="请选择文档分类"
                style="width: 100%"
                @change="handleCategoryChange"
              />
            </el-form-item>

            <!-- 标签 -->
            <el-form-item label="文档标签">
              <div class="tags-container">
                <el-select
                  v-model="selectedTagNames"
                  placeholder="添加标签"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  :reserve-keyword="false"
                  @change="handleTagsChange"
                  style="flex: 1;"
                  class="custom-tags-select"
                >
                  <el-option
                    v-for="tag in availableTags"
                    :key="tag.id"
                    :label="tag.name"
                    :value="tag.name"
                  >
                    <el-tag :color="tag.color" effect="light" size="small" class="option-tag" :style="{ color: isLightColor(tag.color) ? '#333' : '#fff', borderColor: tag.color }">
                      {{ tag.name }}
                    </el-tag>
                  </el-option>
                </el-select>
                
                <el-button 
                  type="primary" 
                  link
                  size="small"
                  :loading="generatingTags"
                  @click="handleAiTags"
                  style="margin-left: 10px;"
                >
                  <el-icon><MagicStick /></el-icon>
                  AI 生成
                </el-button>
              </div>
            </el-form-item>

            <!-- 下载积分 -->
            <el-form-item label="下载积分" prop="downloadPoints">
              <el-input-number
                v-model="uploadForm.downloadPoints"
                :min="0"
                :max="9999"
                controls-position="right"
                style="width: 200px;"
              />
              <span class="form-tip">建议根据文档价值设置合适的积分，0 积分表示免费</span>
            </el-form-item>

            <!-- 预览信息 -->
            <el-form-item v-if="fileInfo" label="文件信息">
              <div class="file-preview">
                <div class="file-icon">
                  <el-icon :size="40" :color="getFileTypeColor(fileInfo.extension)">
                    <component :is="getFileTypeIcon(fileInfo.extension)" />
                  </el-icon>
                </div>
                <div class="file-details">
                  <div class="file-name">{{ fileInfo.name }}</div>
                  <div class="file-meta">
                    {{ fileInfo.extension.toUpperCase() }} • {{ formatFileSize(fileInfo.size) }}
                  </div>
                </div>
              </div>
            </el-form-item>

            <!-- 提交按钮 -->
            <el-form-item>
              <div class="submit-actions">
                <el-button @click="resetForm" size="large">
                  重置
                </el-button>
                <el-button
                  type="primary"
                  @click="handleSubmit"
                  :loading="uploading"
                  size="large"
                >
                  {{ uploading ? '上传中...' : '提交上传' }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 上传进度 -->
        <el-card v-if="uploading" class="progress-card">
          <div class="upload-progress">
            <h3>上传进度</h3>
            <el-progress :percentage="uploadProgress" :status="uploadStatus" />
            <p class="progress-text">{{ progressText }}</p>
          </div>
        </el-card>

        <!-- 上传须知 -->
        <el-card class="notice-card">
          <template #header>
            <h3>上传须知</h3>
          </template>
          <ul class="notice-list">
            <li>请确保上传的文档内容健康、合法，不涉及版权争议</li>
            <li>文档将经过系统自动初审和管理员人工复审，通过后方可公开下载</li>
            <li>系统会自动检查文档格式、内容合规性以及文本相似度</li>
            <li>初审通过后可获得上传积分奖励，管理员复审通过后可获得额外积分</li>
            <li>建议为文档设置合理的下载积分，过高可能影响下载量</li>
            <li>请认真填写文档标题和描述，有助于其他用户找到您的资料</li>
            <li>严禁上传侵犯他人知识产权的文档，一经发现将严肃处理</li>
          </ul>
        </el-card>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadFile, type UploadInstance } from 'element-plus'
import { UploadFilled, Document as DocumentIcon, Picture, VideoPlay, Headset, FolderOpened, MagicStick } from '@element-plus/icons-vue'
import Layout from '@/components/Layout.vue'
import { documentApi } from '@/api/document'
import { categoryApi } from '@/api/category'
import { tagApi } from '@/api/tag'
import { useUserStore } from '@/stores/user'
import type { Category, Tag, DocumentUploadRequest, DocumentUploadResponse } from '@/types'
import type { FileTypeCapabilitiesResponse } from '@/api/document'

const router = useRouter()
const userStore = useUserStore()

const uploadFormRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const generatingSummary = ref(false)
const generatingTags = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref<'success' | 'exception' | 'warning' | undefined>()
const progressText = ref('')
const progressInterval = ref<ReturnType<typeof setInterval> | null>(null)

const categoryTree = ref<Category[]>([])
const availableTags = ref<Tag[]>([])
const fileTypeCapabilities = ref<FileTypeCapabilitiesResponse | null>(null)
const acceptedTypes = ref('')
const uploadTipText = ref('支持 PDF、Word、PowerPoint、Excel、图片、压缩包等格式，单个文件不超过 100MB')
const aiSupportedExtensions = ref(new Set<string>())
const selectedTags = ref<Tag[]>([])
const selectedTagNames = ref<string[]>([])
const fileInfo = ref<{
  name: string
  size: number
  extension: string
  file: File
} | null>(null)

const uploadForm = reactive<DocumentUploadRequest>({
  title: '',
  description: '',
  categoryId: 0,
  tags: [],
  downloadPoints: 0
})

const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

const isAiSupportedFile = () => {
  if (!fileInfo.value) {
    return false
  }
  return aiSupportedExtensions.value.has(fileInfo.value.extension)
}

const showAiUnsupportedMessage = () => {
  ElMessage.warning('该文件类型暂不支持 AI 一键填写，请手动填写摘要和标签')
}

const uploadRules: FormRules = {
  title: [
    { required: true, message: '请输入文档标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择文档分类', trigger: 'change' },
    { type: 'number', message: '请选择有效的文档分类', trigger: 'change', transform: (value) => Number(value) }
  ],
  downloadPoints: [
    { required: true, message: '请设置下载积分', trigger: 'blur' },
    { type: 'number', min: 0, max: 9999, message: '积分范围为 0-9999', trigger: 'blur' }
  ]
}

// 文件变化处理
const handleFileChange = (file: UploadFile) => {
  if (!file.raw) return

  const extension = file.name.split('.').pop()?.toLowerCase() || ''
  fileInfo.value = {
    name: file.name,
    size: file.raw.size,
    extension,
    file: file.raw
  }

  // 自动设置标题（去除扩展名）
  if (!uploadForm.title) {
    uploadForm.title = file.name.replace(/\.[^/.]+$/, '')
  }
}

// 文件移除处理
const handleFileRemove = () => {
  fileInfo.value = null
}

// 上传前检查
const beforeUpload = (file: File) => {
  const extension = file.name.split('.').pop()?.toLowerCase() || ''
  const allowedExtensions = fileTypeCapabilities.value?.uploadExtensions || []
  const maxFileSize = fileTypeCapabilities.value?.maxFileSize || 100 * 1024 * 1024
  const isValidType = allowedExtensions.includes(extension)
  const isValidSize = file.size <= maxFileSize

  if (!isValidType) {
    ElMessage.error('不支持的文件类型')
    return false
  }

  if (!isValidSize) {
    ElMessage.error(`文件大小不能超过 ${Math.floor(maxFileSize / 1024 / 1024)}MB`)
    return false
  }

  return true
}

// 分类变化
const handleCategoryChange = (value: any) => {
  console.log('Category changed:', value, 'categoryId:', uploadForm.categoryId)
}

// 处理标签变化 (支持自定义标签)
const handleTagsChange = (values: string[]) => {
  // 清空当前选中，重新构建
  selectedTags.value = []
  
  values.forEach(tagName => {
    // 尝试在现有标签中查找
    const existingTag = availableTags.value.find(t => t.name === tagName)
    
    if (existingTag) {
      selectedTags.value.push(existingTag)
    } else {
      // 创建新标签（临时对象）
      selectedTags.value.push({
        id: -Date.now() - Math.floor(Math.random() * 1000), // 临时ID
        name: tagName,
        color: '#409EFF', // 默认颜色
        usageCount: 0,
        createdAt: new Date().toISOString()
      })
    }
  })
  
  // 更新表单数据
  uploadForm.tags = selectedTags.value.map(t => t.name)
}

// AI 生成摘要
const handleAiSummary = async () => {
  if (!fileInfo.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  if (!isAiSupportedFile()) {
    showAiUnsupportedMessage()
    return
  }

  try {
    generatingSummary.value = true
    const response = await documentApi.generateAiSummary(fileInfo.value.file)
    const { summary } = response.data.data

    if (summary) {
      uploadForm.description = summary
      ElMessage.success('摘要生成成功')
    } else {
      ElMessage.warning('未能生成摘要，请稍后重试')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '摘要生成失败')
  } finally {
    generatingSummary.value = false
  }
}

// AI 生成标签
const handleAiTags = async () => {
  if (!fileInfo.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  if (!isAiSupportedFile()) {
    showAiUnsupportedMessage()
    return
  }

  try {
    generatingTags.value = true
    const response = await documentApi.generateAiTags(fileInfo.value.file)
    const { tags } = response.data.data

    if (tags && tags.length > 0) {
      // 合并标签，去重
      const currentTagNames = new Set(selectedTagNames.value)
      let addedCount = 0

      tags.forEach((tagName: string) => {
        if (!currentTagNames.has(tagName)) {
          selectedTagNames.value.push(tagName)
          addedCount++
        }
      })

      // 触发更新
      handleTagsChange(selectedTagNames.value)

      if (addedCount > 0) {
        ElMessage.success(`成功生成 ${addedCount} 个新标签`)
      } else {
        ElMessage.info('生成的标签已存在')
      }
    } else {
      ElMessage.warning('未能提取到有效标签')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '标签生成失败')
  } finally {
    generatingTags.value = false
  }
}

// 移除标签
const removeTag = (tag: Tag) => {
  selectedTags.value = selectedTags.value.filter(t => t.id !== tag.id)
  selectedTagNames.value = selectedTags.value.map(t => t.name)
  uploadForm.tags = selectedTags.value.map(t => t.name)
}

// 根据文件扩展名获取图标
const getFileTypeIcon = (extension: string) => {
  const ext = extension.toLowerCase()
  if (['pdf', 'doc', 'docx', 'txt'].includes(ext)) return DocumentIcon
  if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) return Picture
  if (['mp4', 'avi', 'mov'].includes(ext)) return VideoPlay
  if (['mp3', 'wav', 'aac'].includes(ext)) return Headset
  if (['zip', 'rar', '7z'].includes(ext)) return FolderOpened
  return DocumentIcon
}

// 根据文件扩展名获取颜色
const getFileTypeColor = (extension: string) => {
  const ext = extension.toLowerCase()
  if (ext === 'pdf') return '#FF6B6B'
  if (['doc', 'docx'].includes(ext)) return '#4DABF7'
  if (['xls', 'xlsx'].includes(ext)) return '#51CF66'
  if (['ppt', 'pptx'].includes(ext)) return '#FF922B'
  if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) return '#9775FA'
  if (['zip', 'rar', '7z'].includes(ext)) return '#868E96'
  return '#409EFF'
}

// 文件大小格式化
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 判断颜色是否为浅色
const isLightColor = (hexColor: string) => {
  if (!hexColor) return false
  const hex = hexColor.replace('#', '')
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)
  const brightness = (r * 299 + g * 587 + b * 114) / 1000
  return brightness > 128
}

const getUploadSuccessState = (result: DocumentUploadResponse) => {
  switch (result.initialReviewOutcome) {
    case 'PASSED':
      return {
        status: 'success' as const,
        progressText: '上传成功，已通过初审',
        message: '文档已上传并通过初审，等待管理员复审',
      }
    case 'REJECTED':
      return {
        status: 'warning' as const,
        progressText: '文档已保存，初审未通过',
        message: result.rejectionReason
          ? `文档已上传，但初审未通过：${result.rejectionReason}`
          : '文档已上传，但初审未通过，请到我的上传查看原因',
      }
    case 'SYSTEM_REJECTED':
    default:
      return {
        status: 'warning' as const,
        progressText: '文档已保存，请查看上传状态',
        message: '文档已上传，但初审暂未完成，请到我的上传查看状态，不要重复上传',
      }
  }
}

// 提交上传
const handleSubmit = async () => {
  if (!uploadFormRef.value) return

  // 检查是否选择了文件
  if (!fileInfo.value) {
    ElMessage.error('请选择要上传的文件')
    return
  }

  // 检查分类是否选择
  if (Number(uploadForm.categoryId) <= 0) {
    ElMessage.error('请选择文档分类')
    return
  }

  try {
    await uploadFormRef.value.validate()

    uploading.value = true
    uploadProgress.value = 0
    uploadStatus.value = undefined
    progressText.value = '准备上传...'

    const formData = new FormData()
    formData.append('file', fileInfo.value.file)
    formData.append('title', uploadForm.title)
    formData.append('description', uploadForm.description || '')
    formData.append('categoryId', uploadForm.categoryId.toString())
    formData.append('downloadPoints', (uploadForm.downloadPoints || 0).toString())
    if (uploadForm.tags?.length) {
      formData.append('tags', uploadForm.tags.join(','))
    }

    // 开始进度条模拟
    if (progressInterval.value) {
      clearInterval(progressInterval.value)
    }
    progressInterval.value = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += Math.random() * 10
        progressText.value = `上传中... ${uploadProgress.value.toFixed(0)}%`
      }
    }, 200)

    try {
      const response = await documentApi.uploadDocument(formData)
      const result = response.data.data
      const successState = getUploadSuccessState(result)

      if (progressInterval.value) {
        clearInterval(progressInterval.value)
        progressInterval.value = null
      }
      uploadProgress.value = 100
      uploadStatus.value = successState.status
      progressText.value = successState.progressText

      await userStore.getCurrentUser().catch(() => undefined)
      ElMessage[successState.status === 'success' ? 'success' : 'warning'](successState.message)

      if (result.shouldRedirectToMyUploads) {
        setTimeout(() => {
          router.push('/profile')
        }, 2000)
      }
    } catch (error: any) {
      if (progressInterval.value) {
        clearInterval(progressInterval.value)
        progressInterval.value = null
      }
      uploadStatus.value = 'exception'
      progressText.value = '上传失败'
      ElMessage.error(error.message || '上传失败')
    }

  } catch (error: any) {
    uploadStatus.value = 'exception'
    progressText.value = '上传失败'
    ElMessage.error(error.message || '上传失败')
  } finally {
    setTimeout(() => {
      uploading.value = false
    }, 1000)
  }
}

// 重置表单
const resetForm = () => {
  uploadFormRef.value?.resetFields()
  uploadRef.value?.clearFiles()
  selectedTags.value = []
  selectedTagNames.value = []
  fileInfo.value = null
  Object.assign(uploadForm, {
    title: '',
    description: '',
    categoryId: 0,
    tags: [],
    downloadPoints: 0
  })
}

const loadFileTypeCapabilities = async () => {
  try {
    const response = await documentApi.getFileTypeCapabilities()
    fileTypeCapabilities.value = response.data.data
    acceptedTypes.value = response.data.data.uploadAccept || ''
    uploadTipText.value = response.data.data.uploadTipText || uploadTipText.value
    aiSupportedExtensions.value = new Set(response.data.data.aiSupportedExtensions || [])
  } catch (error) {
    console.error('Failed to load file type capabilities:', error)
  }
}

// 加载数据
const loadData = async () => {
  try {
    const [categoriesRes, tagsRes] = await Promise.all([
      categoryApi.getCategoryTree(),
      tagApi.getAllTags(),
      loadFileTypeCapabilities()
    ])

    categoryTree.value = categoriesRes.data.data
    availableTags.value = tagsRes.data.data || []

    console.log('加载到的标签:', availableTags.value)
  } catch (error) {
    console.error('Failed to load data:', error)
    ElMessage.error('加载数据失败，请稍后重试')
  }
}

onMounted(() => {
  loadData()
})

// 组件卸载时清除定时器
onBeforeUnmount(() => {
  if (progressInterval.value) {
    clearInterval(progressInterval.value)
    progressInterval.value = null
  }
})
</script>

<style scoped>
.upload-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.upload-card {
  margin-bottom: 20px;
}

.card-header {
  text-align: center;
}

.card-header h2 {
  margin: 0 0 10px;
  color: #2c3e50;
}

.card-header p {
  margin: 0;
  color: #666;
}

.upload-dragger {
  width: 100%;
}

.upload-area {
  padding: 40px;
  text-align: center;
}

.el-icon--upload {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 16px;
}

.el-upload__text {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.el-upload__tip {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.description-container {
  position: relative;
  width: 100%;
}

.ai-summary-btn {
  position: absolute;
  bottom: 5px;
  right: 65px;
  padding: 0;
  height: auto;
  font-size: 12px;
  color: #409EFF;
  background-color: transparent;
  padding: 2px 6px;
  border-radius: 4px;
}

.ai-summary-btn:hover {
  color: #66b1ff;
}



/* 自定义标签选择器样式 */
.custom-tags-select :deep(.el-tag) {
  background-color: #909399;
  color: #ffffff;
  border-color: #909399;
}

.custom-tags-select :deep(.el-tag__content) {
  color: #ffffff;
}

.custom-tags-select :deep(.el-tag__close) {
  color: #ffffff;
}

.custom-tags-select :deep(.el-tag__close:hover) {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 标签选项样式 */
.option-tag {
  border-width: 1px;
  border-style: solid;
}

.tags-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-tip {
  color: #999;
  font-size: 12px;
  margin-top: 5px;
  line-height: 1.5;
}

.ai-action-container {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.file-details .file-name {
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 5px;
}

.file-details .file-meta {
  font-size: 12px;
  color: #666;
}

.submit-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

.progress-card {
  margin-bottom: 20px;
}

.upload-progress {
  text-align: center;
}

.upload-progress h3 {
  margin: 0 0 20px;
  color: #2c3e50;
}

.progress-text {
  margin: 10px 0 0;
  color: #666;
  font-size: 14px;
}

.notice-card {
  background: #f8f9fa;
}

.notice-list {
  margin: 0;
  padding-left: 20px;
  color: #666;
  line-height: 1.8;
}

.notice-list li {
  margin-bottom: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .upload-area {
    padding: 20px;
  }

  .tags-container {
    flex-direction: column;
    align-items: stretch;
  }

  .submit-actions {
    flex-direction: column;
  }

  .submit-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>