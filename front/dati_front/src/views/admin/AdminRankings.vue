<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'

const rankings = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

async function load() {
  loading.value = true
  try {
    const result = await api.adminRankings({
      page: pagination.page,
      size: pagination.size,
    })
    rankings.value = result.records || []
    pagination.total = result.total || 0
    pagination.page = result.page || pagination.page
    pagination.size = result.size || pagination.size
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  pagination.page = page
  load()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  load()
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
        <el-table-column label="名次" width="80">
          <template #default="{ $index }">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</template>
        </el-table-column>
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
        <!-- <el-table-column label="总用时" width="140">
          <template #default="{ row }">{{ row.totalDurationSeconds }} 秒</template>
        </el-table-column> -->
        <el-table-column label="最近答题" min-width="170">
          <template #default="{ row }">{{ formatDate(row.lastAnsweredAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>
