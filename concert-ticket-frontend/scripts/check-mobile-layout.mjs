import { chromium } from '@playwright/test'
import assert from 'node:assert/strict'

const browser = await chromium.launch({ channel: 'msedge', headless: true })
const name = '手機排版驗證：很長的活動名稱也應該能完整換行呈現'
const activity = { id: 'A1', name, category: 'MUSIC_CONCERT', venue: '測試場館', price: 1200, status: 'TICKETS_ARE_ON_SALE' }
const session = { id: 'S1', activity_id: 'A1', name, date: '2026-11-01', time: '19:00', salesdate: '2026-10-01', salestime: '12:00', capacity: 20, sold: 2, reserved: 1, status: 'COMING_SOON', value: '19:00', label: '19:00 晚場', available: 17 }
const settings = { configured: false, locked: false, version: 0, rowLabels: ['A', 'B'], seatsPerRow: 10, capacity: 20, defaultPrice: 1200, maxTicketsPerMember: 4, remainingAllowance: 4, memberTicketQuantity: 0, zones: [], ticketTypes: [] }
const errors = []
try {
  for (const width of [320, 390, 768, 1280]) {
    const context = await browser.newContext({ viewport: { width, height: 844 } })
    const token = `x.${Buffer.from(JSON.stringify({ authorities: ['ADMIN_ITEM_IMPLEMENT'], exp: 4102444800 })).toString('base64url')}.x`
    await context.addCookies([{ name: 'accessToken', value: token, url: 'http://127.0.0.1:5173' }])
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    await page.route('**/api/**', async route => {
      const path = new URL(route.request().url()).pathname
      let data = []
      if (path.endsWith('/selectAllActivities')) data = [activity]
      else if (path.endsWith('/selectAllSessions')) data = [session]
      else if (path.endsWith('/selectAllticket')) data = [{ orderno: 'O1', name, price: 1200, payprice: 1200, status: 'PAID' }]
      else if (path.endsWith('/sales-settings')) data = settings
      else if (path.endsWith('/selectOnlyActivities')) data = [{ value: '2026-11-01', label: '2026-11-01' }]
      else if (path.endsWith('/selectOnlySession')) data = [session]
      else if (path.includes('/activityImage/')) return route.fulfill({ status: 404, body: '' })
      await route.fulfill({ json: data })
    })
    const checkWidth = async label => {
      await page.waitForTimeout(400)
      const dimensions = await page.evaluate(() => ({ scroll: document.documentElement.scrollWidth, client: document.documentElement.clientWidth }))
      if (dimensions.scroll > dimensions.client + 1) console.log(await page.evaluate(() => [...document.querySelectorAll('body span')].filter(el => el.getBoundingClientRect().right > innerWidth + 1 && el.getBoundingClientRect().width > 450).map(el => el.parentElement.outerHTML).slice(0, 2)))
      assert.ok(dimensions.scroll <= dimensions.client + 1, `${width}px ${label} overflow: ${JSON.stringify(dimensions)}`)
      console.log(`${width}px ${label}: OK`)
    }
    await page.goto('http://127.0.0.1:5173/')
    await page.getByRole('button', { name: '查看詳情', exact: true }).waitFor()
    await checkWidth('public activities')
    assert.equal(await page.locator('.activity-card .record-card').count(), width <= 767 ? 1 : 0)
    if (width === 320) {
      await page.setViewportSize({ width: 1280, height: 844 })
      await page.locator('.activity-card .el-table').waitFor()
      assert.equal(await page.getByRole('button', { name: '查看詳情', exact: true }).count(), 1)
      await page.setViewportSize({ width, height: 844 })
      await page.locator('.activity-card .record-card').waitFor()
    }
    await page.goto('http://127.0.0.1:5173/admin')
    await page.locator('.dashboard canvas').first().waitFor()
    await checkWidth('dashboard')
    for (const tab of ['活動管理', '建立場次', '查看訂單']) {
      await page.getByRole('tab', { name: tab, exact: true }).click()
      await checkWidth(tab)
    }
    await page.getByRole('tab', { name: '分區與限購', exact: true }).click()
    await page.getByRole('combobox', { name: '場次售票設定' }).click()
    await page.getByRole('option').first().click()
    await page.getByRole('button', { name: '新增分區', exact: true }).waitFor()
    await checkWidth('sales settings')
    await page.getByRole('button', { name: '新增分區', exact: true }).click()
    assert.equal(await page.locator('.zone-fields').count(), 2)
    await page.getByRole('button', { name: '刪除分區', exact: true }).last().click()
    assert.equal(await page.locator('.zone-fields').count(), 1)
    await page.goto('http://127.0.0.1:5173/user')
    await page.locator('.account-card').waitFor()
    await checkWidth('member')
    await page.goto('http://127.0.0.1:5173/booking?activity_id=A1&activity_name=' + encodeURIComponent(name))
    await page.locator('.selection-list .el-radio').first().click()
    await page.getByRole('button', { name: '下一步', exact: true }).click()
    await page.locator('.selection-list .el-radio').first().click()
    await page.getByRole('button', { name: '下一步', exact: true }).click()
    await page.locator('.seat').first().waitFor()
    await checkWidth('booking seats')
    await page.locator('.seat').first().click()
    assert.equal(await page.locator('.seat.selected').count(), 1)
    await context.close()
  }
  assert.deepEqual(errors, [])
} finally {
  await browser.close()
}
