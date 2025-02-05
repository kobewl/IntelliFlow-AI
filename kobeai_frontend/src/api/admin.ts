import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export const adminApi = {
  // 用户管理接口
  getUserList: (params: any) => {
    return request.get<ApiResponse>('/admin/users', { params })
  },

  addUser: (data: any) => {
    return request.post<ApiResponse>('/admin/users', data)
  },

  updateUser: (id: number, data: any) => {
    return request.put<ApiResponse>(`/admin/users/${id}`, data)
  },

  deleteUser: (id: number) => {
    return request.delete<ApiResponse>(`/admin/users/${id}`)
  },

  setUserRole: (id: number, data: any) => {
    return request.put<ApiResponse>(`/admin/users/${id}/role`, data)
  },

  // 通知管理接口
  getNotificationList: (params: any) => {
    return request.get<ApiResponse>('/admin/notifications', { params })
  },

  addNotification: (data: any) => {
    return request.post<ApiResponse>('/admin/notifications', data)
  },

  updateNotification: (id: number, data: any) => {
    return request.put<ApiResponse>(`/admin/notifications/${id}`, data)
  },

  deleteNotification: (id: number) => {
    return request.delete<ApiResponse>(`/admin/notifications/${id}`)
  },

  updateNotificationStatus: (id: number, status: string) => {
    return request.put<ApiResponse>(`/admin/notifications/${id}/status`, { status })
  },

  // 获取统计数据
  getStats: () => {
    return request.get<ApiResponse>('/admin/stats')
  }
} 