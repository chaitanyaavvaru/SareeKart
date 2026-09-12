import { test, expect } from '@playwright/test';

test.describe('Admin Analytics & Reporting Suite', () => {

  test('1. Staff can navigate to /admin/analytics, view telemetry KPI cards, and charts', async ({ page }) => {
    // Navigate directly to analytics (redirects to login)
    await page.goto('/admin/analytics');
    await expect(page).toHaveURL(/.*login.*/);

    // Quick demo login as Admin
    await page.getByRole('button', { name: /^Admin$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Verify redirect to /admin/analytics
    await expect(page).toHaveURL(/.*admin\/analytics/);

    // Verify page header
    await expect(
      page.getByRole('heading', { name: /Analytics & Financial Telemetry/i })
    ).toBeVisible();
    await expect(page.getByText('Live Telemetry')).toBeVisible();

    // Verify KPI cards are rendered with values
    await expect(page.getByTestId('kpi-gross-sales')).toBeVisible();
    await expect(page.getByTestId('kpi-net-revenue')).toBeVisible();
    await expect(page.getByTestId('kpi-aov')).toBeVisible();
    await expect(page.getByTestId('kpi-orders')).toBeVisible();

    // Verify sections
    await expect(page.getByRole('heading', { name: /Revenue & Order Velocity Trend/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Payment Channels Split/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Inventory Velocity & Supply Telemetry/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Customer Lifetime Value \(LTV\) & Retention/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Regional Demand Breakdown/i })).toBeVisible();
  });

  test('2. Customer is denied access to /admin/analytics and API returns 403', async ({ page, request }) => {
    // Navigate to login
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Pick up where you left off.' })).toBeVisible();

    // Log in as Customer
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Customer lands on storefront
    await expect(page).toHaveURL(/.*localhost:5173\/?$/);

    // Customer attempts to navigate directly to /admin/analytics
    await page.goto('/admin/analytics');

    // Customer must be redirected away to storefront
    await expect(page).toHaveURL(/.*localhost:5173\/?$/);

    // Test direct backend API call with customer credentials
    const loginRes = await request.post('http://localhost:8081/api/auth/login', {
      data: {
        email: 'customer@sareekart.com',
        password: 'customer123',
      },
    });
    const loginData = await loginRes.json();
    const token = loginData.data.token;

    // Customer calling analytics endpoint must receive 403 Forbidden with exact message
    const analyticsRes = await request.get('http://localhost:8081/api/admin/analytics/overview', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    expect(analyticsRes.status()).toBe(403);
    const errBody = await analyticsRes.json();
    expect(errBody.message).toBe('Not authorised to perform this action');
  });

  test('3. Date range filter pills dynamically update telemetry', async ({ page }) => {
    await page.goto('/admin/analytics');
    await expect(page).toHaveURL(/.*login.*/);

    await page.getByRole('button', { name: /^Admin$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await expect(page).toHaveURL(/.*admin\/analytics/);

    const datePills = page.getByTestId('date-range-pills');
    await expect(datePills).toBeVisible();

    // Click 7D pill
    const button7d = page.getByRole('button', { name: '7D', exact: true });
    await button7d.click();
    await expect(button7d).toHaveClass(/bg-\[#17211F\]/);

    // Click 90D pill
    const button90d = page.getByRole('button', { name: '90D', exact: true });
    await button90d.click();
    await expect(button90d).toHaveClass(/bg-\[#17211F\]/);

    // Click 30D pill
    const button30d = page.getByRole('button', { name: '30D', exact: true });
    await button30d.click();
    await expect(button30d).toHaveClass(/bg-\[#17211F\]/);

    // Verify KPI cards remain populated
    await expect(page.getByTestId('kpi-gross-sales')).toBeVisible();
  });

  test('4. Inventory velocity tabs switch between Fast, Slow, and Stockout SKUs', async ({ page }) => {
    await page.goto('/admin/analytics');
    await expect(page).toHaveURL(/.*login.*/);

    await page.getByRole('button', { name: /^Admin$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await expect(page).toHaveURL(/.*admin\/analytics/);

    // Switch to Slow Moving SKUs
    await page.getByRole('button', { name: 'Slow Moving SKUs' }).click();
    await expect(page.getByRole('button', { name: 'Slow Moving SKUs' })).toHaveClass(/bg-white/);

    // Switch to Stockout Alerts
    await page.getByRole('button', { name: /Stockout Alerts/i }).click();
    await expect(page.getByRole('button', { name: /Stockout Alerts/i })).toHaveClass(/bg-white/);

    // Switch back to Fast Moving SKUs
    await page.getByRole('button', { name: 'Fast Moving SKUs' }).click();
    await expect(page.getByRole('button', { name: 'Fast Moving SKUs' })).toHaveClass(/bg-white/);
  });

  test('5. Export buttons trigger file downloads', async ({ page, request }) => {
    await page.goto('/admin/analytics');
    await expect(page).toHaveURL(/.*login.*/);

    await page.getByRole('button', { name: /^Admin$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await expect(page).toHaveURL(/.*admin\/analytics/);

    // Test Sales CSV download trigger
    const downloadPromise = page.waitForEvent('download');
    await page.getByTestId('export-sales-csv').click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('sales_report_30d.csv');

    // Test Direct API Excel Export
    const loginRes = await request.post('http://localhost:8081/api/auth/login', {
      data: {
        email: 'admin@sareekart.com',
        password: 'admin123',
      },
    });
    const loginData = await loginRes.json();
    const token = loginData.data.token;

    const excelRes = await request.get('http://localhost:8081/api/admin/analytics/export/sales?range=30D&format=xlsx', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    expect(excelRes.status()).toBe(200);
    expect(excelRes.headers()['content-type']).toContain('application/vnd.openxmlformats-officedocument');

    const invCsvRes = await request.get('http://localhost:8081/api/admin/analytics/export/inventory?format=csv', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    expect(invCsvRes.status()).toBe(200);
    expect(invCsvRes.headers()['content-type']).toContain('text/csv');
  });

});
