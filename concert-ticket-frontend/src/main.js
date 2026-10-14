// 閱讀導引：前端啟動入口：建立 Vue 應用程式、安裝路由與 UI 元件並掛載到頁面。
import { createApp, onMounted, onUnmounted } from 'vue'
import App from './App.vue'
import PubSub from 'pubsub-js'
import Router from './router'

import ElementPlus from 'element-plus'

const app = createApp(App)
app.use(Router)
app.use(ElementPlus, PubSub, { size: 'small', zIndex: 3000 })
app.mount('#app')
