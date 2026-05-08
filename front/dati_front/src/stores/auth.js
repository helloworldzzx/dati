import { defineStore } from 'pinia'
import { api, clearAuth, getStoredUser, getToken, inferAuthScope, saveAuth, saveUser, setAuthScope } from '@/services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    scope: inferAuthScope(),
    token: getToken(inferAuthScope()),
    user: getStoredUser(inferAuthScope()),
    ready: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isUser: (state) => state.user?.role === 'USER',
    mustChangePassword: (state) => Boolean(state.user?.mustChangePassword),
  },
  actions: {
    useScope(scope) {
      const nextScope = scope === 'admin' ? 'admin' : 'answer'
      setAuthScope(nextScope)
      if (this.scope === nextScope) return
      this.scope = nextScope
      this.token = getToken(nextScope)
      this.user = getStoredUser(nextScope)
      this.ready = false
    },
    async login(account, password) {
      const data = await api.login({ account, password })
      saveAuth(data.token, data.user)
      this.scope = data.user?.role === 'ADMIN' ? 'admin' : 'answer'
      this.token = data.token
      this.user = data.user
      this.ready = true
      return data
    },
    async loadMe() {
      if (!this.token) {
        this.ready = true
        return null
      }
      const user = await api.me()
      saveUser(user, this.scope)
      this.user = user
      this.ready = true
      return user
    },
    async completeFirstLogin(phone, newPassword) {
      const user = await api.completeFirstLogin({ phone, newPassword })
      saveUser(user, this.scope)
      this.user = user
      return user
    },
    logout() {
      clearAuth(this.scope)
      this.token = null
      this.user = null
      this.ready = true
    },
  },
})
