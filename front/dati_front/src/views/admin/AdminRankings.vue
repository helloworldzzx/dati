<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'

const rankings = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rankings.value = await api.rankings(200)
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
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
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rankings" stripe height="680" empty-text="暂无排行数据">
        <el-table-column type="index" label="名次" width="80" />
        <el-table-column prop="username" label="账号" min-width="150" />
        <el-table-column label="姓名" min-width="140">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="answerCount" label="答题数" width="110" />
        <el-table-column prop="correctCount" label="正确数" width="110" />
        <el-table-column prop="wrongCount" label="错误数" width="110" />
        <el-table-column label="正确率" width="110">
          <template #default="{ row }">
            <el-tag type="success">{{ row.accuracyRate }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总用时" width="140">
          <template #default="{ row }">{{ row.totalDurationSeconds }} 秒</template>
        </el-table-column>
        <el-table-column label="最近答题" min-width="170">
          <template #default="{ row }">{{ formatDate(row.lastAnsweredAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
