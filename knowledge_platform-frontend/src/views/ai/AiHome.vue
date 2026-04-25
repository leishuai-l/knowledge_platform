<template>
  <Layout>
    <div class="ai-home-container">
      <div class="ai-layout">
        <!-- Left Sidebar: Features/History -->
        <div class="ai-sidebar">
          <div class="sidebar-header">
            <el-icon :size="24" color="#409EFF"><Cpu /></el-icon>
            <h3>AI 智能助手</h3>
          </div>
          
          <div class="feature-list">
            <div 
              class="feature-item" 
              :class="{ active: currentTab === 'chat' }"
              @click="handleTabChange('chat')"
            >
              <el-icon><ChatDotRound /></el-icon>
              <span>智能问答</span>
              <el-button 
                circle 
                size="small" 
                type="primary" 
                plain 
                @click.stop="startNewChat"
                style="margin-left: auto;"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>

            <!-- History Section nested under Smart QA -->
            <div v-if="currentTab === 'chat'" class="history-section">
              <div class="history-header">
                <span class="history-title">历史对话</span>
                <el-button link size="small" @click="fetchChatHistory">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
              <div class="history-list" v-loading="historyLoading">
                <div
                  v-for="item in chatHistory"
                  :key="item.id"
                  class="history-item"
                  :class="{ active: currentSessionId === item.id }"
                  @click="loadHistoryItem(item)"
                >
                  <div class="history-content">
                    <span class="history-text">{{ item.title }}</span>
                    <span class="history-time">{{ formatHistoryTime(item.updatedAt) }}</span>
                  </div>
                  <el-icon class="delete-icon" @click.stop="deleteHistory(item.id)"><Close /></el-icon>
                </div>
                <div v-if="chatHistory.length === 0 && !historyLoading" class="empty-history">
                  暂无历史记录
                </div>
              </div>
            </div>

            <div 
              class="feature-item"
              :class="{ active: currentTab === 'recommend' }"
              @click="handleTabChange('recommend')"
            >
              <el-icon><DocumentIcon /></el-icon>
              <span>文档推荐</span>
            </div>
          </div>
        </div>

        <!-- Main Content Area -->
        <div class="ai-main">
          <!-- Chat Tab -->
          <div v-if="currentTab === 'chat'" class="chat-container">
            <div class="chat-messages" ref="messagesRef">
              <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
                <div class="avatar">
                  <el-icon v-if="msg.role === 'assistant'" :size="20"><Cpu /></el-icon>
                  <el-icon v-else :size="20"><User /></el-icon>
                </div>
                <div class="message-content">
                  <div class="sender-name">{{ msg.role === 'assistant' ? '知享助手' : '我' }}</div>
                  <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
                  <div v-if="msg.role === 'assistant' && msg.content" class="message-actions">
                    <el-button link size="small" @click="copyMessage(msg.content)">
                      <el-icon><DocumentCopy /></el-icon> 复制
                    </el-button>
                    <el-button v-if="index === messages.length - 1 && !loading" link size="small" @click="regenerateMessage(index)">
                      <el-icon><Refresh /></el-icon> 重新生成
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <div class="chat-input-area">
              <div class="new-chat-action">
                <el-button link type="primary" @click="startNewChat">
                  <el-icon><Plus /></el-icon> 新建对话
                </el-button>
              </div>
              <div class="input-wrapper">
                <el-input
                  v-model="inputMessage"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入您的问题，支持 Shift + Enter 换行..."
                  @keydown.enter.prevent="handleEnter"
                  resize="none"
                />
                <div class="input-actions">
                  <el-button type="primary" @click="loading ? stopStreaming() : sendMessage()" :loading="false">
                    <el-icon v-if="!loading"><Position /></el-icon>
                    <el-icon v-else><VideoPause /></el-icon>
                    {{ loading ? '暂停' : '发送' }}
                  </el-button>
                </div>
              </div>
              <div class="input-tip">
                AI 生成内容仅供参考，请核对重要信息。
              </div>
            </div>
          </div>

          <!-- Recommendation Tab -->
          <div v-else-if="currentTab === 'recommend'" class="recommend-container" v-loading="recommendLoading">
            <div v-if="recommendations.length > 0" class="recommend-list">
              <DocumentCard 
                v-for="doc in recommendations" 
                :key="doc.id" 
                :document="doc"
                @click="$router.push(`/documents/${doc.id}`)"
              />
            </div>
            <div v-else class="empty-state">
              <el-icon :size="60" color="#909399"><DocumentIcon /></el-icon>
              <h3>智能文档推荐</h3>
              <p>AI 将根据您的学习历史为您推荐相关文档</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Document as DocumentIcon, Cpu, User, Position, Plus, Close, VideoPause, DocumentCopy, Refresh } from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { ElMessage, ElMessageBox } from 'element-plus'
