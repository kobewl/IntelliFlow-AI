import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api', // 设置默认的 API 地址
  timeout: 15000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true // 允许跨域携带 cookie
})

// 请求拦截器
request.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = {
        ...config.headers,
        Authorization: `Bearer ${token}`
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    // 处理其他状态码
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    console.error('请求错误:', error)
    // 处理网络错误
    if (!error.response) {
      ElMessage.error('网络错误，请检查您的网络连接')
      return Promise.reject(new Error('网络错误，请检查您的网络连接'))
    }
    // 处理 HTTP 错误
    const message = error.response.data?.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request 