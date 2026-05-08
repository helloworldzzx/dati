<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: 'admin',
  password: '123456',
})

const rules = {
  account: [{ required: true, message: '请输入账号或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form.account, form.password)
    if (!auth.isAdmin) {
      auth.logout()
      ElMessage.error('当前账号不是管理员')
      return
    }
    if (auth.mustChangePassword) {
      await router.push('/admin/first-login')
      return
    }
    await router.push(route.query.redirect || '/admin')
  } catch (err) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-shell">
    <el-card class="auth-card" shadow="always">
      <h1 class="auth-title">答题系统后台</h1>
      <p class="auth-subtitle">管理员登录</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="账号或手机号" prop="account">
          <el-input v-model.trim="form.account" autocomplete="username" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            show-password
            size="large"
            type="password"
          />
        </el-form-item>

        <el-button type="primary" size="large" :loading="loading" style="width: 100%" @click="submit">
          登录
        </el-button>
      </el-form>
    </el-card>
  </main>
</template>
