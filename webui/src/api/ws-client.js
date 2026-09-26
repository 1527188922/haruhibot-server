import { baseUrl } from '@/config/env'
import { getToken } from '@/util/auth'

/**
 * webui 全局WebSocket客户端(单例)
 *
 * 登录后建立一条连接，任何页面/组件都可以：
 *   this.$ws.on(type, handler)     注册消息监听(返回反注册函数)
 *   this.$ws.send(type, data)      发送消息并按id等待响应(Promise)
 *   this.$ws.subscribe('jm.task')  订阅主题(引用计数，多组件订阅只发一次)
 *
 * 约定：
 *   - 后端路径 /api/webui/ws?token=xxx，与机器人反向ws(/api/ws)无关
 *   - 服务端推送与请求响应都是同一个信封 {type,id,ts,message,data}
 *   - 未连接时 connect() 会自动重试；token失效(4001)会派发 auth.expired 事件
 */

const HEARTBEAT_INTERVAL_MILLIS = 25 * 1000
const HEARTBEAT_TIMEOUT_MILLIS = 60 * 1000
const RECONNECT_BASE_DELAY_MILLIS = 1000
const RECONNECT_MAX_DELAY_MILLIS = 30 * 1000
const REQUEST_TIMEOUT_MILLIS = 10 * 1000

/**
 * 服务端自定义关闭码，与后端 WebuiWsCloseCodes 保持一致
 */
export const WS_CLOSE_AUTH_EXPIRED = 4001
export const WS_CLOSE_LOGOUT = 4002

/**
 * 事件类型：登录态失效，需要重新登录
 */
export const WS_EVENT_AUTH_EXPIRED = 'auth.expired'

class WebuiWsClient {

  constructor() {
    this.socket = null
    this.status = 'idle'
    this.handlers = new Map()
    this.statusHandlers = new Set()
    this.pendingRequests = new Map()
    this.topicRefCount = new Map()
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.reconnectAttempts = 0
    this.lastMessageAt = 0
    this.manualClosed = false
    this.sequence = 0
    this.boundOnline = null
    this.boundVisibility = null
  }

  /**
   * 应用启动时调用一次：注册网络/可见性监听，有token则立即连接
   */
  bootstrap() {
    if (typeof window === 'undefined' || !window.WebSocket) {
      return
    }
    if (!this.boundOnline) {
      this.boundOnline = () => this.connect()
      this.boundVisibility = () => {
        if (!document.hidden) {
          this.connect()
        }
      }
      window.addEventListener('online', this.boundOnline)
      document.addEventListener('visibilitychange', this.boundVisibility)
    }
    if (getToken(false)) {
      this.connect()
    }
  }

  isOpen() {
    return !!this.socket && this.socket.readyState === WebSocket.OPEN
  }

  getStatus() {
    return this.status
  }

  buildUrl() {
    const token = getToken(false)
    const wsBase = baseUrl.replace(/^http/, 'ws')
    const separator = wsBase.indexOf('?') === -1 ? '?' : '&'
    return `${wsBase}/webui/ws${separator}token=${encodeURIComponent(token)}`
  }

  connect() {
    if (typeof window === 'undefined' || !window.WebSocket) {
      return
    }
    if (!getToken(false)) {
      return
    }
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }
    this.clearReconnectTimer()
    this.manualClosed = false
    this.setStatus('connecting')

    let socket
    try {
      socket = new WebSocket(this.buildUrl())
    } catch (e) {
      this.scheduleReconnect()
      return
    }
    this.socket = socket

