<script setup>
import { onMounted, onUnmounted } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function handleExpired() {
  auth.logout()
  router.push('/admin/login')
}

onMounted(() => {
  window.addEventListener('auth-expired', handleExpired)
})

onUnmounted(() => {
  window.removeEventListener('auth-expired', handleExpired)
})
</script>

<template>
  <RouterView />
</template>
