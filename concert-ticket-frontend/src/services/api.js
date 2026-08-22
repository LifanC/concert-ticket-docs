import axios from 'axios'
import {
  clearCookie,
  toFindCookie
} from '@/components/componentsJs/cookie'

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

function setupAuthInterceptor(client) {
  client.interceptors.response.use(
    response => response,

    error => {
      if (error.response?.status === 401) {
        clearCookie('accessToken')

        if (router.currentRoute.value.name !== 'User') {
          router.push({
            name: 'User',
            query: {
              isLoggedIn: false,
              redirect: router.currentRoute.value.fullPath
            }
          })
        }
      }

      return Promise.reject(error)
    }
  )

  return client
}

export const activityApi = createApi('activity')
export const adminApi = setupAuthInterceptor(createApi('admin'))
export const bookingApi = setupAuthInterceptor(createApi('booking'))
export const loginApi = createApi('login')
