<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Document, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const questions = ref([])
const currentIndex = ref(0)
const currentDetail = ref(null)
const sessionId = ref(null)
const selectedAnswers = ref([])
const analysisAnswer = ref('')
const favoriteIds = ref(new Set())

const records = reactive({})

const mode = computed(() => route.query.mode || 'practice')
const categoryId = computed(() => (route.query.categoryId ? Number(route.query.categoryId) : null))
const pageTitle = computed(() => {
  if (mode.value === 'wrong') return '错题本'
  if (mode.value === 'favorite') return '收藏题'
  return route.query.title || '答题练习'
})
const currentQuestion = computed(() => currentDetail.value?.question)
const options = computed(() => {
  if (!currentQuestion.value) return []
  if (currentQuestion.value.type === 'JUDGE' && !currentDetail.value?.options?.length) {
    return [
      { optionKey: 'TRUE', optionContent: '正确' },
      { optionKey: 'FALSE', optionContent: '错误' },
    ]
  }
  return currentDetail.value?.options || []
})
const currentRecord = computed(() => (currentQuestion.value ? records[currentQuestion.value.id] : null))
const submitted = computed(() => Boolean(currentRecord.value?.submitted))
const userAnswerText = computed(() => currentRecord.value?.userAnswer || selectedAnswers.value.join(','))
const isFavorite = computed(() => currentQuestion.value && favoriteIds.value.has(currentQuestion.value.id))

watch(currentIndex, loadCurrentDetail)

function typeLabel(type) {
  return {
    SINGLE: '单选题',
    MULTIPLE: '多选题',
    JUDGE: '判断题',
    ANALYSIS: '分析题',
  }[type] || type
}

function normalizeMode() {
  if (mode.value === 'wrong') return 'WRONG_BOOK'
  if (mode.value === 'favorite') return 'FAVORITE'
  return 'PRACTICE'
}

async function loadQuestions() {
  loading.value = true
  try {
    if (mode.value === 'wrong') {
      questions.value = await api.wrongQuestions(auth.user.id)
    } else if (mode.value === 'favorite') {
      questions.value = await api.favoriteQuestions(auth.user.id)
      favoriteIds.value = new Set(questions.value.map((item) => item.id))
    } else {
      questions.value = await api.questions({
        categoryId: categoryId.value,
        status: 'ENABLED',
        page: 1,
        size: 200,
      })
    }

    if (questions.value.length) {
      const session = await api.startSession({
        userId: auth.user.id,
        categoryId: categoryId.value,
        mode: normalizeMode(),
        totalCount: questions.value.length,
      })
      sessionId.value = session.id
      currentIndex.value = 0
      await loadCurrentDetail()
    }
  } catch (err) {
    ElMessage.error(err.message || '加载题目失败')
  } finally {
    loading.value = false
  }
}

async function loadCurrentDetail() {
  const item = questions.value[currentIndex.value]
  if (!item) {
    currentDetail.value = null
    return
  }
  try {
    currentDetail.value = await api.questionDetail(item.id, auth.user.id)
    const stat = currentDetail.value?.stat
    const ids = new Set(favoriteIds.value)
    if (stat?.favorite) ids.add(item.id)
    else ids.delete(item.id)
    favoriteIds.value = ids

    const record = records[item.id]
    selectedAnswers.value = record?.userAnswer ? record.userAnswer.split(',').filter(Boolean) : []
    analysisAnswer.value = record?.userAnswer || ''
  } catch (err) {
    ElMessage.error(err.message || '加载题目详情失败')
  }
}

function toggleOption(option) {
  if (submitted.value) return
  const key = option.optionKey
  if (currentQuestion.value.type === 'MULTIPLE') {
    if (selectedAnswers.value.includes(key)) {
      selectedAnswers.value = selectedAnswers.value.filter((item) => item !== key)
    } else {
      selectedAnswers.value = [...selectedAnswers.value, key].sort()
    }
    return
  }
  selectedAnswers.value = [key]
  submit()
}

function optionClass(option) {
  const classes = []
  const key = option.optionKey
  const selected = selectedAnswers.value.includes(key) || currentRecord.value?.userAnswer?.split(',').includes(key)
  const correctKeys = splitAnswer(currentQuestion.value?.correctAnswer)

  if (selected) classes.push('selected')
  if (submitted.value && correctKeys.includes(key)) classes.push('correct')
  if (submitted.value && selected && !correctKeys.includes(key)) classes.push('wrong')
  return classes
}

