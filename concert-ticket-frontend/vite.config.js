// 閱讀導引：設定前端建置、路徑別名、自動匯入及開發伺服器代理。
import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'


// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // 開發時將 /api HTTP 與 WebSocket 代理至後端；正式部署的代理另由網站伺服器設定。
  const proxyTarget = env.API_PROXY_TARGET || 'http://localhost:8080'

  return {
  server: {
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        ws: true,
      },
    },
  },
  plugins: [
    vue(),
    // 自動匯入 Vue API 與 Element Plus，讓元件可使用 ref、computed 及 ElMessage 等名稱。
    AutoImport({
      imports: ['vue'],
      resolvers: [
        ElementPlusResolver(),
        IconsResolver({
          prefix: 'Icon',
        }),
      ],
    }),
    Components({
      resolvers: [
        IconsResolver({
          enabledCollections: ['ep'],
        }),
        ElementPlusResolver(),
      ],
    }),
    Icons({
      autoInstall: true,
    }),
  ],
  base: '/',
  build: {
    chunkSizeWarningLimit: 1000,
  },
  define: {
    global: 'globalThis'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
  }
})
