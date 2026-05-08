import { defineStore } from 'pinia'
import { api, clearAuth, getStoredUser, getToken, saveAuth, saveUser } from '@/services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: getStoredUser(),
    ready: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isUser: (state) => state.user?.role === 'USER',
    mustChangePassword: (state) => Boolean(state.user?.mustChangePassword),
  },
  actions: {
    async login(account, password) {
      const data = await api.login({ account, password })
      saveAuth(data.token, data.user)
      this.token = data.token
      this.user = data.user
      return data
    },
    async loadMe() {
      if (!this.token) {
        this.ready = true
        return null
      }
      const user = await api.me()
      saveUser(user)
      this.user = user
      this.ready = true
      return user
    },
    async completeFirstLogin(phone, newPassword) {
      const user = await api.completeFirstLogin({ phone, newPassword })
      saveUser(user)
      this.user = user
      return user
    },
    logout() {
      clearAuth()
      this.token = null
      this.user = null
      this.ready = true
    },
  },
})
