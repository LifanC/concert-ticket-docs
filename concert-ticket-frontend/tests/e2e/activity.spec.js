import { test, expect } from '@playwright/test'

const activities = [
  { id: 'EDGE-101', sessionid: 'S1', name: 'Edge 測試演唱會', venue: '台北測試館', category: 'MUSIC_CONCERT', status: 'TICKETS_ARE_ON_SALE', date: '2026-12-01', price: 1200 },
  { id: 'EDGE-101', sessionid: 'S2', name: 'Edge 第二場', venue: '高雄測試館', category: 'MUSIC_CONCERT', status: 'COMING_SOON', date: '2026-12-02', price: 1500 },
]

test('live homepage loads real activity API without JavaScript errors', async ({ page }) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const response = page.waitForResponse(r => r.url().includes('/activity/selectAllActivities'))
  await page.goto('/')
  expect((await response).status()).toBe(200)
  await expect(page.getByRole('heading', { name: '活動資訊' })).toBeVisible()
  await expect(page.locator('.el-table')).toBeVisible()
  expect(errors).toEqual([])
})

for (const path of ['/booking?session_id=S1', '/admin']) {
  test(`anonymous ${path} redirects to login and retains destination`, async ({ page }) => {
    await page.goto(path)
    await expect(page).toHaveURL(url => url.pathname === '/user' && url.searchParams.get('redirect') === path)
    await expect(page.getByRole('heading', { name: '登入會員帳戶' })).toBeVisible()
  })
}

test.describe('deterministic UI regression with fixture API responses', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/v1/activity/selectAllActivities', route => route.fulfill({ json: activities }))
  })

  test('searches venue and activity number, then resets', async ({ page }) => {
    await page.goto('/')
    const search = page.getByPlaceholder('活動名稱、場地或活動編號')
    await search.fill('台北測試館')
    await expect(page.locator('.el-table__body-wrapper .el-table__row')).toHaveCount(1)
    await search.fill('edge-101')
    await expect(page.locator('.el-table__body-wrapper .el-table__row')).toHaveCount(2)
    await search.fill('不存在的活動')
    await expect(page.getByText('找不到符合篩選條件的活動')).toBeVisible()
    await page.getByRole('button', { name: '重設篩選' }).click()
    await expect(search).toHaveValue('')
    await expect(page.locator('.el-table__body-wrapper .el-table__row')).toHaveCount(2)
  })

  test('favorites filter isolates the favorited session', async ({ page, context }) => {
    await context.addCookies([{ name: 'accessToken', value: 'ui-fixture-only', url: 'http://localhost:5173' }])
    await page.route('**/v1/activity/selectOnlyFavoriteActivities', route => route.fulfill({ json: [{ activity_id: 'EDGE-101', session_id: 'S1' }] }))
    await page.goto('/')
    await expect(page.getByRole('button', { name: '已收藏', exact: true })).toBeVisible()
    await page.getByText('只看收藏', { exact: true }).click()
    await expect(page.getByRole('checkbox', { name: '只看收藏' })).toBeChecked()
    await expect(page.locator('.el-table__body-wrapper .el-table__row')).toHaveCount(1)
    await expect(page.locator('.el-table__body-wrapper')).toContainText('Edge 測試演唱會')
  })

  test('activity details lead anonymous visitors to login with booking destination', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: '查看詳情' }).first().click()
    await expect(page.getByRole('dialog')).toContainText('Edge 測試演唱會')
    await page.getByRole('button', { name: '前往訂票' }).click()
    await expect(page).toHaveURL(url => url.pathname === '/user' && url.searchParams.get('redirect')?.includes('/booking?'))
  })
})
