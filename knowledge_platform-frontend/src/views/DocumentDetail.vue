<template>
  <Layout>
    <div class="document-detail-page">
      <div class="content-wrapper">
        <el-skeleton v-if="loading" animated>
          <template #template>
            <el-skeleton-item variant="h1" />
            <el-skeleton-item variant="text" />
            <el-skeleton-item variant="text" />
            <el-skeleton-item variant="text" />
          </template>
        </el-skeleton>

        <div v-else-if="document" class="document-detail">
          <!-- 返回按钮 -->
          <div class="back-button-wrapper">
            <el-button :icon="ArrowLeft" @click="goBack" plain>返回文档库</el-button>
          </div>

          <!-- 文档信息卡片 -->
          <el-card class="document-info-card">
            <div class="document-header">
              <div class="file-icon">
                <el-icon :size="60" :color="getFileTypeColor(document.fileExtension)">
                  <component :is="getFileTypeIcon(document.fileExtension)" />
                </el-icon>
              </div>

              <div class="document-main-info">
                <h1 class="document-title">{{ document.title }}</h1>
                <div class="document-meta">
                  <el-tag :color="getCategoryColor(document.category?.name)">
                    {{ document.category?.name }}
                  </el-tag>
                  <span class="file-info">
                    {{ document.fileExtension.toUpperCase() }} • {{ formatFileSize(document.fileSize) }}
                  </span>
                  <span class="upload-info">
                    {{ formatDate(document.createdAt) }} •
                    {{ document.uploader?.username }}
                  </span>
                </div>

                <div class="rating-stats">
                  <el-rate :model-value="document.ratingAverage" disabled show-score />
                  <span class="stats-text">
                    ({{ document.ratingCount }} 人评价，{{ document.downloadCount }} 次下载)
                  </span>
                </div>
              </div>

              <div class="action-buttons">
                <template v-if="userStore.isLoggedIn">
                  <el-button
                    type="primary"
                    size="large"
                    :loading="downloadLoading"
                    @click="handleDownload"
                    :disabled="userStore.user?.points! < document.downloadPoints"
                  >
                    <el-icon><Download /></el-icon>
                    下载 ({{ document.downloadPoints }} 积分)
                  </el-button>
                </template>
                <template v-else>
                  <el-button
                    type="primary"
                    size="large"
                    @click="goToLogin"
                  >
                    <el-icon><Download /></el-icon>
                    登录后下载
                  </el-button>
                </template>
                <el-button size="large" @click="handlePreview" v-if="canPreview">
                  <el-icon><ZoomIn /></el-icon>
                  预览
                </el-button>
                <el-button size="large" @click="handleChatWithAi">
                  <el-icon><ChatDotRound /></el-icon>
                  AI 对话
                </el-button>
                <el-button size="large" @click="handleReport" v-if="userStore.isLoggedIn">
                  <el-icon><Warning /></el-icon>
                  举报反馈
                </el-button>
              </div>
            </div>

            <el-divider />

            <div class="document-description" v-if="document.description">
              <h3>文档描述</h3>
              <p>{{ document.description }}</p>
            </div>
            
            <!-- AI Summary Section: Only show for text-extractable file types -->
            <div class="document-ai-summary" v-if="isAiSupported && (document.aiSummary || isAiProcessing)">
              <h3><el-icon class="ai-icon"><MagicStick /></el-icon> AI 智能摘要</h3>

              <div class="ai-summary-content" v-if="document.aiSummary">
                {{ document.aiSummary }}
              </div>

              <div class="ai-processing" v-else-if="isAiProcessing">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>AI 正在分析文档内容，生成摘要和标签...</span>
              </div>
            </div>

            <div class="document-tags" v-if="document.tags?.length">
              <h3>标签</h3>
              <el-tag
                v-for="tag in document.tags"
                :key="tag.id"
                :color="tag.color"
                effect="light"
                class="white-text-tag"
                style="margin-right: 8px; margin-bottom: 8px;"
              >
                {{ tag.name }}
              </el-tag>
            </div>
          </el-card>

          <!-- 评分和评论 -->
          <div class="rating-comments-section">
            <el-row :gutter="20">
              <!-- 评分区域 -->
              <el-col :span="8" :xs="24">
                <el-card class="rating-card">
                  <template #header>
                    <span>文档评价</span>
                  </template>

                  <div class="rating-overview">
                    <div class="rating-score">
                      <span class="score">{{ document.ratingAverage.toFixed(1) }}</span>
                      <el-rate :model-value="document.ratingAverage" disabled />
                      <span class="count">{{ document.ratingCount }} 人评价</span>
                    </div>
                  </div>

                  <!-- 用户评分 -->
                  <div class="user-rating" v-if="userStore.isLoggedIn && hasDownloaded">
                    <el-divider />
                    <h4>为此文档评分</h4>
                    <el-rate
                      v-model="userRating"
                      :disabled="ratingLoading"
                      @change="handleRate"
                    />
                  </div>

                  <!-- 未登录或未下载提示 -->
                  <div class="rating-tip" v-if="!userStore.isLoggedIn">
                    <el-divider />
                    <el-alert
                      title="登录后下载文档即可评分"
                      type="info"
                      :closable="false"
                      show-icon
                    >
                      <template #default>
                        <el-button type="primary" size="small" @click="goToLogin">立即登录</el-button>
                      </template>
                    </el-alert>
                  </div>
                  <div class="rating-tip" v-else-if="!hasDownloaded">
                    <el-divider />
                    <el-alert
                      title="下载文档后即可评分"
                      type="info"
                      :closable="false"
                      show-icon
                    />
                  </div>
                </el-card>
              </el-col>

              <!-- 评论区域 -->
              <el-col :span="16" :xs="24">
                <el-card class="comments-card">
                  <template #header>
                    <span>用户评论 ({{ commentTotal }})</span>
                  </template>

                  <!-- 评论输入 -->
                  <div class="comment-input" v-if="userStore.isLoggedIn && hasDownloaded">
                    <el-input
                      v-model="commentText"
                      type="textarea"
                      :rows="3"
                      placeholder="分享你的使用心得..."
                      maxlength="500"
                      show-word-limit
                    />
                    <div class="comment-actions">
                      <el-button
                        type="primary"
                        @click="handleComment"
                        :loading="commentLoading"
                        :disabled="!commentText.trim()"
                      >
                        发表评论
                      </el-button>
                    </div>
                  </div>

                  <!-- 未登录或未下载评论提示 -->
                  <div class="comment-tip" v-if="!userStore.isLoggedIn">
                    <el-alert
                      title="登录后下载文档即可发表评论"
                      type="info"
                      :closable="false"
                      show-icon
                    >
                      <template #default>
                        <el-button type="primary" size="small" @click="goToLogin">立即登录</el-button>
                      </template>
                    </el-alert>
                  </div>
                  <div class="comment-tip" v-else-if="!hasDownloaded">
                    <el-alert
                      title="下载文档后即可发表评论"
                      type="info"
                      :closable="false"
                      show-icon
                    />
                  </div>

                  <div class="comments-list">
                    <CommentItem
                      v-for="comment in comments"
                      :key="comment.id"
                      :comment="comment"
                      @reply="handleReply"
                    />

                    <el-empty
                      v-if="!comments.length"
                      description="暂无评论"
                      :image-size="100"
                    />
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>

        <el-empty v-else description="文档不存在" />
      </div>
    </div>


    <!-- 举报反馈对话框 -->
    <el-dialog
      v-model="reportDialogVisible"
      title="举报反馈"
      width="650px"
      :close-on-click-modal="false"
      @close="resetReportForm"
    >
      <el-form
        ref="reportFormRef"
        :model="reportForm"
        :rules="reportRules"
        label-width="100px"
      >
        <el-form-item label="举报类型" prop="reportType">
          <el-select v-model="reportForm.reportType" placeholder="请选择举报类型" style="width: 100%">
            <el-option label="版权侵权" value="COPYRIGHT_INFRINGEMENT" />
            <el-option label="抄袭剽窃" value="PLAGIARISM" />
            <el-option label="违法内容" value="ILLEGAL_CONTENT" />
            <el-option label="不当内容" value="INAPPROPRIATE_CONTENT" />
            <el-option label="敏感内容" value="SENSITIVE_CONTENT" />
            <el-option label="虚假信息" value="FALSE_INFORMATION" />
            <el-option label="垃圾广告" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述举报原因（至少20字）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="证据材料">
          <div class="evidence-section">
            <div class="evidence-item">
              <div class="evidence-header">
                <span>图片证据</span>
                <span class="evidence-tip">支持 JPG/PNG，最多5张，每张不超过5MB</span>
              </div>
              <el-upload
                :auto-upload="false"
                :on-change="handleImageUpload"
                :show-file-list="false"
                accept="image/jpeg,image/png"
                multiple
              >
                <el-button size="small" type="primary" plain>
                  <el-icon><Plus /></el-icon>
                  上传图片
                </el-button>
              </el-upload>

              <div class="image-preview-list" v-if="reportForm.uploadedImages.length">
                <div
                  v-for="(img, index) in reportForm.uploadedImages"
                  :key="index"
                  class="image-preview-item"
                >
                  <el-image
                    :src="`/api/copyright/reports/evidence/view?path=${encodeURIComponent(img)}`"
                    fit="cover"
                    :preview-src-list="reportForm.uploadedImages.map(i => `/api/copyright/reports/evidence/view?path=${encodeURIComponent(i)}`)"
                  />
                  <el-icon class="remove-icon" @click="removeImage(index)">
                    <CircleClose />
                  </el-icon>
                </div>
              </div>
            </div>

            <div class="evidence-item">
              <div class="evidence-header">
                <span>文件证据</span>
                <span class="evidence-tip">支持 PDF/Word，最多3个，每个不超过10MB</span>
              </div>
              <el-upload
                :auto-upload="false"
                :on-change="handleDocUpload"
                :show-file-list="false"
                accept=".pdf,.doc,.docx"
              >
                <el-button size="small" type="primary" plain>
                  <el-icon><DocumentIcon /></el-icon>
                  上传文件
                </el-button>
              </el-upload>

              <div class="file-list" v-if="reportForm.documentFiles.length">
                <div
                  v-for="(file, index) in reportForm.documentFiles"
                  :key="index"
                  class="file-item"
                >
                  <el-icon><DocumentIcon /></el-icon>
                  <span class="file-name">{{ file.name }}</span>
                  <el-icon class="remove-icon" @click="removeDoc(index)">
                    <Close />
                  </el-icon>
                </div>
              </div>
            </div>

            <div class="evidence-item">
              <div class="evidence-header">
                <span>链接证据</span>
                <span class="evidence-tip">多个链接用逗号分隔</span>
              </div>
              <el-input
                v-model="reportForm.evidenceLinks"
                placeholder="https://example.com/proof1, https://example.com/proof2"
              />
            </div>
          </div>
        </el-form-item>

        <el-form-item label="联系方式">
          <el-input
            v-model="reportForm.contactInfo"
            placeholder="可选，便于后续沟通（邮箱或手机号）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitReport"
          :loading="reportSubmitting"
        >
          提交举报
        </el-button>
      </template>
    </el-dialog>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, UploadFile } from 'element-plus'
