import { test, expect } from '@playwright/test';

test.describe('Customer Orders History', () => {
  test('should redirect unauthenticated visitor from /orders to login', async ({ page }) => {
    await page.goto('/orders');
    await expect(page).toHaveURL(/.*login/);
  });

  test('should display order history for authenticated customer', async ({ page }) => {
    // 1. Sign in as demo customer
    await page.goto('/login');
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Wait for login to succeed and redirect away from login page
    await expect(page).not.toHaveURL(/.*login/);

    // 2. Navigate to orders page
    await page.goto('/orders');
    await expect(page).toHaveURL(/.*orders/);

    // 3. Verify orders page elements
    await expect(page.getByRole('heading', { name: /Your sarees, in motion/i })).toBeVisible();
    await expect(page.getByText(/Total spent|Orders/i).first()).toBeVisible();
  });
});
