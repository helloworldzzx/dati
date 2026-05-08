<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AnswerTabBar from '@/components/AnswerTabBar.vue'
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

onMounted(load)
</script>

<template>
  <main class="mobile-shell">
    <div class="mobile-page">
      <header class="answer-header">
        <div>
          <div class="answer-header-title">首页</div>
          <div class="muted" style="font-size: 12px">
            欢迎您，{{ auth.user?.realName || auth.user?.username || '答题练习' }}
          </div>
        </div>
      </header>

      <section class="answer-content with-tabbar">
        <div class="answer-hero">
          <h1>开始练习</h1>
          <p>选择题库分类，或直接从全部题目开始练习。</p>
        </div>

        <div class="answer-grid">
          <button class="answer-action" type="button" @click="startPractice(null)">
            <span class="answer-action-icon">
              <el-icon><Tickets /></el-icon>
            </span>
            <span>
              <strong>全部题库</strong>
              <span>从所有已启用题目中练习</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <h3 class="answer-section-heading">题库</h3>
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
              <span>{{ category.status === 'ENABLED' ? '已启用' : '已禁用' }}</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
          <el-empty v-if="!flatCategories.length" description="暂无分类" />
        </div>
      </section>

      <AnswerTabBar />
    </div>
  </main>
</template>
