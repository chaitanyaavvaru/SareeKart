import { test, expect } from '@playwright/test';

test.describe('Login Security & Multi-Channel Recovery Center', () => {

  test('1. should open modal, navigate tabs, and verify recovery channels', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Pick up where you left off.' })).toBeVisible();

    // Click "Security & Recovery" button
    const recoveryBtn = page.getByRole('button', { name: /Security & Recovery/i });
    await expect(recoveryBtn).toBeVisible();
    await recoveryBtn.click();

    // Verify modal is open
    const modal = page.getByRole('dialog', { name: /Login Security & Recovery/i });
    await expect(modal).toBeVisible();

    // Verify Tab 1 (Security Key) elements
    await expect(modal.getByRole('tab', { name: /Security Key/i })).toBeVisible();
    await expect(modal.locator('#recovery-email')).toBeVisible();
    await expect(modal.locator('#recovery-key-input')).toBeVisible();
    await expect(modal.getByRole('button', { name: /Verify Key & Unlock Account/i })).toBeVisible();

    // Verify Tab 2 (Email Link)
    const emailTab = modal.getByRole('tab', { name: /Email Link/i });
    await emailTab.click();
    await expect(modal.locator('#tab-email-input')).toBeVisible();
    await expect(modal.getByRole('button', { name: /Send Reset Link/i })).toBeVisible();

    // Verify Tab 3 (WhatsApp Concierge)
    const whatsappTab = modal.getByRole('tab', { name: /WhatsApp/i });
    await whatsappTab.click();
    await expect(modal.getByText(/Direct Concierge Verification/i)).toBeVisible();
    await expect(modal.getByText('+91 90595 64499')).toBeVisible();
    const waLink = modal.getByRole('link', { name: /Contact Security Concierge on WhatsApp/i });
    await expect(waLink).toBeVisible();
    await expect(waLink).toHaveAttribute('href', /wa\.me\/919059564499/);

    // Close modal via close button
    const closeBtn = modal.getByRole('button', { name: /Close recovery dialog/i });
    await closeBtn.click();
    await expect(modal).not.toBeVisible();
  });

  test('2. should reject invalid recovery key with clear error message', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /Security & Recovery/i }).click();

    const modal = page.getByRole('dialog', { name: /Login Security & Recovery/i });
    await expect(modal).toBeVisible();

    // Fill invalid credentials
    await modal.locator('#recovery-email').fill('customer@sareekart.com');
    await modal.locator('#recovery-key-input').fill('INVALID-KEY-1234');
    await modal.getByRole('button', { name: /Verify Key & Unlock Account/i }).click();

    // Expect error alert
    await expect(modal.getByRole('alert')).toBeVisible();
    await expect(modal.getByText(/Invalid or unrecognized Emergency Security Recovery Key/i)).toBeVisible();
  });

  test('3. should successfully unlock and reset password using Emergency Security Key', async ({ page, request }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /Security & Recovery/i }).click();

    const modal = page.getByRole('dialog', { name: /Login Security & Recovery/i });
    await expect(modal).toBeVisible();

    // Use Customer Demo Key helper
    const demoKeyBtn = modal.getByRole('button', { name: /Customer: SK-REC-CUST-2026/i });
    await demoKeyBtn.click();

    await expect(modal.locator('#recovery-email')).toHaveValue('customer@sareekart.com');
    await expect(modal.locator('#recovery-key-input')).toHaveValue('SK-REC-CUST-2026');

    // Submit key verification
    await modal.getByRole('button', { name: /Verify Key & Unlock Account/i }).click();

    // Expect identity verified banner
    await expect(modal.getByText(/Identity verified successfully!/i)).toBeVisible();
    const proceedBtn = modal.getByRole('button', { name: /Proceed to Set New Password/i });
    await expect(proceedBtn).toBeVisible();
    await proceedBtn.click();

    // Should navigate to reset password page with token in URL
    await expect(page).toHaveURL(/.*reset-password\?token=.*/);
    await expect(page.getByRole('heading', { name: 'Set new password' })).toBeVisible();

    // Set a new password
    const temporaryNewPassword = 'customerNewPass2026!';
    await page.locator('#new-password').fill(temporaryNewPassword);
    await page.locator('#confirm-password').fill(temporaryNewPassword);
    await page.getByRole('button', { name: /Update Password/i }).click();

    // Expect success message
    await expect(page.getByRole('heading', { name: /Password updated successfully/i })).toBeVisible();

    // Click "Sign In With New Password"
    const signInLink = page.getByRole('link', { name: /Sign In With New Password/i });
    await signInLink.click();
    await expect(page).toHaveURL(/.*login/);

    // Sign in with the new password
    await page.locator('#login-email').fill('customer@sareekart.com');
    await page.locator('#login-password').fill(temporaryNewPassword);
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Verify redirected away from login upon successful sign in
    await expect(page).not.toHaveURL(/.*login/);

    // Clean up / restore customer password back to 'customer123'
    // Using another reset token via backend recovery key verification
    const restoreRes = await request.post('http://localhost:8081/api/auth/verify-recovery-key', {
      data: {
        email: 'customer@sareekart.com',
        recoveryKey: 'SK-REC-CUST-2026'
      }
    });
    const restoreJson = await restoreRes.json();
    const restoreToken = restoreJson.data?.resetToken;
    if (restoreToken) {
      await request.post('http://localhost:8081/api/auth/reset-password', {
        data: {
          token: restoreToken,
          newPassword: 'customer123'
        }
      });
    }
  });
});