import {
  Document as DocumentIcon,
  Picture,
  VideoPlay,
  Headset,
  FolderOpened,
  Download,
  ZoomIn,
  MagicStick,
  Loading,
  ChatDotRound,
  Warning,
  Plus,
  CircleClose,
  Close,
  ArrowLeft
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { documentApi } from '@/api/document'
import { ratingApi } from '@/api/rating'
import { commentApi } from '@/api/comment'
import { reportApi } from '@/api/report'
import Layout from '@/components/Layout.vue'
import CommentItem from '@/components/CommentItem.vue'
import type { Document } from '@/types'
import type { Comment } from '@/api/comment'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const downloadLoading = ref(false)
const ratingLoading = ref(false)
const commentLoading = ref(false)

const document = ref<Document | null>(null)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const userRating = ref(0)
const commentText = ref('')
const hasDownloaded = ref(false)
const aiPollingTimer = ref<ReturnType<typeof globalThis.setTimeout> | null>(null)
const aiPollingAttempts = ref(0)
const maxAiPollingAttempts = 20

const documentId = computed(() => Number(route.params.id))

const isAiProcessing = computed(() => {
  return document.value &&
         !document.value.aiSummary &&
         (document.value.aiAnalysisStatus === 'PENDING' || document.value.aiAnalysisStatus === 'PROCESSING')
})

// Check if file type supports AI text extraction
const isAiSupported = computed(() => {
  if (!document.value) return false
  const ext = document.value.fileExtension?.toLowerCase() || ''
  const supportedExts = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt']
  return supportedExts.includes(ext)
})

const canPreview = computed(() => {
  // 已批准的文档始终显示预览按钮，预览页面会处理不支持的情况
  return document.value?.status === 'APPROVED'
})

// 根据文件扩展名获取图标
const getFileTypeIcon = (extension: string) => {
  const ext = extension.toLowerCase()
  if (['pdf', 'doc', 'docx', 'txt', 'rtf'].includes(ext)) return DocumentIcon
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg'].includes(ext)) return Picture
  if (['mp4', 'avi', 'mov', 'wmv', 'flv'].includes(ext)) return VideoPlay
  if (['mp3', 'wav', 'aac', 'flac'].includes(ext)) return Headset
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return FolderOpened
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

// 根据分类获取颜色
const getCategoryColor = (categoryName?: string) => {
  if (!categoryName) return '#409EFF'
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  const index = categoryName.length % colors.length
  return colors[index]
}

// 文件大小格式化
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 日期格式化
const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

// 跳转到登录页
const goToLogin = () => {
  router.push({
    name: 'Login',
    query: { redirect: route.fullPath }
  })
}

// 返回文档库
const goBack = () => {
  router.push('/documents')
}

// 下载文档
const handleDownload = async () => {
  if (!userStore.isLoggedIn) {
    goToLogin()
    return
  }

  if (userStore.user!.points < document.value!.downloadPoints) {
    ElMessage.error('积分不足，无法下载')
    return
  }

  try {
    downloadLoading.value = true

    await ElMessageBox.confirm(
      `下载此文档需要消费 ${document.value!.downloadPoints} 积分，确定要下载吗？`,
      '确认下载',
      { type: 'warning' }
    )

    const response = await documentApi.downloadDocument(documentId.value)

    // 创建下载链接
    const blob = response.data as unknown as Blob
    const url = globalThis.URL.createObjectURL(blob)
    const link = globalThis.document.createElement('a')
    link.href = url
    link.download = document.value!.fileName
    link.click()
    globalThis.URL.revokeObjectURL(url)

    // 刷新用户信息（获取最新积分）
    try {
      await userStore.getCurrentUser()
    } catch (error) {
      console.warn('获取用户信息失败，但下载成功:', error)
      // 如果获取用户信息失败，手动更新积分作为后备
      userStore.updateUserPoints(userStore.user!.points - document.value!.downloadPoints)
    }

    // 重新加载文档信息以更新下载计数
    await loadDocument()
    // 重新检查下载状态以确保状态同步
    await checkDownloadStatus()

    ElMessage.success('下载成功')
  } catch (error: any) {
    if (error.message !== 'cancel') {
      ElMessage.error(error.message || '下载失败')
    }
  } finally {
    downloadLoading.value = false
  }
}

// 预览文档
const handlePreview = () => {
  if (!document.value || !canPreview.value) {
    ElMessage.warning('此文档不支持预览')
    return
  }

  router.push({
    name: 'DocumentPreview',
    params: { id: documentId.value }
  })
}

// 跳转到 AI 对话
const handleChatWithAi = () => {
  router.push({
    path: '/ai',
    query: {
      documentId: documentId.value,
      title: document.value?.title
    }
  })
}

// 跳转到侵权举报
const handleReport = () => {
  reportDialogVisible.value = true
}

// 举报对话框相关
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportFormRef = ref<FormInstance>()
const reportForm = ref<{
  reportType: import('@/api/report').ReportType | ''
  description: string
  contactInfo: string
  evidenceLinks: string
  uploadedImages: string[]
  uploadedDocs: string[]
  documentFiles: { name: string; path: string }[]
}>({
  reportType: '',
  description: '',
  contactInfo: '',
  evidenceLinks: '',
  uploadedImages: [],
  uploadedDocs: [],
  documentFiles: []
})

const reportRules = {
  reportType: [{ required: true, message: '请选择举报类型', trigger: 'change' }],
  description: [
    { required: true, message: '请填写详细描述', trigger: 'blur' },
    { min: 20, message: '描述至少20字', trigger: 'blur' },
    { max: 500, message: '描述最多500字', trigger: 'blur' }
  ]
}

// 图片上传处理
const handleImageUpload = async (uploadFile: UploadFile) => {
  if (reportForm.value.uploadedImages.length >= 5) {
    ElMessage.warning('最多上传5张图片')
    return
  }

  const file = uploadFile.raw
  if (!file) return

  const isImage = ['image/jpeg', 'image/png'].includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只支持 JPG/PNG 格式')
    return
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', 'image')

    const res = await reportApi.uploadEvidence(formData)
    reportForm.value.uploadedImages.push(res.data.data.path)
    ElMessage.success('图片上传成功')
  } catch (error: any) {
    ElMessage.error(error.message || '图片上传失败')
  }
}

// 文档上传处理
const handleDocUpload = async (uploadFile: UploadFile) => {
  if (reportForm.value.documentFiles.length >= 3) {
    ElMessage.warning('最多上传3个文件')
    return
  }

  const file = uploadFile.raw
  if (!file) return

  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ]
  const isAllowed = allowedTypes.includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isAllowed) {
    ElMessage.error('只支持 PDF/Word 格式')
    return
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', 'file')

    const res = await reportApi.uploadEvidence(formData)
    reportForm.value.uploadedDocs.push(res.data.data.path)
    reportForm.value.documentFiles.push({ name: file.name, path: res.data.data.path })
    ElMessage.success('文件上传成功')
  } catch (error: any) {
    ElMessage.error(error.message || '文件上传失败')
  }
}

