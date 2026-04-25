<template>
  <div class="comment-item">
    <div class="comment-header">
      <el-avatar :size="40" :src="getAvatarUrl(comment.user?.avatar)">
        {{ comment.user?.username?.[0] }}
      </el-avatar>
      <div class="user-info">
        <span class="username">{{ comment.user?.username }}</span>
        <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
      </div>
    </div>

    <div class="comment-content">
      <p>{{ comment.content }}</p>
    </div>

    <div class="comment-actions">
      <el-button text size="small" @click="$emit('reply', comment)">
        <el-icon><ChatLineRound /></el-icon>
        回复
      </el-button>
    </div>

    <!-- 回复列表 -->
    <div v-if="comment.replies?.length" class="replies">
      <CommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :is-reply="true"
        @reply="$emit('reply', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatLineRound } from '@element-plus/icons-vue'
import type { Comment } from '@/api/comment'

interface Props {
  comment: Comment
  isReply?: boolean
}

defineProps<Props>()
defineEmits<{
  reply: [comment: Comment]
}>()

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || globalThis.location.origin).replace(/\/$/, '')

// 获取头像URL
const getAvatarUrl = (avatar?: string) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  const path = avatar.startsWith('/') ? avatar : `/${avatar}`
  return `${apiBaseUrl}${path}`
}

// 日期格式化
const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.comment-item {
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.username {
  font-weight: 500;
  color: #2c3e50;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  margin: 10px 0;
  padding-left: 50px;
}

.comment-content p {
  margin: 0;
  color: #333;
  line-height: 1.5;
  word-break: break-word;
}

.comment-actions {
  padding-left: 50px;
}

.replies {
  margin-left: 50px;
  margin-top: 15px;
  padding-left: 20px;
  border-left: 2px solid #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .comment-content,
  .comment-actions {
    padding-left: 0;
    margin-left: 0;
  }

  .replies {
    margin-left: 0;
    padding-left: 15px;
  }
}
</style>