import { test, expect } from '@playwright/test'

test('shows the answer login page', async ({ page }) => {
  await page.goto('/answer/login')
  await expect(page.locator('.mobile-auth-title')).toHaveText('答题练习')
  await expect(page.locator('.mobile-auth-subtitle')).toContainText('账号')
  await expect(page.getByRole('button', { name: '登录' })).toBeVisible()
})
