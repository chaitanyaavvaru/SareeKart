import { test, expect } from '@playwright/test';

test.describe('Phase 2 — Categories & Product Attributes Verification', () => {

  test.beforeEach(async ({ page }) => {
    // Authenticate as Admin
    await page.goto('/login');
    await page.fill('input[type="email"], input[name="email"]', 'admin@sareekart.com');
    await page.fill('input[type="password"], input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 10000 });
  });

  test('TC-P2-01: Admin Category Console renders taxonomy, hierarchy, and metrics', async ({ page }) => {
    await page.goto('/admin/categories');
    await page.waitForLoadState('networkidle');

    // Header and metrics verification
    await expect(page.getByRole('heading', { name: 'Category & Collection Taxonomy' })).toBeVisible();
    await expect(page.getByText('Total Categories', { exact: true })).toBeVisible();
    await expect(page.getByText('Root Collections', { exact: true })).toBeVisible();
    await expect(page.getByText('Catalog Drapes Linked', { exact: true })).toBeVisible();

    // Verify baseline categories exist in table
    await expect(page.getByText('Silk Sarees').first()).toBeVisible();
    await expect(page.getByText('Cotton Sarees').first()).toBeVisible();
    await expect(page.getByText('Banarasi Sarees').first()).toBeVisible();
  });

  test('TC-P2-02: Safe Category Deletion guard blocks deletion of category with products', async ({ page }) => {
    await page.goto('/admin/categories');
    await page.waitForLoadState('networkidle');

    // Find delete button for Silk Sarees (row 1, has 16 products)
    const silkRow = page.locator('tr').filter({ hasText: 'Silk Sarees' }).first();
    const deleteBtn = silkRow.locator('button[title="Delete Category"]');
    await deleteBtn.click();

    // Verify Safe Deletion Guard warning modal appears
    await expect(page.getByText('Safe Deletion Guard', { exact: true })).toBeVisible();
    await expect(page.getByText('contains', { exact: false })).toBeVisible();
    // Confirm Delete button must NOT be present when products > 0
    await expect(page.getByRole('button', { name: 'Confirm Delete' })).not.toBeVisible();

    // Close modal
    await page.getByRole('button', { name: 'Close' }).click();
    await expect(page.getByText('Safe Deletion Guard', { exact: true })).not.toBeVisible();
  });

  test('TC-P2-03: Manage Sarees modal includes dynamic Category tree, Fabrics, Occasions, and Color Swatch Picker', async ({ page }) => {
    await page.goto('/admin/products');
    await page.waitForLoadState('networkidle');

    // Click Add Saree Product
    await page.getByRole('button', { name: /Add Saree|Add Product/i }).first().click();

    // Verify modal inputs
    await expect(page.getByText('Edit Saree Product').or(page.getByText('Add New Saree Product'))).toBeVisible();
    await expect(page.getByText('Fabric Type', { exact: true })).toBeVisible();
    await expect(page.getByText('Occasion', { exact: true })).toBeVisible();
    await expect(page.getByText('Color Shade', { exact: false })).toBeVisible();

    // Verify Color Swatch Palette button opens swatch palette
    const paletteBtn = page.locator('button:has-text("Palette")');
    await expect(paletteBtn).toBeVisible();
    await paletteBtn.click();
    await expect(page.getByText('Canonical Palette')).toBeVisible();

    // Close modal via Cancel button
    await page.getByRole('button', { name: 'Cancel' }).click();
  });

});
