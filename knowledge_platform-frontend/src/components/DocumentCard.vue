<template>
  <el-card
    class="document-card"
    shadow="hover"
    @click="$emit('click')"
    :body-style="{ padding: '0' }"
  >
    <div class="card-content">
      <!-- 文件图标和类型 -->
      <div class="file-icon">
        <el-icon :size="40" :color="getFileTypeColor(document.fileExtension)">
          <component :is="getFileTypeIcon(document.fileExtension)" />
        </el-icon>
        <span class="file-extension">{{ document.fileExtension.toUpperCase() }}</span>
      </div>

      <!-- 文档信息 -->
      <div class="document-info">
        <h3 class="document-title" :title="document.title">
          {{ document.title }}
        </h3>

        <p class="document-description" :title="document.description">
          {{ document.description || '暂无描述' }}
        </p>

        <!-- 评分和下载 -->
        <div class="document-meta">
          <div class="rating">
            <el-rate
              :model-value="document.ratingAverage"
              disabled
              size="small"
              :show-score="false"
            />
            <span class="rating-text">{{ document.ratingAverage.toFixed(1) }}</span>
          </div>
          <div class="download-count">
            <el-icon size="14"><Download /></el-icon>
            {{ document.downloadCount }}
          </div>
        </div>

        <!-- 标签 -->
        <div class="tags" v-if="document.tags && document.tags.length">
          <el-tag
            v-for="tag in document.tags.slice(0, 3)"
            :key="tag.id"
            size="small"
            :color="tag.color"
            effect="light"
            class="white-text-tag"
          >
            {{ tag.name }}
          </el-tag>
          <el-tag v-if="document.tags.length > 3" size="small" type="info" class="white-text-tag">
            +{{ document.tags.length - 3 }}
          </el-tag>
        </div>

        <!-- 底部信息 -->
        <div class="document-footer">
          <div class="file-info">
            <span class="file-size">{{ formatFileSize(document.fileSize) }}</span>
            <span class="upload-time">{{ formatDate(document.createdAt) }}</span>
          </div>
          <div class="points">
            <el-icon color="#E6A23C"><Coin /></el-icon>
            {{ document.downloadPoints }}
          </div>
        </div>
      </div>

      <!-- 悬浮操作按钮 -->
      <div class="card-actions" @click.stop>
        <el-button-group>
          <el-button
            type="primary"
            size="small"
            @click="handlePreview"
            title="预览文档"
          >
            <el-icon><View /></el-icon>
          </el-button>
          <el-button
            type="info"
            size="small"
            @click="handleViewDetail"
            title="查看详情"
          >
            <el-icon><InfoFilled /></el-icon>
          </el-button>
          <el-button
            v-if="document.status === 'APPROVED'"
            type="success"
            size="small"
            @click="handleDownload"
            title="下载文档"
          >
            <el-icon><Download /></el-icon>
          </el-button>
        </el-button-group>
      </div>

      <!-- 状态标识 -->
      <div class="status-badge" v-if="document.status !== 'APPROVED'">
        <el-tag
          :type="getStatusType(document.status)"
          size="small"
          effect="dark"
        >
          {{ getStatusText(document.status) }}
        </el-tag>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document as DocumentIcon,
  Picture,
  VideoPlay,
  Headset,
  FolderOpened,
  Download,
  Coin,
  View,
  InfoFilled
} from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'
import type { Document } from '@/types'

interface Props {
  document: Document
}

const props = defineProps<Props>()
defineEmits<{
  click: []
}>()

const router = useRouter()

// 处理预览
const handlePreview = () => {
  // 统一预览方式：在新窗口打开预览页面（与管理员预览保持一致）
  const routeUrl = router.resolve({
    name: 'DocumentPreview',
    params: { id: props.document.id }
  })

  // 在新窗口打开预览页面
  window.open(routeUrl.href, '_blank')
}

// 查看详情
const handleViewDetail = () => {
  router.push({
    name: 'DocumentDetail',
    params: { id: props.document.id }
  })
}

// 下载文档
const handleDownload = async () => {
  try {
    const response = await documentApi.downloadDocument(props.document.id)

    // 创建下载链接
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data as unknown as BlobPart])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = props.document.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('下载开始')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '下载失败')
  }
}

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

// 文件大小格式化
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
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
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

// 状态类型
const getStatusType = (status: string) => {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'REJECTED': return 'danger'
    default: return 'success'
  }
}

// 状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '待审核'
    case 'REJECTED': return '已拒绝'
    case 'APPROVED': return '已通过'
    default: return status
  }
}
</script>

<style scoped>
.document-card {
  height: 300px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

.document-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.document-card:hover .card-actions {
  opacity: 1;
  transform: translateY(0);
}

.card-content {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.file-icon {
  text-align: center;
  margin-bottom: 15px;
  position: relative;
}

.file-extension {
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  font-weight: bold;
  color: #666;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 6px;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.document-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.document-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.document-description {
  font-size: 12px;
  color: #7f8c8d;
  margin: 0 0 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  flex: 1;
}

.document-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.rating {
  display: flex;
  align-items: center;
  gap: 5px;
}

.rating-text {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.download-count {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #666;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 12px;
  min-height: 20px;
}

.white-text-tag {
  color: white !important;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3) !important;
}

.white-text-tag :deep(.el-tag__content) {
  color: white !important;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3) !important;
}

/* 特殊处理浅色背景 - 使用深色文字 */
.white-text-tag[style*="#ffffff"],
.white-text-tag[style*="#f0f0f0"],
.white-text-tag[style*="#f5f5f5"],
.white-text-tag[style*="#fafafa"],
.white-text-tag[style*="rgb(255, 255, 255)"],
.white-text-tag[style*="white"],
.white-text-tag[style*="White"] {
  color: #333 !important;
  text-shadow: none !important;
}

.white-text-tag[style*="#ffffff"] :deep(.el-tag__content),
.white-text-tag[style*="#f0f0f0"] :deep(.el-tag__content),
.white-text-tag[style*="#f5f5f5"] :deep(.el-tag__content),
.white-text-tag[style*="#fafafa"] :deep(.el-tag__content),
.white-text-tag[style*="rgb(255, 255, 255)"] :deep(.el-tag__content),
.white-text-tag[style*="white"] :deep(.el-tag__content),
.white-text-tag[style*="White"] :deep(.el-tag__content) {
  color: #333 !important;
  text-shadow: none !important;
}

.document-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #eee;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-size {
  font-size: 11px;
  color: #999;
  font-weight: 500;
}

.upload-time {
  font-size: 11px;
  color: #bbb;
}

.points {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 14px;
  font-weight: 600;
  color: #E6A23C;
}

.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
}

.card-actions {
  position: absolute;
  bottom: 16px;
  right: 16px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s ease;
  z-index: 20;
}

.card-actions .el-button {
  border-radius: 20px;
  min-width: 36px;
  height: 32px;
  padding: 0 8px;
}

.card-actions .el-button .el-icon {
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .document-card {
    height: 280px;
  }

  .card-content {
    padding: 15px;
  }

  .document-title {
    font-size: 14px;
  }

  .document-description {
    font-size: 11px;
  }
}
</style>