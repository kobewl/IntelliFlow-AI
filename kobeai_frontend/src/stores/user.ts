import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 用户资料（展示用，认证数据由 auth store 管理）
  const avatar = ref(localStorage.getItem('avatar') || '')
  const username = ref(localStorage.getItem('username') || '')

  // 消息计数
  const todayMessageCount = ref(0)
  const dailyMessageLimit = ref(100)

  function setAvatar(newAvatar: string) {
    avatar.value = newAvatar
    localStorage.setItem('avatar', newAvatar)
  }

  function setUsername(newUsername: string) {
    username.value = newUsername
    localStorage.setItem('username', newUsername)
  }

  function clearUserInfo() {
    avatar.value = ''
    username.value = ''
    localStorage.removeItem('avatar')
    localStorage.removeItem('username')
  }

  function incrementMessageCount() {
    todayMessageCount.value++
  }

  function resetMessageCount() {
    todayMessageCount.value = 0
  }

  function setDailyMessageLimit(limit: number) {
    dailyMessageLimit.value = limit
  }

  return {
    avatar,
    username,
    todayMessageCount,
    dailyMessageLimit,
    setAvatar,
    setUsername,
    clearUserInfo,
    incrementMessageCount,
    resetMessageCount,
    setDailyMessageLimit
  }
})
