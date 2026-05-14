<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Document, Folder, FolderOpened, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AnswerTabBar from '@/components/AnswerTabBar.vue'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const categories = ref([])
const progressByCategory = ref(new Map())
const loading = ref(false)
const treeProps = {
  children: 'children',
  label: 'name',
}

function hasChildren(category) {
  return Boolean(category?.children?.length)
}

function canPractice(category) {
  return !hasChildren(category) && category?.status === 'ENABLED'
}

function progressKey(categoryId) {
  return String(categoryId || 'all')
}

function hasProgress(category) {
  return progressByCategory.value.has(progressKey(category?.id))
}

function statusText(category) {
  if (hasChildren(category)) return `${category.children.length} 个子分类`
  return category.status === 'ENABLED' ? '可开始练习' : '已禁用'
}

function toggleNode(node) {
  if (node.expanded) node.collapse()
  else node.expand()
}

function handleNodeClick(node, category) {
  if (hasChildren(category)) {
    toggleNode(node)
    return
  }
  if (canPractice(category)) startPractice(category, hasProgress(category))
}

async function load() {
  loading.value = true
  try {
    const [tree, progressList] = await Promise.all([
      api.categoryTree(),
      auth.user?.id ? api.practiceProgressList(auth.user.id, { mode: 'PRACTICE' }) : Promise.resolve([]),
    ])
    categories.value = tree
    progressByCategory.value = new Map(
      (Array.isArray(progressList) ? progressList : [])
        .filter((progress) => progress?.categoryId)
        .map((progress) => [progressKey(progress.categoryId), progress])
    )
  } catch (err) {
    ElMessage.error(err.message || '加载分类失败')
  } finally {
    loading.value = false
  }
}

function startPractice(category, resume = false) {
  router.push({
    path: '/answer/practice',
    query: {
      ...(category ? { categoryId: category.id, title: category.name } : {}),
      ...(resume ? { resume: '1' } : {}),
    },
  })
}

onMounted(load)
</script>

<template>
  <main class="mobile-shell">
    <div class="mobile-page">
      <header class="answer-header">
        <div>
          <div class="answer-header-title">首页</div>
          <div class="muted" style="font-size: 12px">
            欢迎您，{{ auth.user?.realName || auth.user?.username || '答题练习' }}
          </div>
        </div>
      </header>

      <section class="answer-content with-tabbar">
        <div class="answer-hero">
          <h1>开始练习</h1>
          <p>选择末级题库开始练习，文件夹分类可点击展开。</p>
        </div>

        <div class="answer-grid">
          <button class="answer-action" type="button" @click="startPractice(null)">
            <span class="answer-action-icon">
              <el-icon><Tickets /></el-icon>
            </span>
            <span>
              <strong>全部题库</strong>
              <span>从所有已启用题目中练习</span>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <h3 class="answer-section-heading">题库</h3>
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else class="category-tree-card">
          <el-tree
            v-if="categories.length"
            class="category-folder-tree"
            :data="categories"
            :props="treeProps"
            node-key="id"
            :indent="18"
            :expand-on-click-node="false"
            :highlight-current="false"
          >
            <template #default="{ node, data }">
              <div
                :class="['category-tree-node', hasChildren(data) ? 'folder' : 'leaf']"
                role="button"
                tabindex="0"
                @click.stop="handleNodeClick(node, data)"
                @keydown.enter.prevent="handleNodeClick(node, data)"
              >
                <span class="category-tree-icon">
                  <el-icon>
                    <FolderOpened v-if="hasChildren(data) && node.expanded" />
                    <Folder v-else-if="hasChildren(data)" />
                    <Document v-else />
                  </el-icon>
                </span>
                <span class="category-tree-main">
                  <strong>{{ data.name }}</strong>
                  <!-- <span>{{ statusText(data) }}</span> -->
                </span>
                <button
                  v-if="canPractice(data)"
                  class="category-practice-button"
                  :class="{ 'is-continue': hasProgress(data) }"
                  type="button"
                  @click.stop="startPractice(data, hasProgress(data))"
                >
                  {{ hasProgress(data) ? '继续练习' : '练习' }}
                </button>
                <el-icon v-else-if="hasChildren(data)" :class="['category-tree-arrow', node.expanded ? 'open' : '']">
                  <ArrowRight />
                </el-icon>
              </div>
            </template>
          </el-tree>
          <el-empty v-else description="暂无分类" />
        </div>
      </section>

      <AnswerTabBar />
    </div>
  </main>
</template>
