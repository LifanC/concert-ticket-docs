import { chromium, expect } from '@playwright/test'

if (!process.env.E2E_ADMIN_ACCOUNT || !process.env.E2E_ADMIN_PASSWORD) {
  throw new Error('Set E2E_ADMIN_ACCOUNT and E2E_ADMIN_PASSWORD before starting the demo')
}

const browser = await chromium.launch({ channel: 'msedge', headless: false, slowMo: 2000 })
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const baseURL = process.env.E2E_BASE_URL || 'http://localhost:5173'
  await page.goto(`${baseURL}/user`)
  await page.getByPlaceholder('wang@example.com').first().fill(process.env.E2E_ADMIN_ACCOUNT)
  await page.getByPlaceholder('請輸入密碼', { exact: true }).fill(process.env.E2E_ADMIN_PASSWORD)
  await page.getByRole('button', { name: '登入', exact: true }).click()
  await expect(page.getByRole('button', { name: '登出', exact: true })).toBeVisible()
  const responses = ['selectAllActivities', 'selectAllSessions', 'selectAllticket'].map(endpoint =>
    page.waitForResponse(response => response.url().includes(`/admin/${endpoint}`)))
  await page.getByRole('link', { name: '管理後台', exact: true }).click()
  await expect(page.getByRole('heading', { name: '管理員後台' })).toBeVisible()
  for (const response of await Promise.all(responses)) expect(response.status()).toBe(200)
  await page.bringToFront()
  console.log('Administrator backend ready; all three data APIs returned 200. Edge stays open until you close it.')
  await new Promise(resolve => browser.once('disconnected', resolve))
} catch (error) {
  await browser.close()
  throw error
}
