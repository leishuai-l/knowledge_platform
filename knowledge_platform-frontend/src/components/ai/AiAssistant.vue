<template>
  <div class="ai-assistant">
    <!-- Chat Window -->
    <transition name="slide-fade">
      <div class="chat-window" v-if="isOpen">
        <div class="chat-header">
          <div class="header-left">
            <el-icon><Cpu /></el-icon>
            <span>智能问答助手</span>
          </div>
          <div class="header-actions">
            <el-icon class="action-btn" @click="toggleSessions"><List /></el-icon>
            <el-icon class="close-btn" @click="toggleChat"><Close /></el-icon>
          </div>
        </div>

  <!-- Conversation List -->
        <div v-if="showSessions" class="session-list">
          <div class="session-header">
            <span>对话列表</span>
            <el-button size="small" @click="createNewConversation">新建对话</el-button>
          </div>
          <div class="session-items">
            <div v-for="conv in conversations" :key="conv.id" class="session-item">
              <div class="session-info" @click="switchConversation(conv.id)">
                <div class="session-title">{{ conv.title }}</div>
                <div class="session-time">{{ formatTime(conv.updatedAt) }}</div>
              </div>
              <el-icon class="delete-icon" @click="deleteConversation(conv.id)"><Delete /></el-icon>
            </div>
          </div>
        </div>

        <div v-else class="chat-messages" ref="messagesRef">
          <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
            <div class="avatar">
              <el-icon v-if="msg.role === 'assistant'"><Cpu /></el-icon>
              <el-icon v-else><User /></el-icon>
            </div>
            <div class="content">
              <div v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)"></div>
              <div v-else>{{ msg.content }}</div>
            </div>
          </div>
          <div v-if="isStreaming" class="message assistant">
             <div class="avatar"><el-icon><Cpu /></el-icon></div>
             <div class="content loading-dots">
               <span>.</span><span>.</span><span>.</span>
             </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            placeholder="请输入您的问题..."
            @keyup.enter="sendMessage"
            :disabled="isStreaming"
          >
            <template #append>
              <el-button @click="sendMessage">
                <el-icon v-if="!isStreaming"><Position /></el-icon>
                <el-icon v-else><VideoPause /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { Close, Position, User, Cpu, VideoPause, List, Delete } from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { ElMessage } from 'element-plus'
import { buildLoginRedirectUrl } from '@/api/request'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || globalThis.location.origin).replace(/\/$/, '')
const isOpen = ref(false)
const inputMessage = ref('')
const isStreaming = ref(false)
const messagesRef = ref<HTMLElement | null>(null)
const abortController = ref<AbortController | null>(null)
const currentConversationId = ref<number | null>(null)
const conversations = ref<any[]>([])
const showSessions = ref(false)

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<Message[]>([
  { role: 'assistant', content: '您好！我是校园知识库的 AI 助手，有什么可以帮您？' }
])

const toggleChat = () => {
  isOpen.value = !isOpen.value
}

const renderMarkdown = (text: string) => {
  return DOMPurify.sanitize(marked.parse(text) as string)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
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

const doStreamRequest = async (token: string | null, userMsg: string, assistantMsgIndex: number) => {
  abortController.value = new AbortController()

  await fetchEventSource(`${apiBaseUrl}/api/ai/stream-chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({
      message: userMsg,
      conversationId: currentConversationId.value,
      messageHistory: messages.value
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
    onmessage(event) {
      if (event.event === 'conversation') {
        currentConversationId.value = Number(event.data)
        return
      }
      messages.value[assistantMsgIndex].content += event.data
      scrollToBottom()
    },
    onclose() {
      isStreaming.value = false
      abortController.value = null
    },
    onerror(err) {
      console.error('SSE错误:', err)
      isStreaming.value = false
      abortController.value = null
      throw err
    }
  })
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
    // 清空之前的回复内容，重新开始
    messages.value[assistantMsgIndex].content = ''
    isStreaming.value = true
    // 使用新token发送请求
    await doStreamRequestWithRetry(newToken, userMsg, assistantMsgIndex, true)
  } catch (refreshError) {
    console.error('Refresh access token failed:', refreshError)
    await redirectToLogin()
  }
}

const runStreamMessage = async (userMsg: string, assistantMsgIndex: number) => {
  try {
    const token = localStorage.getItem('accessToken')
    await doStreamRequestWithRetry(token, userMsg, assistantMsgIndex, false)
  } catch (error: any) {
    console.error('发送消息失败:', error)
    if (error.name === 'AbortError') {
      messages.value[assistantMsgIndex].content += '\n\n[已停止生成]'
      isStreaming.value = false
      return
    }

    if (error.message === 'UNAUTHORIZED') {
      await retryStreamAfterRefresh(userMsg, assistantMsgIndex)
      return
    }

    messages.value[assistantMsgIndex].content = '抱歉，连接 AI 服务失败。'
    isStreaming.value = false
  }
}

// 包装函数，处理认证重试逻辑
const doStreamRequestWithRetry = async (
  token: string | null,
  userMsg: string,
  assistantMsgIndex: number,
  hasRetried: boolean
): Promise<void> => {
  abortController.value = new AbortController()

  let is401Received = false

  try {
    await fetchEventSource(`${apiBaseUrl}/api/ai/stream-chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({
        message: userMsg,
        conversationId: currentConversationId.value,
        messageHistory: messages.value
      }),
      signal: abortController.value?.signal,
      async onopen(response) {
        if (response.status === 401) {
          is401Received = true
          // 如果没有重试过，尝试刷新token
          if (!hasRetried) {
            const storedRefreshToken = localStorage.getItem('refreshToken')
            if (storedRefreshToken) {
              try {
                const { useUserStore } = await import('@/stores/user')
                const userStore = useUserStore()
                const newToken = await userStore.refreshAccessToken()
                // 使用新token重新请求（标记为已重试）
                await doStreamRequestWithRetry(newToken, userMsg, assistantMsgIndex, true)
                return
              } catch (refreshError) {
                console.error('Token refresh failed in onopen:', refreshError)
              }
            }
          }
          throw new Error('UNAUTHORIZED')
        }
        if (!response.ok) {
          throw new Error(`HTTP_ERROR_${response.status}`)
        }
      },
      onmessage(event) {
        if (is401Received) return // 已经处理过401，忽略后续消息
        if (event.event === 'conversation') {
          currentConversationId.value = Number(event.data)
          return
        }
        messages.value[assistantMsgIndex].content += event.data
        scrollToBottom()
      },
      onclose() {
        isStreaming.value = false
        abortController.value = null
      },
      onerror(err) {
        console.error('SSE错误:', err)
        isStreaming.value = false
        abortController.value = null
        // 如果是401错误且还没有重试过，返回以便外部处理
        if (is401Received && !hasRetried) {
          return // 让外层Promise reject
        }
        throw err
      }
    })
  } catch (error) {
    // 如果是因为401导致的错误，且已经尝试过刷新，则抛出给外层处理
    if (is401Received) {
      throw new Error('UNAUTHORIZED')
    }
    throw error
  }
}

