<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Medal, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'

const router = useRouter()
const rankings = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rankings.value = await api.rankings(100)
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="mobile-shell">
    <div class="mobile-page">
      <header class="answer-header">
        <button class="tree-toggle" type="button" @click="router.push('/answer')">‹</button>
        <div class="answer-header-title">
          <el-icon style="vertical-align: -2px"><Medal /></el-icon>
          排行榜
        </div>
        <el-button text :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </header>

      <section class="answer-content">
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else class="answer-grid">
          <div v-for="(item, index) in rankings" :key="item.userId" class="answer-home-card">
            <div style="display: flex; align-items: center; justify-content: space-between; gap: 12px">
              <div>
                <div style="font-size: 18px; font-weight: 900">
                  {{ index + 1 }}. {{ item.realName || item.username }}
                </div>
                <div class="muted" style="margin-top: 4px">{{ item.username }}</div>
              </div>
              <el-tag type="success">{{ item.accuracyRate }}%</el-tag>
            </div>
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 14px">
              <el-statistic title="答题" :value="item.answerCount" />
              <el-statistic title="正确" :value="item.correctCount" />
              <el-statistic title="错误" :value="item.wrongCount" />
            </div>
          </div>
          <el-empty v-if="!rankings.length" description="暂无排行数据">
            <el-button :icon="ArrowLeft" @click="router.push('/answer')">返回首页</el-button>
          </el-empty>
        </div>
      </section>
    </div>
  </main>
</template>
