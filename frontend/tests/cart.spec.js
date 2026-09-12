import { test, expect } from '@playwright/test';

test.describe('Cart', () => {
  test('should add product to cart', async ({ page }) => {
    // Go directly to product details page for product 11 (Taranga Kanchi Silk)
    await page.goto('http://localhost:5173/products/11');

    // Ensure we see the product title on the details page
    await expect(page.getByRole('heading', { name: 'Taranga Kanchi Silk Brocade Green Saree' }).first()).toBeVisible();

    await page.getByRole('button', { name: 'Add to bag' }).click();
    await expect(page.getByRole('heading', { name: 'Nice pick.' })).toBeVisible();
    await expect(page.getByRole('dialog', { name: 'Shopping bag' })).toHaveCount(0);

    await page.getByRole('button', { name: 'View bag' }).click();
    await expect(page.getByRole('dialog', { name: 'Shopping bag' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Checkout' })).toBeVisible();
  });
});
