<script setup>
import { dateConversionYMDhms } from "@/components/componentsJs/ConvertPadding";
import '@/styles/theme.css'
import { isAdmin, syncAuth } from '@/services/auth'

syncAuth()

const date = ref(dateConversionYMDhms(true))

const clockInterval = setInterval(() => {
  date.value = dateConversionYMDhms(true)
}, 1000)
onUnmounted(() => clearInterval(clockInterval))

</script>

<template>
  <div class="site-shell">
    <header class="site-header">
      <router-link class="site-brand" to="/" aria-label="回到活動首頁">
        <span class="brand-emblem" aria-hidden="true">♪</span>
        <span><strong>拾光售票</strong><small>LIVE IN THE MOMENT</small></span>
      </router-link>
      <nav class="site-nav" aria-label="主要導覽">
        <router-link to="/" exact-active-class="is-active">活動</router-link>
        <router-link to="/user" active-class="is-active">會員</router-link>
        <router-link v-if="isAdmin" to="/admin" active-class="is-active">管理後台</router-link>
      </nav>
      <time class="site-clock">{{ date }}</time>
    </header>
    <main class="site-main"><router-view /></main>
    <footer class="site-footer">
      <router-link to="/" class="footer-brand">拾光售票 <span>LIVE IN THE MOMENT</span></router-link>
      <p>把喜歡的聲音，收藏成生活的風景。</p>
      <span class="footer-note">音樂・劇場・展覽</span>
    </footer>
  </div>
</template>

<style scoped>
@media (max-width: 767px) {
  .site-header {
    padding: 16px;
    flex-wrap: wrap;
    gap: 16px;
  }

  .site-clock {
    display: none;
  }

  .site-nav {
    width: 100%;
    margin: 0;
    gap: 12px;
    justify-content: space-between;
  }

  .site-nav a {
    min-height: 44px;
    display: flex;
    align-items: center;
  }

  .site-main {
    padding: 20px 12px 36px;
    min-width: 0;
  }

  .site-footer {
    margin-inline: 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .footer-note {
    margin-left: 0;
  }

  .site-main :deep(.el-tabs__content),
  .site-main :deep(.el-tab-pane) {
    min-width: 0;
  }
}
</style>
