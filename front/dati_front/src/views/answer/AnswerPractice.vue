<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Document, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const QUESTION_BATCH_SIZE = 10
const PREFETCH_REMAINING = 3
const loading = ref(false)
const questionCache = ref(new Map())
const loadedPages = ref(new Set())
const loadingPages = ref(new Set())
const hasMoreQuestions = ref(true)
const totalKnownCount = ref(null)
const restoringProgress = ref(false)
const pageRequests = new Map()
const currentIndex = ref(0)
const currentDetail = ref(null)
const selectedAnswers = ref([])
const analysisAnswer = ref('')
const favoriteIds = ref(new Set())
const pageTurn = ref('next')

const records = reactive({})
const drafts = reactive({})
const swipeGesture = reactive({
  startX: 0,
  startY: 0,
  ignored: false,
})
let saveTimer = null

const SWIPE_MIN_DISTANCE = 72
const SWIPE_DIRECTION_RATIO = 1.35

const mode = computed(() => route.query.mode || 'practice')
const categoryId = computed(() => (route.query.categoryId ? Number(route.query.categoryId) : null))
const backTarget = computed(() => {
  const target = Array.isArray(route.query.return) ? route.query.return[0] : route.query.return
  if (typeof target === 'string' && target.startsWith('/answer')) return target
  if (mode.value === 'wrong' || mode.value === 'favorite') return '/answer/mine'
  return '/answer'
})
const pageTitle = computed(() => {
  if (mode.value === 'wrong') return '错题本'
  if (mode.value === 'favorite') return '收藏题'
  return route.query.title || '答题练习'
})
const currentQuestionSummary = computed(() => questionCache.value.get(currentIndex.value))
const currentQuestion = computed(() => currentDetail.value?.question)
const loadedQuestionCount = computed(() => questionCache.value.size)
const counterText = computed(() => {
  const minimumVisibleCount = Math.max(loadedQuestionCount.value, currentIndex.value + 1)
  const totalText = totalKnownCount.value === null ? `${minimumVisibleCount}+` : totalKnownCount.value
  return `${currentIndex.value + 1}/${totalText}`
})
const nextDisabled = computed(() => !hasMoreQuestions.value && !questionCache.value.has(currentIndex.value + 1))
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
const pageTurnName = computed(() => `page-turn-${pageTurn.value}`)
const showManualSubmit = computed(() => {
  const type = currentQuestion.value?.type
  return (type === 'MULTIPLE' || type === 'ANALYSIS') && !submitted.value
})

watch(currentIndex, async () => {
  if (restoringProgress.value) return
  await ensureQuestionAt(currentIndex.value)
  await loadCurrentDetail()
  prefetchAround(currentIndex.value)
  await saveProgressNow()
})

watch(analysisAnswer, () => {
  if (currentQuestion.value?.type === 'ANALYSIS' && !submitted.value) saveDraft()
})

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

function replaceReactiveObject(target, source) {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source || {})
}

function resetQuestionBuffer() {
  questionCache.value = new Map()
  loadedPages.value = new Set()
  loadingPages.value = new Set()
  hasMoreQuestions.value = true
  totalKnownCount.value = null
  pageRequests.clear()
}

function pageForIndex(index) {
  return Math.floor(Math.max(index, 0) / QUESTION_BATCH_SIZE) + 1
}

function setWithCopy(setRef, value, enabled) {
  const next = new Set(setRef.value)
  if (enabled) next.add(value)
  else next.delete(value)
  setRef.value = next
}

function trimQuestionBuffer(centerIndex = currentIndex.value) {
  const centerPage = pageForIndex(centerIndex)
  const keepPages = new Set([centerPage - 1, centerPage, centerPage + 1].filter((page) => page >= 1))
  const nextCache = new Map()

  questionCache.value.forEach((question, index) => {
    if (keepPages.has(pageForIndex(index))) nextCache.set(index, question)
  })
  questionCache.value = nextCache
  loadedPages.value = new Set([...loadedPages.value].filter((page) => keepPages.has(page)))
}

