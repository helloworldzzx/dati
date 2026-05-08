import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/answer',
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
      meta: { role: 'ADMIN' },
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { role: 'ADMIN' },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('@/views/admin/AdminDashboard.vue') },
        { path: 'users', name: 'admin-users', component: () => import('@/views/admin/AdminUsers.vue') },
        { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/AdminCategories.vue') },
        { path: 'questions', name: 'admin-questions', component: () => import('@/views/admin/AdminQuestions.vue') },
        { path: 'import', name: 'admin-import', component: () => import('@/views/admin/AdminImport.vue') },
        { path: 'rankings', name: 'admin-rankings', component: () => import('@/views/admin/AdminRankings.vue') },
      ],
    },
    {
      path: '/answer/login',
      name: 'answer-login',
      component: () => import('@/views/answer/AnswerLogin.vue'),
      meta: { public: true },
    },
    {
      path: '/answer/first-login',
      name: 'answer-first-login',
      component: () => import('@/views/answer/AnswerFirstLogin.vue'),
      meta: { role: 'USER' },
    },
    {
      path: '/answer',
      name: 'answer-home',
      component: () => import('@/views/answer/AnswerHome.vue'),
      meta: { role: 'USER' },
    },
    {
      path: '/answer/mine',
      name: 'answer-mine',
      component: () => import('@/views/answer/AnswerMine.vue'),
      meta: { role: 'USER' },
    },
    {
      path: '/answer/practice',
      name: 'answer-practice',
      component: () => import('@/views/answer/AnswerPractice.vue'),
      meta: { role: 'USER' },
    },
    {
      path: '/answer/rankings',
      name: 'answer-rankings',
      component: () => import('@/views/answer/AnswerRankings.vue'),
      meta: { role: 'USER' },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.isLoggedIn && !auth.ready) {
      try {
        await auth.loadMe()
      } catch {
        auth.logout()
      }
    }
    if (auth.isLoggedIn && to.name === 'admin-login') return '/admin'
    if (auth.isLoggedIn && to.name === 'answer-login') return auth.isAdmin ? '/admin' : '/answer'
    return true
  }

  if (!auth.isLoggedIn) {
    const loginName = to.path.startsWith('/admin') ? 'admin-login' : 'answer-login'
    return { name: loginName, query: { redirect: to.fullPath } }
  }

  if (!auth.ready) {
    try {
      await auth.loadMe()
    } catch {
      auth.logout()
      const loginName = to.path.startsWith('/admin') ? 'admin-login' : 'answer-login'
      return { name: loginName, query: { redirect: to.fullPath } }
    }
  }

  if (auth.mustChangePassword) {
    if (auth.isAdmin && to.name !== 'admin-first-login') return { name: 'admin-first-login' }
    if (auth.isUser && to.name !== 'answer-first-login') return { name: 'answer-first-login' }
  }

  if (!auth.mustChangePassword && to.name === 'admin-first-login') return '/admin'
  if (!auth.mustChangePassword && to.name === 'answer-first-login') return '/answer'

  if (to.meta.role === 'ADMIN' && !auth.isAdmin) return '/answer'
  if (to.meta.role === 'USER' && !auth.isUser) return '/admin'

  return true
})

export default router
