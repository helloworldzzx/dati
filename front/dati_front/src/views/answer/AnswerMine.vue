<script setup>
import { useRouter } from 'vue-router'
import { ArrowRight, Collection, Medal, Star, SwitchButton } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import AnswerTabBar from '@/components/AnswerTabBar.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

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
  </main>
</template>
