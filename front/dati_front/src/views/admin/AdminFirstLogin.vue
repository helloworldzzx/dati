<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const phone = ref(auth.user?.phone || '')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (newPassword.value.length < 6) {
    error.value = '新密码至少 6 位'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的新密码不一致'
    return
  }

  loading.value = true
  try {
    await auth.completeFirstLogin(phone.value, newPassword.value)
    await router.push('/admin')
  } catch (err) {
    error.value = err.message || '保存失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-panel">
      <h1 class="auth-title">首次登录设置</h1>
      <p class="auth-subtitle">绑定手机号并修改默认密码后即可进入后台。</p>

      <form class="form" @submit.prevent="submit">
        <div class="form-row">
          <label for="phone">手机号</label>
          <input id="phone" v-model.trim="phone" class="input" autocomplete="tel" />
        </div>

        <div class="form-row">
          <label for="newPassword">新密码</label>
          <input id="newPassword" v-model="newPassword" class="input" type="password" />
        </div>

        <div class="form-row">
          <label for="confirmPassword">确认新密码</label>
          <input id="confirmPassword" v-model="confirmPassword" class="input" type="password" />
        </div>

        <p v-if="error" class="error">{{ error }}</p>

        <button class="btn btn-primary" type="submit" :disabled="loading">
          {{ loading ? '保存中' : '保存并进入后台' }}
        </button>
      </form>
    </section>
  </main>
</template>
