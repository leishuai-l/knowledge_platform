<template>
  <Layout>
    <div class="profile-page">
      <div class="content-wrapper">
        <el-row :gutter="20">
          <!-- 左侧用户信息 -->
          <el-col :span="8" :xs="24">
            <el-card class="user-info-card">
              <div class="user-avatar">
                <el-avatar :size="80" :src="avatarUrl">
                  {{ userStore.user?.username?.[0] }}
                </el-avatar>
                <el-button text @click="showAvatarDialog = true">
                  <el-icon><Camera /></el-icon>
                  更换头像
                </el-button>
              </div>

              <div class="user-details">
                <h2>{{ userStore.user?.username }}</h2>
                <p class="user-email">{{ userStore.user?.email }}</p>

                <div class="user-stats">
                  <div class="stat-item">
                    <span class="stat-value">{{ userStore.user?.points }}</span>
                    <span class="stat-label">当前积分</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ userStore.user?.totalPoints }}</span>
                    <span class="stat-label">累计积分</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ uploadCount }}</span>
                    <span class="stat-label">上传文档</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ downloadCount }}</span>
                    <span class="stat-label">下载次数</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ commentCount }}</span>
                    <span class="stat-label">我的评论</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ ratingCount }}</span>
                    <span class="stat-label">我的评分</span>
                  </div>
                </div>

                <el-button type="primary" @click="showEditDialog = true" style="width: 100%;">
                  编辑资料
                </el-button>
              </div>
            </el-card>

            <!-- 积分记录 -->
            <el-card class="points-card">
              <template #header>
                <span>最近积分记录</span>
              </template>

              <div class="points-list">
                <div v-for="record in recentPoints" :key="record.id" class="points-item">
                  <div class="points-info">
                    <span class="points-desc">{{ record.description }}</span>
                    <span class="points-time">{{ formatDate(record.createdAt) }}</span>
                  </div>
                  <span :class="['points-value', isEarnType(record.type) ? 'earn' : 'spend']">
                    {{ isEarnType(record.type) ? '+' : '-' }}{{ record.points }}
                  </span>
                </div>

                <el-empty v-if="!recentPoints.length" description="暂无积分记录" :image-size="60" />
              </div>
            </el-card>
          </el-col>

          <!-- 右侧内容区域 -->
          <el-col :span="16" :xs="24">
            <el-tabs v-model="activeTab" type="card">
              <!-- 我上传的文档 -->
              <el-tab-pane label="我的上传" name="uploads">
                <div v-loading="uploadsLoading" class="documents-grid">
                  <DocumentCard
                    v-for="doc in myUploads"
                    :key="doc.id"
                    :document="doc"
                    @click="goToDetail(doc.id)"
                  />

                  <el-empty v-if="!myUploads.length && !uploadsLoading" description="还没有上传文档" />
                </div>
              </el-tab-pane>

              <!-- 我下载的文档 -->
              <el-tab-pane label="我的下载" name="downloads">
                <div v-loading="downloadsLoading" class="documents-grid">
                  <DocumentCard
                    v-for="doc in myDownloads"
                    :key="doc.id"
                    :document="doc"
                    @click="goToDetail(doc.id)"
                  />

                  <el-empty v-if="!myDownloads.length && !downloadsLoading" description="还没有下载文档" />
                </div>
              </el-tab-pane>

              <!-- 我的评价 -->
              <el-tab-pane label="我的评价" name="ratings">
                <div v-loading="ratingsLoading" class="ratings-grid">
                  <div v-for="rating in myRatings" :key="rating.id" class="rating-card">
                    <div class="rating-document-info">
                      <div class="document-header" @click="goToDetail(rating.document?.id)">
                        <h4 class="document-title">
                          {{ rating.document?.title || '文档已删除' }}
                        </h4>
                        <el-tag size="small" type="info">
                          {{ rating.document?.category?.name || '未分类' }}
                        </el-tag>
                      </div>

                      <p class="document-description">
                        {{ rating.document?.description || '暂无描述' }}
                      </p>

                      <div class="rating-info">
                        <div class="rating-score-display">
                          <el-rate :model-value="rating.score" disabled size="small" />
                          <span class="score-text">{{ rating.score }}/5</span>
                        </div>

                        <div class="rating-meta">
                          <span class="rating-time">{{ formatDate(rating.createdAt) }}</span>
                          <span v-if="rating.comment" class="has-comment">
                            <el-icon><ChatDotRound /></el-icon>
                            有评论
                          </span>
                        </div>
                      </div>

                      <div v-if="rating.comment" class="rating-comment">
                        <div class="comment-label">我的评论：</div>
                        <div class="comment-text">{{ rating.comment }}</div>
                      </div>
                    </div>

                    <div class="rating-actions">
                      <el-button text size="small" @click="editRating(rating)">
                        编辑评价
                      </el-button>
                      <el-button text size="small" type="danger" @click="deleteRating(rating)">
                        删除评价
                      </el-button>
                    </div>
                  </div>

                  <el-empty v-if="!myRatings.length && !ratingsLoading" description="还没有评价文档" />
                </div>
              </el-tab-pane>

              <!-- 我的申诉 -->
              <el-tab-pane label="我的申诉" name="appeals">
                <div v-loading="appealsLoading" class="appeals-list">
                  <div v-for="appeal in myAppeals" :key="appeal.id" class="appeal-card">
                    <div class="appeal-header">
                      <el-tag :type="getAppealStatusType(appeal.status)">
                        {{ getAppealStatusText(appeal.status) }}
                      </el-tag>
                      <span class="appeal-time">{{ formatDate(appeal.createdAt) }}</span>
                    </div>
                    <div class="appeal-content">
                      <div class="appeal-info-item">
                        <span class="label">文档ID:</span>
                        <el-link type="primary" @click="goToDetail(appeal.documentId)">
                          #{{ appeal.documentId }}
                        </el-link>
                      </div>
                      <div class="appeal-info-item">
                        <span class="label">审核类型:</span>
                        <span>{{ getAppealReviewTypeText(appeal.reviewId) }}</span>
                      </div>
                      <div class="appeal-info-item">
                        <span class="label">申诉理由:</span>
                        <span>{{ appeal.appealReason }}</span>
                      </div>
                      <div v-if="appeal.evidence" class="appeal-info-item">
                        <span class="label">证据材料:</span>
                        <span>{{ appeal.evidence }}</span>
                      </div>
                      <div v-if="appeal.handlerComment" class="appeal-info-item">
                        <span class="label">处理意见:</span>
                        <span>{{ appeal.handlerComment }}</span>
                      </div>
                      <div v-if="appeal.finalDecision" class="appeal-info-item">
                        <span class="label">最终决定:</span>
                        <span>{{ appeal.finalDecision }}</span>
                      </div>
                    </div>
                  </div>
                  <el-empty v-if="!myAppeals.length && !appealsLoading" description="暂无申诉记录" />
                </div>
              </el-tab-pane>

            </el-tabs>
          </el-col>
        </el-row>

        <!-- 编辑资料对话框 -->
        <el-dialog v-model="showEditDialog" title="编辑资料" width="500px">
          <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="80px">
            <!-- 用户名显示（不可编辑） -->
            <el-form-item label="用户名">
              <el-input v-model="editForm.username" disabled placeholder="用户名不可修改" />
              <div class="form-tip">用户名不可修改</div>
            </el-form-item>

            <!-- 当前邮箱显示 -->
            <el-form-item label="当前邮箱">
              <el-input v-model="editForm.currentEmail" disabled />
            </el-form-item>

            <!-- 新邮箱输入 -->
            <el-form-item label="新邮箱" prop="newEmail">
              <el-input
                v-model="editForm.newEmail"
                placeholder="请输入新邮箱地址"
                :disabled="verificationSent"
              />
            </el-form-item>

            <!-- 验证码输入 -->
            <el-form-item v-if="verificationSent" label="验证码" prop="verificationCode">
              <div class="verification-input">
                <el-input
                  v-model="editForm.verificationCode"
                  placeholder="请输入6位验证码"
                  maxlength="6"
                  style="flex: 1; margin-right: 10px;"
                />
                <el-button
                  @click="sendVerificationCode"
                  :disabled="sendCodeDisabled"
                  :loading="sendingCode"
                >
                  {{ sendCodeText }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>

          <template #footer>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button
              v-if="!verificationSent"
              type="primary"
              :loading="sendingCode"
              @click="sendVerificationCode"
              :disabled="!editForm.newEmail || editForm.newEmail === editForm.currentEmail"
            >
              发送验证码
            </el-button>
            <el-button
              v-else
              type="primary"
              :loading="editLoading"
              @click="handleEditProfile"
              :disabled="!editForm.verificationCode || editForm.verificationCode.length !== 6"
            >
              确认修改
            </el-button>
          </template>
        </el-dialog>

        <!-- 更换头像对话框 -->
        <el-dialog v-model="showAvatarDialog" title="更换头像" width="400px">
          <el-upload
            class="avatar-uploader"
            action=""
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAvatarChange"
            accept="image/*"
          >
            <img v-if="newAvatarUrl" :src="newAvatarUrl" alt="头像预览" class="avatar-preview" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>

          <template #footer>
            <el-button @click="showAvatarDialog = false">取消</el-button>
            <el-button type="primary" :loading="avatarLoading" @click="handleUploadAvatar">
              保存
            </el-button>
          </template>
        </el-dialog>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type UploadFile } from 'element-plus'
