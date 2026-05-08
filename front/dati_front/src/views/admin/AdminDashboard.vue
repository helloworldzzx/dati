<script setup>
import { onMounted, ref } from 'vue'
import { api } from '@/services/api'

const loading = ref(false)
const error = ref('')
const stats = ref({
  users: 0,
  categories: 0,
  questions: 0,
  rankingUsers: 0,
})
const rankings = ref([])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [users, categories, questions, rank] = await Promise.all([
      api.users(),
      api.categories(),
      api.questions({ page: 1, size: 100, status: 'ENABLED' }),
      api.rankings(10),
    ])
    stats.value = {
      users: users.length,
      categories: categories.length,
      questions: questions.length,
      rankingUsers: rank.length,
    }
    rankings.value = rank.slice(0, 5)
  } catch (err) {
    error.value = err.message || '加载失败'
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
      <button class="btn" type="button" :disabled="loading" @click="load">刷新</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div class="grid grid-4">
      <article class="card stat-card">
        <span>用户数</span>
        <strong>{{ stats.users }}</strong>
      </article>
      <article class="card stat-card">
        <span>分类数</span>
        <strong>{{ stats.categories }}</strong>
      </article>
      <article class="card stat-card">
        <span>启用题目</span>
        <strong>{{ stats.questions }}</strong>
      </article>
      <article class="card stat-card">
        <span>上榜人数</span>
        <strong>{{ stats.rankingUsers }}</strong>
      </article>
    </div>

    <section class="card" style="margin-top: 16px">
      <div class="card-head">
        <h3 class="card-title">排行预览</h3>
      </div>
      <div class="table-wrap" style="border: 0; border-radius: 0">
        <table class="table">
          <thead>
            <tr>
              <th>名次</th>
              <th>姓名</th>
              <th>答题数</th>
              <th>正确数</th>
              <th>正确率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in rankings" :key="item.userId">
              <td>{{ index + 1 }}</td>
              <td>{{ item.realName || item.username }}</td>
              <td>{{ item.answerCount }}</td>
              <td>{{ item.correctCount }}</td>
              <td>{{ item.accuracyRate }}%</td>
            </tr>
            <tr v-if="!rankings.length">
              <td colspan="5" class="muted">暂无排行数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
