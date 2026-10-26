import axios from 'axios'
import { clearSession, getToken } from '../utils/session'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10 * 60 * 1000,
})

const isAuthEntryRequest = (config) => {
  const url = String(config?.url || '')
  return url.includes('/api/auth/login') || url.includes('/api/auth/register')
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (!body || body.code !== 200) {
      const error = new Error(body?.message || '请求失败')
      error.status = body?.code
      return Promise.reject(error)
    }
    return body.data
  },
  (error) => {
    const status = error.response?.status || error.response?.data?.code
    if (status === 401 && !isAuthEntryRequest(error.config)) {
      clearSession()
      window.dispatchEvent(new CustomEvent('videohub:unauthorized'))
    }
    const wrapped = new Error(error.response?.data?.message || error.message || '网络请求失败')
    wrapped.status = status
    return Promise.reject(wrapped)
  },
)

export const resolveMediaUrl = (url) => {
  if (!url) return ''
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  return `${API_BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}

export default http