async function fetchQuestionPage(page) {
  if (page < 1 || loadedPages.value.has(page)) return
  if (pageRequests.has(page)) return pageRequests.get(page)

  const request = (async () => {
    setWithCopy(loadingPages, page, true)
    try {
      const params = { page, size: QUESTION_BATCH_SIZE }
      let rows = []

      if (mode.value === 'wrong') {
        rows = await api.wrongQuestions(auth.user.id, params)
      } else if (mode.value === 'favorite') {
        rows = await api.favoriteQuestions(auth.user.id, params)
      } else {
        rows = await api.questions({
          categoryId: categoryId.value,
          status: 'ENABLED',
          ...params,
        })
      }

      rows = Array.isArray(rows) ? rows : []
      const startIndex = (page - 1) * QUESTION_BATCH_SIZE
      const nextCache = new Map(questionCache.value)
      rows.forEach((item, offset) => {
        nextCache.set(startIndex + offset, item)
      })
      questionCache.value = nextCache
      setWithCopy(loadedPages, page, true)

      if (mode.value === 'favorite') {
        const ids = new Set(favoriteIds.value)
        rows.forEach((item) => ids.add(item.id))
        favoriteIds.value = ids
      }

      if (rows.length < QUESTION_BATCH_SIZE) {
        hasMoreQuestions.value = false
        totalKnownCount.value = startIndex + rows.length
      }
    } finally {
      setWithCopy(loadingPages, page, false)
      pageRequests.delete(page)
    }
  })()

  pageRequests.set(page, request)
  return request
}

async function ensureQuestionAt(index) {
  if (index < 0) return false
  if (questionCache.value.has(index)) return true
  await fetchQuestionPage(pageForIndex(index))
  return questionCache.value.has(index)
}

function prefetchAround(index) {
  const page = pageForIndex(index)
  const pageStartIndex = (page - 1) * QUESTION_BATCH_SIZE
  const pageEndIndex = pageStartIndex + QUESTION_BATCH_SIZE - 1

  trimQuestionBuffer(index)

  if (index - pageStartIndex <= PREFETCH_REMAINING && page > 1) {
    fetchQuestionPage(page - 1).catch(() => {})
  }
  if (pageEndIndex - index <= PREFETCH_REMAINING && hasMoreQuestions.value) {
    fetchQuestionPage(page + 1).catch(() => {})
  }
}

async function loadSavedProgress() {
  return api.practiceProgress(auth.user.id, {
    mode: normalizeMode(),
    categoryId: categoryId.value,
  })
}

function restoreProgress(progress) {
  replaceReactiveObject(records, {})
  replaceReactiveObject(drafts, progress?.drafts)

  const nextIndex = Number.isInteger(progress?.currentIndex) ? Math.max(progress.currentIndex, 0) : 0
  currentIndex.value = nextIndex
  return nextIndex
}

function progressPayload() {
  const current = currentQuestionSummary.value
  const questionIds = [...questionCache.value.entries()]
    .sort(([leftIndex], [rightIndex]) => leftIndex - rightIndex)
    .map(([, item]) => item.id)

  return {
    mode: normalizeMode(),
    categoryId: categoryId.value,
    currentIndex: currentIndex.value,
    currentQuestionId: current?.id || null,
    questionIds,
    drafts: { ...drafts },
  }
}

async function saveProgressNow() {
  if (!auth.user?.id || !questionCache.value.size) return
  if (saveTimer) {
    window.clearTimeout(saveTimer)
    saveTimer = null
  }
  try {
    await api.savePracticeProgress(auth.user.id, progressPayload())
  } catch {
    // 自动保存失败时不打断答题，下一次切题或提交会继续尝试保存。
  }
}

function scheduleSaveProgress() {
  if (saveTimer) window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => {
    saveProgressNow()
  }, 600)
}

function saveDraft() {
  if (!currentQuestion.value || submitted.value) return
  const answer = currentAnswer()
  if (answer) drafts[currentQuestion.value.id] = { userAnswer: answer }
  else delete drafts[currentQuestion.value.id]
  scheduleSaveProgress()
}

async function loadQuestions() {
  loading.value = true
  restoringProgress.value = true
  resetQuestionBuffer()
  try {
    const progress = await loadSavedProgress()
    const savedIndex = restoreProgress(progress)
    let hasQuestion = await ensureQuestionAt(savedIndex)

    if (!hasQuestion && savedIndex > 0) {
      resetQuestionBuffer()
      currentIndex.value = 0
      hasQuestion = await ensureQuestionAt(0)
    }

    restoringProgress.value = false
    if (hasQuestion) {
      await loadCurrentDetail()
      prefetchAround(currentIndex.value)
      await saveProgressNow()
    } else {
      currentDetail.value = null
    }
  } catch (err) {
    ElMessage.error(err.message || '加载题目失败')
  } finally {
    restoringProgress.value = false
    loading.value = false
  }
}

