<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'

const users = ref([])
const selectedId = ref(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({
  username: '',
  phone: '',
  password: '',
  realName: '',
  role: 'USER',
  status: 'ENABLED',
  mustChangePassword: true,
})

const selectedUser = computed(() => users.value.find((item) => item.id === selectedId.value))

function resetForm() {
  selectedId.value = null
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
  error.value = ''
  try {
    users.value = await api.users()
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
    const payload = { ...form }
    if (!payload.password) {
      delete payload.password
    }
    if (selectedId.value) {
      await api.updateUser(selectedId.value, payload)
      success.value = '用户已更新'
    } else {
      await api.createUser(payload)
      success.value = '用户已创建'
      resetForm()
    }
    await load()
  } catch (err) {
    error.value = err.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function disable(user) {
  if (!window.confirm(`确认禁用账号 ${user.username}？`)) return
  error.value = ''
  success.value = ''
  try {
    await api.disableUser(user.id)
    success.value = '用户已禁用'
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
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理员统一创建用户账号并设置初始密码。</p>
      </div>
      <button class="btn" type="button" @click="resetForm">新建用户</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>

    <div class="grid grid-2">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>账号</th>
              <th>姓名</th>
              <th>手机号</th>
              <th>角色</th>
              <th>状态</th>
              <th>首次登录</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.username }}</td>
              <td>{{ user.realName || '-' }}</td>
              <td>{{ user.phone || '-' }}</td>
              <td>
                <span class="badge">{{ user.role }}</span>
              </td>
              <td>
                <span :class="['badge', user.status === 'ENABLED' ? 'badge-success' : 'badge-danger']">
                  {{ user.status }}
                </span>
              </td>
              <td>{{ user.mustChangePassword ? '需完善' : '已完成' }}</td>
              <td>
                <div class="table-actions">
                  <button class="btn" type="button" @click="edit(user)">编辑</button>
                  <button
                    class="btn btn-danger"
                    type="button"
                    :disabled="user.status === 'DISABLED'"
                    @click="disable(user)"
                  >
                    禁用
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!users.length">
              <td colspan="7" class="muted">{{ loading ? '加载中' : '暂无用户' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <section class="card">
        <div class="card-head">
          <h3 class="card-title">{{ selectedUser ? '编辑用户' : '新建用户' }}</h3>
        </div>
        <div class="card-body">
          <form class="form" @submit.prevent="save">
            <div class="form-grid">
              <div class="form-row">
                <label>账号</label>
                <input v-model.trim="form.username" class="input" required />
              </div>
              <div class="form-row">
                <label>姓名</label>
                <input v-model.trim="form.realName" class="input" />
              </div>
            </div>

            <div class="form-row">
              <label>手机号</label>
              <input v-model.trim="form.phone" class="input" />
            </div>

            <div class="form-row">
              <label>{{ selectedUser ? '新密码' : '初始密码' }}</label>
              <input v-model="form.password" class="input" type="password" :required="!selectedUser" />
            </div>

            <div class="form-grid">
              <div class="form-row">
                <label>角色</label>
                <select v-model="form.role" class="select">
                  <option value="USER">用户</option>
                  <option value="ADMIN">管理员</option>
                </select>
              </div>
              <div class="form-row">
                <label>状态</label>
                <select v-model="form.status" class="select">
                  <option value="ENABLED">启用</option>
                  <option value="DISABLED">禁用</option>
                </select>
              </div>
            </div>

            <label class="tree-row" style="justify-content: flex-start; gap: 8px">
              <input v-model="form.mustChangePassword" type="checkbox" />
              首次登录需绑定手机号并修改密码
            </label>

            <button class="btn btn-primary" type="submit" :disabled="saving">
              {{ saving ? '保存中' : '保存' }}
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>
