import { test, expect } from '@playwright/test'

// Credentials and authenticated API payloads must not be retained in traces.
const slowMo = Number(process.env.E2E_SLOW_MO || 350)
const viewingPause = Number(process.env.E2E_VIEW_PAUSE || 0)
test.use({ trace: 'off', screenshot: 'off', video: 'off', launchOptions: { slowMo } })

test('live member login, own tickets, admin denial and logout', async ({ page, context }) => {
  test.setTimeout(180000)
  test.skip(!process.env.E2E_MEMBER_ACCOUNT || !process.env.E2E_MEMBER_PASSWORD,
    'Provide E2E_MEMBER_ACCOUNT and E2E_MEMBER_PASSWORD to run real member login')
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  await page.goto('/booking')
  await expect(page).toHaveURL(/\/user\?redirect=/)
  await page.getByPlaceholder('wang@example.com').first().fill(process.env.E2E_MEMBER_ACCOUNT)
  await page.getByPlaceholder('請輸入密碼', { exact: true }).fill(process.env.E2E_MEMBER_PASSWORD)
  const loginResponse = page.waitForResponse(r => r.url().endsWith('/v1/login/login'))
  await page.getByRole('button', { name: '登入', exact: true }).click()
  const login = await loginResponse
  expect(login.status()).toBe(200)
  await expect(page.getByRole('button', { name: '登出', exact: true })).toBeVisible()
  if (viewingPause) await page.waitForTimeout(viewingPause)

  await page.getByText('訂票', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '線上訂票' })).toBeVisible()
  const ticketsResponse = page.waitForResponse(r => r.url().endsWith('/booking/selectOnlyTicket'))
  await page.getByRole('button', { name: '查看我的票券' }).click()
  expect((await ticketsResponse).status()).toBe(200)
  await expect(page.getByRole('dialog', { name: '我的票券' })).toBeVisible()
  if (viewingPause) await page.waitForTimeout(viewingPause)
  await page.getByRole('dialog', { name: '我的票券' }).getByRole('button', { name: /close/i }).click()

  const token = (await context.cookies()).find(cookie => cookie.name === 'accessToken')?.value
  expect(Boolean(token)).toBe(true)
  const apiBase = login.url().replace(/\/v1\/login\/login$/, '')
  for (const endpoint of ['selectAllActivities', 'selectAllSessions', 'selectAllticket']) {
    const response = await context.request.get(`${apiBase}/v1/admin/${endpoint}`, {
      headers: { Authorization: `Bearer ${decodeURIComponent(token)}` },
    })
    expect(response.status(), `Member must not access admin/${endpoint}`).toBe(403)
  }

  await page.getByText('會員', { exact: true }).click()
  await page.getByRole('tab', { name: '修改會員資料' }).click()
  const logoutResponse = page.waitForResponse(r => r.url().endsWith('/login/logout'))
  await page.getByRole('button', { name: '登出', exact: true }).click()
  expect((await logoutResponse).status()).toBe(200)
  await expect(page.getByRole('heading', { name: '登入會員帳戶' })).toBeVisible()
  expect((await context.cookies()).some(cookie => cookie.name === 'accessToken')).toBe(false)
  await page.getByText('訂票', { exact: true }).click()
  await expect(page).toHaveURL(/\/user\?redirect=/)
  if (viewingPause) await page.waitForTimeout(viewingPause)
  expect(errors).toEqual([])
})
