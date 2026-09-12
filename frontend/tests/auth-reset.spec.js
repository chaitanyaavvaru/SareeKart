import { test, expect } from '@playwright/test';

test.describe('Forgot Password and Password Reset Experience', () => {

  test('1. should navigate from Login to Forgot Password page and validate email input', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Pick up where you left off.' })).toBeVisible();

    // Verify "Forgot password?" link exists
    const forgotLink = page.getByRole('link', { name: /Forgot password\?/i });
    await expect(forgotLink).toBeVisible();
    await forgotLink.click();

    // Verify redirected to /forgot-password
    await expect(page).toHaveURL(/.*forgot-password/);
    await expect(page.getByRole('heading', { name: 'Forgot password?' })).toBeVisible();

    // Try submitting without entering email
    await page.getByRole('button', { name: /Send Reset Instructions/i }).click();
    await expect(page.getByText('Please enter your email address.')).toBeVisible();

    // Try submitting invalid email format
    await page.locator('#reset-email').fill('invalid-email-format');
    await page.getByRole('button', { name: /Send Reset Instructions/i }).click();
    await expect(page.getByText('Please enter a valid email address.')).toBeVisible();
  });

  test('2. should show error when accessing Reset Password page with invalid or missing token', async ({ page }) => {
    // Missing token
    await page.goto('/reset-password');
    await expect(page.getByText(/Password reset token is missing/i)).toBeVisible();

    // Invalid token
    await page.goto('/reset-password?token=invalid-random-uuid');
    await expect(page.getByRole('heading', { name: /Unable to reset password/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Request New Reset Link/i })).toBeVisible();
  });

  test('3. should complete full password reset flow and sign in successfully', async ({ page, request }) => {
    // Create an isolated user specifically for this test to avoid race conditions with parallel tests
    const uniqueEmail = `reset.tester.${Date.now()}@sareekart.com`;
    const initialPass = 'initialPass123';
    const newPass = 'updatedSecretPass456';

    await request.post('http://localhost:8081/api/auth/register', {
      data: {
        firstName: 'Reset',
        lastName: 'Tester',
        email: uniqueEmail,
        mobile: '9876543299',
        password: initialPass
      }
    });

    // 1. Go to forgot-password page
    await page.goto('/forgot-password');
    await page.locator('#reset-email').fill(uniqueEmail);
    await page.getByRole('button', { name: /Send Reset Instructions/i }).click();

    // 2. Expect success instructions
    await expect(page.getByText('Instructions sent!')).toBeVisible();
    await expect(page.getByText(new RegExp(uniqueEmail, 'i'))).toBeVisible();

    // 3. Click the development shortcut to open the reset form
    const directLink = page.getByRole('link', { name: /Open Password Reset Form/i });
    await expect(directLink).toBeVisible();
    await directLink.click();

    // 4. Verify on reset-password page with active token
    await expect(page).toHaveURL(/.*reset-password\?token=.+/);
    await expect(page.getByRole('heading', { name: 'Set new password' })).toBeVisible();

    // Test password validation: mismatch
    await page.locator('#new-password').fill(newPass);
    await page.locator('#confirm-password').fill('differentPassMismatch');
    await page.getByRole('button', { name: /Update Password/i }).click();
    await expect(page.getByText('Passwords do not match. Please re-enter.')).toBeVisible();

    // Submit matching passwords
    await page.locator('#confirm-password').fill(newPass);
    await page.getByRole('button', { name: /Update Password/i }).click();

    // 5. Expect success screen
    await expect(page.getByRole('heading', { name: /Password updated successfully/i })).toBeVisible();

    // 6. Click button to sign in with new password
    await page.getByRole('link', { name: /Sign In With New Password/i }).click();
    await expect(page).toHaveURL(/.*login/);

    // Sign in with the newly set password
    await page.locator('#login-email').fill(uniqueEmail);
    await page.locator('#login-password').fill(newPass);
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Verify redirected to storefront home page authenticated
    await expect(page).toHaveURL(/.*localhost:5173\/?$/);
  });
});
