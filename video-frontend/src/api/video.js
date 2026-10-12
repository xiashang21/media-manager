import axios from 'axios'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10 * 60 * 1000,
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (!body || body.code !== 200) {
      return Promise.reject(new Error(body?.message || '请求失败'))
    }
    return body.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)

export const fetchVideoPage = (params) => http.get('/api/video/page', { params })
export const fetchVideoDetail = (id) => http.get(`/api/video/${id}`)
export const removeVideo = (id) => http.delete(`/api/video/${id}`)
export const increasePlay = (id) => http.post(`/api/video/${id}/play`)
export const increaseLike = (id) => http.post(`/api/video/${id}/like`)
export const fetchCategories = () => http.get('/api/video/categories')
export const fetchHotVideos = (limit = 6) => http.get('/api/video/hot', { params: { limit } })

export const uploadVideo = (formData, onProgress) => http.post('/api/video/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' },
  onUploadProgress: onProgress,
})

export const streamUrl = (url) => {
  if (/^https?:\/\//i.test(url)) return url
  return `${API_BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}
