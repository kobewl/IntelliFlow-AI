import request from '../utils/request'
import { ElMessage } from 'element-plus'

/**
 * 上传文件
 * @param file 文件对象
 * @returns 文件访问URL
 */
export const uploadFile = async (file: File): Promise<string> => {
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    throw new Error('只能上传图片文件')
  }

  // 检查文件大小（5MB）
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    throw new Error('文件大小不能超过5MB')
  }

  const formData = new FormData()
  formData.append('data', file)
  
  try {
    const response = await request.post('/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (response.data) {
      return response.data
    } else {
      throw new Error(response.message || '上传失败')
    }
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || error.message || '上传失败'
    ElMessage.error(errorMessage)
    throw new Error(errorMessage)
  }
} 