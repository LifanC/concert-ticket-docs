import { ref, readonly } from 'vue'
import { toFindCookie } from '@/components/componentsJs/cookie.js'

const admin = ref(false)
export const isAdmin = readonly(admin)

// Decoding only controls navigation; the API verifies the token and permissions.
export function syncAuth() {
  let nextAdmin = false
  const token = toFindCookie('accessToken')
  try {
    const parts = typeof token === 'string' ? token.split('.') : []
    if (parts.length === 3) {
      const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
      const bytes = Uint8Array.from(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, '=')), c => c.charCodeAt(0))
      const claims = JSON.parse(new TextDecoder().decode(bytes))
      nextAdmin = Array.isArray(claims.authorities) && claims.authorities.includes('ADMIN_ITEM_IMPLEMENT')
    }
  } catch {
    nextAdmin = false
  }
  // Publish once, avoiding transient removal of the navigation link.
  admin.value = nextAdmin
}

export function resetAuth() {
  admin.value = false
}
