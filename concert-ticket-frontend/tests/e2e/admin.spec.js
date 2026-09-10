import { test, expect } from '@playwright/test'

test.use({ trace: 'off', screenshot: 'off', video: 'off',
  launchOptions: { slowMo: Number(process.env.E2E_SLOW_MO || 350) } })

test('live administrator login and read-only backend navigation', async ({ page }) => {
  test.setTimeout(240000)
  const pause = async () => {
    if (process.env.E2E_VIEW_PAUSE) await page.waitForTimeout(Number(process.env.E2E_VIEW_PAUSE))
  }
  test.skip(!process.env.E2E_ADMIN_ACCOUNT || !process.env.E2E_ADMIN_PASSWORD,
    'Provide E2E_ADMIN_ACCOUNT and E2E_ADMIN_PASSWORD to run real administrator login')
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  await page.goto('/admin')
  await expect(page).toHaveURL(/\/user\?redirect=/)
  await page.getByPlaceholder('wang@example.com').first().fill(process.env.E2E_ADMIN_ACCOUNT)
  await page.getByPlaceholder('請輸入密碼', { exact: true }).fill(process.env.E2E_ADMIN_PASSWORD)
  await page.getByRole('button', { name: '登入', exact: true }).click()
  await expect(page.getByRole('button', { name: '登出', exact: true })).toBeVisible()
  const responses = ['selectAllActivities', 'selectAllSessions', 'selectAllticket'].map(endpoint =>
    page.waitForResponse(r => r.url().includes(`/admin/${endpoint}`)))
  await page.getByText('管理後台', { exact: true }).click()
  await expect(page).toHaveURL(/\/admin$/)
  for (const response of await Promise.all(responses)) expect(response.status()).toBe(200)
  await expect(page.locator('.el-tabs')).toBeVisible()
  await pause()
  const activityPanel = page.getByRole('tabpanel', { name: '活動管理' })
  const firstId = (await activityPanel.locator('.el-table__row').first().locator('td').first().innerText()).trim()
  const search = page.getByPlaceholder('搜尋活動名稱、編號或場地')
  await search.fill(firstId)
  await expect(activityPanel.locator('.el-table__row')).toHaveCount(1)
  await pause()
  await search.fill('EDGE-NO-SUCH-ACTIVITY')
  await expect(activityPanel.getByText('找不到活動', { exact: true })).toBeVisible()
  await search.fill('')
  await page.getByRole('button', { name: '新增活動', exact: true }).click()
  const addDialog = page.getByRole('dialog', { name: '新增活動', exact: true })
  await expect(addDialog).toBeVisible()
  await pause()
  await addDialog.getByRole('button', { name: /close/i }).click()
  await activityPanel.getByRole('button', { name: '修改活動', exact: true }).first().click()
  const editDialog = page.getByRole('dialog', { name: '修改活動', exact: true })
  await expect(editDialog).toBeVisible()
  await pause()
  await editDialog.getByRole('button', { name: /close/i }).click()
  await page.getByRole('tab', { name: '建立場次', exact: true }).click()
  const sessionsPanel = page.getByRole('tabpanel', { name: '建立場次' })
  await expect(sessionsPanel.getByText('建立新場次', { exact: true })).toBeVisible()
  await pause()
  await sessionsPanel.getByText('已建立場次', { exact: true }).scrollIntoViewIfNeeded()
  await expect(sessionsPanel.locator('.el-table__row').first()).toBeVisible()
  await pause()
  await page.getByRole('tab', { name: '查看訂單', exact: true }).click()
  const ordersPanel = page.getByRole('tabpanel', { name: '查看訂單' })
  await expect(ordersPanel.getByText('訂單列表', { exact: true })).toBeVisible()
  if (process.env.E2E_VERIFY_ORDER) {
    const order = ordersPanel.locator('.el-table__row').filter({ hasText: process.env.E2E_VERIFY_ORDER })
    await expect(order).toBeVisible()
    await order.scrollIntoViewIfNeeded()
  }
  await pause()
  expect(errors).toEqual([])
})
