<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/services/api'
import bgImage from '@/images/background.jpg'
import goldMedal from '@/images/jinpai.png'
import silverMedal from '@/images/yinpai.png'
import bronzeMedal from '@/images/tongpai.png'

const route = useRoute()
const router = useRouter()
const rankings = ref([])
const loading = ref(false)
const sortMode = ref('answerCount')
let refreshTimer = null

const REFRESH_INTERVAL = 5000

const sortOptions = [
  { label: '答题数量', value: 'answerCount' },
  { label: '准确率', value: 'accuracy' },
]

const medalIcons = [goldMedal, silverMedal, bronzeMedal]

const backTarget = computed(() => {
  const target = Array.isArray(route.query.return) ? route.query.return[0] : route.query.return
  return typeof target === 'string' && target.startsWith('/answer') ? target : '/answer'
})
const rankTitle = computed(() => (sortMode.value === 'accuracy' ? '准确率排行榜' : '答题数量排行榜'))
const metricLabel = computed(() => (sortMode.value === 'accuracy' ? '准确率' : '答题数'))
const topPodium = computed(() => [
  { rank: 2, item: rankings.value[1], icon: silverMedal },
  { rank: 1, item: rankings.value[0], icon: goldMedal },
  { rank: 3, item: rankings.value[2], icon: bronzeMedal },
].filter((entry) => entry.item))

function userName(item) {
  return item?.realName || item?.username || '答题用户'
}

function numberValue(value) {
  const parsed = Number(value ?? 0)
  return Number.isFinite(parsed) ? parsed : 0
}

function accuracyText(item) {
  const value = numberValue(item?.accuracyRate)
  const rounded = Math.round(value * 100) / 100
  return `${rounded}%`
}

function metricValue(item) {
  return sortMode.value === 'accuracy' ? accuracyText(item) : `${numberValue(item?.answerCount)}题`
}

function subMetric(item) {
  return sortMode.value === 'accuracy'
    ? `${numberValue(item?.answerCount)}题`
    : accuracyText(item)
}

async function load(options = {}) {
  const quiet = Boolean(options.quiet)
  if (!quiet) loading.value = true
  try {
    const result = await api.rankings(100, sortMode.value)
    rankings.value = Array.isArray(result) ? result : []
  } catch (err) {
    if (!quiet) ElMessage.error(err.message || '加载失败')
  } finally {
    if (!quiet) loading.value = false
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = window.setInterval(() => {
    if (document.visibilityState === 'visible') {
      load({ quiet: true })
    }
  }, REFRESH_INTERVAL)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

function medalIcon(index) {
  return medalIcons[index]
}

function goBack() {
  router.push(backTarget.value)
}

watch(sortMode, () => load())
onMounted(() => {
  load()
  startAutoRefresh()
})
onBeforeUnmount(stopAutoRefresh)
</script>

<template>
  <main class="mobile-shell ranking-shell">
    <div class="mobile-page ranking-page" :style="{ '--ranking-bg': `url(${bgImage})` }">
      <header class="ranking-nav">
        <button class="tree-toggle ranking-back" type="button" @click="goBack">‹</button>
        <el-button text :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </header>

      <section class="ranking-poster">
        <div class="ranking-hero">
          <div class="ranking-kicker">TOP 榜单</div>
          <h1>答题排行榜</h1>
          <!-- <p>{{ rankTitle }}</p> -->
        </div>

        <div class="ranking-sort">
          <el-radio-group v-model="sortMode" size="small">
            <el-radio-button v-for="item in sortOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </div>

        <!-- <div v-if="topPodium.length" class="ranking-podium">
          <div
            v-for="entry in topPodium"
            :key="entry.rank"
            :class="['ranking-podium-item', entry.rank === 1 ? 'first' : '']"
          >
            <img :src="entry.icon" alt="" />
            <strong>{{ userName(entry.item) }}</strong>
            <span>{{ metricValue(entry.item) }}</span>
          </div>
        </div> -->

        <section class="ranking-board">
          <div class="ranking-board-title">
            <span>{{ rankTitle }}</span>
            <strong>{{ rankings.length }}人</strong>
          </div>

          <el-skeleton v-if="loading" :rows="7" animated />
          <template v-else>
            <div v-if="rankings.length" class="ranking-table">
              <div class="ranking-table-head">
                <span>排名</span>
                <span>用户</span>
                <span>{{ metricLabel }}</span>
              </div>
              <div v-for="(item, index) in rankings" :key="item.userId" class="ranking-row">
                <div class="ranking-place">
                  <img v-if="index < 3" :src="medalIcon(index)" alt="" />
                  <span v-else>{{ index + 1 }}</span>
                </div>
                <div class="ranking-user">
                  <strong>{{ userName(item) }}</strong>
                  <!-- <span>{{ item.username }}</span> -->
                </div>
                <div class="ranking-score">
                  <strong>{{ metricValue(item) }}</strong>
                  <span>{{ subMetric(item) }}</span>
                </div>
              </div>
            </div>

            <el-empty v-else description="暂无排行数据">
              <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
            </el-empty>
          </template>
        </section>
      </section>
    </div>
  </main>
</template>