// 删除已上传的图片
const removeImage = (index: number) => {
  reportForm.value.uploadedImages.splice(index, 1)
}

// 删除已上传的文档
const removeDoc = (index: number) => {
  reportForm.value.uploadedDocs.splice(index, 1)
  reportForm.value.documentFiles.splice(index, 1)
}

// 提交举报
const submitReport = async () => {
  if (!reportFormRef.value || !reportForm.value.reportType) {
    return
  }

  await reportFormRef.value.validate()

  reportSubmitting.value = true

  try {
    const submitData = {
      documentId: documentId.value,
      reportType: reportForm.value.reportType,
      description: reportForm.value.description,
      contactInfo: reportForm.value.contactInfo,
      evidenceLinks: reportForm.value.evidenceLinks
        ? reportForm.value.evidenceLinks.split(',').map(s => s.trim()).filter(Boolean)
        : [],
      imageEvidences: reportForm.value.uploadedImages,
      fileEvidences: reportForm.value.uploadedDocs
    }

    await reportApi.submitReport(submitData)

    ElMessage.success('举报提交成功，我们会尽快处理')
    reportDialogVisible.value = false
    resetReportForm()
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败，请稍后重试')
  } finally {
    reportSubmitting.value = false
  }
}

// 重置表单
const resetReportForm = () => {
  reportFormRef.value?.resetFields()
  reportForm.value.reportType = ''
  reportForm.value.description = ''
  reportForm.value.contactInfo = ''
  reportForm.value.evidenceLinks = ''
  reportForm.value.uploadedImages = []
  reportForm.value.uploadedDocs = []
  reportForm.value.documentFiles = []
}

