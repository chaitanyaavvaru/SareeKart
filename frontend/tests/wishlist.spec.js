import { test, expect } from '@playwright/test';

test.describe('Customer Wishlist Experience', () => {
  test('should load empty wishlist state with explore CTA', async ({ page }) => {
    // Navigate directly to /wishlist
    await page.goto('/wishlist');

    // Verify page heading and empty state
    await expect(page.getByRole('heading', { name: /Saved for Special Moments/i })).toBeVisible();
    await expect(page.getByText(/Your wishlist is waiting/i)).toBeVisible();
    await expect(page.getByRole('link', { name: /Explore the Edit/i })).toBeVisible();
  });

  test('should navigate to wishlist when clicking heart icon in navbar', async ({ page }) => {
    await page.goto('/');

    // Click Heart icon in navbar
    const wishlistIcon = page.locator('nav').getByRole('link', { name: 'Saved Wishlist' });
    await wishlistIcon.click();

    // Verify navigated to /wishlist
    await expect(page).toHaveURL(/.*wishlist/);
    await expect(page.getByRole('heading', { name: /Saved for Special Moments/i })).toBeVisible();
  });
});
