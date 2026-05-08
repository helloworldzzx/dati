<script setup>
import { onMounted, ref } from 'vue'
import { api } from '@/services/api'

const rankings = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    rankings.value = await api.rankings(200)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">排行榜</h2>
        <p class="page-desc">按正确题数、答题数和正确率排序。</p>
      </div>
      <button class="btn" type="button" :disabled="loading" @click="load">刷新</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>名次</th>
            <th>账号</th>
            <th>姓名</th>
            <th>答题数</th>
            <th>正确数</th>
            <th>错误数</th>
            <th>正确率</th>
            <th>总用时</th>
            <th>最近答题</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in rankings" :key="item.userId">
            <td>{{ index + 1 }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.realName || '-' }}</td>
            <td>{{ item.answerCount }}</td>
            <td>{{ item.correctCount }}</td>
            <td>{{ item.wrongCount }}</td>
            <td>{{ item.accuracyRate }}%</td>
            <td>{{ item.totalDurationSeconds }} 秒</td>
            <td>{{ formatDate(item.lastAnsweredAt) }}</td>
          </tr>
          <tr v-if="!rankings.length">
            <td colspan="9" class="muted">{{ loading ? '加载中' : '暂无排行数据' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
