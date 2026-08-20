import axios from 'axios'
import { toFindCookie } from '@/components/componentsJs/cookie'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '')

function createApi(resourcePath) {
  const client = axios.create({
    baseURL: `${apiBaseUrl}/v1/${resourcePath}`,
    timeout: 10000,
    withCredentials: true,
  })

  client.interceptors.request.use((config) => {
    const accessToken = toFindCookie('accessToken')
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  })

  return client
}

export const activityApi = createApi('activity')
export const adminApi = createApi('admin')
export const bookingApi = createApi('booking')
export const loginApi = createApi('login')
