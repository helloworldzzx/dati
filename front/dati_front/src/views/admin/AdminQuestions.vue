<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Delete as DeleteIcon, Download, Edit, Plus, Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, downloadBlob } from '@/services/api'

const MAX_OPTIONS = 10

const categories = ref([])
const questions = ref([])
const selectedId = ref(null)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const importVisible = ref(false)
const importing = ref(false)
const downloadingTemplate = ref(false)
const importFile = ref(null)
const importResult = ref(null)
const selectedQuestionIds = ref([])
const formRef = ref()
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

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

const filters = reactive({
  level1Id: '',
  level2Id: '',
  level3Id: '',
  type: '',
  status: 'ENABLED',
})

const form = reactive({
  level1Id: '',
  level2Id: '',
  level3Id: '',
  type: 'SINGLE',
  title: '',
  correctAnswer: '',
  analysis: '',
  sourceFile: '',
  status: 'ENABLED',
  options: buildEmptyOptions(),
})

const rules = {
  level1Id: [{ required: true, message: '请选择一级分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  title: [{ required: true, message: '请输入题干', trigger: 'blur' }],
}

const categoryById = computed(() => new Map(categories.value.map((item) => [item.id, item])))
const level1Options = computed(() => childrenOf(null, 1))
const filterCategoryId = computed(() => filters.level3Id || filters.level2Id || filters.level1Id || '')
const selectedFormCategoryId = computed(() => form.level3Id || form.level2Id || form.level1Id || '')
const singleCorrectKey = computed(() => form.options.find((option) => option.correct)?.optionKey || '')

watch(
  () => filters.level1Id,
  () => {
    filters.level2Id = ''
    filters.level3Id = ''
  },
)

watch(
  () => filters.level2Id,
  () => {
    filters.level3Id = ''
  },
)

watch(
  () => form.level1Id,
  () => {
    form.level2Id = ''
    form.level3Id = ''
  },
)

watch(
  () => form.level2Id,
  () => {
    form.level3Id = ''
  },
)

watch(
  () => form.type,
  (type) => {
    if (type === 'SINGLE' || type === 'MULTIPLE') {
      if (!form.options.length) form.options = buildEmptyOptions()
      if (type === 'SINGLE') {
        const firstCorrect = form.options.find((option) => option.correct)
        form.options.forEach((option) => {
          option.correct = option === firstCorrect
        })
      }
    }
    if (type === 'JUDGE') {
      form.options = []
      if (!['TRUE', 'FALSE'].includes(form.correctAnswer)) form.correctAnswer = 'TRUE'
    }
    if (type === 'ANALYSIS') {
      form.options = []
    }
  },
)

function optionKeyByIndex(index) {
  return String.fromCharCode(65 + index)
}

function buildEmptyOptions(count = 4) {
  return Array.from({ length: count }, (_, index) => ({
    optionKey: optionKeyByIndex(index),
    optionContent: '',
    correct: false,
    sortNo: index,
  }))
}

function normalizeOptions(options = []) {
  const source = options.length ? options : buildEmptyOptions()
  return source
    .slice()
    .sort((left, right) => (left.sortNo ?? 0) - (right.sortNo ?? 0))
    .map((option, index) => ({
      optionKey: option.optionKey || optionKeyByIndex(index),
      optionContent: option.optionContent || '',
      correct: Boolean(option.correct),
      sortNo: index,
    }))
}

function childrenOf(parentId, level) {
  const normalizedParentId = parentId ? Number(parentId) : null
  return categories.value
    .filter((item) => {
      const parentMatched = normalizedParentId === null ? !item.parentId : item.parentId === normalizedParentId
      return parentMatched && (!level || item.level === level)
    })
    .sort((left, right) => (left.sortNo || 0) - (right.sortNo || 0) || left.id - right.id)
}

function categoryPath(categoryId) {
  const path = { level1: '-', level2: '-', level3: '-' }
  const chain = []
  let current = categoryById.value.get(Number(categoryId))
  while (current) {
    chain.unshift(current)
    current = current.parentId ? categoryById.value.get(current.parentId) : null
  }
  chain.forEach((item) => {
    path[`level${item.level}`] = item.name
  })
  return path
}

function setCategoryLevels(categoryId) {
  const chain = []
  let current = categoryById.value.get(Number(categoryId))
  while (current) {
    chain.unshift(current)
    current = current.parentId ? categoryById.value.get(current.parentId) : null
  }
  form.level1Id = chain.find((item) => item.level === 1)?.id || ''
  form.level2Id = chain.find((item) => item.level === 2)?.id || ''
  form.level3Id = chain.find((item) => item.level === 3)?.id || ''
}

function typeLabel(type) {
  return {
    SINGLE: '单选',
    MULTIPLE: '多选',
    JUDGE: '判断',
    ANALYSIS: '分析',
  }[type] || type
}

function typeTag(type) {
  return {
    SINGLE: 'primary',
    MULTIPLE: 'success',
    JUDGE: 'warning',
    ANALYSIS: 'info',
  }[type] || 'info'
}

function statusLabel(status) {
  return {
    DRAFT: '草稿',
    ENABLED: '启用',
    DISABLED: '禁用',
  }[status] || status
}

function formatAnswer(answer) {
  if (answer === 'TRUE') return '正确'
  if (answer === 'FALSE') return '错误'
  return answer || '-'
}

function formatImportRawData(rawData) {
  if (!rawData) return '-'
  try {
    const value = JSON.parse(rawData)
    if (Array.isArray(value)) {
      return value.filter(Boolean).join(' / ') || '-'
    }
    return String(value)
  } catch {
    return rawData
  }
}

function resetForm() {
  selectedId.value = null
  Object.assign(form, {
    level1Id: '',
    level2Id: '',
    level3Id: '',
    type: 'SINGLE',
    title: '',
    correctAnswer: '',
    analysis: '',
    sourceFile: '',
    status: 'ENABLED',
    options: buildEmptyOptions(),
  })
  nextTick(() => formRef.value?.clearValidate())
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function openEdit(question) {
  selectedId.value = question.id
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    const detail = await api.questionDetail(question.id)
    const item = detail.question
    setCategoryLevels(item.categoryId)
    Object.assign(form, {
      type: item.type,
      title: item.title,
      correctAnswer: item.correctAnswer || '',
      analysis: item.analysis || '',
      sourceFile: item.sourceFile || '',
      status: item.status || 'ENABLED',
      options: item.type === 'SINGLE' || item.type === 'MULTIPLE' ? normalizeOptions(detail.options) : [],
    })
    await nextTick()
    formRef.value?.clearValidate()
  } catch (err) {
    ElMessage.error(err.message || '加载题目失败')
  } finally {
    dialogLoading.value = false
  }
}

async function loadCategories() {
  categories.value = await api.categories()
}

async function loadQuestions() {
  loading.value = true
  try {
    const result = await api.adminQuestions({
      categoryId: filterCategoryId.value,
      type: filters.type,
      status: filters.status,
      page: pagination.page,
      size: pagination.size,
    })
    const list = result.records || []
    pagination.total = result.total || 0
    pagination.page = result.page || pagination.page
    pagination.size = result.size || pagination.size
    questions.value = await Promise.all(
      list.map(async (question) => {
        try {
          const detail = await api.questionDetail(question.id)
          return { ...question, options: detail.options || [] }
        } catch {
          return { ...question, options: [] }
        }
      }),
    )
    selectedQuestionIds.value = []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function searchQuestions() {
  pagination.page = 1
  loadQuestions()
}

function handlePageChange(page) {
  pagination.page = page
  loadQuestions()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadQuestions()
}

function handleSelectionChange(rows) {
  selectedQuestionIds.value = rows.map((item) => item.id)
}

async function removeQuestion(question) {
  try {
    await ElMessageBox.confirm('确认删除这道题吗？删除后相关选项、答题记录和用户题目统计会一起删除。', '删除题目', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    deleting.value = true
    await api.deleteQuestion(question.id)
    ElMessage.success('题目已删除')
    await loadQuestions()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '删除失败')
  } finally {
    deleting.value = false
  }
}

async function removeSelectedQuestions() {
  if (!selectedQuestionIds.value.length) {
    ElMessage.warning('请先选择要删除的题目')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${selectedQuestionIds.value.length} 道题吗？删除后相关选项、答题记录和用户题目统计会一起删除。`,
      '批量删除题目',
      {
        type: 'warning',
        confirmButtonText: '批量删除',
        cancelButtonText: '取消',
      },
    )
    deleting.value = true
    await api.deleteQuestions(selectedQuestionIds.value)
    ElMessage.success('题目已批量删除')
    await loadQuestions()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '批量删除失败')
  } finally {
    deleting.value = false
  }
}

async function downloadTemplate(type = 'all') {
  downloadingTemplate.value = true
  try {
    const blob = await api.importTemplate(type)
    downloadBlob(blob, templateNames[type] || templateNames.all)
  } catch (err) {
    ElMessage.error(err.message || '模板下载失败')
  } finally {
    downloadingTemplate.value = false
  }
}

function openImport() {
  importVisible.value = true
  importFile.value = null
  importResult.value = null
}

function beforeImportUpload(rawFile) {
  importFile.value = rawFile
  importResult.value = null
  return false
}

function handleImportChange(uploadFile) {
  importFile.value = uploadFile.raw || uploadFile
  importResult.value = null
}

function removeImportFile() {
  importFile.value = null
}

async function uploadImport() {
  if (!importFile.value) {
    ElMessage.warning('请选择 Excel 文件')
    return
  }
  importing.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    importResult.value = await api.importQuestions(formData)
    if (importResult.value.failCount) {
      ElMessage.warning('导入完成，部分题目失败')
    } else {
      ElMessage.success('题目导入完成')
    }
    await loadQuestions()
  } catch (err) {
    ElMessage.error(err.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function loadAll() {
  try {
    await loadCategories()
    await loadQuestions()
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

function setSingleCorrect(key) {
  form.options.forEach((option) => {
    option.correct = option.optionKey === key
  })
}

function addOption() {
  if (form.options.length >= MAX_OPTIONS) {
    ElMessage.warning(`最多支持 ${MAX_OPTIONS} 个选项`)
    return
  }
  form.options.push({
    optionKey: optionKeyByIndex(form.options.length),
    optionContent: '',
    correct: false,
    sortNo: form.options.length,
  })
}

function removeOption(index) {
  if (form.options.length <= 2) {
    ElMessage.warning('选择题至少保留 2 个选项')
    return
  }
  form.options.splice(index, 1)
  form.options.forEach((option, nextIndex) => {
    option.optionKey = optionKeyByIndex(nextIndex)
    option.sortNo = nextIndex
  })
}

function buildPayload() {
  const categoryId = selectedFormCategoryId.value
  if (!categoryId) {
    throw new Error('请选择分类')
  }
  const options =
    form.type === 'SINGLE' || form.type === 'MULTIPLE'
      ? form.options
          .filter((option) => option.optionContent.trim())
          .map((option, index) => ({
            optionKey: optionKeyByIndex(index),
            optionContent: option.optionContent.trim(),
            correct: Boolean(option.correct),
            sortNo: index,
          }))
      : []

  if ((form.type === 'SINGLE' || form.type === 'MULTIPLE') && options.length < 2) {
    throw new Error('选择题至少需要 2 个选项')
  }
  const correctAnswer =
    form.type === 'SINGLE' || form.type === 'MULTIPLE'
      ? options.filter((option) => option.correct).map((option) => option.optionKey).join(',')
      : form.correctAnswer

  return {
    categoryId: Number(categoryId),
    type: form.type,
    title: form.title,
    correctAnswer,
    analysis: form.analysis,
    sourceFile: form.sourceFile,
    status: form.status,
    options,
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = buildPayload()
    if ((payload.type === 'SINGLE' || payload.type === 'MULTIPLE') && !payload.correctAnswer) {
      throw new Error('请选择正确答案')
    }
    if (selectedId.value) {
      await api.updateQuestion(selectedId.value, payload)
      ElMessage.success('题目已更新')
    } else {
      await api.createQuestion(payload)
      ElMessage.success('题目已创建')
    }
    dialogVisible.value = false
    resetForm()
    await loadQuestions()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="admin-list-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">题目管理</h2>
        <p class="page-desc">维护单选、多选、判断和分析题。</p>
      </div>
      <div class="toolbar-inline">
        <el-dropdown trigger="click" @command="downloadTemplate">
          <el-button :icon="Download" :loading="downloadingTemplate">下载模板</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="item in templateOptions" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button :icon="UploadFilled" @click="openImport">批量导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建题目</el-button>
      </div>
    </div>

    <el-card class="toolbar-card" shadow="never">
      <div class="question-filter-grid">
        <el-select v-model="filters.level1Id" clearable placeholder="一级分类">
          <el-option v-for="item in level1Options" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="filters.level2Id" clearable :disabled="!filters.level1Id" placeholder="二级分类">
          <el-option
            v-for="item in childrenOf(filters.level1Id, 2)"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="filters.level3Id" clearable :disabled="!filters.level2Id" placeholder="三级分类">
          <el-option
            v-for="item in childrenOf(filters.level2Id, 3)"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="filters.type" clearable placeholder="全部题型">
          <el-option label="单选" value="SINGLE" />
          <el-option label="多选" value="MULTIPLE" />
          <el-option label="判断" value="JUDGE" />
          <el-option label="分析" value="ANALYSIS" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option label="启用" value="ENABLED" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" :loading="loading" @click="searchQuestions">查询</el-button>
      </div>
    </el-card>

    <el-card class="question-table-card admin-list-card" shadow="never">
      <template #header>
        <div class="toolbar-inline" style="justify-content: space-between">
          <strong>题目列表</strong>
          <div class="toolbar-inline">
            <el-button
              type="danger"
              plain
              :icon="DeleteIcon"
              :disabled="!selectedQuestionIds.length"
              :loading="deleting"
              @click="removeSelectedQuestions"
            >
              批量删除
            </el-button>
            <el-button :icon="Refresh" :loading="loading" @click="loadQuestions">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="questions"
        class="admin-fill-table"
        stripe
        height="100%"
        empty-text="暂无题目"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" fixed="left" />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="一级分类" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ categoryPath(row.categoryId).level1 }}</template>
        </el-table-column>
        <el-table-column label="二级分类" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ categoryPath(row.categoryId).level2 }}</template>
        </el-table-column>
        <el-table-column label="三级分类" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ categoryPath(row.categoryId).level3 }}</template>
        </el-table-column>
        <el-table-column label="题干" min-width="280">
          <template #default="{ row }">
            <div class="question-title-cell">{{ row.title }}</div>
          </template>
        </el-table-column>
        <el-table-column label="选项" min-width="280">
          <template #default="{ row }">
            <div v-if="row.options?.length" class="option-preview">
              <span
                v-for="option in row.options"
                :key="option.id || option.optionKey"
                :class="['option-preview-item', option.correct ? 'correct' : '']"
              >
                {{ option.optionKey }}. {{ option.optionContent }}
              </span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="答案" min-width="100">
          <template #default="{ row }">{{ formatAnswer(row.correctAnswer) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'warning'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :icon="DeleteIcon"
              :loading="deleting"
              @click="removeQuestion(row)"
            >
              删除
            </el-button>
          </template>
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

    <el-dialog v-model="importVisible" class="admin-scroll-dialog" title="批量导入题目" width="680px">
      <div class="file-box">
        <el-alert type="info" :closable="false" show-icon>
          请先下载对应模板填写题目。选择题选项可写在一个单元格内，支持不固定数量的选项。
        </el-alert>

        <el-upload
          drag
          action="#"
          accept=".xlsx,.xls"
          :auto-upload="false"
          :limit="1"
          :before-upload="beforeImportUpload"
          :on-change="handleImportChange"
          :on-remove="removeImportFile"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽 Excel 到这里，或 <em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 .xlsx / .xls，按模板工作表导入。</div>
          </template>
        </el-upload>

        <el-alert v-if="importFile" type="success" :closable="false" show-icon>
          当前文件：{{ importFile.name }}
        </el-alert>

        <el-card v-if="importResult" shadow="never">
          <template #header>
            <div class="toolbar-inline" style="justify-content: space-between">
              <strong>导入结果</strong>
              <el-tag
                :type="
                  importResult.status === 'SUCCESS'
                    ? 'success'
                    : importResult.status === 'FAILED'
                      ? 'danger'
                      : 'warning'
                "
              >
                {{ importResult.status }}
              </el-tag>
            </div>
          </template>
          <div class="stat-grid">
            <el-statistic title="总行数" :value="importResult.totalCount" />
            <el-statistic title="成功" :value="importResult.successCount" />
            <el-statistic title="失败" :value="importResult.failCount" />
            <el-statistic title="批次 ID" :value="importResult.id" />
          </div>
          <el-alert
            v-if="importResult.failCount && !importResult.errors?.length"
            class="card-gap"
            type="warning"
            :closable="false"
            show-icon
            title="没有返回失败明细，请重启后端后重新导入。"
          />
          <el-table
            v-if="importResult.errors?.length"
            class="card-gap"
            :data="importResult.errors"
            size="small"
            max-height="260"
            border
          >
            <el-table-column label="行号" width="86">
              <template #default="{ row }">{{ row.rowNo === 0 ? '文件' : row.rowNo }}</template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="失败原因" min-width="260" show-overflow-tooltip />
            <el-table-column label="原始数据" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ formatImportRawData(row.rawData) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="uploadImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialogVisible"
      class="admin-scroll-dialog"
      :title="selectedId ? '编辑题目' : '新建题目'"
      width="760px"
      destroy-on-close
      @closed="resetForm"
    >
      <div v-loading="dialogLoading">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="question-dialog-grid">
            <el-form-item label="一级分类" prop="level1Id">
              <el-select v-model="form.level1Id" placeholder="请选择一级分类" style="width: 100%">
                <el-option v-for="item in level1Options" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="二级分类">
              <el-select v-model="form.level2Id" clearable :disabled="!form.level1Id" placeholder="请选择二级分类" style="width: 100%">
                <el-option
                  v-for="item in childrenOf(form.level1Id, 2)"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="三级分类">
              <el-select v-model="form.level3Id" clearable :disabled="!form.level2Id" placeholder="请选择三级分类" style="width: 100%">
                <el-option
                  v-for="item in childrenOf(form.level2Id, 3)"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </div>

          <div class="question-dialog-grid two">
            <el-form-item label="题型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="单选" value="SINGLE" />
                <el-option label="多选" value="MULTIPLE" />
                <el-option label="判断" value="JUDGE" />
                <el-option label="分析" value="ANALYSIS" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" value="ENABLED" />
                <el-option label="草稿" value="DRAFT" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="题干" prop="title">
            <el-input v-model.trim="form.title" type="textarea" :rows="4" />
          </el-form-item>

          <el-form-item v-if="form.type === 'SINGLE' || form.type === 'MULTIPLE'" label="选项">
            <div class="option-list">
              <div v-for="(option, index) in form.options" :key="option.optionKey" class="option-row">
                <el-tag>{{ option.optionKey }}</el-tag>
                <el-input v-model="option.optionContent" placeholder="请输入选项内容" />
                <el-radio
                  v-if="form.type === 'SINGLE'"
                  :model-value="singleCorrectKey"
                  :label="option.optionKey"
                  @change="setSingleCorrect(option.optionKey)"
                >
                  正确
                </el-radio>
                <el-checkbox v-else v-model="option.correct">正确</el-checkbox>
                <el-button :icon="DeleteIcon" circle plain @click="removeOption(index)" />
              </div>
              <el-button :icon="Plus" plain @click="addOption">添加选项</el-button>
            </div>
          </el-form-item>

          <el-form-item v-if="form.type === 'JUDGE'" label="正确答案">
            <el-radio-group v-model="form.correctAnswer">
              <el-radio-button label="TRUE">正确</el-radio-button>
              <el-radio-button label="FALSE">错误</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="form.type === 'ANALYSIS'" label="参考答案">
            <el-input v-model="form.correctAnswer" type="textarea" :rows="4" />
          </el-form-item>

          <el-form-item label="解析">
            <el-input v-model="form.analysis" type="textarea" :rows="4" />
          </el-form-item>

          <el-form-item label="来源文件">
            <el-input v-model.trim="form.sourceFile" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存题目</el-button>
      </template>
    </el-dialog>
  </div>
</template>
