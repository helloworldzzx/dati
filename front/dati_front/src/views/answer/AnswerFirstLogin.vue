<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const dialogVisible = ref(true)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_, value, callback) => {
  if (value !== form.newPassword) callback(new Error('两次输入的新密码不一致'))
  else callback()
}

const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码不得低于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.completeFirstLogin('', form.newPassword)
    ElMessage.success('密码已修改')
    await router.push('/answer')
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="mobile-auth">
    <el-dialog
      v-model="dialogVisible"
      title="首次登录修改密码"
      width="calc(100% - 32px)"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      align-center
    >
      <p class="dialog-tip">首次登录需要先修改初始密码，不需要输入原密码。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" size="large" type="password" show-password autocomplete="new-password" />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            size="large"
            type="password"
            show-password
            autocomplete="new-password"
            @keyup.enter="submit"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="submit">
          保存并开始答题
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>
