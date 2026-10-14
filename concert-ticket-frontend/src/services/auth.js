// 閱讀導引：維護全站共用的登入與管理員顯示狀態；JWT 解碼用於畫面，權限仍由後端驗證。
import { ref, readonly } from 'vue'
import { toFindCookie } from '@/components/componentsJs/cookie.js'

const admin = ref(false)
export const isAdmin = readonly(admin)
const loggedIn = ref(false)
const name = ref('')
export const isLoggedIn = readonly(loggedIn)
export const accessName = readonly(name)

/** 讀取 cookie 並解碼 JWT，更新登入名稱與管理員顯示狀態；實際權限仍由後端驗證。 */
export function syncAuth() {
  let nextAdmin = false
  const token = toFindCookie('accessToken')
  loggedIn.value = !!token
  name.value = token ? (toFindCookie('accessName') ?? '') : ''
  try {
    const parts = typeof token === 'string' ? token.split('.') : []
    if (parts.length === 3) {
      // JWT 使用 Base64URL；還原字元與補齊長度後，以 UTF-8 解碼才能正確讀取中文欄位。
      const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
      const bytes = Uint8Array.from(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, '=')), c => c.charCodeAt(0))
      const claims = JSON.parse(new TextDecoder().decode(bytes))
      nextAdmin = Array.isArray(claims.authorities) && claims.authorities.includes('ADMIN_ITEM_IMPLEMENT')
    }
  } catch {
    nextAdmin = false
  }
  // 解碼結束後才一次更新管理員狀態，避免解析途中讓導覽連結短暫消失。
  admin.value = nextAdmin
}

/** 清空共用登入、名稱與管理員狀態，讓全站畫面立即同步登出。 */
export function resetAuth() {
  admin.value = false
  loggedIn.value = false
  name.value = ''
}
