import { test, expect } from '@playwright/test';

test.describe('Home Page', () => {
  test('should load the homepage and display key elements', async ({ page }) => {
    await page.goto('http://localhost:5173/');
    await expect(page.getByRole('heading', { name: /Sarees with a point of view/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Shop all sarees/i }).first()).toBeVisible();
    await expect(page.getByText('Free shipping', { exact: false }).first()).toBeVisible();
  });
});
