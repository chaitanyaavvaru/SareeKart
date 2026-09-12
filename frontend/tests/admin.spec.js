import { test, expect } from '@playwright/test';

test.describe('Admin Panel', () => {
  test('should load admin dashboard', async ({ page }) => {
    // Go directly to admin page (should redirect to login)
    await page.goto('http://localhost:5173/admin');
    await expect(page).toHaveURL(/.*login.*/);

    // Click Admin demo login button
    await page.getByRole('button', { name: /^Admin$/i }).click();

    // Click Sign In
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Wait for redirect back to admin dashboard
    await expect(page).toHaveURL('http://localhost:5173/admin');

    // We should see Dashboard heading and Gross sales
    await expect(page.getByRole('heading', { name: 'Dashboard' }).first()).toBeVisible();
    await expect(page.getByText('Gross sales').first()).toBeVisible();
  });

  test('should navigate to products in admin', async ({ page }) => {
    // Go directly to admin page (should redirect to login if not authenticated)
    await page.goto('http://localhost:5173/admin');
    await expect(page).toHaveURL(/.*login.*/);

    // Click Admin demo login button
    await page.getByRole('button', { name: /^Admin$/i }).click();

    // Click Sign In
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Wait for redirect
    await expect(page).toHaveURL('http://localhost:5173/admin');

    // Click on "Sarees" in sidebar (it is a link)
    await page.getByRole('link', { name: /^Sarees$/i }).click();
    
    // Verify Products page loaded
    await expect(page.getByRole('heading', { name: /Manage Sarees/i }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: /Add Saree Product/i }).first()).toBeVisible();
  });
});