async function loadCurrentDetail() {
  const item = currentQuestionSummary.value
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

    if (!records[item.id] && stat?.lastAnswer) {
      records[item.id] = {
        submitted: true,
        userAnswer: stat.lastAnswer,
        correct: stat.lastCorrect,
      }
    }

    const savedAnswer = records[item.id]?.userAnswer || drafts[item.id]?.userAnswer || ''
    selectedAnswers.value = savedAnswer ? savedAnswer.split(',').filter(Boolean) : []
    analysisAnswer.value = savedAnswer
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
    saveDraft()
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
  if (!currentQuestion.value) return ''
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
      sessionId: null,
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
    delete drafts[currentQuestion.value.id]
    await saveProgressNow()
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

async function previous() {
  if (currentIndex.value <= 0) return
  try {
    const targetIndex = currentIndex.value - 1
    if (await ensureQuestionAt(targetIndex)) {
      pageTurn.value = 'prev'
      currentIndex.value = targetIndex
    }
  } catch (err) {
    ElMessage.error(err.message || '加载上一题失败')
  }
}

function goBack() {
  router.push(backTarget.value)
}

async function next() {
  try {
    const targetIndex = currentIndex.value + 1
    if (await ensureQuestionAt(targetIndex)) {
      pageTurn.value = 'next'
      currentIndex.value = targetIndex
      return
    }
  } catch (err) {
    ElMessage.error(err.message || '加载下一题失败')
    return
  }
  ElMessage.info('已经是最后一题')
}

function globalAccuracy(question) {
  if (!question?.answerCount) return '0%'
  return `${Math.round(((question.correctCount || 0) * 100) / question.answerCount)}%`
}

function personalAccuracy(record) {
  if (!record?.submitted || record.correct === null || record.correct === undefined) return '-'
  return record.correct ? '100%' : '0%'
}

function isSwipeIgnored(target) {
  return Boolean(target?.closest?.('input, textarea, select, .el-input, .el-textarea, .question-bottom'))
}

function handleTouchStart(event) {
  const touch = event.touches?.[0]
  if (!touch) return
  swipeGesture.startX = touch.clientX
  swipeGesture.startY = touch.clientY
  swipeGesture.ignored = isSwipeIgnored(event.target)
}

function handleTouchEnd(event) {
  if (swipeGesture.ignored || !currentQuestion.value) return
  const touch = event.changedTouches?.[0]
  if (!touch) return

  const deltaX = touch.clientX - swipeGesture.startX
  const deltaY = touch.clientY - swipeGesture.startY
  const absX = Math.abs(deltaX)
  const absY = Math.abs(deltaY)

  if (absX < SWIPE_MIN_DISTANCE || absX < absY * SWIPE_DIRECTION_RATIO) return

  if (deltaX > 0) previous()
  else next()
}

onMounted(loadQuestions)
onBeforeUnmount(() => {
  saveProgressNow()
})
</script>

<template>
  <main class="mobile-shell">
    <div class="mobile-page practice-page" @touchstart.passive="handleTouchStart" @touchend.passive="handleTouchEnd">
      <template v-if="currentQuestion">
        <div class="question-topbar">
          <div class="question-tools">
            <button class="tree-toggle" type="button" @click="goBack">‹</button>
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
            <span class="question-counter">{{ counterText }}</span>
          </div>
        </div>

        <Transition :name="pageTurnName" mode="out-in">
          <div :key="currentQuestion.id" class="question-page-frame">
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
            </section>

            <section v-if="submitted" class="answer-section">
              <div class="answer-section-inner">
                <h2 class="answer-section-title">
                  <el-icon><Document /></el-icon>答案</h2>
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
          </div>
        </Transition>

        <footer class="question-bottom">
          <el-button
            v-if="showManualSubmit"
            class="question-submit-button"
            type="primary"
            size="large"
            @click="submit"
          >
            提交答案
          </el-button>
          <div class="question-nav-row">
            <el-button :icon="ArrowLeft" :disabled="currentIndex === 0" @click="previous">上一题</el-button>
            <el-button type="primary" :disabled="nextDisabled" @click="next">
              下一题
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </footer>
      </template>

      <div v-else class="question-empty">
        <el-empty :description="loading ? '题目加载中' : '暂无题目'">
          <el-button type="primary" @click="goBack">返回</el-button>
        </el-empty>
      </div>
    </div>
  </main>
</template>