import { Camera, Plus, ChatDotRound } from '@element-plus/icons-vue'
import Layout from '@/components/Layout.vue'
import DocumentCard from '@/components/DocumentCard.vue'
import { useUserStore } from '@/stores/user'
import { documentApi } from '@/api/document'
import { pointsApi } from '@/api/points'
import { userApi } from '@/api/user'
import { getMyAppeals, normalizePageResponse, type DocumentAppeal } from '@/api/review'
import { http } from '@/api/request'
import type { Document, Rating, PointsRecord } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || globalThis.location.origin).replace(/\/$/, '')

const activeTab = ref('uploads')
const showEditDialog = ref(false)
const showAvatarDialog = ref(false)
const editLoading = ref(false)
const avatarLoading = ref(false)
const uploadsLoading = ref(false)
const downloadsLoading = ref(false)
const ratingsLoading = ref(false)
const appealsLoading = ref(false)

const uploadCount = ref(0)
const downloadCount = ref(0)
const commentCount = ref(0)
const ratingCount = ref(0)
const myUploads = ref<Document[]>([])
const myDownloads = ref<Document[]>([])
const myRatings = ref<Rating[]>([])
const myAppeals = ref<DocumentAppeal[]>([])
const recentPoints = ref<PointsRecord[]>([])
const newAvatarUrl = ref('')
const newAvatarFile = ref<File | null>(null)