function splitAnswer(value) {
  if (!value) return []
  return String(value)
    .split(/[,，、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function currentAnswer() {
  if (currentQuestion.value.type === 'ANALYSIS') return analysisAnswer.value.trim()
  return selectedAnswers.value.join(',')
}

function answerLabel(answer) {
  if (answer === 'TRUE') return '正确'
  if (answer === 'FALSE') return '错误'
  return answer || '-'
}

async function submit() {
  if (!currentQuestion.value || submitted.value) return
  const answer = currentAnswer()
  if (!answer) {
    ElMessage.warning('请先作答')
    return
  }

  try {
    const record = await api.submitAnswer({
      sessionId: sessionId.value,
      userId: auth.user.id,
      questionId: currentQuestion.value.id,
      userAnswer: answer,
      durationSeconds: 0,
    })
    records[currentQuestion.value.id] = {
      submitted: true,
      userAnswer: answer,
      correct: record.correct,
    }
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
  }
}

async function toggleFavorite() {
  if (!currentQuestion.value) return
  try {
    const next = !favoriteIds.value.has(currentQuestion.value.id)
    const stat = await api.updateFavorite(auth.user.id, currentQuestion.value.id, next)
    const ids = new Set(favoriteIds.value)
    if (stat?.favorite ?? next) ids.add(currentQuestion.value.id)
    else ids.delete(currentQuestion.value.id)
    favoriteIds.value = ids
    ElMessage.success((stat?.favorite ?? next) ? '已收藏' : '已取消收藏')
  } catch (err) {
    ElMessage.error(err.message || '收藏失败')
  }
}

function previous() {
  if (currentIndex.value > 0) currentIndex.value -= 1
}

function next() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value += 1
  } else {
    finish()
  }
}

async function finish() {
  if (sessionId.value) {
    try {
      await api.finishSession(sessionId.value)
    } catch {
      // 结束练习失败不阻塞返回首页。
    }
  }
  router.push('/answer')
}

function globalAccuracy(question) {
  if (!question?.answerCount) return '0%'
  return `${Math.round(((question.correctCount || 0) * 100) / question.answerCount)}%`
}

function personalAccuracy(record) {
  if (!record?.submitted || record.correct === null || record.correct === undefined) return '-'
  return record.correct ? '100%' : '0%'
}

onMounted(loadQuestions)
</script>

<template>
  <main class="mobile-shell">
    <div class="mobile-page">
      <template v-if="currentQuestion">
        <div class="question-topbar">
          <div class="question-tools">
            <button class="tree-toggle" type="button" @click="router.push('/answer')">‹</button>
            <strong class="question-page-title">{{ pageTitle }}</strong>
            <el-tag size="small">{{ typeLabel(currentQuestion.type) }}</el-tag>
          </div>
          <div class="answer-header-actions">
            <el-button text circle @click="toggleFavorite">
              <el-icon :size="20">
                <StarFilled v-if="isFavorite" style="color: #f59e0b" />
                <Star v-else />
              </el-icon>
            </el-button>
            <span class="question-counter">{{ currentIndex + 1 }}/{{ questions.length }}</span>
          </div>
        </div>

        <section class="question-card">
          <span class="question-type">{{ typeLabel(currentQuestion.type) }}</span>
          <h1 class="question-title">{{ currentQuestion.title }}</h1>

          <template v-if="currentQuestion.type === 'ANALYSIS'">
            <el-input
              v-model="analysisAnswer"
              type="textarea"
              :rows="7"
              :disabled="submitted"
              placeholder="请输入你的答案"
            />
            <el-button
              v-if="!submitted"
              type="primary"
              size="large"
              style="width: 100%; margin-top: 14px"
              @click="submit"
            >
              提交答案
            </el-button>
          </template>

          <div v-else class="option-mobile-list">
            <button
              v-for="option in options"
              :key="option.optionKey"
              class="option-mobile"
              :class="optionClass(option)"
              type="button"
              @click="toggleOption(option)"
            >
              <span class="option-key">
                {{ option.optionKey === 'TRUE' ? '对' : option.optionKey === 'FALSE' ? '错' : option.optionKey }}
              </span>
              <span class="option-text">{{ option.optionContent }}</span>
            </button>
          </div>

          <el-button
            v-if="currentQuestion.type === 'MULTIPLE' && !submitted"
            type="primary"
            size="large"
            style="width: 100%; margin-top: 22px"
            @click="submit"
          >
            提交答案
          </el-button>
        </section>

        <section v-if="submitted" class="answer-section">
          <div class="answer-section-inner">
            <h2 class="answer-section-title">答案</h2>
            <div class="answer-result-box">
              <div class="answer-result-row">
                <span>正确答案：</span>
                <strong style="color: #13c56b">{{ answerLabel(currentQuestion.correctAnswer) }}</strong>
                <span>你的答案：</span>
                <strong :style="{ color: currentRecord?.correct ? '#13c56b' : '#ff4d4f' }">
                  {{ answerLabel(userAnswerText) }}
                </strong>
              </div>
              <div class="answer-result-row">
                <span>个人正确率：</span>
                <strong style="color: #1677ff">{{ personalAccuracy(currentRecord) }}</strong>
                <span>全站正确率：</span>
                <strong style="color: #1677ff">{{ globalAccuracy(currentQuestion) }}</strong>
              </div>
            </div>
          </div>

          <div class="answer-section-inner" style="border-top: 8px solid #f3f4f6">
            <h2 class="answer-section-title">
              <el-icon><Document /></el-icon>
              解析
            </h2>
            <div class="answer-analysis">
              <strong>【答案解析】</strong>
              <br />
              {{ currentQuestion.analysis || '暂无解析' }}
              <template v-if="currentQuestion.sourceFile">
                <br /><br />
                <strong>【来源文件】</strong>
                {{ currentQuestion.sourceFile }}
              </template>
            </div>
          </div>
        </section>

        <footer class="question-bottom">
          <el-button :icon="ArrowLeft" :disabled="currentIndex === 0" @click="previous">上一题</el-button>
          <el-button type="primary" plain @click="finish">结束</el-button>
          <el-button type="primary" @click="next">
            {{ currentIndex === questions.length - 1 ? '完成' : '下一题' }}
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </footer>
      </template>

      <div v-else class="question-empty">
        <el-empty :description="loading ? '题目加载中' : '暂无题目'">
          <el-button type="primary" @click="router.push('/answer')">返回首页</el-button>
        </el-empty>
      </div>
    </div>
  </main>
</template>