// 评分
const handleRate = async (rating: number) => {
  if (!rating) return

  try {
    ratingLoading.value = true
    await ratingApi.createRating({ documentId: documentId.value, score: rating })
    ElMessage.success('评分成功')
    await loadDocument() // 重新加载文档信息
  } catch (error: any) {
    ElMessage.error(error.message || '评分失败')
    userRating.value = 0
  } finally {
    ratingLoading.value = false
  }
}

// 发表评论
const handleComment = async () => {
  if (!commentText.value.trim()) return

  try {
    commentLoading.value = true
    await commentApi.createComment({
      documentId: documentId.value,
      content: commentText.value.trim()
    })
    commentText.value = ''
    ElMessage.success('评论成功')
    await loadComments() // 重新加载评论
  } catch (error: any) {
    ElMessage.error(error.message || '评论失败')
  } finally {
    commentLoading.value = false
  }
}

// 回复评论
const handleReply = (parentComment: Comment) => {
  commentText.value = `@${parentComment.user?.username} `
}

const stopAiPolling = () => {
  if (aiPollingTimer.value !== null) {
    globalThis.clearTimeout(aiPollingTimer.value)
    aiPollingTimer.value = null
  }
}

const scheduleAiPolling = () => {
  if (!isAiProcessing.value || aiPollingAttempts.value >= maxAiPollingAttempts) {
    stopAiPolling()
    return
  }

  stopAiPolling()
  aiPollingTimer.value = globalThis.setTimeout(async () => {
    aiPollingAttempts.value += 1
    await loadDocument(false)
  }, 3000)
}