const editFormRef = ref<FormInstance>()
const editForm = reactive({
  username: '',
  currentEmail: '',
  newEmail: '',
  verificationCode: ''
})

// 验证码相关状态
const verificationSent = ref(false)
const sendingCode = ref(false)
const sendCodeDisabled = ref(false)
const sendCodeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 表单验证规则
const editFormRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码必须是6位数字', trigger: 'blur' }
  ]
}

// 发送验证码按钮文本
const sendCodeText = computed(() => {
  if (sendCodeCountdown.value > 0) {
    return `${sendCodeCountdown.value}s后重发`
  }
  return verificationSent.value ? '重新发送' : '发送验证码'
})

// 计算属性：处理头像URL，添加时间戳防止缓存
const avatarUrl = computed(() => {
  const baseAvatar = userStore.user?.avatar
  if (!baseAvatar) return ''

  // 如果头像URL是相对路径，转换为绝对路径
  let fullAvatarUrl = baseAvatar
  if (!baseAvatar.startsWith('http')) {
    const path = baseAvatar.startsWith('/') ? baseAvatar : `/${baseAvatar}`
    fullAvatarUrl = `${apiBaseUrl}${path}`
  }

  // 强制缓存更新：使用 store 中的时间戳
  const timestamp = userStore.avatarTimestamp || Date.now()
  const separator = fullAvatarUrl.includes('?') ? '&' : '?'
  return `${fullAvatarUrl}${separator}t=${timestamp}`
})

// 初始化编辑表单
const initEditForm = () => {
  editForm.username = userStore.user?.username || ''
  editForm.currentEmail = userStore.user?.email || ''
  editForm.newEmail = ''
  editForm.verificationCode = ''
  verificationSent.value = false
  sendCodeCountdown.value = 0
  sendCodeDisabled.value = false
}

