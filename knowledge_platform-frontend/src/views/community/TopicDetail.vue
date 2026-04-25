<template>
  <Layout>
    <div class="topic-detail-page">
      <div class="page-header">
        <el-button link @click="$router.push('/community')">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
      </div>

      <div v-loading="loading" class="main-container">
        <!-- Topic Card -->
        <div v-if="topic" class="topic-card">
          <div class="topic-header">
            <h1 class="topic-title">
              <span v-if="topic.isPinned" class="badge pinned">置顶</span>
              <span v-if="topic.isEssence" class="badge essence">精华</span>
              {{ topic.title }}
            </h1>
            
            <div class="topic-meta-row">
              <div class="meta-left">
                <el-avatar :size="32" :src="getAvatarUrl(topic.author?.avatar)" class="author-avatar">
                  {{ topic.author?.username?.charAt(0) }}
                </el-avatar>
                <div class="author-details">
                  <span class="author-name">{{ topic.author?.username }}</span>
                  <span class="publish-time">{{ formatDate(topic.createdAt) }}</span>
                </div>
              </div>
              
              <div class="meta-right">
                <span class="meta-item"><el-icon><View /></el-icon> {{ topic.viewCount }}</span>
                <span class="meta-item"><el-icon><ChatDotRound /></el-icon> {{ topic.replyCount }}</span>
              </div>
            </div>

            <div class="topic-tags" v-if="topic.tags && topic.tags.length">
              <span 
                v-for="(tag, index) in topic.tags" 
                :key="tag.id" 
                class="custom-tag"
                :style="getTagStyle(index)"
              >
                # {{ tag.name }}
              </span>
            </div>
          </div>

          <div class="topic-body">
            <div class="content-text">{{ topic.content || '' }}</div>
          </div>

          <div class="topic-actions">
            <el-button 
              :type="topic.isLiked ? 'primary' : 'default'" 
              round
              size="large"
              @click="handleToggleTopicLike"
              class="action-btn"
            >
              <el-icon><Pointer /></el-icon> 
              <span>{{ topic.isLiked ? '已点赞' : '点赞' }} {{ topic.likeCount || 0 }}</span>
            </el-button>
            <el-button 
              v-if="userStore.isAdmin"
              :type="topic.isPinned ? 'warning' : 'default'" 
              round
              size="large"
              @click="handleTogglePin"
              class="action-btn"
            >
              <el-icon><Top /></el-icon> 
              <span>{{ topic.isPinned ? '取消置顶' : '置顶' }}</span>
            </el-button>
            <el-button 
              v-if="userStore.isAdmin"
              :type="topic.isEssence ? 'danger' : 'default'" 
              round
              size="large"
              @click="handleToggleEssence"
              class="action-btn"
            >
              <el-icon><Medal /></el-icon> 
              <span>{{ topic.isEssence ? '取消加精' : '加精' }}</span>
            </el-button>
            <el-button round size="large" class="action-btn" @click="scrollToReply">
              <el-icon><ChatLineRound /></el-icon> 
              <span>回复</span>
            </el-button>
          </div>
        </div>

        <!-- Replies Section -->
        <div class="replies-card">
          <div class="section-title">
            <h3>全部回复 ({{ totalReplies }})</h3>
          </div>

          <div v-if="replies.length === 0" class="empty-replies">
            <el-empty description="暂无回复，快来抢沙发！" :image-size="100" />
          </div>

          <div v-else class="reply-list">
            <div v-for="(reply, index) in replies" :key="reply.id" class="reply-item">
              <div class="reply-left">
                <el-avatar :size="40" :src="getAvatarUrl(reply.author?.avatar)" shape="square">
                  {{ reply.author?.username?.charAt(0) }}
                </el-avatar>
              </div>
              
              <div class="reply-right">
                <div class="reply-header">
                  <span class="reply-author-name">{{ reply.author?.username }}</span>
                  <span class="reply-floor">#{{ (currentPage - 1) * pageSize + index + 1 }}</span>
                </div>
                
                <div class="reply-content-box">
                  <div v-if="reply.parentId" class="reply-quote">
                    回复 #{{ reply.parentId }}:
                  </div>
                  {{ reply.content }}
                </div>
                
                <div class="reply-footer">
                  <span class="reply-time">{{ formatDate(reply.createdAt) }}</span>
                  <div class="reply-actions">
                    <span 
                      class="action-link" 
                      :class="{ active: reply.isLiked }"
                      @click="handleToggleReplyLike(reply)"
                    >
                      <el-icon><Pointer /></el-icon> {{ reply.likeCount || 0 }}
                    </span>
                    <span class="action-link" @click="handleReply(reply)">
                      <el-icon><ChatLineRound /></el-icon> 回复
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="pagination-wrapper">
            <el-pagination
              v-if="totalReplies > 0"
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="totalReplies"
              layout="prev, pager, next"
              @current-change="fetchReplies"
              background
            />
          </div>
        </div>

        <!-- Reply Editor -->
        <div class="editor-card" id="reply-editor">
          <div class="editor-header">
            <h3>发表回复</h3>
          </div>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="6"
            placeholder="友善的评论是交流的起点..."
            resize="none"
          />
          <div class="editor-footer">
            <el-button type="primary" size="large" :loading="submitting" @click="submitReply">
              发表回复
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  getTopicDetail, 
  getReplies, 
  createReply, 
  toggleTopicLike, 
  toggleReplyLike, 
  toggleTopicPin,
  toggleTopicEssence,
  type ForumTopic, 
  type ForumReply 
} from '@/api/forum'
import { ElMessage } from 'element-plus'
import Layout from '@/components/Layout.vue'
import { useUserStore } from '@/stores/user'
import { ArrowLeft, Pointer, View, ChatDotRound, ChatLineRound, Top, Medal } from '@element-plus/icons-vue'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const topicId = Number(route.params.id)
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || globalThis.location.origin).replace(/\/$/, '')

