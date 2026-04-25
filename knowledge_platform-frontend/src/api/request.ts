import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

export const buildLoginRedirectUrl = () => {
  const redirect = `${globalThis.location.pathname}${globalThis.location.search}${globalThis.location.hash}`
  return `/login?redirect=${encodeURIComponent(redirect)}`
}

// 判断是否为公开文档接口（游客可访问）
const isPublicDocumentApi = (url: string): boolean => {
  const publicPatterns = [
    /^\/api\/documents\/?$/,
    /^\/api\/documents\/search/,
    /^\/api\/documents\/stats/,
    /^\/api\/documents\/latest/,
    /^\/api\/documents\/popular/,
    /^\/api\/documents\/approved/,
    /^\/api\/documents\/top-rated/,
    /^\/api\/documents\/file-types\//,
    /^\/api\/documents\/\d+$/, // 单个文档详情
    /^\/api\/documents\/\d+\/preview-info$/,
    /^\/api\/documents\/\d+\/previewable$/,
    /^\/api\/documents\/category\/\d+$/,
    /^\/api\/categories\//,
    /^\/api\/tags\//,
    /^\/api\/comments\/documents\/\d+$/,
    /^\/api\/ratings\/documents\/\d+$/,
    /^\/api\/forum\//,
    /^\/api\/points\/leaderboard/,
    /^\/api\/points\/recent/,
  ]
  return publicPatterns.some(pattern => pattern.test(url))
}

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 添加认证令牌
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 如果是FormData，让浏览器自动设置Content-Type（包括boundary）
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  (error) => {
    console.error('Request error:', error)
    throw error
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 如果是 blob 或 text 类型响应，直接返回，避免后续逻辑干扰
    if (response.config.responseType === 'blob' || response.config.responseType === 'text') {
      return response
    }

    // 检查响应数据是否存在
    if (!response.data) {
      return response
    }

    // 如果响应数据是 Blob 或 ArrayBuffer，直接返回（异常情况）
    if (response.data instanceof Blob || response.data instanceof ArrayBuffer) {
      return response
    }

    const { code, message } = response.data

    // 业务成功
    if (code === 0 || code === 200) {
      return response
    }

    // 业务错误 - 不在这里显示错误消息，让调用方处理
    return Promise.reject(new Error(message || '操作失败'))
  },
  async (error) => {
    const { response } = error

    if (!response) {
      ElMessage.error('网络连接异常')
      throw error
    }

    const { status, data } = response

    switch (status) {
      case 401: {
        // 检查是否是公开接口（游客可访问），如果是则不跳转登录
        const requestUrl = response.config.url || ''
        const isPublicApi = isPublicDocumentApi(requestUrl)

        if (isPublicApi) {
          // 公开接口返回401，不跳转登录，直接返回错误让调用方处理
          throw error
        }

        // 如果是刷新令牌请求本身返回401，说明刷新令牌也失效了，直接跳转登录
        if (requestUrl.includes('/api/auth/refresh')) {
          const { useUserStore } = await import('@/stores/user')
          const userStore = useUserStore()
          userStore.clearAuth()
          globalThis.location.href = buildLoginRedirectUrl()
          throw error
        }

        // 未授权，尝试刷新令牌
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          try {
            // 动态导入以避免循环依赖
            const { useUserStore } = await import('@/stores/user')
            const userStore = useUserStore()

            // 确保刷新令牌操作完成
            const newAccessToken = await userStore.refreshAccessToken()

            // 重新发起原请求
            const originalRequest = error.config
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
            return request(originalRequest)
          } catch (refreshError) {
            // 刷新失败，清除认证信息并跳转登录
            const { useUserStore } = await import('@/stores/user')
            const userStore = useUserStore()
            userStore.clearAuth()

            globalThis.location.href = buildLoginRedirectUrl()
            throw refreshError
          }
        } else {
          // 没有刷新令牌，跳转登录
          ElMessage.error('登录已过期，请重新登录')
          globalThis.location.href = buildLoginRedirectUrl()
        }
        break
      }

      case 403:
        ElMessage.error('没有权限访问该资源')
        break

      case 404:
        ElMessage.error('请求的资源不存在')
        break

      case 500:
        // 对于500错误，如果有具体的业务错误消息，优先使用
        if (data?.message) {
          throw new Error(data.message)
        }
        ElMessage.error('服务器内部错误')
        break

      default:
        // 对于其他HTTP错误，如果有具体消息则使用，否则显示通用消息
        if (data?.message) {
          throw new Error(data.message)
        }
        ElMessage.error(`请求失败 (${status})`)
    }

    throw error
  }
)

// 封装通用请求方法
export const http = {
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> =>
    request.get(url, config),

  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> =>
    request.post(url, data, config),

  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> =>
    request.put(url, data, config),

  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> =>
    request.patch(url, data, config),

  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> =>
    request.delete(url, config)
}

export default request