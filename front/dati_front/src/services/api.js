const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const TOKEN_KEY = 'dati_admin_token'
const USER_KEY = 'dati_admin_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser() {
  const value = localStorage.getItem(USER_KEY)
  if (!value) return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export function saveAuth(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function saveUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    const body = await response.json()
    if (!response.ok || body.success === false) {
      throw new Error(body.message || '请求失败')
    }
    return body.data
  }
  if (!response.ok) {
    throw new Error('请求失败')
  }
  return response.blob()
}

export async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  const token = getToken()
  const hasBody = options.body !== undefined && options.body !== null
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (hasBody && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: hasBody && !isFormData ? JSON.stringify(options.body) : options.body,
  })

  if (response.status === 401) {
    clearAuth()
    window.dispatchEvent(new CustomEvent('auth-expired'))
  }

  return parseResponse(response)
}

export function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function query(params = {}) {
  const filtered = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  return new URLSearchParams(filtered).toString()
}

export const api = {
  login: (body) => request('/api/auth/login', { method: 'POST', body }),
  me: () => request('/api/auth/me'),
  completeFirstLogin: (body) => request('/api/auth/complete-first-login', { method: 'POST', body }),
  changePassword: (body) => request('/api/auth/change-password', { method: 'POST', body }),

  users: () => request('/api/admin/users'),
  createUser: (body) => request('/api/admin/users', { method: 'POST', body }),
  updateUser: (id, body) => request(`/api/admin/users/${id}`, { method: 'PUT', body }),
  disableUser: (id) => request(`/api/admin/users/${id}/disable`, { method: 'PATCH' }),

  categories: () => request('/api/categories'),
  categoryTree: () => request('/api/categories/tree'),
  createCategory: (body) => request('/api/categories', { method: 'POST', body }),
  updateCategory: (id, body) => request(`/api/categories/${id}`, { method: 'PUT', body }),
  disableCategory: (id) => request(`/api/categories/${id}/disable`, { method: 'PATCH' }),

  questions: (params = {}) => request(`/api/questions?${query(params)}`),
  questionDetail: (id) => request(`/api/questions/${id}`),
  createQuestion: (body) => request('/api/admin/questions', { method: 'POST', body }),
  updateQuestion: (id, body) => request(`/api/admin/questions/${id}`, { method: 'PUT', body }),

  rankings: (limit = 50) => request(`/api/rankings?limit=${limit}`),
  importQuestions: (formData) =>
    request('/api/admin/questions/import', { method: 'POST', body: formData }),
  importTemplate: () => request('/api/admin/questions/import-template'),
}
