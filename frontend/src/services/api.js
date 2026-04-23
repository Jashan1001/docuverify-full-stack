import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })
        localStorage.setItem('accessToken', data.data.accessToken)
        localStorage.setItem('refreshToken', data.data.refreshToken)
        original.headers.Authorization = `Bearer ${data.data.accessToken}`
        return api(original)
      } catch {
        localStorage.clear()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
}

export const documentApi = {
  upload: (formData) => api.post('/documents', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  getMyDocuments: (page = 0, size = 10) => api.get(`/documents/my?page=${page}&size=${size}`),
  getById: (id) => api.get(`/documents/${id}`),
  submitForReview: (id) => api.patch(`/documents/${id}/submit`),
  delete: (id) => api.delete(`/documents/${id}`),
  getPending: (page = 0, size = 10) => api.get(`/documents/pending?page=${page}&size=${size}`),
  getInstitutionDocuments: (page = 0, size = 10, status = '') =>
    api.get(`/documents/institution?page=${page}&size=${size}${status ? '&status=' + status : ''}`),
}

export const verificationApi = {
  approve: (data) => api.post('/verification/approve', data),
  reject: (data) => api.post('/verification/reject', data),
  getLogs: (documentId) => api.get(`/verification/logs/${documentId}`),
}

export const statsApi = {
  user: () => api.get('/stats/user'),
  verifier: () => api.get('/stats/verifier'),
  institution: () => api.get('/stats/institution'),
  admin: () => api.get('/stats/admin'),
}

export const adminApi = {
  getInstitutions: (page = 0, size = 10) => api.get(`/admin/institutions?page=${page}&size=${size}`),
  createInstitution: (data) => api.post('/admin/institutions', data),
  toggleInstitution: (id) => api.patch(`/admin/institutions/${id}/toggle`),
  getUsers: (page = 0, size = 10, role = '') =>
    api.get(`/admin/users?page=${page}&size=${size}${role ? '&role=' + role : ''}`),
  toggleUser: (id) => api.patch(`/admin/users/${id}/toggle`),
  assignRole: (data) => api.post('/admin/users/assign-role', data),
  getInstitutionMembers: (page = 0, size = 10) =>
    api.get(`/admin/institution/members?page=${page}&size=${size}`),
}

export const publicApi = {
  verify: (token) => axios.get(`/api/public/verify/${token}`),
}

export default api
