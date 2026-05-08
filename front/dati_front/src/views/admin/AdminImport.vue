<script setup>
import { ref } from 'vue'
import { api, downloadBlob } from '@/services/api'

const file = ref(null)
const loading = ref(false)
const downloading = ref(false)
const error = ref('')
const result = ref(null)
const templateType = ref('choice')

const templateNames = {
  choice: 'choice-question-import-template.xlsx',
  judge: 'judge-question-import-template.xlsx',
  analysis: 'analysis-question-import-template.xlsx',
  all: 'question-import-template.xlsx',
}

function onFileChange(event) {
  file.value = event.target.files?.[0] || null
  result.value = null
  error.value = ''
}

async function downloadTemplate() {
  downloading.value = true
  error.value = ''
  try {
    const blob = await api.importTemplate(templateType.value)
    downloadBlob(blob, templateNames[templateType.value])
  } catch (err) {
    error.value = err.message || '模板下载失败'
  } finally {
    downloading.value = false
  }
}

async function upload() {
  if (!file.value) {
    error.value = '请选择 Excel 文件'
    return
  }
  loading.value = true
  error.value = ''
  result.value = null
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    result.value = await api.importQuestions(formData)
  } catch (err) {
    error.value = err.message || '导入失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">批量导入</h2>
        <p class="page-desc">使用 Excel 模板导入题目、选项、答案、解析和来源文件。</p>
      </div>
      <div style="display: flex; gap: 10px">
        <select v-model="templateType" class="select" style="width: 180px">
          <option value="choice">选择题模板</option>
          <option value="judge">判断题模板</option>
          <option value="analysis">分析题模板</option>
          <option value="all">全部模板</option>
        </select>
        <button class="btn" type="button" :disabled="downloading" @click="downloadTemplate">
          {{ downloading ? '下载中' : '下载模板' }}
        </button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="card">
      <div class="card-head">
        <h3 class="card-title">上传 Excel</h3>
      </div>
      <div class="card-body">
        <div class="file-box">
          <input class="input" type="file" accept=".xlsx,.xls" @change="onFileChange" />
          <div class="muted">
            当前文件：{{ file?.name || '未选择' }}
          </div>
          <button class="btn btn-primary" type="button" :disabled="loading" @click="upload">
            {{ loading ? '导入中' : '开始导入' }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="result" class="card" style="margin-top: 16px">
      <div class="card-head">
        <h3 class="card-title">导入结果</h3>
        <span
          :class="[
            'badge',
            result.status === 'SUCCESS'
              ? 'badge-success'
              : result.status === 'FAILED'
                ? 'badge-danger'
                : 'badge-warning',
          ]"
        >
          {{ result.status }}
        </span>
      </div>
      <div class="card-body">
        <div class="grid grid-4">
          <article class="card stat-card">
            <span>总行数</span>
            <strong>{{ result.totalCount }}</strong>
          </article>
          <article class="card stat-card">
            <span>成功</span>
            <strong>{{ result.successCount }}</strong>
          </article>
          <article class="card stat-card">
            <span>失败</span>
            <strong>{{ result.failCount }}</strong>
          </article>
          <article class="card stat-card">
            <span>批次 ID</span>
            <strong>{{ result.id }}</strong>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>
