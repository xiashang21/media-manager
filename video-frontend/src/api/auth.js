import http from './http'

export const login = (payload) => http.post('/api/auth/login', payload)
export const register = (payload) => http.post('/api/auth/register', payload)
export const fetchMe = () => http.get('/api/auth/me')
export const logout = () => http.post('/api/auth/logout')