const loadUserRating = async () => {
  if (!userStore.isLoggedIn || !hasDownloaded.value) {
    userRating.value = 0
    return
  }

  try {
    const response = await ratingApi.getUserRatingForDocument(documentId.value)
    const rating = response.data.data as { rating?: number; score?: number } | null
    userRating.value = rating?.rating ?? rating?.score ?? 0
  } catch {
    userRating.value = 0
  }
}

// 加载预览能力信息（可选，用于日志或调试）
const loadPreviewCapability = async () => {
  if (!document.value) return

  try {
    await documentApi.getDocumentPreviewInfo(documentId.value)
  } catch (error: any) {
    console.warn('预览检查失败:', error?.message || '未知错误')
  }
}

// 加载文档详情
const loadDocument = async (resetAiPolling = true) => {
  try {
    const response = await documentApi.getDocument(documentId.value)
    document.value = response.data.data
    await loadPreviewCapability()

    if (resetAiPolling) {
      aiPollingAttempts.value = 0
    }

    if (isAiProcessing.value) {
      scheduleAiPolling()
    } else {
      stopAiPolling()
    }
  } catch (error: any) {
    stopAiPolling()
    ElMessage.warning(error.message || '该文档不存在或已被删除')
    router.replace('/documents')
  }
}

// 检查下载状态
const checkDownloadStatus = async () => {
  if (!userStore.isLoggedIn) {
    hasDownloaded.value = false
    return
  }

  try {
    const response = await documentApi.checkDownloadStatus(documentId.value)
    hasDownloaded.value = response.data.data.hasDownloaded
  } catch (error) {
    console.error('检查下载状态失败:', error)
    hasDownloaded.value = false
  }
}

