<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ArrowRight,
  Delete as DeleteIcon,
  Document,
  Edit,
  Folder,
  FolderOpened,
  Plus,
  Refresh,
  SwitchButton,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/services/api'

const categories = ref([])
const selectedId = ref(null)
const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const treeProps = {
  children: 'children',
  label: 'name',
}

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
  return nodes.flatMap((node) => [node, ...flattenTree(node.children || [])])
}

function hasChildren(category) {
  return Boolean(category?.children?.length)
}

function canCreateChild(category) {
  return Number(category?.level || 0) < 3
}

function statusText(status) {
  return status === 'ENABLED' ? '启用' : '禁用'
}

function statusType(status) {
  return status === 'ENABLED' ? 'success' : 'danger'
}

function categoryLabel(category) {
  return `${'　'.repeat(category.level - 1)}${category.name}`
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
  edit(category)
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

function createChild(category) {
  selectedId.value = null
  formRef.value?.clearValidate()
  Object.assign(form, {
    parentId: category.id,
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

async function remove(category) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类 ${category.name}？删除前请确保该分类下没有子分类和题目。`,
      '删除分类',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
    await api.deleteCategory(category.id)
    ElMessage.success('分类已删除')
    if (selectedId.value === category.id) resetForm()
    await load()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-list-page">
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
            <strong>分类树</strong>
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
          </div>
        </template>

        <div class="admin-category-tree-card">
          <el-tree
            v-if="treeNodes.length"
            class="category-folder-tree admin-category-tree"
            :data="treeNodes"
            :props="treeProps"
            node-key="id"
            default-expand-all
            :indent="22"
            :expand-on-click-node="false"
            :highlight-current="false"
          >
            <template #default="{ node, data }">
              <div
                :class="['category-tree-node admin-category-node', hasChildren(data) ? 'folder' : 'leaf']"
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

                <span class="admin-category-main">
                  <span class="admin-category-title">
                    <strong>{{ data.name }}</strong>
                    <el-tag size="small" :type="statusType(data.status)">{{ statusText(data.status) }}</el-tag>
                  </span>
                  <span>
                    L{{ data.level }} · 排序 {{ data.sortNo || 0 }}
                    <template v-if="hasChildren(data)"> · {{ data.children.length }} 个子分类</template>
                  </span>
                </span>

                <div class="admin-category-actions">
                  <el-button
                    v-if="canCreateChild(data)"
                    size="small"
                    :icon="Plus"
                    @click.stop="createChild(data)"
                  >
                    新增下级
                  </el-button>
                  <el-button size="small" :icon="Edit" @click.stop="edit(data)">编辑</el-button>
                  <el-button size="small" type="danger" plain :icon="SwitchButton" @click.stop="disable(data)">
                    禁用
                  </el-button>
                  <el-button size="small" type="danger" :icon="DeleteIcon" @click.stop="remove(data)">
                    删除
                  </el-button>
                  <el-icon v-if="hasChildren(data)" :class="['category-tree-arrow', node.expanded ? 'open' : '']">
                    <ArrowRight />
                  </el-icon>
                </div>
              </div>
            </template>
          </el-tree>
          <el-empty v-else description="暂无分类" />
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <strong>{{ selectedCategory ? '编辑分类' : '新建分类' }}</strong>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="上级分类">
            <el-select v-model="form.parentId" clearable placeholder="留空则创建一级分类" style="width: 100%">
              <el-option
                v-for="item in parentOptions"
                :key="item.id"
                :label="categoryLabel(item)"
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
