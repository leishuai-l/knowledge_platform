import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, RegisterRequest, JwtResponse } from '@/types'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const avatarTimestamp = ref<number>(Date.now())

  // 计算属性
  const isLoggedIn = computed(() => !!user.value && !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  // 方法
  const setTokens = (tokens: { accessToken: string; refreshToken: string }) => {
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    localStorage.setItem('accessToken', tokens.accessToken)
    localStorage.setItem('refreshToken', tokens.refreshToken)
  }

  const setUser = (userData: User) => {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
  }

  const clearAuth = () => {
    user.value = null
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  // 从本地存储恢复用户信息
  const restoreAuth = () => {
    const storedUser = localStorage.getItem('user')
    const storedAccessToken = localStorage.getItem('accessToken')

    if (storedUser && storedAccessToken) {
      try {
        user.value = JSON.parse(storedUser)
        accessToken.value = storedAccessToken
        refreshToken.value = localStorage.getItem('refreshToken')
      } catch (error) {
        console.error('Failed to restore auth state:', error)
        clearAuth()
      }
    }
  }

  // 登录
  const login = async (loginData: LoginRequest): Promise<JwtResponse> => {
    try {
      const response = await authApi.login(loginData)
      const result = response.data.data

      setTokens({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken
      })
      setUser(result.user)

      return result
    } catch (error) {
      clearAuth()
      throw error
    }
  }

  // 注册
  const register = async (registerData: RegisterRequest): Promise<JwtResponse> => {
    try {
      const response = await authApi.register(registerData)
      const result = response.data.data

      setTokens({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken
      })
      setUser(result.user)

      return result
    } catch (error) {
      clearAuth()
      throw error
    }
  }

  // 登出
  const logout = async () => {
    try {
      if (refreshToken.value) {
        await authApi.logout(refreshToken.value)
      }
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      clearAuth()
    }
  }

  // 获取当前用户信息
  const getCurrentUser = async (): Promise<User> => {
    try {
      const response = await userApi.getCurrentUser()
      const userData = response.data.data
      setUser(userData)
      return userData
    } catch (error) {
      // 不要在获取用户信息失败时清除认证
      throw error
    }
  }

  // 刷新令牌
  const refreshAccessToken = async (): Promise<string> => {
    if (!refreshToken.value) {
      throw new Error('No refresh token available')
    }

    try {
      const response = await authApi.refreshToken(refreshToken.value)
      const result = response.data.data

      setTokens({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken
      })

      return result.accessToken
    } catch (error) {
      clearAuth()
      throw error
    }
  }

  // 更新用户积分
  const updateUserPoints = (points: number) => {
    if (user.value) {
      user.value.points = points
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  // 强制更新用户信息（用于头像等资源更新）
  const forceUpdateUser = async () => {
    if (accessToken.value) {
      try {
        await getCurrentUser()
      } catch (error) {
        console.error('Failed to force update user:', error)
      }
    }
  }

  return {
    // 状态
    user,
    accessToken,
    refreshToken,
    avatarTimestamp,

    // 计算属性
    isLoggedIn,
    isAdmin,

    // 方法
    setTokens,
    setUser,
    clearAuth,
    restoreAuth,
    login,
    register,
    logout,
    getCurrentUser,
    refreshAccessToken,
    updateUserPoints,
    forceUpdateUser
  }
})