import Layout from '@/components/Layout.vue'
import DocumentCard from '@/components/DocumentCard.vue'
import { buildLoginRedirectUrl, http } from '@/api/request'
import type { Document } from '@/types'

const route = useRoute()
const router = useRouter()

interface Message {
  role: 'user' | 'assistant'
  content: string
}

interface ChatConversationItem {
  id: number
  title: string
  createdAt: string
  updatedAt: string
  messageCount: number
}

const currentTab = ref('chat')
const recommendations = ref<Document[]>([])
const recommendLoading = ref(false)
const chatHistory = ref<ChatConversationItem[]>([])
const historyLoading = ref(false)

const messages = ref<Message[]>([
  { role: 'assistant', content: '# 您好！\n我是知享助手。您可以问我关于知识库文档的问题，或者让我为您推荐相关资料。' }
])

const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement | null>(null)
const currentDocumentId = ref<number | null>(null)
const currentDocumentTitle = ref<string>('')
const currentSessionId = ref<number | null>(null)
const abortController = ref<AbortController | null>(null)

// Initialize from route query
const initFromQuery = () => {
  const { documentId, title } = route.query
  if (documentId) {
    currentDocumentId.value = Number(documentId)
    currentDocumentTitle.value = title as string || '当前文档'
    messages.value = [
      { role: 'assistant', content: `# 您好！\n我已准备好回答关于文档 **《${currentDocumentTitle.value}》** 的问题。` }
    ]
  }
}

// Load chat history
const fetchChatHistory = async () => {
  historyLoading.value = true
  try {
    const res = await http.get('/api/ai/conversations')
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      chatHistory.value = res.data.data.list
    }
  } catch (error) {
    console.error('Failed to fetch chat history:', error)
  } finally {
    historyLoading.value = false
  }
}

const deleteHistory = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条对话记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await http.delete(`/api/ai/conversations/${id}`)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      ElMessage.success('删除成功')
      fetchChatHistory()
      startNewChat()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete history:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

const loadHistoryItem = async (item: ChatConversationItem) => {
  try {
    const res = await http.get(`/api/ai/conversations/${item.id}/messages`)
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      messages.value = res.data.data
      currentSessionId.value = item.id
      scrollToBottom()
    }
  } catch (error) {
    console.error('Failed to load session messages:', error)
    ElMessage.error('加载会话失败')
  }
}

const handleTabChange = (tab: string) => {
  currentTab.value = tab
  if (currentTab.value === 'chat') {
    scrollToBottom()
  } else if (tab === 'recommend' && recommendations.value.length === 0) {
    fetchRecommendations()
  }
}

onMounted(() => {
  initFromQuery()
  fetchChatHistory()
  scrollToBottom()
})

const fetchRecommendations = async () => {
  recommendLoading.value = true
  try {
    const res = await http.get<Document[]>('/api/ai/recommend')
    if (res.data && (res.data.code === 200 || res.data.code === 0)) {
      recommendations.value = res.data.data
    }
  } catch (error) {
    console.error('Failed to fetch recommendations:', error)
  } finally {
    recommendLoading.value = false
  }
}

