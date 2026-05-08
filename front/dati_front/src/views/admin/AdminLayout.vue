<script setup>
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { Collection, DataAnalysis, Document, Files, House, Rank, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { path: '/admin', label: '工作台', icon: House },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/categories', label: '题库分类', icon: Collection },
  { path: '/admin/questions', label: '题目管理', icon: Document },
  { path: '/admin/rankings', label: '排行榜', icon: Rank },
]

const activeMenu = computed(() => {
  const matched = navItems.find((item) => item.path !== '/admin' && route.path.startsWith(item.path))
  return matched?.path || '/admin'
})

const pageTitle = computed(() => navItems.find((item) => item.path === activeMenu.value)?.label || '后台')

function navigate(path) {
  router.push(path)
}

function logout() {
  auth.logout()
  router.push('/admin/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="232px" class="admin-aside">
      <div class="admin-brand">
        <h1>答题系统</h1>
        <p>管理后台</p>
      </div>

      <el-menu class="admin-menu" :default-active="activeMenu" @select="navigate">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="admin-sidebar-footer">电脑端后台</div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="admin-header-title">
          <el-icon style="vertical-align: -2px"><DataAnalysis /></el-icon>
          {{ pageTitle }}
        </div>
        <div class="admin-user">
          <div class="admin-user-name">
            <strong>{{ auth.user?.realName || auth.user?.username }}</strong>
            <span>{{ auth.user?.role }}</span>
          </div>
          <el-avatar :size="34" :icon="User" />
          <el-button :icon="Files" @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
