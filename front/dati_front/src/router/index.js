import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/admin',
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLogin.vue'),
      meta: { public: true },
    },
    {
      path: '/admin/first-login',
      name: 'admin-first-login',
      component: () => import('@/views/admin/AdminFirstLogin.vue'),
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboard.vue'),
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUsers.vue'),
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/views/admin/AdminCategories.vue'),
        },
        {
          path: 'questions',
          name: 'admin-questions',
          component: () => import('@/views/admin/AdminQuestions.vue'),
        },
        {
          path: 'import',
          name: 'admin-import',
          component: () => import('@/views/admin/AdminImport.vue'),
        },
        {
          path: 'rankings',
          name: 'admin-rankings',
          component: () => import('@/views/admin/AdminRankings.vue'),
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.isLoggedIn && to.name === 'admin-login') {
      return '/admin'
    }
    return true
  }

  if (!auth.isLoggedIn) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }

  if (!auth.ready) {
    try {
      await auth.loadMe()
    } catch {
      auth.logout()
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
  }

  if (auth.mustChangePassword && to.name !== 'admin-first-login') {
    return { name: 'admin-first-login' }
  }

  if (!auth.mustChangePassword && to.name === 'admin-first-login') {
    return '/admin'
  }

  if (!auth.isAdmin) {
    auth.logout()
    return { name: 'admin-login' }
  }

  return true
})

export default router
