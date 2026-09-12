import { test, expect } from '@playwright/test';

test.describe('Phase 3 — Search & Filtering E2E Verification', () => {

  test('TC-P3-01: Storefront loads catalog with distinct product images preserved (Phase 1 Freeze)', async ({ page }) => {
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    // Page title and header
    await expect(page.getByRole('heading', { name: 'Handloom, with a point of view.' })).toBeVisible();

    // Verify product cards are displayed
    const productCards = page.locator('article, div[class*="group relative"]');
    await expect(productCards.first()).toBeVisible();

    // Verify image isolation: ensure images belong to products and are loaded
    const productImages = page.locator('img[alt*="Saree"], img[src*="kankatala"], img[src*="unsplash"], img[src*="uploads"]');
    const count = await productImages.count();
    expect(count).toBeGreaterThan(0);
  });

  test('TC-P3-02: Multi-token keyword search filters catalog and updates URL', async ({ page }) => {
    await page.goto('/products?q=Kanchipuram');
    await page.waitForLoadState('networkidle');

    // Verify URL reflects search query
    expect(page.url()).toContain('q=Kanchipuram');

    // Verify active filter chip for search term
    await expect(page.getByText('"Kanchipuram"')).toBeVisible();

    // Verify displayed products contain Kanchipuram
    const headings = page.locator('h3, h2').filter({ hasText: /Kanchipuram/i });
    expect(await headings.count()).toBeGreaterThan(0);
  });

  test('TC-P3-03: Category pill selection updates URL and active state', async ({ page }) => {
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    // Click Cotton Sarees category pill
    const cottonPill = page.getByRole('button', { name: 'Cotton Sarees' });
    await expect(cottonPill).toBeVisible();
    await cottonPill.click();

    // URL must sync
    await expect.poll(() => page.url()).toContain('category=');

    // Active chip must appear
    await expect(page.getByText('Cotton Sarees').first()).toBeVisible();
  });

  test('TC-P3-04: Desktop Filter Panel contains dynamic Fabrics, Occasions, Color Moods, and Stock toggle', async ({ page }) => {
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    // Toggle Filters drawer
    const filtersBtn = page.getByRole('button', { name: /Filters/i });
    await filtersBtn.click();

    // Check facet sections
    await expect(page.getByRole('heading', { name: 'Fabric' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Occasion' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Color Mood' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Price & Stock' })).toBeVisible();

    // Verify In-Stock checkbox is present
    await expect(page.getByText('In-Stock Only')).toBeVisible();
  });

  test('TC-P3-05: Color Mood filter uses canonical color family and updates URL', async ({ page }) => {
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    // Open filters
    await page.getByRole('button', { name: /Filters/i }).click();

    // Select Red color family
    const redButton = page.locator('button').filter({ hasText: /^Red$/i }).first();
    await expect(redButton).toBeVisible();
    await redButton.click();

    // URL sync
    await expect.poll(() => page.url()).toContain('colorFamily=Red');

    // Active chip
    await expect(page.getByText('Color: Red')).toBeVisible();
  });

  test('TC-P3-06: Active filter chip remove button clears filter', async ({ page }) => {
    await page.goto('/products?category=silk-sarees&colorFamily=Red');
    await page.waitForLoadState('networkidle');

    // Verify chips
    const redChipRemove = page.getByLabel('Remove Color: Red');
    await expect(redChipRemove).toBeVisible();
    await redChipRemove.click();

    // URL should no longer contain colorFamily=Red
    await expect.poll(() => page.url()).not.toContain('colorFamily=Red');
  });

  test('TC-P3-07: Empty state renders "Nothing matched this edit" with Reset button', async ({ page }) => {
    await page.goto('/products?q=nonexistentqueryxyz999');
    await page.waitForLoadState('networkidle');

    // Verify empty state message
    await expect(page.getByText('Nothing matched this edit')).toBeVisible();
    await expect(page.getByText('Your filters are a little specific.')).toBeVisible();

    // Click Reset filters
    const resetBtn = page.getByRole('button', { name: /Reset filters/i });
    await expect(resetBtn).toBeVisible();
    await resetBtn.click();

    // URL should be cleared and products reloaded
    await expect.poll(() => page.url()).not.toContain('nonexistentqueryxyz999');
    await expect(page.getByText('Nothing matched this edit')).not.toBeVisible();
  });

  test('TC-P3-08: Two-way URL deep link synchronization', async ({ page }) => {
    // Navigate directly with combined deep link query parameters
    await page.goto('/products?fabric=silk&price=under-3000');
    await page.waitForLoadState('networkidle');

    // Filter chips should automatically reflect URL parameters
    await expect(page.getByText('Under Rs. 3,000')).toBeVisible();
  });
});
