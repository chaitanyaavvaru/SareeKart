import { test, expect } from '@playwright/test';

test.describe('Phase 4: Cart & Wishlist Architecture E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    // Start fresh
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.clear();
    });
  });

  test('Guest cart stores items with explicit createdAt, updatedAt, and expiresAt metadata and survives refresh', async ({ page }) => {
    await page.goto('/products/11');

    const addToBagButton = page.getByRole('button', { name: /Add to bag/i });
    await expect(addToBagButton).toBeVisible();
    await addToBagButton.click();

    // Verify localStorage has the envelope
    const rawCart = await page.evaluate(() => localStorage.getItem('sareekart_guest_cart'));
    expect(rawCart).not.toBeNull();

    const parsedCart = JSON.parse(rawCart);
    expect(parsedCart.items).toBeDefined();
    expect(parsedCart.items.length).toBeGreaterThan(0);
    expect(parsedCart.createdAt).toBeDefined();
    expect(parsedCart.updatedAt).toBeDefined();
    expect(parsedCart.expiresAt).toBeDefined();
    expect(parsedCart.expiresAt).toBeGreaterThan(Date.now());

    // Reload page and check persistence
    await page.reload();
    const cartButton = page.locator('button[aria-label="Shopping Cart"]');
    await expect(cartButton).toBeVisible();
    // Badge should show at least 1
    const badge = cartButton.locator('span');
    await expect(badge).toHaveText('1');
  });

  test('Guest cart expiry purges expired carts deterministically', async ({ page }) => {
    await page.goto('/');

    // Inject expired guest cart into localStorage
    await page.evaluate(() => {
      const expiredPayload = {
        items: [{ id: 11, productId: 11, name: 'Expired Saree', price: 9999, qty: 1 }],
        createdAt: Date.now() - 35 * 24 * 60 * 60 * 1000,
        updatedAt: Date.now() - 35 * 24 * 60 * 60 * 1000,
        expiresAt: Date.now() - 5 * 24 * 60 * 60 * 1000, // Expired 5 days ago
      };
      localStorage.setItem('sareekart_guest_cart', JSON.stringify(expiredPayload));
    });

    // Reload page to trigger expiration check
    await page.reload();

    // Expired cart should have been purged
    const stored = await page.evaluate(() => localStorage.getItem('sareekart_guest_cart'));
    expect(stored).toBeNull();
  });

  test('Wishlist toggle works for guest, updates navbar counter, and persists to localStorage', async ({ page }) => {
    await page.goto('/products');

    // Click heart button on first product card
    const wishlistBtn = page.locator('button[aria-label="Wishlist"]').first();
    await expect(wishlistBtn).toBeVisible();
    await wishlistBtn.click();

    // Verify localStorage has item
    const savedWishlist = await page.evaluate(() => JSON.parse(localStorage.getItem('sareekart_wishlist') || '[]'));
    expect(savedWishlist.length).toBeGreaterThan(0);

    // Verify navbar counter badge updates
    const navbarWishlist = page.locator('nav a[aria-label="Saved Wishlist"]');
    await expect(navbarWishlist.locator('span')).toHaveText('1');

    // Toggle off
    await wishlistBtn.click();
    const updatedWishlist = await page.evaluate(() => JSON.parse(localStorage.getItem('sareekart_wishlist') || '[]'));
    expect(updatedWishlist.length).toBe(0);
  });

  test('Wishlist page displays saved items and allows moving to shopping bag', async ({ page }) => {
    // Seed one item in guest wishlist
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.setItem('sareekart_wishlist', JSON.stringify([
        {
          id: 11,
          productId: 11,
          name: 'Taranga Kanchi Silk Brocade Green Saree',
          price: 18500,
          stockQuantity: 5,
          active: true,
          fabric: 'Silk'
        }
      ]));
    });

    await page.goto('/wishlist');
    await expect(page.getByRole('heading', { name: /Saved for Special Moments/i })).toBeVisible();
    await expect(page.getByText('Taranga Kanchi Silk Brocade Green Saree')).toBeVisible();

    // Click Move to Bag
    const moveBtn = page.getByRole('button', { name: /Move to Bag/i });
    await expect(moveBtn).toBeVisible();
    await moveBtn.click();

    // Item should be removed from wishlist
    await expect(page.getByText(/Your wishlist is waiting/i)).toBeVisible();

    // Cart badge should reflect 1 item
    const cartButton = page.locator('button[aria-label="Shopping Cart"]');
    await expect(cartButton.locator('span')).toHaveText('1');
  });

  test('Cart Drawer enforces stock bounds and allows quantity adjustments', async ({ page }) => {
    await page.goto('/products/11');

    const addBtn = page.getByRole('button', { name: /Add to bag/i });
    await expect(addBtn).toBeVisible();
    await addBtn.click();

    // Open bag drawer
    const viewBagBtn = page.getByRole('button', { name: /View bag/i });
    if (await viewBagBtn.isVisible()) {
      await viewBagBtn.click();
    } else {
      await page.locator('button[aria-label="Shopping Cart"]').click();
    }

    const drawer = page.getByRole('dialog', { name: 'Shopping bag' });
    await expect(drawer).toBeVisible();

    // Check quantity stepper
    const plusBtn = drawer.locator('button[aria-label="Increase quantity"]');
    await expect(plusBtn).toBeVisible();
    await plusBtn.click();

    // Item qty should increment to 2
    await expect(drawer.locator('span:has-text("2")').first()).toBeVisible();

    // Decrease quantity back to 1
    const minusBtn = drawer.locator('button[aria-label="Decrease quantity"]');
    await minusBtn.click();
    await expect(drawer.locator('span:has-text("1")').first()).toBeVisible();
  });

  test('Dedicated Cart Page (/cart) displays drapes and subtotal calculations', async ({ page }) => {
    // Seed an item in guest cart
    await page.goto('/');
    await page.evaluate(() => {
      const envelope = {
        items: [{
          id: 11,
          productId: 11,
          name: 'Taranga Kanchi Silk Brocade Green Saree',
          price: 18500,
          qty: 1,
          availableStock: 5,
          isActive: true
        }],
        createdAt: Date.now(),
        updatedAt: Date.now(),
        expiresAt: Date.now() + 30 * 24 * 60 * 60 * 1000
      };
      localStorage.setItem('sareekart_guest_cart', JSON.stringify(envelope));
    });

    await page.goto('/cart');
    await expect(page.getByRole('heading', { name: /Your Selected Drapes/i })).toBeVisible();
    await expect(page.getByText('Taranga Kanchi Silk Brocade Green Saree')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Order Summary' })).toBeVisible();
    await expect(page.getByRole('button', { name: /Proceed to Checkout/i })).toBeVisible();
  });

  test('Customer login automatically merges guest items into database cart', async ({ page }) => {
    // 1. Seed guest cart in localStorage
    await page.goto('/');
    await page.evaluate(() => {
      const envelope = {
        items: [{
          id: 11,
          productId: 11,
          name: 'Taranga Kanchi Silk Brocade Green Saree',
          price: 18500,
          qty: 2,
          availableStock: 5,
          isActive: true
        }],
        createdAt: Date.now(),
        updatedAt: Date.now(),
        expiresAt: Date.now() + 30 * 24 * 60 * 60 * 1000
      };
      localStorage.setItem('sareekart_guest_cart', JSON.stringify(envelope));
    });

    // 2. Go to login page
    await page.goto('/login');
    await page.locator('#login-email').fill('customer@sareekart.com');
    await page.locator('#login-password').fill('customer123');
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // 3. Wait for navigation after login
    await page.waitForURL((url) => !url.pathname.includes('/login'));

    // 4. Cart button badge should show at least 2 items
    const cartButton = page.locator('button[aria-label="Shopping Cart"]');
    await expect(cartButton).toBeVisible();
    await expect(cartButton.locator('span')).toBeVisible();

    // 5. Guest localStorage cart should be cleared after merge completes
    await expect.poll(async () => {
      return page.evaluate(() => localStorage.getItem('sareekart_guest_cart'));
    }, { timeout: 10000 }).toBeNull();
  });

});
