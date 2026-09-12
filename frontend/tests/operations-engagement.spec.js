import { test, expect } from '@playwright/test';

test.describe('Operations & Customer Engagement Suite', () => {

  test('1. Event-Driven Notifications - Navbar bell widget and telemetry drawer', async ({ page }) => {
    // Navigate to homepage
    await page.goto('/');
    
    // Notification bell button must be visible in navbar
    const bellBtn = page.getByRole('button', { name: /View notifications and dispatch telemetry/i });
    await expect(bellBtn).toBeVisible();

    // Click bell to open popover drawer
    await bellBtn.click();
    await expect(page.getByRole('heading', { name: /Notifications/i })).toBeVisible();
    await expect(page.getByText(/SareeKart Dispatch & Operations Telemetry/i)).toBeVisible();

    // Verify notification content exists
    const notificationList = page.locator('div.max-h-\\[380px\\]');
    await expect(notificationList).toBeVisible();

    // Close popover
    await page.keyboard.press('Escape');
  });

  test('2. Customer Reviews - Star breakdown, verified buyer badge, and review submission', async ({ page }) => {
    // Navigate to a product page
    await page.goto('/products/1');
    await page.waitForLoadState('domcontentloaded');

    // Scroll down to customer reviews section
    const reviewsHeading = page.getByRole('heading', { name: /Patron Chronicles & Feedback/i });
    await reviewsHeading.scrollIntoViewIfNeeded();
    await expect(reviewsHeading).toBeVisible();

    // Verify rating breakdown bars are rendered (5★, 4★, 3★, etc.)
    await expect(page.getByText('5★')).toBeVisible();
    await expect(page.getByText('4★')).toBeVisible();

    // Log in as customer to submit a review
    await page.goto('/login');
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await expect(page).toHaveURL(/.*localhost:5173\/?$/);

    // Navigate back to product page
    await page.goto('/products/1');
    const commentBox = page.getByPlaceholder(/Tell shoppers about fabric feel/i);
    await commentBox.scrollIntoViewIfNeeded();
    await expect(commentBox).toBeVisible();

    // Fill review form
    await commentBox.fill('The zari work has an authentic antique luster. Pure handloom perfection!');
    await page.getByRole('button', { name: /Post review/i }).click();

    // Verify new review appears in the review list
    await expect(page.locator('article p', { hasText: 'The zari work has an authentic antique luster' }).first()).toBeVisible();
  });

  test('3. Admin Review Moderation Console - Status filtering and moderation controls', async ({ page }) => {
    // Log in as Owner
    await page.goto('/login');
    await page.getByRole('button', { name: /^Owner$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await expect(page).toHaveURL('http://localhost:5173/admin');

    // Navigate to Reviews console
    await page.goto('/admin/reviews');
    await expect(page.getByRole('heading', { name: /Customer Reviews Console/i })).toBeVisible();
    await expect(page.getByText(/Verified Buyer Moderation/i)).toBeVisible();

    // Filter tabs exist
    await expect(page.getByRole('button', { name: /All Reviews/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Pending Moderation/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Approved/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Featured Drapes/i })).toBeVisible();

    // Click 'Approved' tab
    await page.getByRole('button', { name: /Approved/i }).click();
    await page.waitForTimeout(500);

    // Verify reviews table rendered
    const table = page.locator('table');
    await expect(table).toBeVisible();
  });

  test('4. Multi-Warehouse Stock Transfer - Manager initiates and Owner approves (Maker-Checker)', async ({ page }) => {
    // 1. Manager logs in and initiates stock transfer
    await page.goto('/login');
    await page.getByRole('button', { name: /^Manager$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await expect(page).toHaveURL('http://localhost:5173/admin');

    await page.goto('/admin/inventory');
    await expect(page.getByRole('heading', { name: /Inventory & Supply Chain Console/i })).toBeVisible();

    // Click Inter-Warehouse Transfer button
    const transferBtn = page.getByRole('button', { name: /Inter-Warehouse Transfer/i });
    await expect(transferBtn).toBeVisible();
    await transferBtn.click();

    // Transfer Modal opens
    await expect(page.getByRole('heading', { name: /Inter-Warehouse Stock Rebalance/i })).toBeVisible();
    await expect(page.getByText(/Maker-Checker Protocol/i)).toBeVisible();

    // Fill transfer reason
    const reasonInput = page.getByPlaceholder(/Navratri showroom stock rebalance/i);
    await reasonInput.fill('Diwali wedding demand transfer to Mumbai hub');

    // Submit transfer request
    await page.getByRole('button', { name: /Submit Stock Transfer Request/i }).click();

    // Verify confirmation notification
    await expect(page.getByText(/Transfer request submitted. Status: Pending owner approval/i)).toBeVisible();

    // Verify listed in Transfers activity table
    await expect(page.getByText('Diwali wedding demand transfer to Mumbai hub').first()).toBeVisible();

    // 2. Clear manager session and log in as Owner to approve in Approval Center
    await page.context().clearCookies();
    await page.evaluate(() => localStorage.clear());
    await page.goto('/login');
    await page.getByRole('button', { name: /^Owner$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await expect(page).toHaveURL('http://localhost:5173/admin');

    await page.goto('/admin/approvals');
    await expect(page.getByRole('heading', { name: /Owner Approval Center/i })).toBeVisible();

    // Click Warehouse Transfers tab
    const transferTab = page.getByRole('button', { name: /Warehouse Transfers/i });
    await expect(transferTab).toBeVisible();
    await transferTab.click();

    // Verify pending transfer card exists
    await expect(page.getByText(/Pending Owner Approval/i).first()).toBeVisible();
    await expect(page.getByRole('button', { name: /Approve & Rebalance/i }).first()).toBeVisible();

    // Owner approves transfer
    await page.getByRole('button', { name: /Approve & Rebalance/i }).first().click();
    await expect(page.getByText(/approved and balance updated/i)).toBeVisible();
  });

  test('5. Artisan Heritage Showcase - Guild profiles and catalog search linkage', async ({ page }) => {
    // Navigate to Artisans page
    await page.goto('/artisans');
    await expect(page.getByRole('heading', { name: /The Hands Behind the Heritage/i })).toBeVisible();

    // Verify artisan cluster names are displayed
    await expect(page.getByText('Kanchipuram Silk Guild')).toBeVisible();
    await expect(page.getByText('Varanasi Zari Cooperative')).toBeVisible();
    await expect(page.getByText('Patan Double-Ikat Guild')).toBeVisible();
    await expect(page.getByText('Chanderi Royal Looms Society')).toBeVisible();

    // Click Patola filter
    await page.getByRole('button', { name: /^patola$/i }).click();
    await expect(page.getByText('Patan Double-Ikat Guild')).toBeVisible();

    // Click 'View Sarees' link on the Patola card
    const viewSareesBtn = page.getByRole('link', { name: /View Sarees/i }).first();
    await viewSareesBtn.click();

    // Verify routed to products page with search query
    await expect(page).toHaveURL(/.*products\?search=.*/);
  });

  test('6. RBAC & Security Hardening - Unauthorized endpoints return 403 Forbidden', async ({ request }) => {
    // 1. Unauthenticated request to /api/admin/reviews must return 403
    const unauthReviewRes = await request.get('http://localhost:8081/api/admin/reviews');
    expect(unauthReviewRes.status()).toBe(403);
    const unauthBody = await unauthReviewRes.json();
    expect(unauthBody.message).toBe('Not authorised to perform this action');

    // 2. Customer token attempting /api/admin/inventory/transfer must return 403
    const custLoginRes = await request.post('http://localhost:8081/api/auth/login', {
      data: { email: 'customer@sareekart.com', password: 'customer123' }
    });
    const custToken = (await custLoginRes.json()).data.token;

    const custTransferRes = await request.post('http://localhost:8081/api/admin/inventory/transfer', {
      headers: { Authorization: `Bearer ${custToken}`, 'Content-Type': 'application/json' },
      data: { sku: 'SK-KANCHI-01', sourceWarehouse: 'WH-01', targetWarehouse: 'WH-02', quantity: 2 }
    });
    expect(custTransferRes.status()).toBe(403);

    // 3. Manager token attempting to approve transfer must return 403
    const mgrLoginRes = await request.post('http://localhost:8081/api/auth/login', {
      data: { email: 'manager@sareekart.com', password: 'manager123' }
    });
    const mgrToken = (await mgrLoginRes.json()).data.token;

    const mgrApproveRes = await request.put('http://localhost:8081/api/admin/inventory/transfer/1/approve', {
      headers: { Authorization: `Bearer ${mgrToken}`, 'Content-Type': 'application/json' },
      data: { note: 'Manager illegal approval attempt' }
    });
    expect(mgrApproveRes.status()).toBe(403);
    const mgrErrBody = await mgrApproveRes.json();
    expect(mgrErrBody.message).toBe('Not authorised to perform this action');
  });

});
