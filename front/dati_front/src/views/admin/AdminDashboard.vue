<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'

const loading = ref(false)
const stats = ref({
  users: 0,
  categories: 0,
  questions: 0,
  rankingUsers: 0,
})
const rankings = ref([])

async function load() {
  loading.value = true
  try {
    const [users, categories, questions, rank] = await Promise.all([
      api.users({ page: 1, size: 1 }),
      api.categories(),
      api.adminQuestions({ page: 1, size: 1, status: 'ENABLED' }),
      api.rankings(10),
    ])
    stats.value = {
      users: users.total || 0,
      categories: categories.length,
      questions: questions.total || 0,
      rankingUsers: rank.length,
    }
    rankings.value = rank.slice(0, 5)
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">工作台</h2>
        <p class="page-desc">查看后台基础数据和近期排行。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <el-card class="stat-card" shadow="never">
        <el-statistic title="用户数" :value="stats.users" />
      </el-card>
      <el-card class="stat-card" shadow="never">
        <el-statistic title="分类数" :value="stats.categories" />
      </el-card>
      <el-card class="stat-card" shadow="never">
        <el-statistic title="启用题目" :value="stats.questions" />
      </el-card>
      <el-card class="stat-card" shadow="never">
        <el-statistic title="上榜人数" :value="stats.rankingUsers" />
      </el-card>
    </div>

    <el-card class="card-gap" shadow="never">
      <template #header>
        <strong>排行预览</strong>
      </template>
      <el-table :data="rankings" stripe empty-text="暂无排行数据">
        <el-table-column type="index" label="名次" width="80" />
        <el-table-column label="姓名" min-width="160">
          <template #default="{ row }">{{ row.realName || row.username }}</template>
        </el-table-column>
        <el-table-column prop="answerCount" label="答题数" width="120" />
        <el-table-column prop="correctCount" label="正确数" width="120" />
        <el-table-column label="正确率" width="120">
          <template #default="{ row }">{{ row.accuracyRate }}%</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
