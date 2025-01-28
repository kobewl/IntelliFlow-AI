// API响应接口
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应接口
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// 错误响应接口
export interface ErrorResponse {
  code: number
  message: string
  timestamp: string
  path: string
} 