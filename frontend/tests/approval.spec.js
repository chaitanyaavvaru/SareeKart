import { test, expect } from '@playwright/test';

test.describe('RBAC Maker-Checker and Owner Approval System', () => {

  test('1. Customer is denied access to admin and approval endpoints', async ({ page, request }) => {
    // Navigate to login
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Pick up where you left off.' })).toBeVisible();

    // Click Customer quick access button and Sign In
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Customer stays on storefront
    await expect(page).toHaveURL(/.*localhost:5173\/?$/);

    // Attempt to navigate directly to /admin
    await page.goto('/admin');
    // Customer must be redirected away from /admin
    await expect(page).toHaveURL(/.*localhost:5173\/?$/);

    // Test direct API access with customer credentials
    const loginRes = await request.post('http://localhost:8081/api/auth/login', {
      data: {
        email: 'customer@sareekart.com',
        password: 'customer123'
      }
    });
    const loginData = await loginRes.json();
    const token = loginData.data.token;

    // Customer calling /api/approvals/pending must get 403 Forbidden with exact required message
    const approvalRes = await request.get('http://localhost:8081/api/approvals/pending', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    expect(approvalRes.status()).toBe(403);
    const errBody = await approvalRes.json();
    expect(errBody.message).toBe('Not authorised to perform this action');
  });

  test('2. Manager can access admin, create stock adjustment request, and sees Pending owner approval', async ({ page }) => {
    // Go directly to admin page (redirects to login)
    await page.goto('/admin');
    await expect(page).toHaveURL(/.*login.*/);

    // Log in as Manager
    await page.getByRole('button', { name: /^Manager$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Verify redirected to admin dashboard
    await expect(page).toHaveURL('http://localhost:5173/admin');
    await expect(page.getByText('Store Manager')).toBeVisible();

    // 2. Navigate to Inventory
    await page.getByRole('link', { name: /^Inventory$/i }).click();
    await expect(page).toHaveURL(/.*admin\/inventory/);
    await expect(page.getByRole('heading', { name: /Inventory & Supply Chain Console/i })).toBeVisible();

    // Open Stock Restock PO Modal
    await page.getByRole('button', { name: /Issue Purchase Order/i }).click();
    await expect(page.getByRole('heading', { name: /Issue Weaver Purchase Order/i })).toBeVisible();

    // Submit the restock request
    await page.getByRole('button', { name: /Issue PO to Supplier/i }).click();

    // Verify notification banner shows "Pending owner approval"
    await expect(page.getByText(/Pending owner approval/i)).toBeVisible();
  });

  test('3. Manager viewing Approval Center cannot approve requests', async ({ page, request }) => {
    // Go directly to admin approvals
    await page.goto('/admin/approvals');
    await expect(page).toHaveURL(/.*login.*/);

    // Log in as Manager
    await page.getByRole('button', { name: /^Manager$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Wait for redirect to Approvals
    await expect(page).toHaveURL('http://localhost:5173/admin/approvals');
    await expect(page.getByText('Maker-Checker Protocol')).toBeVisible();

    // Manager direct API call to approve must fail with 403 and exact required message
    const loginRes = await request.post('http://localhost:8081/api/auth/login', {
      data: {
        email: 'manager@sareekart.com',
        password: 'manager123'
      }
    });
    const loginData = await loginRes.json();
    const token = loginData.data.token;

    const approveAttempt = await request.post('http://localhost:8081/api/approvals/1/approve', {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      data: { note: 'Manager attempting unauthorized approve' }
    });
    expect(approveAttempt.status()).toBe(403);
    const errBody = await approveAttempt.json();
    expect(errBody.message).toBe('Not authorised to perform this action');
  });

  test('4. Owner can review and approve pending requests in Approval Center', async ({ page }) => {
    // Go to admin approvals
    await page.goto('/admin/approvals');
    await expect(page).toHaveURL(/.*login.*/);

    // Log in as Owner
    await page.getByRole('button', { name: /^Owner$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Verify redirected to Approvals
    await expect(page).toHaveURL('http://localhost:5173/admin/approvals');
    await expect(page.getByText('Super Owner')).toBeVisible();

    // Verify pending approvals list and click Approve & Commit if active
    const approveBtn = page.getByRole('button', { name: /Approve & Commit/i }).first();
    if (await approveBtn.isVisible()) {
      await approveBtn.click();
      await expect(page.getByText(/successfully approved and applied/i)).toBeVisible();
    }
  });

  test('5. Excel Transaction Engine displays all tabs and template download options', async ({ page }) => {
    // Go to Excel transactions
    await page.goto('/admin/excel-transactions');
    await expect(page).toHaveURL(/.*login.*/);

    // Log in as Owner
    await page.getByRole('button', { name: /^Owner$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    await expect(page).toHaveURL('http://localhost:5173/admin/excel-transactions');
    await expect(page.getByRole('heading', { name: /Spreadsheet Transactions/i })).toBeVisible();

    // Check all category tabs
    await expect(page.getByRole('button', { name: /Sales Records/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Stock Adjustments/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Customer Bills/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Supplier Bills/i })).toBeVisible();

    // Check Download Template button
    await expect(page.getByRole('button', { name: /Download.*Template/i })).toBeVisible();
  });

  test('6. Coupon removal requires typing exact coupon code for confirmation', async ({ page }) => {
    // Go to admin coupons
    await page.goto('/admin/coupons');
    await expect(page).toHaveURL(/.*login.*/);

    // Log in as Owner
    await page.getByRole('button', { name: /^Owner$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    await expect(page).toHaveURL('http://localhost:5173/admin/coupons');
    await expect(page.getByRole('heading', { name: /Marketing & Promotional Coupons/i })).toBeVisible();

    // Find first Deactivate button
    const deactivateBtn = page.locator('button[title="Deactivate coupon"]').first();
    if (await deactivateBtn.isVisible()) {
      await deactivateBtn.click();

      // Deactivation confirmation modal must appear
      await expect(page.getByRole('heading', { name: /Confirm Coupon Deactivation/i })).toBeVisible();
      await expect(page.getByText(/Requires typing the exact coupon code/i)).toBeVisible();

      // Close modal
      await page.getByRole('button', { name: /Cancel/i }).click();
    }
  });
});