const startNewChat = () => {
  messages.value = [
    { role: 'assistant', content: '# 您好！\n我是知享助手。您可以问我关于知识库文档的问题，或者让我为您推荐相关资料。' }
  ]
  inputMessage.value = ''
  currentTab.value = 'chat'
  currentDocumentId.value = null
  currentDocumentTitle.value = ''
  currentSessionId.value = null

  // Clear query params
  router.replace({ query: {} })

  if (messagesRef.value) {
    messagesRef.value.scrollTop = 0
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const renderMarkdown = (text: string) => {
  if (!text) return ''
  return DOMPurify.sanitize(marked.parse(text) as string)
}

const handleEnter = (e: KeyboardEvent) => {
  if (!e.shiftKey) {
    sendMessage()
  }
}

const doStreamRequest = async (token: string | null, userMsg: string, assistantMsgIndex: number) => {
  abortController.value = new AbortController()

  await fetchEventSource('/api/ai/stream-chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({
      message: userMsg,
      documentId: currentDocumentId.value,
      conversationId: currentSessionId.value
    }),
    signal: abortController.value.signal,
    async onopen(response) {
      if (response.status === 401) {
        throw new Error('UNAUTHORIZED')
      }
      if (!response.ok) {
        throw new Error(`HTTP_ERROR_${response.status}`)
      }
    },
    onmessage(msg) {
      if (msg.event === 'conversation') {
        currentSessionId.value = Number(msg.data)
        return
      }
      if (messages.value[assistantMsgIndex]) {
        messages.value[assistantMsgIndex].content += msg.data
      }
      scrollToBottom()
    },
    onclose() {
      loading.value = false
      abortController.value = null
      fetchChatHistory()
    },
    onerror(err) {
      console.error('Stream error:', err)
      loading.value = false
      abortController.value = null
      throw err
    }
  })
}

const redirectToLogin = async () => {
  const { useUserStore } = await import('@/stores/user')
  const userStore = useUserStore()
  userStore.clearAuth()
  ElMessage.error('登录已过期，请重新登录')
  globalThis.location.href = buildLoginRedirectUrl()
}

const markStreamStopped = (assistantMsgIndex: number) => {
  if (messages.value[assistantMsgIndex]) {
    messages.value[assistantMsgIndex].content += '\n\n[已停止生成]'
  }
  loading.value = false
}

const setStreamErrorMessage = (assistantMsgIndex: number) => {
  if (messages.value[assistantMsgIndex] && !messages.value[assistantMsgIndex].content) {
    messages.value[assistantMsgIndex].content = '**连接失败**：无法连接到 AI 服务。请检查网络或联系管理员配置 API Key。'
  }
  loading.value = false
}

const retryStreamAfterRefresh = async (userMsg: string, assistantMsgIndex: number) => {
  const storedRefreshToken = localStorage.getItem('refreshToken')
  if (!storedRefreshToken) {
    await redirectToLogin()
    return
  }

  try {
    const { useUserStore } = await import('@/stores/user')
    const userStore = useUserStore()
    const newToken = await userStore.refreshAccessToken()
    messages.value[assistantMsgIndex].content = ''
    loading.value = true
    await doStreamRequest(newToken, userMsg, assistantMsgIndex)
  } catch (refreshError) {
    console.error('Refresh access token failed:', refreshError)
    await redirectToLogin()
  }
}

const runStreamMessage = async (userMsg: string, assistantMsgIndex: number) => {
  try {
    const token = localStorage.getItem('accessToken')
    await doStreamRequest(token, userMsg, assistantMsgIndex)
  } catch (error: any) {
    if (error.name === 'AbortError') {
      markStreamStopped(assistantMsgIndex)
      return
    }

    if (error.message === 'UNAUTHORIZED') {
      await retryStreamAfterRefresh(userMsg, assistantMsgIndex)
      return
    }

    console.error(error)
    setStreamErrorMessage(assistantMsgIndex)
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  const userMsg = inputMessage.value
  messages.value.push({ role: 'user', content: userMsg })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  const assistantMsgIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })

  await runStreamMessage(userMsg, assistantMsgIndex)
}

const stopStreaming = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
}