const topic = ref<ForumTopic | null>(null)
const replies = ref<ForumReply[]>([])
const loading = ref(false)
const submitting = ref(false)
const replyContent = ref('')
const replyParentId = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = ref(10)
const totalReplies = ref(0)

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getAvatarUrl = (avatar?: string) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  const path = avatar.startsWith('/') ? avatar : `/${avatar}`
  return `${apiBaseUrl}${path}`
}

const getTagStyle = (index: number) => {
  const colors = [
    { bg: '#c6e2ff', border: '#409eff' }, // Darker Blue
    { bg: '#d1edc4', border: '#67c23a' }, // Darker Green
    { bg: '#f8e3c5', border: '#e6a23c' }, // Darker Orange
    { bg: '#fcd3d3', border: '#f56c6c' }, // Darker Red
    { bg: '#d9d9d9', border: '#909399' }, // Darker Grey
  ]
  const color = colors[index % colors.length]
  return {
    backgroundColor: color.bg,
    color: '#000000',
    borderColor: color.border,
    fontWeight: 'bold',
    '--el-tag-text-color': '#000000'
  }
}

const fetchTopic = async () => {
  try {
    const res = await getTopicDetail(topicId)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      topic.value = res.data.data
    } else {
      ElMessage.error(res.data?.message || '获取帖子详情失败')
    }
  } catch (error) {
    console.error('Failed to fetch topic:', error)
    ElMessage.error('获取帖子详情失败')
  }
}

const fetchReplies = async () => {
  try {
    const res = await getReplies(topicId, {
      page: currentPage.value - 1,
      size: pageSize.value
    })
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      replies.value = res.data.data.list
      totalReplies.value = res.data.data.total
    }
  } catch (error) {
    console.error('Failed to fetch replies:', error)
  }
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  submitting.value = true
  try {
    const res = await createReply({
      topicId,
      content: replyContent.value,
      parentId: replyParentId.value
    })
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      ElMessage.success('回复成功')
      replyContent.value = ''
      replyParentId.value = undefined
      await fetchReplies() 
    } else {
      ElMessage.error(res.data?.message || '回复失败')
    }
  } catch (error: any) {
    console.error('Reply error:', error)
    ElMessage.error(error.response?.data?.message || '回复失败')
  } finally {
    submitting.value = false
  }
}

const handleReply = (reply: ForumReply) => {
  replyContent.value = `回复 @${reply.author?.username} : `
  replyParentId.value = reply.id
  scrollToReply()
}

const scrollToReply = () => {
  const editor = document.getElementById('reply-editor')
  if (editor) {
    editor.scrollIntoView({ behavior: 'smooth' })
    const textarea = editor.querySelector('textarea')
    if (textarea) textarea.focus()
  }
}

