import {createRouter, createWebHistory} from "vue-router";
import { toFindCookie } from '@/components/componentsJs/cookie.js'
import { isAdmin, syncAuth } from '@/services/auth'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Activity',
            component: () => import("@/components/Activity.vue")
        },
        {
            path: '/booking',
            name: 'Booking',
            meta: { requiresAuth: true },
            component: () => import("@/components/Booking.vue")
        },
        {
            path: '/user',
            name: 'User',
            component: () => import("@/components/User.vue")
        },
        {
            path: '/admin',
            name: 'Admin',
            meta: { requiresAuth: true, requiresAdmin: true },
            component: () => import("@/components/Admin.vue")
        },
    ]
})

router.beforeEach((to) => {
    syncAuth()
    if (to.meta.requiresAuth && !toFindCookie('accessToken')) {
        return { name: 'User', query: { redirect: to.fullPath } }
    }
    if (to.meta.requiresAdmin && !isAdmin.value) {
        ElMessage.error('無權限')
        return { name: 'Activity' }
    }
})

export default router
