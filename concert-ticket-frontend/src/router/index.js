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

    try {
        await syncAuth()
    } catch (error) {
        console.error('登入狀態同步失敗', error)
        return {
            name: 'User',
            query: { redirect: to.fullPath }
        }
    }

    // 這裡需要根據 syncAuth() 實際維護的登入狀態判斷
    // 不應依賴讀取 HttpOnly refreshToken

    if (to.meta.requiresAdmin && !isAdmin.value) {
        ElMessage.error('無權限')
        return { name: 'Activity' }
    }

    return true
})

export default router