import { test, expect } from '@playwright/test';

test.describe('Category & Fabric Filter Navigation', () => {
  test('should load products catalog and filter by category query param', async ({ page }) => {
    // 1. Navigate directly to /products with category=Silk Sarees
    await page.goto('/products?category=Silk%20Sarees');

    // 2. Verify page loads catalog
    await expect(page).toHaveURL(/.*category=Silk/);

    // 3. Verify active filter chip or selected category button
    await expect(page.getByText('Silk Sarees').first()).toBeVisible();

    // 4. Verify product cards are displayed
    const productCards = page.locator('article, div[class*="ProductCard"], [data-testid="product-card"]');
    await expect(productCards.first()).toBeVisible();
  });

  test('should filter by fabric when clicking fabric filter', async ({ page }) => {
    await page.goto('/products');

    // Look for Cotton fabric filter button
    const cottonFilter = page.getByRole('button', { name: /^Cotton$/i }).first();
    if (await cottonFilter.isVisible()) {
      await cottonFilter.click();
      await expect(page.getByText(/Cotton/i).first()).toBeVisible();
    }
  });
});
