<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CategoryTree from '@/components/admin/CategoryTree.vue'
import { api } from '@/services/api'

const categories = ref([])
const selectedId = ref(null)
const expandedIds = ref(new Set())
const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  parentId: '',
  name: '',
  sortNo: 0,
  status: 'ENABLED',
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

const selectedCategory = computed(() => categories.value.find((item) => item.id === selectedId.value))
const treeNodes = computed(() => buildTree(categories.value))
const parentOptions = computed(() =>
  flattenTree(treeNodes.value).filter((item) => item.level < 3 && item.id !== selectedId.value),
)

function buildTree(items) {
  const nodeMap = new Map()
  const roots = []
  items.forEach((item) => nodeMap.set(item.id, { ...item, children: [] }))
  items.forEach((item) => {
    const node = nodeMap.get(item.id)
    const parent = item.parentId ? nodeMap.get(item.parentId) : null
    if (parent) parent.children.push(node)
    else roots.push(node)
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
  if (next.has(node.id)) next.delete(node.id)
  else next.add(node.id)
  expandedIds.value = next
}

function resetForm() {
  selectedId.value = null
  formRef.value?.clearValidate()
  Object.assign(form, {
    parentId: '',
    name: '',
    sortNo: 0,
    status: 'ENABLED',
  })
}

function edit(category) {
  selectedId.value = category.id
  formRef.value?.clearValidate()
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
  try {
    categories.value = await api.categories()
    syncExpandedTree(treeNodes.value)
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = buildPayload()
    if (selectedId.value) {
      await api.updateCategory(selectedId.value, payload)
      ElMessage.success('分类已更新')
    } else {
      await api.createCategory(payload)
      ElMessage.success('分类已创建')
      resetForm()
    }
    await load()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function disable(category) {
  try {
    await ElMessageBox.confirm(`确认禁用分类 ${category.name}？`, '禁用分类', { type: 'warning' })
    await api.disableCategory(category.id)
    ElMessage.success('分类已禁用')
    await load()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '操作失败')
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
      <el-button type="primary" :icon="Plus" @click="resetForm">新建分类</el-button>
    </div>

    <div class="content-grid">
      <el-card shadow="never" v-loading="loading">
        <template #header>
          <div class="toolbar-inline" style="justify-content: space-between">
            <strong>分类列表</strong>
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
          </div>
        </template>

        <CategoryTree
          v-if="treeNodes.length"
          :nodes="treeNodes"
          :expanded-ids="expandedIds"
          @toggle="toggleNode"
          @edit="edit"
          @disable="disable"
        />
        <el-empty v-else description="暂无分类" />
      </el-card>

      <el-card shadow="never">
        <template #header>
          <strong>{{ selectedCategory ? '编辑分类' : '新建分类' }}</strong>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="上级分类">
            <el-select v-model="form.parentId" clearable placeholder="无，上设为一级分类" style="width: 100%">
              <el-option
                v-for="item in parentOptions"
                :key="item.id"
                :label="`${'　'.repeat(item.level - 1)}${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分类名称" prop="name">
            <el-input v-model.trim="form.name" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortNo" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-button type="primary" :loading="saving" style="width: 100%" @click="save">保存</el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
