<template>
  <el-card class="document-list-item" shadow="hover" @click="$emit('click')">
    <div class="item-content">
      <!-- 文件图标 -->
      <div class="file-icon">
        <el-icon :size="32" :color="getFileTypeColor(document.fileExtension)">
          <component :is="getFileTypeIcon(document.fileExtension)" />
        </el-icon>
      </div>

      <!-- 文档信息 -->
      <div class="document-info">
        <div class="title-row">
          <h3 class="document-title">{{ document.title }}</h3>
          <div class="status-badge" v-if="document.status !== 'APPROVED'">
            <el-tag :type="getStatusType(document.status)" size="small">
              {{ getStatusText(document.status) }}
            </el-tag>
          </div>
        </div>

        <p class="document-description">{{ document.description || '暂无描述' }}</p>

        <div class="meta-info">
          <span class="file-meta">
            {{ document.fileExtension.toUpperCase() }} • {{ formatFileSize(document.fileSize) }}
          </span>
          <span class="upload-time">{{ formatDate(document.createdAt) }}</span>
          <span class="uploader" v-if="document.uploader">
            上传者: {{ document.uploader.username }}
          </span>
        </div>

        <!-- 标签 -->
        <div class="tags" v-if="document.tags && document.tags.length">
          <el-tag
            v-for="tag in document.tags.slice(0, 5)"
            :key="tag.id"
            size="small"
            :color="tag.color"
            effect="light"
            class="white-text-tag"
          >
            {{ tag.name }}
          </el-tag>
          <el-tag v-if="document.tags.length > 5" size="small" type="info" class="white-text-tag">
            +{{ document.tags.length - 5 }}
          </el-tag>
        </div>
      </div>

      <!-- 右侧信息 -->
      <div class="right-info">
        <div class="rating">
          <el-rate
            :model-value="document.ratingAverage"
            disabled
            size="small"
            :show-score="false"
          />
          <span class="rating-text">{{ document.ratingAverage.toFixed(1) }}</span>
        </div>

        <div class="stats">
          <div class="stat-item">
            <el-icon size="14"><Download /></el-icon>
            <span>{{ document.downloadCount }}</span>
          </div>
          <div class="stat-item">
            <el-icon size="14"><ChatDotRound /></el-icon>
            <span>{{ document.ratingCount }}</span>
          </div>
        </div>

        <div class="points">
          <el-icon color="#E6A23C"><Coin /></el-icon>
          <span>{{ document.downloadPoints }} 积分</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import {
  Document as DocumentIcon,
  Picture,
  VideoPlay,
  Headset,
  FolderOpened,
  Download,
  ChatDotRound,
  Coin
} from '@element-plus/icons-vue'
import type { Document } from '@/types'

interface Props {
  document: Document
}

defineProps<Props>()
defineEmits<{
  click: []
}>()

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
.document-list-item {
  margin-bottom: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.document-list-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.item-content {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 20px;
}

.file-icon {
  flex-shrink: 0;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 8px;
}

.document-info {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.document-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 10px;
}

.document-description {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0 0 10px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #999;
  flex-wrap: wrap;
}

.file-meta {
  font-weight: 500;
  color: #666;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.white-text-tag {
  color: white !important;
}

.right-info {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  min-width: 120px;
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

.stats {
  display: flex;
  gap: 15px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #666;
}

.points {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  font-weight: 600;
  color: #E6A23C;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .item-content {
    flex-direction: column;
    gap: 10px;
    padding: 15px;
  }

  .title-row {
    flex-direction: column;
    gap: 5px;
  }

  .document-title {
    white-space: normal;
    overflow: visible;
    text-overflow: unset;
  }

  .meta-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }

  .right-info {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    min-width: auto;
  }

  .stats {
    gap: 10px;
  }
}
</style>