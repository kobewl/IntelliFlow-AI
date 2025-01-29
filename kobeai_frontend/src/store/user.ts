import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const todayMessageCount = ref(0)
  const dailyMessageLimit = ref(100)

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
    todayMessageCount,
    dailyMessageLimit,
    incrementMessageCount,
    resetMessageCount,
    setDailyMessageLimit
  }
}) 