<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { path: '/admin', label: '工作台', exact: true },
  { path: '/admin/users', label: '用户管理' },
  { path: '/admin/categories', label: '题库分类' },
  { path: '/admin/questions', label: '题目管理' },
  { path: '/admin/import', label: '批量导入' },
  { path: '/admin/rankings', label: '排行榜' },
]

const pageTitle = computed(() => {
  const item = navItems.find((nav) =>
    nav.exact ? route.path === nav.path : route.path.startsWith(nav.path),
  )
  return item?.label || '后台'
})

function logout() {
  auth.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <h1>答题系统</h1>
        <p>管理后台</p>
      </div>

      <nav class="admin-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="{ 'router-link-active': item.exact && route.path === item.path }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="admin-sidebar-footer">电脑端后台</div>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div class="admin-topbar-title">{{ pageTitle }}</div>
        <div class="admin-user">
          <div class="admin-user-name">
            <strong>{{ auth.user?.realName || auth.user?.username }}</strong>
            <span>{{ auth.user?.role }}</span>
          </div>
          <button class="btn" type="button" @click="logout">退出</button>
        </div>
      </header>

      <section class="admin-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>
