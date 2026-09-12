import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should open login page and show validation errors', async ({ page }) => {
    await page.goto('http://localhost:5173/');

    await page.getByRole('link', { name: 'Sign in' }).click();

    // Verify redirect and heading
    await expect(page).toHaveURL(/.*login/);
    await expect(page.getByRole('heading', { name: 'Pick up where you left off.' })).toBeVisible();

    // Click sign in without credentials
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Expect validation error
    await expect(page.getByText('Please fill in both email and password.')).toBeVisible();
  });

  test('should navigate to register page', async ({ page }) => {
    await page.goto('http://localhost:5173/');

    await page.getByRole('link', { name: 'Sign in' }).click();

    // Click Create Account link
    await page.getByRole('link', { name: /Create an account/i }).click();
    
    // Verify register page loaded
    await expect(page).toHaveURL(/.*register/);
    await expect(page.getByRole('heading', { name: 'Make room for color.' })).toBeVisible();
  });
});
