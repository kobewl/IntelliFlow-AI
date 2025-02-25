/**
 * 网络状态监测工具
 * 用于检测和通知应用程序的网络连接状态
 */

import { ref } from 'vue'

class NetworkStatus {
  // 是否在线
  public isOnline = ref(navigator.onLine)
  
  // 上次检查的延迟（毫秒）
  public latency = ref(0)
  
  // 检查间隔（毫秒）
  private checkInterval = 30000 // 30秒检查一次
  
  // 检查服务器的URL - 改用已存在的API端点
  private pingUrl = '/api/user/profile'
  
  // 定时器ID
  private timerId: number | null = null
  
  constructor() {
    // 监听在线/离线事件
    window.addEventListener('online', () => this.handleOnline())
    window.addEventListener('offline', () => this.handleOffline())
    
    // 初始检查
    this.checkConnection()
    
    // 启动定期检查
    this.startPeriodicCheck()
  }
  
  /**
   * 处理设备在线事件
   */
  private handleOnline(): void {
    console.log('设备网络连接已恢复')
    this.isOnline.value = true
    
    // 当恢复在线时，立即检查连接
    this.checkConnection()
  }
  
  /**
   * 处理设备离线事件
   */
  private handleOffline(): void {
    console.log('设备网络连接已断开')
    this.isOnline.value = false
  }
  
  /**
   * 启动定期检查
   */
  private startPeriodicCheck(): void {
    if (this.timerId !== null) {
      window.clearInterval(this.timerId)
    }
    
    // 设置定期检查
    this.timerId = window.setInterval(() => {
      this.checkConnection()
    }, this.checkInterval)
  }
  
  /**
   * 检查实际连接
   */
  public async checkConnection(): Promise<boolean> {
    if (!navigator.onLine) {
      this.isOnline.value = false
      return false
    }
    
    try {
      const startTime = Date.now()
      const response = await fetch(`${this.pingUrl}?t=${startTime}`, {
        method: 'HEAD',
        cache: 'no-store'
      })
      
      // 计算延迟
      this.latency.value = Date.now() - startTime
      
      // 修改判断逻辑：服务器返回了响应（包括401、403等）表示服务器在线
      // 只有网络错误才表示离线
      const statusCode = response.status;
      // 如果状态码在200-599范围内，认为服务器在线
      const isConnected = statusCode >= 200 && statusCode < 600;
      
      console.log(`网络状态检查: 状态码=${statusCode}, 认为${isConnected ? '在线' : '离线'}`);
      this.isOnline.value = isConnected;
      
      return isConnected;
    } catch (error) {
      console.warn('检查网络连接失败:', error)
      this.isOnline.value = false
      return false
    }
  }
  
  /**
   * 停止网络监测
   */
  public stopMonitoring(): void {
    if (this.timerId !== null) {
      window.clearInterval(this.timerId)
      this.timerId = null
    }
    
    // 移除事件监听器
    window.removeEventListener('online', () => this.handleOnline())
    window.removeEventListener('offline', () => this.handleOffline())
  }
}

// 导出单例
export const networkStatus = new NetworkStatus() 