<script setup>
import { ref } from 'vue'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api, downloadBlob } from '@/services/api'

const file = ref(null)
const loading = ref(false)
const downloading = ref(false)
const result = ref(null)
const templateType = ref('choice')

const templateNames = {
  choice: 'choice-question-import-template.xlsx',
  judge: 'judge-question-import-template.xlsx',
  analysis: 'analysis-question-import-template.xlsx',
  all: 'question-import-template.xlsx',
}

const templateOptions = [
  { label: '选择题模板', value: 'choice' },
  { label: '判断题模板', value: 'judge' },
  { label: '分析题模板', value: 'analysis' },
  { label: '全部模板', value: 'all' },
]

function beforeUpload(rawFile) {
  file.value = rawFile
  result.value = null
  return false
}

function removeFile() {
  file.value = null
}

async function downloadTemplate() {
  downloading.value = true
  try {
    const blob = await api.importTemplate(templateType.value)
    downloadBlob(blob, templateNames[templateType.value])
  } catch (err) {
    ElMessage.error(err.message || '模板下载失败')
  } finally {
    downloading.value = false
  }
}

async function upload() {
  if (!file.value) {
    ElMessage.warning('请选择 Excel 文件')
    return
  }
  loading.value = true
  result.value = null
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    result.value = await api.importQuestions(formData)
    ElMessage.success('导入完成')
  } catch (err) {
    ElMessage.error(err.message || '导入失败')
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
        <p class="page-desc">按题型下载模板，再上传 Excel 批量导入题目。</p>
      </div>
      <div class="toolbar-inline">
        <el-select v-model="templateType" style="width: 180px">
          <el-option
            v-for="item in templateOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button :icon="Download" :loading="downloading" @click="downloadTemplate">下载模板</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>
        <strong>上传 Excel</strong>
      </template>

      <div class="file-box">
        <el-upload
          drag
          action="#"
          accept=".xlsx,.xls"
          :auto-upload="false"
          :limit="1"
          :before-upload="beforeUpload"
          :on-remove="removeFile"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽 Excel 到这里，或 <em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 .xlsx / .xls，最大 10MB。</div>
          </template>
        </el-upload>

        <el-alert v-if="file" type="info" :closable="false" show-icon>
          当前文件：{{ file.name }}
        </el-alert>

        <el-button type="primary" :loading="loading" style="width: 180px" @click="upload">开始导入</el-button>
      </div>
    </el-card>

    <el-card v-if="result" class="card-gap" shadow="never">
      <template #header>
        <div class="toolbar-inline" style="justify-content: space-between">
          <strong>导入结果</strong>
          <el-tag
            :type="
              result.status === 'SUCCESS' ? 'success' : result.status === 'FAILED' ? 'danger' : 'warning'
            "
          >
            {{ result.status }}
          </el-tag>
        </div>
      </template>

      <div class="stat-grid">
        <el-card shadow="never">
          <el-statistic title="总行数" :value="result.totalCount" />
        </el-card>
        <el-card shadow="never">
          <el-statistic title="成功" :value="result.successCount" />
        </el-card>
        <el-card shadow="never">
          <el-statistic title="失败" :value="result.failCount" />
        </el-card>
        <el-card shadow="never">
          <el-statistic title="批次 ID" :value="result.id" />
        </el-card>
      </div>
    </el-card>
  </div>
</template>
