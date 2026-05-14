<script setup>
import { nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Collection, Lock, Medal, Star, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AnswerTabBar from '@/components/AnswerTabBar.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const menuItems = [
  {
    title: '错题本',
    desc: '复习曾经答错的题目',
    icon: Collection,
    to: { path: '/answer/practice', query: { mode: 'wrong', return: '/answer/mine' } },
  },
  {
    title: '收藏题',
    desc: '查看主动收藏的题目',
    icon: Star,
    to: { path: '/answer/practice', query: { mode: 'favorite', return: '/answer/mine' } },
  },
  {
    title: '排行榜',
    desc: '查看答题排名',
    icon: Medal,
    to: { path: '/answer/rankings', query: { return: '/answer/mine' } },
  },
]

const validateConfirmPassword = (_, value, callback) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次输入的新密码不一致'))
  else callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码不得低于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

function openPasswordDialog() {
  passwordDialogVisible.value = true
  nextTick(() => passwordFormRef.value?.clearValidate())
}

function resetPasswordForm() {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  nextTick(() => passwordFormRef.value?.clearValidate())
}

async function submitPassword() {
  await passwordFormRef.value.validate()
  passwordLoading.value = true
  try {
    await auth.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    ElMessage.success('密码已修改')
    passwordDialogVisible.value = false
    resetPasswordForm()
  } catch (err) {
    ElMessage.error(err.message || '修改失败')
  } finally {
    passwordLoading.value = false
  }
}

async function logout() {
  try {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
    auth.logout()
    router.push('/answer/login')
  } catch {
    // 用户取消，无需处理
  }
}
</script>

<template>
  <main class="mobile-shell">
    <div class="mobile-page">
      <header class="answer-header">
        <div>
          <div class="answer-header-title">个人中心</div>
        </div>
      </header>

      <section class="answer-content with-tabbar">
        <div class="mine-profile">
          <div class="mine-avatar">
            {{ (auth.user?.realName || auth.user?.username || '我').slice(0, 1) }}
          </div>
          <div class="mine-profile-main">
            <strong>{{ auth.user?.realName || auth.user?.username || '答题用户' }}</strong>
            <span>{{ auth.user?.username }}</span>
          </div>
        </div>

        <div class="answer-grid">
          <button
            v-for="item in menuItems"
            :key="item.title"
            class="answer-action"
            type="button"
            @click="router.push(item.to)"
          >
            <span class="answer-action-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span>
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <button class="answer-action mine-logout" type="button" @click="openPasswordDialog">
          <span class="answer-action-icon">
            <el-icon><Lock /></el-icon>
          </span>
          <span>
            <strong>修改密码</strong>
            <span>需要验证当前登录密码</span>
          </span>
          <el-icon><ArrowRight /></el-icon>
        </button>

        <button class="answer-action mine-logout" type="button" @click="logout">
          <span class="answer-action-icon">
            <el-icon><SwitchButton /></el-icon>
          </span>
          <span>
            <strong>退出登录</strong>
            <span>回到答题端登录页</span>
          </span>
          <el-icon><ArrowRight /></el-icon>
        </button>
      </section>

      <AnswerTabBar />
    </div>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="calc(100% - 32px)"
      destroy-on-close
      @closed="resetPasswordForm"
    >
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            @keyup.enter="submitPassword"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>