const copyMessage = async (content: string) => {
  try {
    const plainText = content.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ')
    await navigator.clipboard.writeText(plainText)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const regenerateMessage = async (index: number) => {
  if (index < 1 || loading.value) return

  const userMessage = messages.value[index - 1]
  if (userMessage.role !== 'user') return

  messages.value[index].content = ''
  loading.value = true
  await runStreamMessage(userMessage.content, index)
}

const formatHistoryTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.ai-home-container {
  height: calc(100vh - 110px); /* Subtract header/footer height approx */
  background: #f0f2f5;
  padding: 20px;
  display: flex;
  justify-content: center;
}

.ai-layout {
  width: 100%;
  max-width: 1200px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  display: flex;
  overflow: hidden;
}

.ai-sidebar {
  width: 260px;
  background: #f8f9fa;
  border-right: 1px solid #eef0f5;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #eef0f5;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.feature-list {
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 15px;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  transition: all 0.2s;
}

.feature-item:hover {
  background: #eef0f5;
  color: #409EFF;
}

.feature-item.active {
  background: #ecf5ff;
  color: #409EFF;
  font-weight: 500;
}

.history-section {
  padding: 0 15px 15px 35px;
  overflow-y: auto;
  max-height: 400px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  margin-bottom: 8px;
  border-bottom: 1px solid #eef0f5;
}

.history-title {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-left: 2px solid #eef0f5;
  padding-left: 10px;
}

.history-item {
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 6px;
  transition: all 0.2s;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.history-item.active {
  background: #ecf5ff;
  color: #409EFF;
  border-left: 2px solid #409EFF;
  margin-left: -2px;
  padding-left: 10px;
}

.history-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.history-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
}

.history-time {
  font-size: 11px;
  color: #c0c4cc;
}

.delete-icon {
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 14px;
  color: #909399;
  padding: 2px;
}

.delete-icon:hover {
  color: #F56C6C;
  background: #fef0f0;
  border-radius: 50%;
}

.history-item:hover .delete-icon {
  opacity: 1;
}

.history-item:hover {
  background: #f5f7fa;
  color: #409EFF;
}

.empty-history {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 20px 0;
}

.ai-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-messages {
  flex: 1;
  padding: 30px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 15px;
  max-width: 80%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.assistant .avatar {
  background: #e6f7ff;
  color: #1890ff;
}

.message.user .avatar {
  background: #f6ffed;
  color: #52c41a;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.sender-name {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.message.user .sender-name {
  text-align: right;
  margin-right: 2px;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
}

.message.assistant .bubble {
  background: #f4f4f5;
  color: #303133;
  border-top-left-radius: 2px;
}

.message.user .bubble {
  background: #409EFF;
  color: white;
  border-top-right-radius: 2px;
}

.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message:hover .message-actions {
  opacity: 1;
}

.message-actions .el-button {
  padding: 4px 8px;
  font-size: 12px;
  color: #909399;
}

.message-actions .el-button:hover {
  color: #409EFF;
}

.chat-input-area {
  padding: 10px 30px 20px;
  border-top: 1px solid #eef0f5;
  background: #fff;
}

.new-chat-action {
  margin-bottom: 10px;
  display: flex;
  justify-content: flex-end;
}

.input-wrapper {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: #409EFF;
}

:deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 12px;
  padding-bottom: 50px; /* Space for button */
  background: transparent;
}

.input-actions {
  position: absolute;
  bottom: 10px;
  right: 10px;
}

.input-tip {
  margin-top: 8px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.loading-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 24px;
}

.dot {
  width: 6px;
  height: 6px;
  background: #909399;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Export & Recommend Styles */
.recommend-container {
  flex: 1;
  display: flex;
  flex-direction: column; /* Allow list to scroll */
  align-items: center;
  justify-content: flex-start; /* Start from top */
  padding: 40px;
  background: #fcfcfc;
  overflow-y: auto; /* Enable scrolling */
}

.recommend-list {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding-bottom: 20px;
}

.empty-state {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  max-width: 400px;
}

.empty-state h3 {
  margin: 0;
  color: #303133;
  font-size: 20px;
}

.empty-state p {
  margin: 0;
  color: #909399;
  line-height: 1.6;
}

/* Markdown Styles */
:deep(p) { margin: 0 0 10px 0; }
:deep(p:last-child) { margin: 0; }
:deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 10px 0;
}
:deep(code) {
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
}
</style>
