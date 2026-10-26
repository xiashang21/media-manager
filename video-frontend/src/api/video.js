import http, { API_BASE_URL, resolveMediaUrl } from './http'

export { API_BASE_URL }

export const fetchVideoPage = (params) => http.get('/api/video/page', { params })
export const fetchVideoDetail = (id) => http.get(`/api/video/${id}`)
export const removeVideo = (id) => http.delete(`/api/video/${id}`)
export const increasePlay = (id) => http.post(`/api/video/${id}/play`)
export const increaseLike = (id) => http.post(`/api/video/${id}/like`)
export const fetchCategories = () => http.get('/api/video/categories')
export const fetchHotVideos = (limit = 6) => http.get('/api/video/hot', { params: { limit } })

export const uploadVideo = (formData, onProgress) => http.post('/api/video/upload', formData, {
  onUploadProgress: onProgress,
})

export const uploadVideoCover = (id, formData, onProgress) => http.post(`/api/video/${id}/cover`, formData, {
  onUploadProgress: onProgress,
})

export const streamUrl = resolveMediaUrl
