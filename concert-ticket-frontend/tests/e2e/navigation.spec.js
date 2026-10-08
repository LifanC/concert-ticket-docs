import { test, expect } from '@playwright/test'

for (const role of ['guest', 'member', 'admin']) {
  test(`${role} can switch between activities and membership`, async ({ page, context }) => {
    const errors = []
    page.on('pageerror', error => errors.push(error.message))
    if (role !== 'guest') {
      const authorities = [role === 'admin' ? 'ADMIN_ITEM_IMPLEMENT' : 'USER_ITEM_IMPLEMENT']
      const token = `header.${Buffer.from(JSON.stringify({ authorities })).toString('base64url')}.signature`
      await context.addCookies([{ name: 'accessToken', value: token, url: test.info().project.use.baseURL }])
    }
    await page.route('**/v1/**', route => route.fulfill({ json: [] }))
    await page.goto('/')
    const adminLink = page.getByRole('navigation').getByRole('link', { name: '管理後台' })
    await expect(adminLink).toHaveCount(role === 'admin' ? 1 : 0)
    for (let i = 0; i < 3; i++) {
      await page.getByRole('navigation').getByRole('link', { name: '會員', exact: true }).click()
      await expect(page).toHaveURL(/\/user$/)
      await expect(page.getByRole('heading', { name: '登入會員帳戶' })).toBeVisible()
      await page.getByRole('navigation').getByRole('link', { name: '活動', exact: true }).click()
      await expect(page).toHaveURL(/\/$/)
      await expect(page.getByRole('navigation')).toBeVisible()
    }
    await page.reload()
    await expect(adminLink).toHaveCount(role === 'admin' ? 1 : 0)
    if (role !== 'admin') {
      await page.goto('/admin')
      await expect(page).toHaveURL(role === 'guest' ? /\/user\?redirect=/ : /\/$/)
    }
    expect(errors).toEqual([])
  })
}