    socket.onopen = () => {
      this.reconnectAttempts = 0
      this.lastMessageAt = Date.now()
      this.setStatus('open')
      this.restoreSubscriptions()
      this.startHeartbeat()
    }
    socket.onmessage = (event) => this.handleRawMessage(event)
    socket.onerror = () => {
      // 错误后紧跟 onclose，统一在 onclose 里处理重连
    }
    socket.onclose = (event) => {
      this.stopHeartbeat()
      this.rejectPendingRequests('WebSocket连接已断开')
      if (this.socket === socket) {
        this.socket = null
      }
      this.setStatus('closed')
      if (event && event.code === WS_CLOSE_AUTH_EXPIRED) {
        // 登录态失效：不再重连，交给上层跳登录
        this.manualClosed = true
        this.emit(WS_EVENT_AUTH_EXPIRED, null)
        return
      }
      if (event && event.code === WS_CLOSE_LOGOUT) {
        this.manualClosed = true
        return
      }
      this.scheduleReconnect()
    }
  }

  /**
   * 主动关闭且不再自动重连
   */
  close(code, reason) {
    this.manualClosed = true
    this.clearReconnectTimer()
    this.stopHeartbeat()
    this.rejectPendingRequests('WebSocket已关闭')
    const socket = this.socket
    this.socket = null
    if (socket) {
      try {
        socket.close(code, reason)
      } catch (e) {
        // 忽略：关闭失败不影响后续重连
      }
    }
    this.setStatus('closed')
  }

  scheduleReconnect() {
    if (this.manualClosed || this.reconnectTimer || !getToken(false)) {
      return
    }
    if (this.socket && (this.socket.readyState === WebSocket.CONNECTING || this.socket.readyState === WebSocket.OPEN)) {
      return
    }
    const delay = Math.min(RECONNECT_BASE_DELAY_MILLIS * Math.pow(2, this.reconnectAttempts), RECONNECT_MAX_DELAY_MILLIS)
    this.reconnectAttempts += 1
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, delay)
  }

  clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 注册消息监听，返回反注册函数
   */
  on(type, handler) {
    if (!type || typeof handler !== 'function') {
      return () => {}
    }
    if (!this.handlers.has(type)) {
      this.handlers.set(type, new Set())
    }
    this.handlers.get(type).add(handler)
    return () => this.off(type, handler)
  }

  off(type, handler) {
    const set = this.handlers.get(type)
    if (!set) {
      return
    }
    set.delete(handler)
    if (set.size === 0) {
      this.handlers.delete(type)
    }
  }

  /**
   * 连接状态监听，注册时立即回调一次当前状态
   */
  onStatus(handler) {
    if (typeof handler !== 'function') {
      return () => {}
    }
    this.statusHandlers.add(handler)
    handler(this.status)
    return () => this.statusHandlers.delete(handler)
  }

  /**
   * 发送消息并按id等待服务端响应
   */
  send(type, data, options) {
    const opts = options || {}
    if (!this.isOpen()) {
      return Promise.reject(new Error('WebSocket未连接'))
    }
    const id = opts.id || `c-${++this.sequence}`
    const timeout = opts.timeout === undefined ? REQUEST_TIMEOUT_MILLIS : opts.timeout
    return new Promise((resolve, reject) => {
      const timer = timeout > 0
        ? setTimeout(() => {
          this.pendingRequests.delete(id)
          reject(new Error('WebSocket请求超时'))
        }, timeout)
        : null
      this.pendingRequests.set(id, { resolve, reject, timer })
      this.rawSend({ type, id, data })
    })
  }

  rawSend(payload) {
    if (!this.isOpen()) {
      return false
    }
    try {
      this.socket.send(JSON.stringify(payload))
      return true
    } catch (e) {
      return false
    }
  }

  /**
   * 订阅主题，引用计数：最后一个订阅者取消时才通知服务端
   */
  subscribe(topic) {
    if (!topic) {
      return
    }
    const count = this.topicRefCount.get(topic) || 0
    this.topicRefCount.set(topic, count + 1)
    if (count === 0 && this.isOpen()) {
      this.rawSend({ type: 'subscribe', data: { topics: [topic] } })
    }
  }

  unsubscribe(topic) {
    if (!topic) {
      return
    }
    const count = this.topicRefCount.get(topic) || 0
    if (count > 1) {
      this.topicRefCount.set(topic, count - 1)
      return
    }
    this.topicRefCount.delete(topic)
    if (this.isOpen()) {
      this.rawSend({ type: 'unsubscribe', data: { topics: [topic] } })
    }
  }

  /**
   * 重连后恢复订阅关系
   */
  restoreSubscriptions() {
    const topics = Array.from(this.topicRefCount.keys())
    if (topics.length > 0) {
      this.rawSend({ type: 'subscribe', data: { topics } })
    }
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (!this.isOpen()) {
        return
      }
      if (Date.now() - this.lastMessageAt > HEARTBEAT_TIMEOUT_MILLIS) {
        // 连接可能已被代理静默断开，主动重建
        this.forceReconnect()
        return
      }
      this.rawSend({ type: 'ping' })
    }, HEARTBEAT_INTERVAL_MILLIS)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  forceReconnect() {
    const socket = this.socket
    this.socket = null
    this.stopHeartbeat()
    if (socket) {
      try {
        socket.close()
      } catch (e) {
        // 忽略
      }
    }
    this.scheduleReconnect()
  }

  handleRawMessage(event) {
    this.lastMessageAt = Date.now()
    let message
    try {
      message = JSON.parse(event.data)
    } catch (e) {
      return
    }
    if (!message || !message.type) {
      return
    }
    if (message.id && this.pendingRequests.has(message.id)) {
      const pending = this.pendingRequests.get(message.id)
      this.pendingRequests.delete(message.id)
      if (pending.timer) {
        clearTimeout(pending.timer)
      }
      if (message.type === 'error') {
        pending.reject(new Error(message.message || '请求失败'))
      } else {
        pending.resolve(message)
      }
    }
    this.emit(message.type, message.data, message)
  }

  emit(type, data, message) {
    const set = this.handlers.get(type)
    if (!set || set.size === 0) {
      return
    }
    Array.from(set).forEach(handler => {
      try {
        handler(data, message)
      } catch (e) {
        // 单个监听器异常不影响其他监听器
      }
    })
  }

  setStatus(status) {
    if (this.status === status) {
      return
    }
    this.status = status
    Array.from(this.statusHandlers).forEach(handler => {
      try {
        handler(status)
      } catch (e) {
        // 忽略
      }
    })
  }

  rejectPendingRequests(reason) {
    if (this.pendingRequests.size === 0) {
      return
    }
    const pendings = Array.from(this.pendingRequests.values())
    this.pendingRequests.clear()
    pendings.forEach(pending => {
      if (pending.timer) {
        clearTimeout(pending.timer)
      }
      pending.reject(new Error(reason))
    })
  }
}

const webuiWsClient = new WebuiWsClient()

export default webuiWsClient