// 判断是否为获得积分类型
const isEarnType = (type: string) => type === 'EARN'

// 处理头像文件变化
const handleAvatarChange = (file: UploadFile) => {
  if (!file.raw) return

  const reader = new FileReader()
  reader.onload = (e) => {
    newAvatarUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
  newAvatarFile.value = file.raw
}

// 上传头像
const handleUploadAvatar = async () => {
  if (!newAvatarFile.value) return

  try {
    avatarLoading.value = true

    const formData = new FormData()
    formData.append('file', newAvatarFile.value)

    const response = await userApi.uploadAvatar(formData)

    if (response.data.code === 0) {
      // 强制更新用户信息以获取新的头像URL
      await userStore.forceUpdateUser()

      // 更新 store 中的时间戳以防止浏览器缓存
      userStore.avatarTimestamp = Date.now()

      ElMessage.success('头像更新成功')
      showAvatarDialog.value = false
      newAvatarUrl.value = ''
      newAvatarFile.value = null
    } else {
      ElMessage.error(response.data.message || '头像上传失败')
    }
  } catch (error: any) {
    console.error('Avatar upload error:', error)
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    avatarLoading.value = false
  }
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!editFormRef.value) return

  // 验证新邮箱格式
  try {
    await editFormRef.value.validateField('newEmail')
  } catch {
    return
  }

  // 检查是否与当前邮箱相同
  if (editForm.newEmail === editForm.currentEmail) {
    ElMessage.warning('新邮箱不能与当前邮箱相同')
    return
  }

  try {
    sendingCode.value = true

    await userApi.sendEmailVerification(editForm.newEmail)

    ElMessage.success('验证码已发送到您的新邮箱，请查收')
    verificationSent.value = true

    // 开始倒计时
    startCountdown()
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  } finally {
    sendingCode.value = false
  }
}

// 倒计时
const startCountdown = () => {
  sendCodeCountdown.value = 60
  sendCodeDisabled.value = true

  countdownTimer = setInterval(() => {
    sendCodeCountdown.value--
    if (sendCodeCountdown.value <= 0) {
      if (countdownTimer !== null) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
      sendCodeDisabled.value = false
    }
  }, 1000)
}

// 编辑资料（验证邮箱并更新）
const handleEditProfile = async () => {
  if (!editFormRef.value) return

  try {
    editLoading.value = true

    await editFormRef.value.validate()

    await userApi.verifyAndUpdateEmail(
      editForm.newEmail,
      editForm.verificationCode
    )

    // 更新用户状态
    await userStore.getCurrentUser()

    ElMessage.success('邮箱更新成功')
    showEditDialog.value = false
    initEditForm()
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    editLoading.value = false
  }
}

// 取消编辑
const cancelEdit = () => {
  showEditDialog.value = false
  initEditForm()
}

// 跳转到文档详情
const goToDetail = (id?: number) => {
  if (!id) return
  router.push(`/documents/${id}`)
}

// 日期格式化
const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

// 加载用户统计
const loadUserStatistics = async () => {
  try {
    const response = await userApi.getUserStatistics()
    const statistics = response.data.data
    uploadCount.value = statistics.totalUploads || 0
    downloadCount.value = statistics.totalDownloads || 0
    commentCount.value = statistics.totalComments || 0
    ratingCount.value = statistics.totalRatings || 0
  } catch (error) {
    console.error('Failed to load user statistics:', error)
    uploadCount.value = 0
    downloadCount.value = 0
    commentCount.value = 0
    ratingCount.value = 0
  }
}

// 加载我的上传
const loadMyUploads = async () => {
  try {
    uploadsLoading.value = true
    const response = await documentApi.getMyDocuments(1, 20)
    myUploads.value = response.data.data.list
  } catch (error) {
    console.error('Failed to load uploads:', error)
  } finally {
    uploadsLoading.value = false
  }
}