const sendMessage = async () => {
  if (isStreaming.value) {
    stopStreaming()
    return
  }

  if (!inputMessage.value.trim()) return

  const userMsg = inputMessage.value
  messages.value.push({ role: 'user', content: userMsg })
  inputMessage.value = ''
  isStreaming.value = true
  scrollToBottom()

  const assistantMsgIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })

  await runStreamMessage(userMsg, assistantMsgIndex)
}

const stopStreaming = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
    isStreaming.value = false
  }
}

const loadConversations = async () => {
  try {
    const response = await fetch(`${apiBaseUrl}/api/ai/conversations?page=0&size=20`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken') || ''}` }
    })
    const data = await response.json()
    if (data.success) {
      conversations.value = data.data.list
    }
  } catch (error) {
    console.error('加载对话失败', error)
  }
}

const createNewConversation = () => {
  currentConversationId.value = null
  messages.value = [{ role: 'assistant', content: '您好！我是校园知识库的 AI 助手，有什么可以帮您？' }]
  showSessions.value = false
  loadConversations()
}

const switchConversation = async (conversationId: number) => {
  try {
    const response = await fetch(`${apiBaseUrl}/api/ai/conversations/${conversationId}/messages`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken') || ''}` }
    })
    const data = await response.json()
    if (data.success) {
      currentConversationId.value = conversationId
      messages.value = data.data
      showSessions.value = false
    }
  } catch (error) {
    console.error('加载对话消息失败', error)
  }
}

const deleteConversation = async (conversationId: number) => {
  try {
    await fetch(`${apiBaseUrl}/api/ai/conversations/${conversationId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken') || ''}` }
    })
    loadConversations()
    if (currentConversationId.value === conversationId) {
      createNewConversation()
    }
  } catch (error) {
    console.error('删除对话失败', error)
  }
}

const toggleSessions = () => {
  showSessions.value = !showSessions.value
  if (showSessions.value) {
    loadConversations()
  }
}

const formatTime = (time: string) => {
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
  return date.toLocaleDateString()
}

watch(messages, () => scrollToBottom(), { deep: true })
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 2000;
}

.chat-window {
  width: 350px;
  height: 500px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: #f5f7fa;
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn, .close-btn {
  cursor: pointer;
  color: #909399;
}

.session-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.session-header {
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  font-weight: bold;
}

.session-items {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.session-item:hover {
  background: #e6f7ff;
}

.session-info {
  flex: 1;
}

.session-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: #909399;
}

.delete-icon {
  color: #f56c6c;
  cursor: pointer;
}

.chat-messages {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  background: #fff;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
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

.content {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.message.assistant .content {
  background: #f4f4f5;
  color: #606266;
  border-top-left-radius: 2px;
}

.message.user .content {
  background: #409EFF;
  color: white;
  border-top-right-radius: 2px;
}

.chat-input {
  padding: 15px;
  border-top: 1px solid #ebeef5;
}

.loading-dots span {
  animation: dots 1.4s infinite ease-in-out both;
  margin: 0 2px;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes dots {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease-out;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(20px);
  opacity: 0;
}
</style>
