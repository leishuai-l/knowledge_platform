import type { AxiosError } from 'axios'

const CANCEL_TOKENS = new Set(['cancel', 'close'])

/**
 * 判断用户是否主动取消了操作（如点击对话框的取消/关闭）
 */
export const isUserCancelled = (error: unknown): boolean => {
  if (!error) return false

  if (typeof error === 'string') {
    return CANCEL_TOKENS.has(error)
  }

  if (error instanceof Error) {
    if (error.message && CANCEL_TOKENS.has(error.message)) {
      return true
    }
  }

  const maybeError = error as Partial<AxiosError> & { code?: string; message?: string }
  if (maybeError.code === 'ERR_CANCELED') {
    return true
  }

  if (maybeError.message && CANCEL_TOKENS.has(maybeError.message)) {
    return true
  }

  return false
}

/**
 * 从后端响应或错误对象中提取友好的错误提示
 */
export const extractErrorMessage = (error: unknown, fallback: string): string => {
  if (!error) {
    return fallback
  }

  if (typeof error === 'string') {
    return CANCEL_TOKENS.has(error) ? fallback : error
  }

  const maybeAxios = error as Partial<AxiosError>

  if (maybeAxios.response?.data && typeof maybeAxios.response.data === 'object') {
    const data = maybeAxios.response.data as Record<string, unknown>
    const message = data.message || data.error || data.msg
    if (typeof message === 'string' && message.trim().length > 0) {
      return message
    }
  }

  const message = (error as { message?: string }).message
  if (message && !CANCEL_TOKENS.has(message)) {
    return message
  }

  return fallback
}