// 加载我的下载
const loadMyDownloads = async () => {
  try {
    downloadsLoading.value = true
    // 使用正确的下载记录接口
    const response = await http.get('/api/downloads/my', {
      params: { page: 0, size: 20 }
    })

    if (response.data.code === 0) {
      // 处理下载记录数据，提取文档信息
      const downloadRecords = response.data.data.list || []

      // 转换下载记录为文档格式以便在DocumentCard中显示
      myDownloads.value = downloadRecords.map((record: any) => {
        // 使用JOIN FETCH获取的文档信息
        const document = record.document
        if (document) {
          return {
            id: document.id,
            title: document.title,
            description: document.description,
            downloadCount: document.downloadCount || 0,
            ratingAverage: document.ratingAverage || 0,
            fileSize: document.fileSize || 0,
            fileExtension: document.fileExtension || 'pdf',
            downloadPoints: document.downloadPoints || record.pointsCost || 0,
            createdAt: document.createdAt,
            status: document.status || 'APPROVED',
            tags: document.tags || [],
            // 添加下载记录信息
            downloadTime: record.downloadTime,
            pointsCost: record.pointsCost
          }
        }

        return {
          id: record.documentId,
          title: `文档 #${record.documentId}`,
          description: '文档信息加载中...',
          downloadCount: 0,
          ratingAverage: 0,
          fileSize: 0,
          fileExtension: 'pdf',
          downloadPoints: record.pointsCost || 0,
          createdAt: record.downloadTime || record.createdAt,
          status: 'APPROVED',
          tags: [],
          downloadTime: record.downloadTime,
          pointsCost: record.pointsCost
        }
      }).filter(Boolean)
    } else {
      console.error('下载记录API返回错误:', response.data.message)
      myDownloads.value = []
    }
  } catch (error) {
    console.error('Failed to load downloads:', error)
    myDownloads.value = []
  } finally {
    downloadsLoading.value = false
  }
}

// 加载我的评分
const loadMyRatings = async () => {
  try {
    ratingsLoading.value = true
    // 使用正确的评分API端点
    const response = await http.get('/api/ratings/user')

    if (response.data.code === 0) {
      // 处理评分数据，确保使用正确的字段名
      const ratings = response.data.data || []
      myRatings.value = ratings.map((rating: any) => ({
        id: rating.id,
        score: rating.score,  // 使用 score 而不是 rating
        comment: rating.comment,
        createdAt: rating.createdAt,
        updatedAt: rating.updatedAt,
        document: rating.document
      }))
    } else {
      console.error('评分API返回错误:', response.data.message)
      myRatings.value = []
    }
  } catch (error) {
    console.error('Failed to load ratings:', error)
    myRatings.value = []
  } finally {
    ratingsLoading.value = false
  }
}

// 加载我的申诉
const loadMyAppeals = async () => {
  try {
    appealsLoading.value = true
    const res = await getMyAppeals(0, 20)
    const pageData = normalizePageResponse<DocumentAppeal>(res.data)
    myAppeals.value = pageData.list
  } catch (error) {
    console.error('Failed to load appeals:', error)
    myAppeals.value = []
  } finally {
    appealsLoading.value = false
  }
}

const getAppealReviewTypeText = (reviewId?: number) => {
  return reviewId ? '复审' : '初审'
}

// 获取申诉状态类型
const getAppealStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取申诉状态文本
const getAppealStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '待处理',
    APPROVED: '已批准',
    REJECTED: '已驳回'
  }
  return textMap[status] || status
}

// 编辑评价
const editRating = (rating: any) => {
  // 跳转到文档详情页面的评价部分
  router.push(`/documents/${rating.document?.id}#rating-${rating.id}`)
}

// 删除评价
const deleteRating = async (rating: any) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个评价吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await http.delete(`/api/ratings/${rating.id}`)

    if (response.data.code === 0) {
      ElMessage.success('评价删除成功')
      await loadMyRatings() // 重新加载评价列表
    } else {
      ElMessage.error(response.data.message || '删除评价失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除评价失败:', error)
      ElMessage.error('删除评价失败')
    }
  }
}


// 加载数据
const loadData = async () => {
  await Promise.all([
    loadUserStatistics(),
    loadMyUploads(),
    loadMyDownloads(),
    loadMyRatings(),
    loadMyAppeals(),
    loadPointsRecords()
  ])
}

// 加载积分记录
const loadPointsRecords = async () => {
  try {
    const pointsResponse = await pointsApi.getPointsRecords(0, 10)
    recentPoints.value = pointsResponse.data.data?.list || []
  } catch (error) {
    console.error('Failed to load points records:', error)
    recentPoints.value = []
  }
}

