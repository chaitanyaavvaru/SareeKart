import { test, expect } from '@playwright/test';

test.describe('Products', () => {
  test('should display product list', async ({ page }) => {
    await page.goto('http://localhost:5173/products');

    // Wait for the page to render completely
    await expect(page.getByRole('heading', { name: 'Handloom, with a point of view.' })).toBeVisible();

    // Check for product names
    await expect(page.getByText('Taranga Kanchi Silk Brocade Green Saree').first()).toBeVisible();
  });

  test('should be able to view product details', async ({ page }) => {
    // Go directly to product details page for product 11 (Taranga Kanchi Silk)
    await page.goto('http://localhost:5173/products/11');

    // Ensure we see the product title on the details page
    await expect(page.getByRole('heading', { name: 'Taranga Kanchi Silk Brocade Green Saree' }).first()).toBeVisible();

    await expect(page.getByRole('button', { name: 'Add to bag' })).toBeVisible();
  });
});