// 加载评论
const loadComments = async () => {
  try {
    const response = await commentApi.getDocumentComments(documentId.value)
    const pageData = response.data.data as any
    comments.value = pageData?.list || pageData?.content || []
    commentTotal.value = pageData?.total || pageData?.totalElements || comments.value.length
  } catch (error) {
    console.error('Failed to load comments:', error)
    comments.value = []
    commentTotal.value = 0
  }
}

// 监听用户登录状态变化
watch(() => userStore.isLoggedIn, async (newVal) => {
  if (newVal) {
    await checkDownloadStatus()
    await loadUserRating()
  } else {
    hasDownloaded.value = false
    userRating.value = 0
  }
})

watch(hasDownloaded, async (downloaded) => {
  if (downloaded) {
    await loadUserRating()
  } else {
    userRating.value = 0
  }
})

onMounted(async () => {
  await loadDocument()
  await Promise.all([
    loadComments(),
    checkDownloadStatus()
  ])
  await loadUserRating()
  loading.value = false
})

onUnmounted(() => {
  stopAiPolling()
})
</script>

<style scoped>
.document-detail-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.back-button-wrapper {
  margin-bottom: 16px;
}

.document-info-card {
  margin-bottom: 20px;
}

.document-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.file-icon {
  flex-shrink: 0;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 12px;
}

.document-main-info {
  flex: 1;
  min-width: 0;
}