const handleToggleTopicLike = async () => {
  if (!topic.value) return
  try {
    const res = await toggleTopicLike(topicId)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      const isLiked = res.data.data
      topic.value.isLiked = isLiked
      const currentCount = topic.value.likeCount || 0
      topic.value.likeCount = isLiked ? currentCount + 1 : Math.max(0, currentCount - 1)
      ElMessage.success(isLiked ? '点赞成功' : '取消点赞成功')
    } else {
       ElMessage.error(res.data?.message || '操作失败')
    }
  } catch (error) {
    console.error('Like topic error:', error)
    ElMessage.error('操作失败')
  }
}

const handleToggleReplyLike = async (reply: ForumReply) => {
  try {
    const res = await toggleReplyLike(reply.id)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      const isLiked = res.data.data
      reply.isLiked = isLiked
      const currentCount = reply.likeCount || 0
      reply.likeCount = isLiked ? currentCount + 1 : Math.max(0, currentCount - 1)
      ElMessage.success(isLiked ? '点赞成功' : '取消点赞成功')
    } else {
       ElMessage.error(res.data?.message || '操作失败')
    }
  } catch (error) {
    console.error('Like reply error:', error)
    ElMessage.error('操作失败')
  }
}

const handleTogglePin = async () => {
  if (!topic.value) return
  try {
    const res = await toggleTopicPin(topicId)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      const isPinned = res.data.data
      topic.value.isPinned = isPinned
      ElMessage.success(isPinned ? '置顶成功' : '取消置顶成功')
    } else {
       ElMessage.error(res.data?.message || '操作失败')
    }
  } catch (error) {
    console.error('Pin topic error:', error)
    ElMessage.error('操作失败')
  }
}

const handleToggleEssence = async () => {
  if (!topic.value) return
  try {
    const res = await toggleTopicEssence(topicId)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      const isEssence = res.data.data
      topic.value.isEssence = isEssence
      ElMessage.success(isEssence ? '加精成功' : '取消加精成功')
    } else {
       ElMessage.error(res.data?.message || '操作失败')
    }
  } catch (error) {
    console.error('Essence topic error:', error)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  if (topicId) {
    loading.value = true
    Promise.all([fetchTopic(), fetchReplies()]).finally(() => {
      loading.value = false
    })
  } else {
    ElMessage.error('无效的帖子ID')
    router.push('/community')
  }
})
</script>

<style scoped>
.topic-detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  margin-bottom: 20px;
}

.main-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.topic-card, .replies-card, .editor-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.topic-card {
  padding: 30px;
}

.topic-header {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 24px;
  margin-bottom: 24px;
}

.topic-title {
  font-size: 24px;
  color: #303133;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  line-height: 1.4;
}

.badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fff;
  font-weight: normal;
  vertical-align: middle;
}

.badge.pinned { background: #f56c6c; }
.badge.essence { background: #e6a23c; }

.topic-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-details {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.publish-time {
  font-size: 12px;
  color: #909399;
}

.meta-right {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.topic-tags {
  display: flex;
  gap: 8px;
}

.custom-tag {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  font-size: 12px;
  line-height: 1;
  border-width: 1px;
  border-style: solid;
  border-radius: 4px;
  box-sizing: border-box;
  white-space: nowrap;
}

.topic-body {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  min-height: 150px;
  margin-bottom: 30px;
  white-space: pre-wrap;
}

.topic-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
}

.action-btn {
  min-width: 120px;
}

.replies-card {
  padding: 0;
}

.section-title {
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
}

.section-title h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.reply-list {
  padding: 0 24px;
}

.reply-item {
  display: flex;
  gap: 20px;
  padding: 24px 0;
  border-bottom: 1px solid #f0f2f5;
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-right {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.reply-author-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.reply-floor {
  color: #909399;
  font-size: 12px;
}

.reply-content-box {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}

.reply-quote {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}

.reply-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.reply-actions {
  display: flex;
  gap: 16px;
}

.action-link {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: color 0.2s;
}

.action-link:hover {
  color: #409EFF;
}

.action-link.active {
  color: #409EFF;
}

.pagination-wrapper {
  padding: 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #ebeef5;
}

.editor-card {
  padding: 24px;
}

.editor-header {
  margin-bottom: 16px;
}

.editor-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.editor-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
