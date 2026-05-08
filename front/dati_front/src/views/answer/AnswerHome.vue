<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Collection, Medal, Star, SwitchButton, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const categories = ref([])
const loading = ref(false)

const flatCategories = computed(() => flatten(categories.value))

function flatten(nodes) {
  return nodes.flatMap((node) => [node, ...flatten(node.children || [])])
}

async function load() {
  loading.value = true
  try {
    categories.value = await api.categoryTree()
  } catch (err) {
    ElMessage.error(err.message || '加载分类失败')
  } finally {
    loading.value = false
  }
}

function startPractice(category) {
  router.push({
    path: '/answer/practice',
    query: category ? { categoryId: category.id, title: category.name } : {},
  })
}

function logout() {
  auth.logout()
  router.push('/answer/login')
}

onMounted(load)
</script>

<template>
  <main class="mobile-shell">
    <div class="mobile-page">
      <header class="answer-header">
        <div>
          <div class="answer-header-title">答题练习</div>
          <div class="muted" style="font-size: 12px">{{ auth.user?.realName || auth.user?.username }}</div>
        </div>
        <el-button text :icon="SwitchButton" @click="logout">退出</el-button>
      </header>

      <section class="answer-content">
        <div class="answer-hero">
          <h1>开始练习</h1>
          <p>选择题库分类，或直接进入错题和收藏。</p>
        </div>

        <div class="answer-grid">
          <button class="answer-action" type="button" @click="startPractice(null)">
            <span class="answer-action-icon"><el-icon><Tickets /></el-icon></span>
            <span>
              <strong>全部题库</strong>
              <span>从所有已启用题目中练习</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>

          <button class="answer-action" type="button" @click="router.push('/answer/practice?mode=wrong')">
            <span class="answer-action-icon"><el-icon><Collection /></el-icon></span>
            <span>
              <strong>错题本</strong>
              <span>复习曾经答错的题目</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>

          <button class="answer-action" type="button" @click="router.push('/answer/practice?mode=favorite')">
            <span class="answer-action-icon"><el-icon><Star /></el-icon></span>
            <span>
              <strong>收藏题</strong>
              <span>查看主动收藏的题目</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>

          <button class="answer-action" type="button" @click="router.push('/answer/rankings')">
            <span class="answer-action-icon"><el-icon><Medal /></el-icon></span>
            <span>
              <strong>排行榜</strong>
              <span>查看答题排名</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <h3 style="margin: 22px 0 10px; font-weight: 900">题库分类</h3>
        <el-skeleton v-if="loading" :rows="4" animated />
        <div v-else class="category-mobile-list">
          <button
            v-for="category in flatCategories"
            :key="category.id"
            class="answer-action"
            type="button"
            @click="startPractice(category)"
          >
            <span class="answer-action-icon">L{{ category.level }}</span>
            <span>
              <strong>{{ '　'.repeat(category.level - 1) }}{{ category.name }}</strong>
              <span>{{ category.status }}</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
          <el-empty v-if="!flatCategories.length" description="暂无分类" />
        </div>
      </section>
    </div>
  </main>
</template>
