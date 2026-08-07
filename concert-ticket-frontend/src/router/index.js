import {createRouter, createWebHistory} from "vue-router";

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
            component: () => import("@/components/Admin.vue")
        },
    ]
})

export default router
