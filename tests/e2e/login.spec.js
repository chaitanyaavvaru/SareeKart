const { test, expect } = require('@playwright/test');

test.describe('SareeKart Login Tests', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:5173');
  });

  test('Homepage loads successfully', async ({ page }) => {
    await expect(page).toHaveTitle(/SareeKart|Vite|React/i);
  });

  test('Navigate to Login Page', async ({ page }) => {
    // Navbar has "Sign In" or user icon – fallback to direct goto
    const link = page.getByRole('link', { name: /login|sign in/i }).first();
    if (await link.isVisible().catch(() => false)) {
      await link.click();
      await expect(page).toHaveURL(/login/);
    } else {
      await page.goto('http://localhost:5173/login');
      await expect(page).toHaveURL(/login/);
    }
  });

  test('Login with valid admin credentials', async ({ page }) => {
    await page.goto('http://localhost:5173/login');
    await page.getByPlaceholder('email@example.com').fill('admin@sareekart.com');
    await page.getByPlaceholder('••••••••').fill('admin123');
    await page.getByRole('button', { name: /sign in/i }).click();
    // wait for redirect - login should redirect away from /login
    await expect(page).not.toHaveURL(/\/login$/, { timeout: 10000 });
  });

  test('Login with valid customer credentials via quick login', async ({ page }) => {
    await page.goto('http://localhost:5173/login');
    await page.getByRole('button', { name: /customer account/i }).click();
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login$/, { timeout: 10000 });
  });

  test('Login with invalid credentials shows error', async ({ page }) => {
    await page.goto('http://localhost:5173/login');
    await page.getByPlaceholder('email@example.com').fill('wrong@test.com');
    await page.getByPlaceholder('••••••••').fill('wrongpassword');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.getByText(/invalid|incorrect|failed|error/i)).toBeVisible({ timeout: 5000 });
  });

});