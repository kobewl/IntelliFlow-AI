export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface LoginResponse {
  token: string
  user: any
}

export interface RegisterResponse {
  token: string
  user: any
}

export interface EmailCodeResponse {
  message: string
} 