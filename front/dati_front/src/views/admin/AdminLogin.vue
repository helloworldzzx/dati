<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const account = ref('admin')
const password = ref('123456')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(account.value, password.value)
    if (!auth.isAdmin) {
      auth.logout()
      error.value = '当前账号不是管理员'
      return
    }
    if (auth.mustChangePassword) {
      await router.push('/admin/first-login')
      return
    }
    await router.push(route.query.redirect || '/admin')
  } catch (err) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-panel">
      <h1 class="auth-title">答题系统后台</h1>
      <p class="auth-subtitle">管理员登录</p>

      <form class="form" @submit.prevent="submit">
        <div class="form-row">
          <label for="account">账号或手机号</label>
          <input id="account" v-model.trim="account" class="input" autocomplete="username" />
        </div>

        <div class="form-row">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            class="input"
            type="password"
            autocomplete="current-password"
          />
        </div>

        <p v-if="error" class="error">{{ error }}</p>

        <button class="btn btn-primary" type="submit" :disabled="loading">
          {{ loading ? '登录中' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>
