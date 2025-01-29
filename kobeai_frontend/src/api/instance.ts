import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from './types'
import { getAuthToken, clearAuth, shouldRefreshToken } from './auth'

// 创建统一的axios实例
export const apiInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiInstance.interceptors.request.use(
  async (config) => {
    console.log('请求拦截器 - 请求URL:', config.url)
    
    // 如果是登录或注册请求，不需要添加token
    if (config.url?.includes('/auth/login') || config.url?.includes('/auth/register')) {
      return config
    }
    
    const token = getAuthToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = token
      console.log('请求拦截器 - 已添加Authorization头')
    } else {
      console.log('请求拦截器 - 没有token')
    }
    
    return config
  },
  (error) => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
apiInstance.interceptors.response.use(
  (response: AxiosResponse) => {
      return response
  },
  async (error) => {
    console.error('响应拦截器 - 错误:', error.response?.status, error.response?.data)
    
    if (error.response?.status === 401) {
      // 获取当前请求的URL
      const currentUrl = error.config.url
    
      // 如果是登录相关的请求，直接返回错误
      if (currentUrl?.includes('/auth/login') || 
          currentUrl?.includes('/auth/register')) {
        return Promise.reject(error)
    }
    
      // 清除认证状态并跳转到登录页
      clearAuth()
      window.location.href = '/auth/login'
    }
    return Promise.reject(error)
  }
)

export default apiInstance 