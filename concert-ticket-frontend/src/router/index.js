import {createRouter, createWebHistory} from "vue-router";
import { toFindCookie } from '@/components/componentsJs/cookie'

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
            meta: { requiresAuth: true },
            component: () => import("@/components/Admin.vue")
        },
    ]
})

router.beforeEach((to) => {
    if (to.meta.requiresAuth && !toFindCookie('accessToken')) {
        return { name: 'User', query: { redirect: to.fullPath } }
    }
})

export default router
