<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'

const categories = ref([])
const selectedId = ref(null)
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
const parentOptions = computed(() => categories.value.filter((item) => item.level < 3 && item.id !== selectedId.value))

const orderedCategories = computed(() =>
  [...categories.value].sort((left, right) => {
    if (left.level !== right.level) return left.level - right.level
    if ((left.parentId || 0) !== (right.parentId || 0)) return (left.parentId || 0) - (right.parentId || 0)
    return left.sortNo - right.sortNo || left.id - right.id
  }),
)

function indent(category) {
  return `${(category.level - 1) * 24}px`
}

function parentName(category) {
  if (!category.parentId) return '-'
  return categories.value.find((item) => item.id === category.parentId)?.name || '-'
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
            <div v-for="category in orderedCategories" :key="category.id" class="tree-row">
              <div class="tree-row-main" :style="{ paddingLeft: indent(category) }">
                <span class="badge">L{{ category.level }}</span>
                <strong>{{ category.name }}</strong>
                <span class="muted">上级：{{ parentName(category) }}</span>
                <span :class="['badge', category.status === 'ENABLED' ? 'badge-success' : 'badge-danger']">
                  {{ category.status }}
                </span>
              </div>
              <div class="table-actions">
                <button class="btn" type="button" @click="edit(category)">编辑</button>
                <button class="btn btn-danger" type="button" @click="disable(category)">禁用</button>
              </div>
            </div>
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
