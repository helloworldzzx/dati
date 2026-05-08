<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api } from '@/services/api'

const categories = ref([])
const questions = ref([])
const selectedId = ref(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

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

function resetForm() {
  selectedId.value = null
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
  error.value = ''
  try {
    questions.value = await api.questions({
      categoryId: filters.categoryId,
      type: filters.type,
      status: filters.status,
      page: 1,
      size: 100,
    })
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  try {
    await loadCategories()
    await loadQuestions()
  } catch (err) {
    error.value = err.message || '加载失败'
  }
}

async function edit(question) {
  error.value = ''
  success.value = ''
  selectedId.value = question.id
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
    error.value = err.message || '加载题目失败'
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
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const payload = buildPayload()
    if (!payload.categoryId) {
      throw new Error('请选择分类')
    }
    if ((payload.type === 'SINGLE' || payload.type === 'MULTIPLE') && !payload.correctAnswer) {
      throw new Error('请选择正确答案')
    }
    if (selectedId.value) {
      await api.updateQuestion(selectedId.value, payload)
      success.value = '题目已更新'
    } else {
      await api.createQuestion(payload)
      success.value = '题目已创建'
      resetForm()
    }
    await loadQuestions()
  } catch (err) {
    error.value = err.message || '保存失败'
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
      <button class="btn" type="button" @click="resetForm">新建题目</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>

    <div class="toolbar">
      <select v-model="filters.categoryId" class="select" style="width: 220px">
        <option value="">全部分类</option>
        <option v-for="item in categoryOptions" :key="item.id" :value="item.id">
          {{ '　'.repeat(item.level - 1) }}{{ item.name }}
        </option>
      </select>
      <select v-model="filters.type" class="select" style="width: 140px">
        <option value="">全部题型</option>
        <option value="SINGLE">单选</option>
        <option value="MULTIPLE">多选</option>
        <option value="JUDGE">判断</option>
        <option value="ANALYSIS">分析</option>
      </select>
      <select v-model="filters.status" class="select" style="width: 140px">
        <option value="ENABLED">启用</option>
        <option value="DRAFT">草稿</option>
        <option value="DISABLED">禁用</option>
        <option value="">全部状态</option>
      </select>
      <button class="btn btn-primary" type="button" :disabled="loading" @click="loadQuestions">查询</button>
    </div>

    <div class="grid grid-2">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>题型</th>
              <th>题干</th>
              <th>分类</th>
              <th>答案</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="question in questions" :key="question.id">
              <td>
                <span class="badge">{{ typeLabel(question.type) }}</span>
              </td>
              <td class="question-title-cell">{{ question.title }}</td>
              <td>{{ categoryName(question.categoryId) }}</td>
              <td>{{ question.correctAnswer || '-' }}</td>
              <td>
                <span :class="['badge', question.status === 'ENABLED' ? 'badge-success' : 'badge-warning']">
                  {{ question.status }}
                </span>
              </td>
              <td>
                <button class="btn" type="button" @click="edit(question)">编辑</button>
              </td>
            </tr>
            <tr v-if="!questions.length">
              <td colspan="6" class="muted">{{ loading ? '加载中' : '暂无题目' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <section class="card">
        <div class="card-head">
          <h3 class="card-title">{{ selectedQuestion ? '编辑题目' : '新建题目' }}</h3>
        </div>
        <div class="card-body">
          <form class="form" @submit.prevent="save">
            <div class="form-grid">
              <div class="form-row">
                <label>分类</label>
                <select v-model="form.categoryId" class="select" required>
                  <option value="" disabled>请选择分类</option>
                  <option v-for="item in categoryOptions" :key="item.id" :value="item.id">
                    {{ '　'.repeat(item.level - 1) }}{{ item.name }}
                  </option>
                </select>
              </div>
              <div class="form-row">
                <label>题型</label>
                <select v-model="form.type" class="select">
                  <option value="SINGLE">单选</option>
                  <option value="MULTIPLE">多选</option>
                  <option value="JUDGE">判断</option>
                  <option value="ANALYSIS">分析</option>
                </select>
              </div>
            </div>

            <div class="form-row">
              <label>题干</label>
              <textarea v-model.trim="form.title" class="textarea" required />
            </div>

            <div v-if="form.type === 'SINGLE' || form.type === 'MULTIPLE'" class="form-row">
              <label>选项</label>
              <div class="option-grid">
                <div v-for="option in form.options" :key="option.optionKey" class="option-row">
                  <strong>{{ option.optionKey }}</strong>
                  <input v-model="option.optionContent" class="input" />
                  <label class="muted">
                    <input
                      v-if="form.type === 'SINGLE'"
                      type="radio"
                      name="single-answer"
                      :checked="option.correct"
                      @change="setSingleCorrect(option.optionKey)"
                    />
                    <input v-else v-model="option.correct" type="checkbox" />
                    正确
                  </label>
                </div>
              </div>
            </div>

            <div v-if="form.type === 'JUDGE'" class="form-row">
              <label>正确答案</label>
              <select v-model="form.correctAnswer" class="select">
                <option value="TRUE">正确</option>
                <option value="FALSE">错误</option>
              </select>
            </div>

            <div v-if="form.type === 'ANALYSIS'" class="form-row">
              <label>参考答案</label>
              <textarea v-model="form.correctAnswer" class="textarea" />
            </div>

            <div class="form-row">
              <label>解析</label>
              <textarea v-model="form.analysis" class="textarea" />
            </div>

            <div class="form-row">
              <label>来源文件</label>
              <input v-model.trim="form.sourceFile" class="input" />
            </div>

            <div class="form-row">
              <label>状态</label>
              <select v-model="form.status" class="select">
                <option value="ENABLED">启用</option>
                <option value="DRAFT">草稿</option>
                <option value="DISABLED">禁用</option>
              </select>
            </div>

            <button class="btn btn-primary" type="submit" :disabled="saving">
              {{ saving ? '保存中' : '保存题目' }}
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>
