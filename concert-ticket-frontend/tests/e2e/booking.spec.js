import { test, expect } from '@playwright/test'

test.use({
  trace: 'off', screenshot: 'off', video: 'off',
  launchOptions: { slowMo: Number(process.env.E2E_SLOW_MO || 350) },
})

test('live activity to booking and optional order submission with ticket lookup', async ({ page }) => {
  test.setTimeout(240000)
  test.skip(!process.env.E2E_MEMBER_ACCOUNT || !process.env.E2E_MEMBER_PASSWORD,
    'Provide E2E_MEMBER_ACCOUNT and E2E_MEMBER_PASSWORD for live booking navigation')
  const pause = async () => {
    if (process.env.E2E_VIEW_PAUSE) await page.waitForTimeout(Number(process.env.E2E_VIEW_PAUSE))
  }
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  await page.goto('/')
  await page.getByText('會員', { exact: true }).click()
  await page.getByPlaceholder('wang@example.com').first().fill(process.env.E2E_MEMBER_ACCOUNT)
  await page.getByPlaceholder('請輸入密碼', { exact: true }).fill(process.env.E2E_MEMBER_PASSWORD)
  await page.getByRole('button', { name: '登入', exact: true }).click()
  await expect(page.getByRole('button', { name: '登出', exact: true })).toBeVisible()
  const lookupOrder = async (orderno, expectedSeat) => {
    await page.getByRole('link', { name: '訂票', exact: true }).click()
    const lookup = page.waitForResponse(r => r.url().endsWith('/booking/selectOnlyTicket'))
    await page.getByRole('button', { name: '查看我的票券', exact: true }).click()
    const response = await lookup
    expect(response.status()).toBe(200)
    const saved = (await response.json()).find(ticket => ticket.orderno === orderno)
    expect(saved?.seat).toBe(expectedSeat)
    expect(saved?.status).toBe('PENDING_PAYMENT')
    const row = page.getByRole('dialog', { name: '我的票券', exact: true })
      .locator('.el-table__row').filter({ hasText: orderno })
    await expect(row).toBeVisible()
    await row.scrollIntoViewIfNeeded()
    await expect(row).toContainText(expectedSeat)
    console.log(`Verified saved order: ${orderno}; seat: ${expectedSeat}; status: ${saved.status}`)
    await pause()
  }
  if (process.env.E2E_VERIFY_ORDER) {
    await lookupOrder(process.env.E2E_VERIFY_ORDER, process.env.E2E_VERIFY_SEAT)
    expect(errors).toEqual([])
    return
  }
  await page.getByRole('link', { name: '活動', exact: true }).click()
  await expect(page.getByRole('heading', { name: '活動資訊' })).toBeVisible()
  await pause()
  const onSale = page.locator('.el-table__body-wrapper .el-table__row')
    .filter({ has: page.getByText('售票中', { exact: true }) }).first()
  await expect(onSale, 'A real on-sale activity is required for this flow').toBeVisible()
  await onSale.getByRole('button', { name: '查看詳情' }).click()
  await expect(page.getByRole('dialog', { name: '活動詳情' })).toBeVisible()
  await pause()
  await page.getByRole('button', { name: '前往訂票' }).click()
  await expect(page.getByRole('heading', { name: '線上訂票' })).toBeVisible()
  await expect(page.locator('.selection-list .el-radio').first()).toBeVisible()
  await page.locator('.selection-list .el-radio').first().click()
  await pause()
  await page.getByRole('button', { name: '下一步' }).click()
  await expect(page.locator('.selection-list .el-radio').first()).toBeVisible()
  await page.locator('.selection-list .el-radio').first().click()
  await pause()
  const seatsLoaded = page.waitForResponse(r => r.url().includes('/booking/selectOnlyUnavailableSeats'))
  await page.getByRole('button', { name: '下一步' }).click()
  expect((await seatsLoaded).status()).toBe(200)
  const seat = page.locator('.seat-map button.seat:not([disabled])').first()
  await expect(seat, 'The selected session must have an available seat').toBeVisible()
  await seat.click()
  await expect(seat).toHaveAttribute('aria-pressed', 'true')
  await pause()
  await page.getByRole('button', { name: '建立訂單', exact: true }).click()
  await expect(page.getByRole('dialog', { name: '確認建立訂單', exact: true })).toBeVisible()
  await expect(page.getByRole('dialog', { name: '確認建立訂單', exact: true })).toContainText('座位：')
  await pause()
  if (process.env.E2E_CREATE_ORDER === '1') {
    const creation = page.waitForResponse(r => r.url().endsWith('/booking/saveTicket') && r.request().method() === 'POST')
    await page.getByRole('button', { name: '確認建立', exact: true }).click()
    const response = await creation
    expect(response.status()).toBe(201)
    const request = response.request().postDataJSON()
    const body = await response.json()
    const created = body.data.filter(ticket => ticket.session_id === request.session_id
      && ticket.seat === request.seat && ticket.status === 'PENDING_PAYMENT')
    expect(created).toHaveLength(1)
    const orderno = created[0].orderno
    console.log(`Created order: ${orderno}; seat: ${request.seat}; status: PENDING_PAYMENT`)
    const tickets = page.getByRole('dialog', { name: '我的票券', exact: true })
    await expect(tickets).toBeVisible()
    await expect(tickets.locator('.el-table__row').filter({ hasText: orderno })).toContainText(request.seat)
    await pause()
    await tickets.getByRole('button', { name: /close/i }).click()
    // Navigate away and query again to prove this is persisted, not just the POST response.
    await page.getByRole('link', { name: '活動', exact: true }).click()
    await pause()
    await lookupOrder(orderno, request.seat)
  } else {
    // Submission is explicitly opt-in because it reserves a real seat.
    await page.getByRole('button', { name: '返回修改' }).click()
  }
  expect(errors).toEqual([])
})
