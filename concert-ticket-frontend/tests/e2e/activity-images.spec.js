import { test, expect } from '@playwright/test'

test('images display independently and distinguish missing and failed images', async ({ page }) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const activities = ['fast', 'slow', 'missing', 'failed'].map(id => ({
    id, name: id, category: 'MUSIC_CONCERT', status: 'COMING_SOON', venue: '測試館',
  }))
  await page.route('**/v1/activity/selectAllActivities', route => route.fulfill({ json: activities }))
  let releaseSlow
  const slowGate = new Promise(resolve => { releaseSlow = resolve })
  await page.route('**/v1/activity/activityImage/*', async route => {
    const id = route.request().url().split('/').pop()
    if (id === 'slow') await slowGate
    if (id === 'missing' || id === 'failed') {
      await route.fulfill({ status: id === 'missing' ? 404 : 500, json: {} })
    } else {
      await route.fulfill({ contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><rect width="10" height="10" fill="blue"/></svg>' })
    }
  })
  await page.goto('/')
  try {
    const rows = page.locator('.el-table__body-wrapper .el-table__row')
    await expect(rows).toHaveCount(4)
    await expect(page.getByAltText('fast圖片')).toBeVisible()
    await expect(rows.nth(1)).toContainText('圖片載入中…')
    await expect(rows.nth(2)).toContainText('無圖片')
    await expect(rows.nth(3)).toContainText('圖片載入失敗')
    releaseSlow()
    await expect(page.getByAltText('slow圖片')).toBeVisible()
    await page.getByRole('navigation').getByRole('link', { name: '會員', exact: true }).click()
    await expect(page).toHaveURL(/\/user$/)
    expect(errors).toEqual([])
  } finally {
    releaseSlow()
  }
})
