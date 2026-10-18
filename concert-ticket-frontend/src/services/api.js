// 閱讀導引：集中建立 Axios API 客戶端，自動附上 access token；遇到 401 時更新 token 並重試一次。
import router from '@/router'
import axios from 'axios'
import { syncAuth, resetAuth } from '@/services/auth'

import {
  addCookie,
  clearCookie,
  toFindCookie
} from '@/components/componentsJs/cookie.js'

const apiBaseUrl = (
  import.meta.env.VITE_API_BASE_URL || '/api'
).replace(/\/$/, '');

/** 將圖片 API 路徑加上目前 API 基底位址；空路徑回傳空字串。 */
export function resolveActivityImageUrl(path) {
  return path ? `${apiBaseUrl}${path}` : ''
}

// 更新 token 使用獨立客戶端，避免更新請求本身再次觸發同一個 401 重試流程。
const refreshClient = axios.create({
  baseURL: `${apiBaseUrl}/v1/login`,
  timeout: 10000,
  withCredentials: true,
})

let refreshPromise = null

/** 多個 API 同時遇到 401 時共用一次更新，避免消耗 session 限流配額。 */
export function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = refreshClient.post('/validate').then(response => {
      const token = response.data?.data?.[0]?.accessToken
      if (!token) throw new Error('Refresh API 沒有回傳 accessToken')
      addCookie('accessToken', token)
      syncAuth()
      return token
    }).finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

/**
 * 建立 /v1/{resourcePath} 的 Axios 客戶端，設定 10 秒逾時並攜帶 cookie。
 * 每次請求才讀 access token cookie，避免客戶端保留登入或更新 token 之前的舊值。
 * @param {string} resourcePath activity、admin、booking 或 login
 * @returns {import('axios').AxiosInstance} 尚未安裝回應攔截器的客戶端
 */
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

/** 安裝回應攔截器；401 更新 token 後重試一次，失敗時清除登入狀態，403 返回活動頁。 */
function setupAuthInterceptor(client) {
  client.interceptors.response.use(
    response => response,

    async error => {
      const status = error.response?.status
      const originalRequest = error.config

      // 401 可能來自 token 過期或其他登入失效；只對尚未重試的原請求嘗試更新 token。
      if (status === 401 && originalRequest && !originalRequest._retry) {
        // 每個原始請求只自動重試一次，避免失效 token 導致無限循環。
        originalRequest._retry = true

        try {
          // 1. 用 refresh token 重新取得 access token
          const newToken = await refreshAccessToken()

          // 3. 原本 request 改成新 token
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${newToken}`

          // 4. 重送原本 request
          return client(originalRequest)

        } catch (refreshError) {
          console.error('refresh token 失敗:', refreshError)

          // 更新 token 失敗或回應沒有新 token 時，清除本地登入狀態並保留返回頁面路徑。
          clearCookie('accessName')
          clearCookie('accessToken')
          resetAuth()

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

      // 400 交由原表單顯示輸入錯誤，不清除登入狀態；其他未特別處理的錯誤也原樣拒絕。

      if (status === 403) {
        if (error.response?.data?.code === 'PAYMENT_SIMULATION_DISABLED') {
          ElMessage.error(error.response.data.message || '目前尚未開放付款')
          return Promise.reject(error)
        }
        ElMessage({
          type: 'error',
          message: `${'無權限'}`,
        })
        await router.push({
          name: 'Activity',
        })
      }

      return Promise.reject(error)
    }
  )

  return client
}

export const activityApi = setupAuthInterceptor(createApi('activity'))
export const adminApi = setupAuthInterceptor(createApi('admin'))
export const bookingApi = setupAuthInterceptor(createApi('booking'))
export const loginApi = setupAuthInterceptor(createApi('login'))
