<template>
  <div class="office-preview-container">
    <div class="preview-card">
      <div class="file-icon-large">
        <el-icon :size="72" :color="getFileTypeColor(fileExtension)">
          <component :is="getFileTypeIcon(fileExtension)" />
        </el-icon>
      </div>

      <h3>{{ fileName }}</h3>
      <p class="file-info">{{ fileExtension.toUpperCase() }} 文件 • {{ formatFileSize(fileSize) }}</p>

      <el-alert
        title="当前环境暂不支持 Office 文件在线渲染"
        type="info"
        :closable="false"
        show-icon
        class="preview-alert"
      >
        <p>已提供稳定预览入口，当前展示文件信息并支持直接下载。</p>
        <p>请下载后使用本地 Office、WPS 等软件打开。</p>
      </el-alert>

      <div class="actions">
        <el-button type="primary" @click="downloadFile">
          <el-icon><Download /></el-icon>
          下载文件
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Download, Document, DocumentCopy, Notebook } from '@element-plus/icons-vue'

interface Props {
  documentId: number
  fileName: string
  fileExtension: string
  fileSize: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  download: [documentId: number]
}>()

const downloadFile = () => {
  emit('download', props.documentId)
}

const getFileTypeIcon = (extension: string) => {
  const ext = extension.toLowerCase()
  if (['doc', 'docx'].includes(ext)) return Document
  if (['ppt', 'pptx'].includes(ext)) return DocumentCopy
  if (['xls', 'xlsx'].includes(ext)) return Notebook
  return Document
}

const getFileTypeColor = (extension: string) => {
  const ext = extension.toLowerCase()
  if (['doc', 'docx'].includes(ext)) return '#4DABF7'
  if (['ppt', 'pptx'].includes(ext)) return '#FF922B'
  if (['xls', 'xlsx'].includes(ext)) return '#51CF66'
  return '#409EFF'
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}
</script>

<style scoped>
.office-preview-container {
  width: 100%;
  height: 100%;
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.preview-card {
  width: min(560px, 100%);
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.file-icon-large {
  margin-bottom: 20px;
}

.preview-card h3 {
  margin: 0 0 8px;
  color: var(--el-text-color-primary);
  font-size: 20px;
  word-break: break-word;
}

.file-info {
  margin: 0 0 20px;
  color: var(--el-text-color-secondary);
}

.preview-alert {
  text-align: left;
}

.preview-alert p {
  margin: 0 0 8px;
}

.preview-alert p:last-child {
  margin-bottom: 0;
}

.actions {
  margin-top: 20px;
}
</style>
