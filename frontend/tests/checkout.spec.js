import { test, expect } from '@playwright/test';

test.describe('Checkout Flow & Protection', () => {
  test('should redirect unauthenticated users from checkout to login with redirect param', async ({ page }) => {
    // Navigate directly to checkout without logging in
    await page.goto('/checkout');

    // Should redirect to /login with redirect parameter for checkout
    await expect(page).toHaveURL(/.*login.*redirect=.*checkout/);
    await expect(page.getByRole('heading', { name: /Pick up where you left off/i })).toBeVisible();
  });

  test('should allow authenticated customer to proceed to checkout after adding an item to bag', async ({ page }) => {
    // 1. Sign in as demo customer
    await page.goto('/login');
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Verify redirected to homepage or orders after login
    await expect(page).not.toHaveURL(/.*login/);

    // 2. Go to a product details page
    await page.goto('/products/11');
    await expect(page.getByRole('heading', { name: /Taranga Kanchi Silk/i }).first()).toBeVisible();

    // 3. Add to bag
    await page.getByRole('button', { name: 'Add to bag' }).click();
    await expect(page.getByRole('heading', { name: 'Nice pick.' })).toBeVisible();

    // 4. Open shopping bag drawer and click Checkout
    await page.getByRole('button', { name: 'View bag' }).click();
    const cartDialog = page.getByRole('dialog', { name: 'Shopping bag' });
    await expect(cartDialog).toBeVisible();

    const checkoutButton = cartDialog.getByRole('button', { name: 'Checkout' });
    await expect(checkoutButton).toBeVisible();
    await checkoutButton.click();

    // 5. Verify landed on checkout page
    await expect(page).toHaveURL(/.*checkout/);
    await expect(page.getByText(/Delivery address|Shipping details|Checkout/i).first()).toBeVisible();
  });
});
