// 閱讀導引：設定 Edge 瀏覽器端測試、失敗追蹤與截圖；網站需先啟動，測試位址可由 E2E_BASE_URL 指定。
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'Microsoft Edge', use: { ...devices['Desktop Edge'], channel: 'msedge' } }],
})