onMounted(() => {
  initEditForm()
  loadData()
})

onBeforeUnmount(() => {
  if (countdownTimer !== null) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.user-info-card {
  margin-bottom: 20px;
}

.user-avatar {
  text-align: center;
  margin-bottom: 20px;
}

.user-avatar .el-button {
  margin-top: 10px;
}

.user-details h2 {
  text-align: center;
  margin: 0 0 10px;
  color: #2c3e50;
}

.user-email {
  text-align: center;
  color: #666;
  margin: 0 0 20px;
}

.user-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.points-card {
  margin-top: 20px;
}

.points-list {
  max-height: 300px;
  overflow-y: auto;
}

.points-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.points-item:last-child {
  border-bottom: none;
}

.points-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.points-desc {
  color: #333;
  font-size: 14px;
}

.points-time {
  color: #999;
  font-size: 12px;
}

.points-value {
  font-weight: 600;
  font-size: 16px;
}

.points-value.earn {
  color: #67C23A;
}

.points-value.spend {
  color: #E6A23C;
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.ratings-list {
  min-height: 200px;
}

.rating-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.rating-document h4 {
  margin: 0 0 8px;
  color: #2c3e50;
  font-size: 16px;
}

.rating-document p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.rating-score {
  text-align: right;
}

.rating-time {
  display: block;
  color: #999;
  font-size: 12px;
  margin-top: 5px;
}

.clickable-title {
  cursor: pointer;
  transition: color 0.2s;
}

.clickable-title:hover {
  color: #409EFF;
}

/* 评论列表样式 */
.comments-list {
  min-height: 200px;
}

.comment-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border-left: 3px solid #409EFF;
}

.comment-content {
  flex: 1;
  margin-right: 15px;
}

.comment-document h4 {
  margin: 0 0 8px;
  color: #2c3e50;
  font-size: 16px;
}

.comment-text p {
  margin: 8px 0;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
  background: #f8f9fa;
  padding: 10px;
  border-radius: 6px;
}

.comment-meta {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-top: 8px;
}

.comment-time {
  color: #999;
  font-size: 12px;
}

.reply-count {
  color: #666;
  font-size: 12px;
  background: #e8f4f8;
  padding: 2px 6px;
  border-radius: 10px;
}

.comment-actions {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

/* 评价样式 */
.ratings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.rating-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
  transition: box-shadow 0.2s ease;
}

.rating-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.rating-document-info {
  margin-bottom: 12px;
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  cursor: pointer;
}

.document-header:hover .document-title {
  color: #409EFF;
}

.document-title {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  transition: color 0.2s ease;
}

.document-description {
  color: #666;
  font-size: 14px;
  margin: 0 0 12px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rating-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.rating-score-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-text {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.rating-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.rating-time {
  color: #999;
}

.has-comment {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #67C23A;
  font-weight: 500;
}

.rating-comment {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 12px;
  margin: 8px 0;
}

.comment-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  font-weight: 500;
}

.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  word-break: break-word;
}

.rating-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.avatar-uploader {
  display: flex;
  justify-content: center;
}

.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: 0.2s;
  width: 178px;
  height: 178px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.avatar-preview {
  width: 178px;
  height: 178px;
  object-fit: cover;
}

/* 编辑资料表单样式 */
.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.verification-input {
  display: flex;
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-stats {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .documents-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .ratings-grid {
    grid-template-columns: 1fr;
  }

  .rating-card {
    margin-bottom: 15px;
  }

  .document-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .rating-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .rating-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }

  .verification-input {
    flex-direction: column;
    gap: 10px;
  }

  .verification-input .el-input {
    margin-right: 0 !important;
  }
}

/* 申诉列表样式 */
.appeals-list {
  min-height: 200px;
}

.appeal-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-left: 3px solid #409EFF;
}

.appeal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.appeal-time {
  color: #999;
  font-size: 12px;
}

.appeal-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.appeal-info-item {
  display: flex;
  gap: 8px;
  font-size: 14px;
}

.appeal-info-item .label {
  color: #666;
  font-weight: 500;
  min-width: 80px;
}
</style>


