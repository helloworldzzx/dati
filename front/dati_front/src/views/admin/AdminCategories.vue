<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import CategoryTree from '@/components/admin/CategoryTree.vue'
import { api } from '@/services/api'

const categories = ref([])
const selectedId = ref(null)
const expandedIds = ref(new Set())
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({
  parentId: '',
  name: '',
  sortNo: 0,
  status: 'ENABLED',
})

const selectedCategory = computed(() => categories.value.find((item) => item.id === selectedId.value))
const parentOptions = computed(() =>
  flattenTree(treeNodes.value).filter((item) => item.level < 3 && item.id !== selectedId.value),
)

const treeNodes = computed(() => buildTree(categories.value))

function buildTree(items) {
  const nodeMap = new Map()
  const roots = []

  items.forEach((item) => {
    nodeMap.set(item.id, { ...item, children: [] })
  })

  items.forEach((item) => {
    const node = nodeMap.get(item.id)
    const parent = item.parentId ? nodeMap.get(item.parentId) : null
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  sortTree(roots)
  return roots
}

function sortTree(nodes) {
  nodes.sort((left, right) => (left.sortNo || 0) - (right.sortNo || 0) || left.id - right.id)
  nodes.forEach((node) => sortTree(node.children))
}

function flattenTree(nodes) {
  return nodes.flatMap((node) => [node, ...flattenTree(node.children)])
}

function syncExpandedTree(nodes) {
  const next = new Set(expandedIds.value)
  flattenTree(nodes)
    .filter((node) => node.children.length > 0)
    .forEach((node) => next.add(node.id))
  expandedIds.value = next
}

function toggleNode(node) {
  const next = new Set(expandedIds.value)
  if (next.has(node.id)) {
    next.delete(node.id)
  } else {
    next.add(node.id)
  }
  expandedIds.value = next
}

function resetForm() {
  selectedId.value = null
  Object.assign(form, {
    parentId: '',
    name: '',
    sortNo: 0,
    status: 'ENABLED',
  })
}

function edit(category) {
  selectedId.value = category.id
  Object.assign(form, {
    parentId: category.parentId || '',
    name: category.name || '',
    sortNo: category.sortNo || 0,
    status: category.status || 'ENABLED',
  })
}

function buildPayload() {
  const parentId = form.parentId ? Number(form.parentId) : null
  const parent = parentId ? categories.value.find((item) => item.id === parentId) : null
  return {
    parentId,
    name: form.name,
    level: parent ? parent.level + 1 : 1,
    sortNo: Number(form.sortNo || 0),
    status: form.status,
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await api.categories()
    syncExpandedTree(treeNodes.value)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const payload = buildPayload()
    if (selectedId.value) {
      await api.updateCategory(selectedId.value, payload)
      success.value = '分类已更新'
    } else {
      await api.createCategory(payload)
      success.value = '分类已创建'
      resetForm()
    }
    await load()
  } catch (err) {
    error.value = err.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function disable(category) {
  if (!window.confirm(`确认禁用分类 ${category.name}？`)) return
  error.value = ''
  success.value = ''
  try {
    await api.disableCategory(category.id)
    success.value = '分类已禁用'
    await load()
  } catch (err) {
    error.value = err.message || '操作失败'
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">题库分类</h2>
        <p class="page-desc">维护 1-3 级题库目录。</p>
      </div>
      <button class="btn" type="button" @click="resetForm">新建分类</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>

    <div class="grid grid-2">
      <section class="card">
        <div class="card-head">
          <h3 class="card-title">分类列表</h3>
          <button class="btn" type="button" :disabled="loading" @click="load">刷新</button>
        </div>
        <div class="card-body">
          <div class="tree-list">
            <CategoryTree
              v-if="treeNodes.length"
              :nodes="treeNodes"
              :expanded-ids="expandedIds"
              @toggle="toggleNode"
              @edit="edit"
              @disable="disable"
            />
            <p v-if="!categories.length" class="muted">{{ loading ? '加载中' : '暂无分类' }}</p>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <h3 class="card-title">{{ selectedCategory ? '编辑分类' : '新建分类' }}</h3>
        </div>
        <div class="card-body">
          <form class="form" @submit.prevent="save">
            <div class="form-row">
              <label>上级分类</label>
              <select v-model="form.parentId" class="select">
                <option value="">无，上设为一级分类</option>
                <option v-for="item in parentOptions" :key="item.id" :value="item.id">
                  {{ '　'.repeat(item.level - 1) }}{{ item.name }}
                </option>
              </select>
            </div>

            <div class="form-row">
              <label>分类名称</label>
              <input v-model.trim="form.name" class="input" required />
            </div>

            <div class="form-grid">
              <div class="form-row">
                <label>排序</label>
                <input v-model.number="form.sortNo" class="input" type="number" />
              </div>
              <div class="form-row">
                <label>状态</label>
                <select v-model="form.status" class="select">
                  <option value="ENABLED">启用</option>
                  <option value="DISABLED">禁用</option>
                </select>
              </div>
            </div>

            <button class="btn btn-primary" type="submit" :disabled="saving">
              {{ saving ? '保存中' : '保存' }}
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>
