<template>
  <el-dialog v-model="visible" title="文档审核" width="700px" @close="handleClose">
    <div class="review-content">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文档标题">{{ document?.title }}</el-descriptions-item>
        <el-descriptions-item label="上传者">{{ document?.uploader?.username }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ document?.fileExtension }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(document?.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ document?.category?.name }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ formatDate(document?.createdAt) }}</el-descriptions-item>
      </el-descriptions>

      <div class="preview-section">
        <el-button type="primary" @click="handlePreview" style="margin-top: 15px">
          <el-icon><View /></el-icon>
          在线预览
        </el-button>
      </div>

      <el-form :model="form" label-width="80px" style="margin-top: 20px">
        <el-form-item label="审核意见">
          <el-input
            v-model="form.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="danger" @click="handleReject">拒绝审核</el-button>
      <el-button type="success" @click="handleApprove">通过审核</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import type { Document } from '@/types'

const props = defineProps<{
  modelValue: boolean
  document: Document | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  approve: [document: Document, comment: string]
  reject: [document: Document, comment: string]
}>()

const visible = ref(false)
const form = ref({ comment: '' })

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) form.value.comment = ''
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatFileSize = (size?: number) => {
  if (!size) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(size) / Math.log(k))
  return parseFloat((size / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const handleClose = () => {
  visible.value = false
}

const handlePreview = () => {
  if (props.document) {
    window.open(`/documents/${props.document.id}/preview?admin=true`, '_blank')
  }
}

const handleApprove = () => {
  if (props.document) {
    emit('approve', props.document, form.value.comment)
    handleClose()
  }
}

const handleReject = () => {
  if (!form.value.comment.trim()) {
    ElMessage.warning('拒绝审核时必须填写审核意见')
    return
  }
  if (props.document) {
    emit('reject', props.document, form.value.comment)
    handleClose()
  }
}
</script>

<style scoped>
.review-content {
  padding: 10px 0;
}

.preview-section {
  text-align: center;
}
</style>
