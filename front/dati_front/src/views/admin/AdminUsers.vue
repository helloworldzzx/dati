<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/services/api'

const users = ref([])
const selectedId = ref(null)
const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  username: '',
  phone: '',
  password: '',
  realName: '',
  role: 'USER',
  status: 'ENABLED',
  mustChangePassword: true,
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    {
      validator: (_, value, callback) => {
        if (!selectedId.value && !value) callback(new Error('请输入初始密码'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

const selectedUser = computed(() => users.value.find((item) => item.id === selectedId.value))

function resetForm() {
  selectedId.value = null
  formRef.value?.clearValidate()
  Object.assign(form, {
    username: '',
    phone: '',
    password: '',
    realName: '',
    role: 'USER',
    status: 'ENABLED',
    mustChangePassword: true,
  })
}

function edit(user) {
  selectedId.value = user.id
  formRef.value?.clearValidate()
  Object.assign(form, {
    username: user.username || '',
    phone: user.phone || '',
    password: '',
    realName: user.realName || '',
    role: user.role || 'USER',
    status: user.status || 'ENABLED',
    mustChangePassword: Boolean(user.mustChangePassword),
  })
}

async function load() {
  loading.value = true
  try {
    users.value = await api.users()
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
    const payload = { ...form }
    if (!payload.password) delete payload.password

    if (selectedId.value) {
      await api.updateUser(selectedId.value, payload)
      ElMessage.success('用户已更新')
    } else {
      await api.createUser(payload)
      ElMessage.success('用户已创建')
      resetForm()
    }
    await load()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function disable(user) {
  try {
    await ElMessageBox.confirm(`确认禁用账号 ${user.username}？`, '禁用用户', { type: 'warning' })
    await api.disableUser(user.id)
    ElMessage.success('用户已禁用')
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
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理员统一创建用户账号并设置初始密码。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="resetForm">新建用户</el-button>
    </div>

    <div class="content-grid">
      <el-card shadow="never">
        <template #header>
          <div class="toolbar-inline" style="justify-content: space-between">
            <strong>用户列表</strong>
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
          </div>
        </template>

        <el-table v-loading="loading" :data="users" stripe height="620" empty-text="暂无用户">
          <el-table-column prop="username" label="账号" min-width="130" />
          <el-table-column label="姓名" min-width="120">
            <template #default="{ row }">{{ row.realName || '-' }}</template>
          </el-table-column>
          <el-table-column label="手机号" min-width="140">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="首次登录" width="110">
            <template #default="{ row }">{{ row.mustChangePassword ? '需完善' : '已完成' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Edit" @click="edit(row)">编辑</el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :icon="SwitchButton"
                :disabled="row.status === 'DISABLED'"
                @click="disable(row)"
              >
                禁用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <strong>{{ selectedUser ? '编辑用户' : '新建用户' }}</strong>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="账号" prop="username">
            <el-input v-model.trim="form.username" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model.trim="form.realName" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model.trim="form.phone" />
          </el-form-item>
          <el-form-item :label="selectedUser ? '新密码' : '初始密码'" prop="password">
            <el-input v-model="form.password" show-password type="password" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="form.role" style="width: 100%">
              <el-option label="用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="form.mustChangePassword">首次登录需绑定手机号并修改密码</el-checkbox>
          </el-form-item>
          <el-button type="primary" :loading="saving" style="width: 100%" @click="save">保存</el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
