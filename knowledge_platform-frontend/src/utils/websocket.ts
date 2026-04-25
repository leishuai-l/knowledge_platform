import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { Notification, BatchUpdateNotification, AllReadNotification } from '@/types'

export interface WebSocketCallbacks {
  onNotification?: (notification: Notification) => void
  onBatchUpdate?: (update: BatchUpdateNotification) => void
  onAllRead?: (update: AllReadNotification) => void
  onSystemAnnouncement?: (announcement: Notification) => void
  onConnect?: () => void
  onDisconnect?: () => void
  onError?: (error: any) => void
}

const wsBaseUrl = (import.meta.env.VITE_API_BASE_URL || globalThis.location.origin).replace(/\/$/, '')

class WebSocketService {
  private client: Client | null = null
  private callbacks: WebSocketCallbacks = {}
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectInterval = 5000 // 5秒
  private isManualDisconnect = false

  constructor() {
    this.setupClient()
  }

  private setupClient() {
    this.client = new Client({
      // 使用SockJS作为WebSocket的fallback
      webSocketFactory: () => new SockJS(`${wsBaseUrl}/ws`),
      connectHeaders: {},
      debug: (str) => {
        console.log('WebSocket Debug:', str)
      },
      reconnectDelay: this.reconnectInterval,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    // 连接成功回调
    this.client.onConnect = (frame) => {
      console.log('WebSocket连接成功:', frame)
      this.reconnectAttempts = 0
      this.callbacks.onConnect?.()
      this.subscribeToChannels()
    }

    // 连接错误回调
    this.client.onStompError = (frame) => {
      console.error('WebSocket STOMP错误:', frame.headers['message'])
      console.error('错误详情:', frame.body)
      this.callbacks.onError?.(frame)
    }

    // WebSocket错误回调
    this.client.onWebSocketError = (error) => {
      console.error('WebSocket错误:', error)
      this.callbacks.onError?.(error)
    }

    // 断开连接回调
    this.client.onDisconnect = () => {
      console.log('WebSocket连接断开')
      this.callbacks.onDisconnect?.()

      // 如果不是手动断开，尝试重连
      if (!this.isManualDisconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++
        console.log(`尝试重连... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
        setTimeout(() => {
          if (this.client && !this.isManualDisconnect) {
            this.connect()
          }
        }, this.reconnectInterval)
      }
    }
  }

  private subscribeToChannels() {
    if (!this.client?.connected) return

    const userId = this.getCurrentUserId()
    if (!userId) {
      console.warn('用户未登录，无法订阅个人通知')
      return
    }

    // 订阅个人通知
    this.client.subscribe(`/user/queue/notifications`, (message) => {
      try {
        const notification: Notification = JSON.parse(message.body)
        console.log('收到个人通知:', notification)
        this.callbacks.onNotification?.(notification)
      } catch (error) {
        console.error('解析通知消息失败:', error)
      }
    })

    // 订阅批量更新通知
    this.client.subscribe(`/user/queue/batch-update`, (message) => {
      try {
        const update: BatchUpdateNotification = JSON.parse(message.body)
        console.log('收到批量更新通知:', update)
        this.callbacks.onBatchUpdate?.(update)
      } catch (error) {
        console.error('解析批量更新消息失败:', error)
      }
    })

    // 订阅全部已读通知
    this.client.subscribe(`/user/queue/all-read`, (message) => {
      try {
        const update: AllReadNotification = JSON.parse(message.body)
        console.log('收到全部已读通知:', update)
        this.callbacks.onAllRead?.(update)
      } catch (error) {
        console.error('解析全部已读消息失败:', error)
      }
    })

    // 订阅系统公告
    this.client.subscribe('/topic/announcements', (message) => {
      try {
        const announcement: Notification = JSON.parse(message.body)
        console.log('收到系统公告:', announcement)
        this.callbacks.onSystemAnnouncement?.(announcement)
      } catch (error) {
        console.error('解析系统公告失败:', error)
      }
    })

    // 发送连接确认
    this.sendMessage('/app/connect', {})
  }

  private getAuthHeader(): string {
    const token = localStorage.getItem('accessToken')
    return token ? `Bearer ${token}` : ''
  }

  private getCurrentUserId(): string | null {
    // 从localStorage或其他地方获取当前用户ID
    const token = localStorage.getItem('accessToken')
    if (!token) return null

    try {
      // JWT使用Base64URL编码，需要正确解码
      const base64Url = token.split('.')[1]
      // 将Base64URL转换为标准Base64
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
      // 添加padding
      const padded = base64 + '='.repeat((4 - base64.length % 4) % 4)
      const payload = JSON.parse(atob(padded))
      return payload.sub || payload.userId
    } catch {
      return null
    }
  }

  // 连接WebSocket
  connect() {
    if (this.client?.connected) {
      console.log('WebSocket已连接')
      return
    }

    // 重新设置认证头（token可能已更新）
    if (this.client) {
      const authHeader = this.getAuthHeader()
      if (authHeader) {
        this.client.connectHeaders = {
          Authorization: authHeader
        }
      }
    }

    this.isManualDisconnect = false
    this.client?.activate()
  }

  // 断开WebSocket连接
  disconnect() {
    this.isManualDisconnect = true
    this.client?.deactivate()
  }

  // 发送消息
  sendMessage(destination: string, body: any) {
    if (this.client?.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body)
      })
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  // 标记通知为已读
  markNotificationAsRead(notificationId: number) {
    this.sendMessage('/app/notification/read', { notificationId })
  }

  // 获取未读通知数量
  getUnreadCount() {
    this.sendMessage('/app/notification/unread-count', {})
  }

  // 发送心跳
  sendHeartbeat() {
    this.sendMessage('/app/heartbeat', { timestamp: Date.now() })
  }

  // 设置回调函数
  setCallbacks(callbacks: WebSocketCallbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks }
  }

  // 获取连接状态
  isConnected(): boolean {
    return this.client?.connected || false
  }

  // 清理资源
  destroy() {
    this.disconnect()
    this.callbacks = {}
  }
}

// 单例模式
export const webSocketService = new WebSocketService()
export default webSocketService