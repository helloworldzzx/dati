<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'

const categories = ref([])
const questions = ref([])
const selectedId = ref(null)
const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const filters = reactive({
  categoryId: '',
  type: '',
  status: 'ENABLED',
})

const optionKeys = ['A', 'B', 'C', 'D', 'E']
const form = reactive({
  categoryId: '',
  type: 'SINGLE',
  title: '',
  correctAnswer: '',
  analysis: '',
  sourceFile: '',
  status: 'ENABLED',
  options: buildEmptyOptions(),
})

const rules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  title: [{ required: true, message: '请输入题干', trigger: 'blur' }],
}

const categoryOptions = computed(() =>
  [...categories.value].sort((left, right) => left.level - right.level || left.id - right.id),
)
const selectedQuestion = computed(() => questions.value.find((item) => item.id === selectedId.value))

watch(
  () => form.type,
  (type) => {
    if ((type === 'SINGLE' || type === 'MULTIPLE') && !form.options.length) {
      form.options = buildEmptyOptions()
    }
    if (type === 'JUDGE' && !['TRUE', 'FALSE'].includes(form.correctAnswer)) {
      form.correctAnswer = 'TRUE'
    }
    if (type === 'ANALYSIS') {
      form.options = []
    }
  },
)

function buildEmptyOptions() {
  return optionKeys.map((key, index) => ({
    optionKey: key,
    optionContent: '',
    correct: false,
    sortNo: index,
  }))
}

function categoryName(id) {
  return categories.value.find((item) => item.id === id)?.name || '-'
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

function resetForm() {
  selectedId.value = null
  formRef.value?.clearValidate()
  Object.assign(form, {
    categoryId: categories.value[0]?.id || '',
    type: 'SINGLE',
    title: '',
    correctAnswer: '',
    analysis: '',
    sourceFile: '',
    status: 'ENABLED',
    options: buildEmptyOptions(),
  })
}

async function loadCategories() {
  categories.value = await api.categories()
  if (!form.categoryId && categories.value.length) {
    form.categoryId = categories.value[0].id
  }
}

async function loadQuestions() {
  loading.value = true
  try {
    questions.value = await api.questions({
      categoryId: filters.categoryId,
      type: filters.type,
      status: filters.status,
      page: 1,
      size: 100,
    })
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
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

async function edit(question) {
  selectedId.value = question.id
  formRef.value?.clearValidate()
  try {
    const detail = await api.questionDetail(question.id)
    const item = detail.question
    const options = detail.options?.length
      ? detail.options.map((option, index) => ({
          optionKey: option.optionKey,
          optionContent: option.optionContent,
          correct: Boolean(option.correct),
          sortNo: option.sortNo ?? index,
        }))
      : buildEmptyOptions()

    Object.assign(form, {
      categoryId: item.categoryId,
      type: item.type,
      title: item.title,
      correctAnswer: item.correctAnswer || '',
      analysis: item.analysis || '',
      sourceFile: item.sourceFile || '',
      status: item.status || 'ENABLED',
      options: item.type === 'ANALYSIS' ? [] : options,
    })
  } catch (err) {
    ElMessage.error(err.message || '加载题目失败')
  }
}

function setSingleCorrect(key) {
  form.options.forEach((option) => {
    option.correct = option.optionKey === key
  })
}

function buildCorrectAnswer() {
  if (form.type === 'JUDGE' || form.type === 'ANALYSIS') {
    return form.correctAnswer
  }
  return form.options
    .filter((option) => option.correct && option.optionContent)
    .map((option) => option.optionKey)
    .join(',')
}

function buildPayload() {
  const options =
    form.type === 'SINGLE' || form.type === 'MULTIPLE'
      ? form.options
          .filter((option) => option.optionContent.trim())
          .map((option, index) => ({
            optionKey: option.optionKey,
            optionContent: option.optionContent.trim(),
            correct: Boolean(option.correct),
            sortNo: index,
          }))
      : []

  return {
    categoryId: Number(form.categoryId),
    type: form.type,
    title: form.title,
    correctAnswer: buildCorrectAnswer(),
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
      resetForm()
    }
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
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">题目管理</h2>
        <p class="page-desc">维护单选、多选、判断和分析题。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="resetForm">新建题目</el-button>
    </div>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-inline">
        <el-select v-model="filters.categoryId" clearable placeholder="全部分类" style="width: 220px">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="`${'　'.repeat(item.level - 1)}${item.name}`"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="filters.type" clearable placeholder="全部题型" style="width: 140px">
          <el-option label="单选" value="SINGLE" />
          <el-option label="多选" value="MULTIPLE" />
          <el-option label="判断" value="JUDGE" />
          <el-option label="分析" value="ANALYSIS" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" :loading="loading" @click="loadQuestions">查询</el-button>
      </div>
    </el-card>

    <div class="content-grid">
      <el-card shadow="never">
        <template #header>
          <div class="toolbar-inline" style="justify-content: space-between">
            <strong>题目列表</strong>
            <el-button :icon="Refresh" :loading="loading" @click="loadQuestions">刷新</el-button>
          </div>
        </template>

        <el-table v-loading="loading" :data="questions" stripe height="620" empty-text="暂无题目">
          <el-table-column label="题型" width="90">
            <template #default="{ row }">
              <el-tag :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题干" min-width="320">
            <template #default="{ row }">
              <div class="question-title-cell">{{ row.title }}</div>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="140">
            <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
          </el-table-column>
          <el-table-column label="答案" min-width="90">
            <template #default="{ row }">{{ row.correctAnswer || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'warning'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Edit" @click="edit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <strong>{{ selectedQuestion ? '编辑题目' : '新建题目' }}</strong>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option
                v-for="item in categoryOptions"
                :key="item.id"
                :label="`${'　'.repeat(item.level - 1)}${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="题型" prop="type">
            <el-select v-model="form.type" style="width: 100%">
              <el-option label="单选" value="SINGLE" />
              <el-option label="多选" value="MULTIPLE" />
              <el-option label="判断" value="JUDGE" />
              <el-option label="分析" value="ANALYSIS" />
            </el-select>
          </el-form-item>

          <el-form-item label="题干" prop="title">
            <el-input v-model.trim="form.title" type="textarea" :rows="4" />
          </el-form-item>

          <el-form-item v-if="form.type === 'SINGLE' || form.type === 'MULTIPLE'" label="选项">
            <div class="option-list">
              <div v-for="option in form.options" :key="option.optionKey" class="option-row">
                <el-tag>{{ option.optionKey }}</el-tag>
                <el-input v-model="option.optionContent" />
                <el-radio
                  v-if="form.type === 'SINGLE'"
                  :model-value="option.correct"
                  :label="true"
                  @change="setSingleCorrect(option.optionKey)"
                >
                  正确
                </el-radio>
                <el-checkbox v-else v-model="option.correct">正确</el-checkbox>
              </div>
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

          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="草稿" value="DRAFT" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>

          <el-button type="primary" :loading="saving" style="width: 100%" @click="save">保存题目</el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
