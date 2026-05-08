<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Download, Edit, Plus, Refresh, SwitchButton, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, downloadBlob } from '@/services/api'

const users = ref([])
const selectedId = ref(null)
const loading = ref(false)
const saving = ref(false)
const downloading = ref(false)
const importVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importResult = ref(null)
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

async function downloadTemplate() {
  downloading.value = true
  try {
    const blob = await api.userImportTemplate()
    downloadBlob(blob, 'user-import-template.xlsx')
  } catch (err) {
    ElMessage.error(err.message || '模板下载失败')
  } finally {
    downloading.value = false
  }
}

function beforeImportUpload(rawFile) {
  importFile.value = rawFile
  importResult.value = null
  return false
}

function handleImportChange(uploadFile) {
  importFile.value = uploadFile.raw || uploadFile
  importResult.value = null
}

function removeImportFile() {
  importFile.value = null
}

function openImport() {
  importVisible.value = true
  importFile.value = null
  importResult.value = null
}

async function uploadImport() {
  if (!importFile.value) {
    ElMessage.warning('请选择 Excel 文件')
    return
  }
  importing.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    importResult.value = await api.importUsers(formData)
    if (importResult.value.failCount) {
      ElMessage.warning('导入完成，部分用户失败')
    } else {
      ElMessage.success('用户导入完成')
    }
    await load()
  } catch (err) {
    ElMessage.error(err.message || '导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理员统一创建用户账号，并设置初始密码。</p>
      </div>
      <div class="toolbar-inline">
        <el-button :icon="Download" :loading="downloading" @click="downloadTemplate">下载模板</el-button>
        <el-button :icon="UploadFilled" @click="openImport">Excel 导入</el-button>
        <el-button type="primary" :icon="Plus" @click="resetForm">新建用户</el-button>
      </div>
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
              <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'">
                {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
                {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
              </el-tag>
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

    <el-dialog v-model="importVisible" title="Excel 导入用户" width="620px">
      <div class="file-box">
        <el-alert type="info" :closable="false" show-icon>
          请先下载模板填写用户信息。账号必填，初始密码为空时默认 123456。
        </el-alert>

        <el-upload
          drag
          action="#"
          accept=".xlsx,.xls"
          :auto-upload="false"
          :limit="1"
          :before-upload="beforeImportUpload"
          :on-change="handleImportChange"
          :on-remove="removeImportFile"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽 Excel 到这里，或 <em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 .xlsx / .xls，按模板第一张工作表导入。</div>
          </template>
        </el-upload>

        <el-alert v-if="importFile" type="success" :closable="false" show-icon>
          当前文件：{{ importFile.name }}
        </el-alert>

        <el-card v-if="importResult" shadow="never">
          <div class="stat-grid">
            <el-statistic title="总行数" :value="importResult.totalCount" />
            <el-statistic title="成功" :value="importResult.successCount" />
            <el-statistic title="失败" :value="importResult.failCount" />
          </div>

          <el-table
            v-if="importResult.errors?.length"
            class="card-gap"
            :data="importResult.errors"
            size="small"
            max-height="220"
          >
            <el-table-column prop="rowNo" label="行号" width="90" />
            <el-table-column prop="message" label="失败原因" min-width="260" />
          </el-table>
        </el-card>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="uploadImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>
