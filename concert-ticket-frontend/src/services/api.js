import router from '@/router'
import axios from 'axios'

import {
  addCookie,
  clearCookie,
  toFindCookie
} from '@/components/componentsJs/cookie'

const apiBaseUrl = (
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
).replace(/\/$/, '')

// 專門 refresh token，用「乾淨」axios，不掛 auth interceptor
const refreshClient = axios.create({
  baseURL: `${apiBaseUrl}/v1/login`,
  timeout: 10000,
  withCredentials: true,
})

function createApi(resourcePath) {
  const client = axios.create({
    baseURL: `${apiBaseUrl}/v1/${resourcePath}`,
    timeout: 10000,
    withCredentials: true,
  })

  client.interceptors.request.use((config) => {
    const accessToken = toFindCookie('accessToken')

    if (accessToken) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    return config
  })

  return client
}

function setupAuthInterceptor(client) {
  client.interceptors.response.use(
    response => response,

    async error => {
      const status = error.response?.status
      const originalRequest = error.config

      // access token 過期
      if (status === 401 && originalRequest && !originalRequest._retry) {
        originalRequest._retry = true

        try {
          // 1. 用 refresh token 重新取得 access token
          const response = await refreshClient.post('/validate')

          const newToken = response.data?.data?.[0]?.accessToken

          if (!newToken) {
            throw new Error('Refresh API 沒有回傳 accessToken')
          }

          // 2. 儲存新 access token
          addCookie('accessToken', newToken)

          // 3. 原本 request 改成新 token
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${newToken}`

          // 4. 重送原本 request
          return client(originalRequest)

        } catch (refreshError) {
          console.error('refresh token 失敗:', refreshError)

          // refresh token 也失效 → 登出
          clearCookie('accessName')
          clearCookie('accessToken')

          const currentRoute = router.currentRoute.value

          if (currentRoute.name !== 'User') {
            await router.push({
              name: 'User',
              query: {
                isLoggedIn: 'false',
                redirect: currentRoute.fullPath
              }
            })
          }

          return Promise.reject(refreshError)
        }
      }

      const currentRoute = router.currentRoute.value

      // 如果你的後端確定 400 代表需要重新登入，可以保留
      if (status === 400) {
        ElMessage({
          type: 'info',
          message: '重新登入',
        })
        
        if (currentRoute.name !== 'User') {
          clearCookie('accessName')
          clearCookie('accessToken')

          await router.push({
            name: 'User',
            query: {
              isLoggedIn: 'false',
              redirect: currentRoute.fullPath
            }
          })
        }
      }

      if (status === 403) {
        ElMessage({
          type: 'error',
          message: `${'無權限'}`,
        })
        await router.push({
          name: 'User',
          query: {
            isLoggedIn: 'true',
            redirect: currentRoute.fullPath
          }
        })
      }

      return Promise.reject(error)
    }
  )

  return client
}

export const activityApi = createApi('activity')
export const adminApi = setupAuthInterceptor(createApi('admin'))
export const bookingApi = setupAuthInterceptor(createApi('booking'))
export const loginApi = setupAuthInterceptor(createApi('login'))