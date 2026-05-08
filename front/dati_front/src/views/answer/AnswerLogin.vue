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
  account: '',
  password: '',
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
    if (auth.isAdmin) {
      await router.push('/admin')
      return
    }
    if (auth.mustChangePassword) {
      await router.push('/answer/first-login')
      return
    }
    await router.push(route.query.redirect || '/answer')
  } catch (err) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="mobile-auth">
    <section class="mobile-auth-card">
      <h1 class="mobile-auth-title">答题练习</h1>
      <p class="mobile-auth-subtitle">请输入管理员分配的账号登录。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号或手机号" prop="account">
          <el-input v-model.trim="form.account" size="large" autocomplete="username" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password />
        </el-form-item>

        <el-button type="primary" size="large" :loading="loading" style="width: 100%" @click="submit">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>
