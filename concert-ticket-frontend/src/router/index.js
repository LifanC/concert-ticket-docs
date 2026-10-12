import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isAdmin, syncAuth } from '@/services/auth'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Activity',
            component: () => import('@/components/Activity.vue')
        },
        {
            path: '/booking',
            name: 'Booking',
            meta: { requiresAuth: true },
            component: () => import('@/components/Booking.vue')
        },
        {
            path: '/user',
            name: 'User',
            component: () => import('@/components/User.vue')
        },
        {
            path: '/admin',
            name: 'Admin',
            meta: {
                requiresAuth: true,
                requiresAdmin: true
            },
            component: () => import('@/components/Admin.vue')
        }
    ]
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth && !to.meta.requiresAdmin) {
    return true
  }

  const loggedIn = await restoreSession()

  if (!loggedIn) {
    return {
      name: 'User',
      query: { redirect: to.fullPath }
    }
  }

  if (to.meta.requiresAdmin && !isAdmin.value) {
    ElMessage.error('無權限')
    return { name: 'Activity' }
  }

  return true
})

router.onError((error, to) => {
    console.error('[Router] 載入失敗:', to.fullPath, error)
})

router.afterEach((to, from, failure) => {
    console.log('[Router] 導航完成:', to.fullPath, failure)
})

export default router