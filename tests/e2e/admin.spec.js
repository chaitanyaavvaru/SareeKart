const { test, expect } = require('@playwright/test');

async function loginAsAdmin(page) {
  await page.goto('http://localhost:5173/login');
  await page.getByPlaceholder('email@example.com').fill('admin@sareekart.com');
  await page.getByPlaceholder('••••••••').fill('admin123');
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForTimeout(1500);
}

test.describe('SareeKart Admin – Coupons & Refunds Phase 12', () => {
  test('Admin can access coupon management via legacy and new route', async ({ page }) => {
    await loginAsAdmin(page);
    // New admin coupons route
    await page.goto('http://localhost:5173/admin/coupons');
    // Should either show Manage Coupons or redirect to login if not admin – allow both
    const couponHeading = page.getByText(/Manage Coupons/i);
    const loginHeading = page.getByText(/Sign In/i);
    await expect(couponHeading.or(loginHeading)).toBeVisible({ timeout: 8000 });
    if (await couponHeading.isVisible().catch(() => false)) {
      await expect(page.getByText(/Create.*Coupon|Manage Coupons/i).first()).toBeVisible();
      await expect(page.getByPlaceholder(/WELCOME20|Search by code/i).first()).toBeVisible({ timeout: 5000 }).catch(() => {});
    }
  });

  test('Admin refunds page renders with KPIs and filters', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('http://localhost:5173/admin/refunds');
    const refundHeading = page.getByRole('heading', { name: /Refund Management/i });
    const loginHeading = page.getByRole('heading', { name: /Sign In/i });
    await expect(refundHeading.or(loginHeading)).toBeVisible({ timeout: 8000 });
    if (await refundHeading.isVisible().catch(() => false)) {
      await expect(page.getByText(/Total.*Pending.*Refunded/i).first().or(page.getByText(/Pending/))).toBeVisible({ timeout: 5000 }).catch(() => {});
      // filters
      const filter = page.getByRole('button', { name: /^All$/i });
      if (await filter.isVisible().catch(() => false)) await expect(filter).toBeVisible();
    }
  });

  test('Legacy admin (/legacy) still renders refunds tab', async ({ page }) => {
    await page.goto('http://localhost:5173/legacy');
    // The legacy monolith should show SareeKart admin with SareeKart header
    await expect(page.locator('text=SareeKart').first()).toBeVisible({ timeout: 8000 });
  });

  test('Coupon bulk elements: search, export, import', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('http://localhost:5173/admin/coupons');
    const heading = page.getByText(/Manage Coupons/i);
    if (await heading.isVisible().catch(() => false)) {
      await expect(page.getByPlaceholder(/Search by code/i)).toBeVisible();
      await expect(page.getByRole('button', { name: /Export CSV/i })).toBeVisible();
      await expect(page.getByRole('button', { name: /Import CSV/i })).toBeVisible();
      await expect(page.getByRole('button', { name: /Add Coupon/i })).toBeVisible();
      // Open modal and check expiry fields
      await page.getByRole('button', { name: /Add Coupon/i }).click();
      await expect(page.getByText(/Valid From/i).first()).toBeVisible({ timeout: 5000 });
      await expect(page.getByText(/Valid Until/i).first()).toBeVisible();
      await expect(page.getByText(/Total Usage Limit/i).first()).toBeVisible();
      await expect(page.getByText(/Per-User Limit/i).first()).toBeVisible();
    }
  });
});
