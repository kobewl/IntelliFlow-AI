import { defineStore } from 'pinia'

interface UserState {
  token: string
  avatar: string
  username: string
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token') || '',
    avatar: localStorage.getItem('avatar') || '',
    username: localStorage.getItem('username') || ''
  }),

  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },

    setAvatar(avatar: string) {
      this.avatar = avatar
      localStorage.setItem('avatar', avatar)
    },

    setUsername(username: string) {
      this.username = username
      localStorage.setItem('username', username)
    },

    clearUserInfo() {
      this.token = ''
      this.avatar = ''
      this.username = ''
      localStorage.removeItem('token')
      localStorage.removeItem('avatar')
      localStorage.removeItem('username')
    }
  }
}) 