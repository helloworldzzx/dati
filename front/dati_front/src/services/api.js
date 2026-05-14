const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const AUTH_KEYS = {
  admin: {
    token: 'dati_admin_auth_token',
    user: 'dati_admin_auth_user',
  },
  answer: {
    token: 'dati_answer_auth_token',
    user: 'dati_answer_auth_user',
  },
}
let activeAuthScope = inferAuthScope()

export function inferAuthScope() {
  return window.location.pathname.startsWith('/admin') ? 'admin' : 'answer'
}

export function setAuthScope(scope) {
  activeAuthScope = scope === 'admin' ? 'admin' : 'answer'
}

export function scopeForUser(user) {
  return user?.role === 'ADMIN' ? 'admin' : 'answer'
}

function keysFor(scope = activeAuthScope) {
  return AUTH_KEYS[scope === 'admin' ? 'admin' : 'answer']
}

export function getToken(scope = activeAuthScope) {
  return localStorage.getItem(keysFor(scope).token)
}

export function getStoredUser(scope = activeAuthScope) {
  const value = localStorage.getItem(keysFor(scope).user)
  if (!value) return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export function saveAuth(token, user) {
  const scope = scopeForUser(user)
  setAuthScope(scope)
  localStorage.setItem(keysFor(scope).token, token)
  localStorage.setItem(keysFor(scope).user, JSON.stringify(user))
}

export function saveUser(user, scope = activeAuthScope) {
  localStorage.setItem(keysFor(scope).user, JSON.stringify(user))
}

export function clearAuth(scope = activeAuthScope) {
  localStorage.removeItem(keysFor(scope).token)
  localStorage.removeItem(keysFor(scope).user)
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
  const token = getToken(options.authScope || activeAuthScope)
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
    clearAuth(options.authScope || activeAuthScope)
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

  users: (params = {}) => request(`/api/admin/users?${query(params)}`),
  createUser: (body) => request('/api/admin/users', { method: 'POST', body }),
  updateUser: (id, body) => request(`/api/admin/users/${id}`, { method: 'PUT', body }),
  disableUser: (id) => request(`/api/admin/users/${id}/disable`, { method: 'PATCH' }),
  importUsers: (formData) => request('/api/admin/users/import', { method: 'POST', body: formData }),
  userImportTemplate: () => request('/api/admin/users/import-template'),

  categories: () => request('/api/categories'),
  categoryTree: () => request('/api/categories/tree'),
  createCategory: (body) => request('/api/categories', { method: 'POST', body }),
  updateCategory: (id, body) => request(`/api/categories/${id}`, { method: 'PUT', body }),
  disableCategory: (id) => request(`/api/categories/${id}/disable`, { method: 'PATCH' }),
  deleteCategory: (id) => request(`/api/categories/${id}`, { method: 'DELETE' }),

  questions: (params = {}) => request(`/api/questions?${query(params)}`),
  questionPage: (params = {}) => request(`/api/questions/page?${query(params)}`),
  adminQuestions: (params = {}) => request(`/api/admin/questions?${query(params)}`),
  questionDetail: (id, userId) => request(`/api/questions/${id}${userId ? `?userId=${encodeURIComponent(userId)}` : ''}`),
  createQuestion: (body) => request('/api/admin/questions', { method: 'POST', body }),
  updateQuestion: (id, body) => request(`/api/admin/questions/${id}`, { method: 'PUT', body }),
  deleteQuestion: (id) => request(`/api/admin/questions/${id}`, { method: 'DELETE' }),
  deleteQuestions: (ids) => request('/api/admin/questions/batch-delete', { method: 'POST', body: { ids } }),

  rankings: (limit = 50, sort) => request(`/api/rankings?${query({ limit, sort })}`),
  adminRankings: (params = {}) => request(`/api/admin/rankings?${query(params)}`),
  startSession: (body) => request('/api/practice/sessions', { method: 'POST', body }),
  finishSession: (sessionId) => request(`/api/practice/sessions/${sessionId}/finish`, { method: 'PATCH' }),
  submitAnswer: (body) => request('/api/practice/answers', { method: 'POST', body }),
  wrongQuestions: (userId, params = {}) => request(`/api/users/${userId}/wrong-questions?${query(params)}`),
  wrongQuestionsPage: (userId, params = {}) => request(`/api/users/${userId}/wrong-questions/page?${query(params)}`),
  favoriteQuestions: (userId, params = {}) => request(`/api/users/${userId}/favorite-questions?${query(params)}`),
  favoriteQuestionsPage: (userId, params = {}) => request(`/api/users/${userId}/favorite-questions/page?${query(params)}`),
  practiceProgress: (userId, params = {}) => request(`/api/users/${userId}/practice-progress?${query(params)}`),
  savePracticeProgress: (userId, body) =>
    request(`/api/users/${userId}/practice-progress`, {
      method: 'PUT',
      body,
    }),
  updateFavorite: (userId, questionId, favorite) =>
    request(`/api/users/${userId}/questions/${questionId}/favorite`, {
      method: 'PUT',
      body: { favorite },
    }),
  importQuestions: (formData) =>
    request('/api/admin/questions/import', { method: 'POST', body: formData }),
  importTemplate: (type = 'all') => request(`/api/admin/questions/import-template?type=${encodeURIComponent(type)}`),
}