.document-title {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 15px;
  line-height: 1.4;
}

.document-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.file-info,
.upload-info {
  color: #666;
  font-size: 14px;
}

.rating-stats {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-text {
  color: #666;
  font-size: 14px;
}

.action-buttons {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.document-description h3,
.document-tags h3,
.document-ai-summary h3 {
  color: #2c3e50;
  margin: 0 0 15px;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.document-ai-summary {
  margin-top: 20px;
  padding: 15px;
  background-color: #f0f9eb;
  border-radius: 8px;
  border-left: 4px solid #67C23A;
}

.ai-summary-content {
  color: #5e6d82;
  line-height: 1.6;
  font-size: 14px;
}

.ai-icon {
  color: #67C23A;
}

.ai-processing {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #909399;
  font-size: 14px;
  padding: 10px 0;
}

.ai-processing .el-icon {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.document-description p {
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.white-text-tag {
  color: white !important;
}

.rating-comments-section {
  margin-top: 20px;
}

.rating-card,
.comments-card {
  height: fit-content;
}

.rating-overview {
  text-align: center;
  padding: 20px 0;
}

.rating-score .score {
  font-size: 48px;
  font-weight: 600;
  color: #409EFF;
  display: block;
  line-height: 1;
}

.rating-score .count {
  color: #666;
  font-size: 14px;
  margin-top: 10px;
  display: block;
}

.user-rating h4 {
  margin: 15px 0;
  color: #2c3e50;
}

.comment-input {
  margin-bottom: 20px;
}

.comment-actions {
  margin-top: 10px;
  text-align: right;
}

.comments-list {
  max-height: 600px;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .document-header {
    flex-direction: column;
    gap: 15px;
  }

  .document-main-info {
    order: 1;
  }

  .file-icon {
    order: 2;
    align-self: center;
  }

  .action-buttons {
    order: 3;
    flex-direction: row;
    width: 100%;
  }

  .action-buttons :deep(.el-button) {
    flex: 1;
  }

  .document-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .rating-stats {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
}

/* 预览对话框样式 */
:deep(.preview-dialog) {
  .el-dialog {
    margin-top: 5vh !important;
    margin-bottom: 5vh !important;
    height: 90vh;
    max-height: 90vh;
  }

  .el-dialog__body {
    padding: 10px;
    height: calc(90vh - 120px);
  }

  .preview-content {
    height: 100%;
  }
}

@media (max-width: 768px) {
  :deep(.preview-dialog) {
    .el-dialog {
      margin: 0 !important;
      height: 100vh !important;
      max-height: 100vh !important;
      width: 100% !important;
      border-radius: 0 !important;
    }

    .el-dialog__body {
      height: calc(100vh - 120px);
    }
  }
}

/* 举报反馈对话框样式 */
.evidence-section {
  width: 100%;

  .evidence-item {
    margin-bottom: 16px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;

    .evidence-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      span:first-child {
        font-weight: 500;
        color: #303133;
      }

      .evidence-tip {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .image-preview-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;

    .image-preview-item {
      position: relative;
      width: 80px;
      height: 80px;
      border-radius: 4px;
      overflow: hidden;

      .el-image {
        width: 100%;
        height: 100%;
      }

      .remove-icon {
        position: absolute;
        top: 2px;
        right: 2px;
        font-size: 18px;
        color: #fff;
        background: rgba(0, 0, 0, 0.5);
        border-radius: 50%;
        cursor: pointer;

        &:hover {
          background: rgba(0, 0, 0, 0.7);
        }
      }
    }
  }

  .file-list {
    margin-top: 12px;

    .file-item {
      display: flex;
      align-items: center;
      padding: 8px;
      background: #fff;
      border-radius: 4px;
      margin-bottom: 8px;

      .el-icon:first-child {
        font-size: 20px;
        color: #409eff;
        margin-right: 8px;
      }

      .file-name {
        flex: 1;
        font-size: 14px;
        color: #606266;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .remove-icon {
        font-size: 16px;
        color: #f56c6c;
        cursor: pointer;

        &:hover {
          color: #f78989;
        }
      }
    }
  }
}
</style>