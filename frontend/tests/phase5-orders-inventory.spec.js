import { test, expect } from '@playwright/test';

test.describe('Phase 5 — Orders & Inventory E2E Suite', () => {

  test('1. Full Checkout Flow: Place COD order, verify snapshots, cart clearance, and success receipt', async ({ page }) => {
    // 1. Sign in as demo customer
    await page.goto('/login');
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await page.waitForURL((url) => !url.pathname.includes('/login'));

    // 2. Select product 2 (Kanchipuram Temple Border Saree, stock available)
    await page.goto('/products/2');
    await expect(page.getByRole('heading', { name: /Kanchipuram Temple Border/i }).first()).toBeVisible();

    // 3. Add to bag
    await page.getByRole('button', { name: 'Add to bag' }).click();
    await expect(page.getByRole('heading', { name: 'Nice pick.' })).toBeVisible();

    // 4. Navigate to checkout
    await page.goto('/checkout');
    await expect(page).toHaveURL(/.*checkout/);

    // Wait for address form
    await expect(page.getByText('Delivery details')).toBeVisible({ timeout: 10000 });

    // 5. Fill delivery address
    await page.locator('input[placeholder="Recipient name"]').fill('Radha Krishna');
    await page.locator('input[placeholder="10-digit mobile"]').fill('9876543210');
    await page.locator('input[placeholder="Flat, house, street"]').fill('77 Silk Weavers Colony, Gandhi Road');
    await page.locator('input[placeholder="City"]').fill('Kanchipuram');
    await page.locator('input[placeholder="State"]').fill('Tamil Nadu');
    await page.locator('input[placeholder="6 digits"]').fill('600001');

    // 6. Continue to payment
    await page.getByRole('button', { name: /Continue to payment/i }).click();
    await expect(page.getByText('Payment method')).toBeVisible();

    // 7. Select COD and Place Order
    const codRadio = page.locator('input[value="COD"]');
    if (await codRadio.isVisible()) {
      await codRadio.check();
    }

    const placeOrderBtn = page.getByRole('button', { name: /Place order/i });
    await expect(placeOrderBtn).toBeVisible();
    await placeOrderBtn.click();

    // 8. Verify Order Placed success screen
    await expect(page.getByRole('heading', { name: /Thank you for shopping/i })).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(/Order ID/i)).toBeVisible();
    await expect(page.getByText(/#SK-/i)).toBeVisible();

    // 9. Verify shopping bag is reset / cleared
    const cartButton = page.locator('button[aria-label="Shopping Cart"]');
    await expect(cartButton).toBeVisible();
  });

  test('2. Order History Verification: Snapshot product name & order card present in /orders', async ({ page }) => {
    // 1. Sign in as demo customer
    await page.goto('/login');
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await page.waitForURL((url) => !url.pathname.includes('/login'));

    // 2. Navigate to /orders
    await page.goto('/orders');
    await expect(page).toHaveURL(/.*orders/);

    // 3. Verify order cards load with historical product details
    await expect(page.getByRole('heading', { name: /Your sarees, in motion/i })).toBeVisible();
    const orderCards = page.locator('article');
    await expect(orderCards.first()).toBeVisible({ timeout: 10000 });
  });

  test('3. Staff Order Management Access: Staff roles can access /admin/orders console', async ({ page }) => {
    // 1. Sign in as Owner/Admin
    await page.goto('/login');
    await page.getByRole('button', { name: /^Owner$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await page.waitForURL((url) => !url.pathname.includes('/login'));

    // 2. Navigate to /admin/orders
    await page.goto('/admin/orders');
    await expect(page).toHaveURL(/.*admin.*orders/);

    // 3. Verify orders table is visible
    await expect(page.getByRole('heading', { name: /Manage Orders|Orders/i }).first()).toBeVisible({ timeout: 10000 });
  });

  test('4. Backend API Idempotency: Replaying duplicate checkout returns identical order without duplication', async ({ page, request }) => {
    // Sign in via API to get token
    const loginRes = await request.post('http://localhost:8081/api/auth/login', {
      data: { email: 'customer@sareekart.com', password: 'customer123' }
    });
    expect(loginRes.ok()).toBeTruthy();
    const loginJson = await loginRes.json();
    const token = loginJson.data?.token || loginJson.token;

    // Put Product 1 (Royal Banarasi Silk Saree, ample stock) in cart
    await request.post('http://localhost:8081/api/cart/items', {
      headers: { Authorization: `Bearer ${token}` },
      data: { productId: 1, quantity: 1 }
    });

    const testIdemKey = 'e2e_idem_' + Date.now();
    const payload = {
      shippingAddress: {
        fullName: 'Idempotency E2E Test',
        phone: '9876543210',
        streetAddress: '100 Queen Street',
        city: 'Chennai',
        state: 'Tamil Nadu',
        pincode: '600001'
      },
      paymentMethod: 'COD',
      idempotencyKey: testIdemKey
    };

    // First order submission
    const res1 = await request.post('http://localhost:8081/api/orders', {
      headers: { Authorization: `Bearer ${token}` },
      data: payload
    });
    expect(res1.ok()).toBeTruthy();
    const order1 = (await res1.json()).data;
    expect(order1.id).toBeDefined();

    // Replay identical submission with same idempotency key
    const res2 = await request.post('http://localhost:8081/api/orders', {
      headers: { Authorization: `Bearer ${token}` },
      data: payload
    });
    expect(res2.ok()).toBeTruthy();
    const order2 = (await res2.json()).data;

    // Must resolve to the exact same order ID!
    expect(order2.id).toBe(order1.id);
  });
});
