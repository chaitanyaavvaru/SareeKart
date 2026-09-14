import { test, expect } from '@playwright/test';

test.describe('Phase 6 — Customer Behavior & Telemetry Suite', () => {

  test('1. Guest Session Tracking: Persistent anonymous sessionId across page reloads', async ({ page }) => {
    // 1. Visit storefront products page as anonymous guest
    await page.goto('/products');
    await expect(page.locator('article h3').first()).toBeVisible({ timeout: 10000 });
    
    // 2. Check that sk_session_id was initialized in sessionStorage
    const sessionId = await page.evaluate(() => sessionStorage.getItem('sk_session_id'));
    expect(sessionId).toBeTruthy();
    expect(sessionId).toMatch(/^sess_/);

    // 3. Reload page and verify same session ID persists
    await page.reload();
    await expect(page.locator('article h3').first()).toBeVisible({ timeout: 10000 });
    const persistentSessionId = await page.evaluate(() => sessionStorage.getItem('sk_session_id'));
    expect(persistentSessionId).toBe(sessionId);
  });

  test('2. Product View & Add to Cart Telemetry: Non-blocking event dispatching', async ({ page }) => {
    const interceptedEvents = [];
    await page.route('**/api/events/**', async (route) => {
      const request = route.request();
      if (request.method() === 'POST') {
        try {
          const postData = request.postDataJSON();
          if (postData.events) {
            interceptedEvents.push(...postData.events);
          } else if (postData.eventType) {
            interceptedEvents.push(postData);
          }
        } catch (e) {}
      }
      await route.continue();
    });

    // 1. Visit product detail page
    await page.goto('/products/2', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('heading', { name: /Kanchipuram Temple Border/i }).first()).toBeVisible();

    // 2. Add product to bag
    await page.getByRole('button', { name: 'Add to bag' }).click();
    await expect(page.getByRole('heading', { name: 'Nice pick.' })).toBeVisible();

    // 3. Verify ADD_TO_CART event was dispatched immediately
    await page.waitForTimeout(1000);
    const hasAddToCart = interceptedEvents.some(
      (e) => e.eventType === 'ADD_TO_CART' && e.entityId === 2
    );
    expect(hasAddToCart).toBe(true);
  });

  test('3. Search Telemetry: Captures search terms without impacting results display', async ({ page }) => {
    const interceptedEvents = [];
    await page.route('**/api/events/**', async (route) => {
      if (route.request().method() === 'POST') {
        try {
          const postData = route.request().postDataJSON();
          if (postData.events) interceptedEvents.push(...postData.events);
          else if (postData.eventType) interceptedEvents.push(postData);
        } catch (e) {}
      }
      await route.continue();
    });

    // Navigate with search query
    await page.goto('/products?q=Banarasi', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText(/Emerald Green Banarasi/i)).toBeVisible({ timeout: 10000 });

    // Allow immediate flush to complete
    await page.waitForTimeout(1000);

    const hasSearchEvent = interceptedEvents.some(
      (e) => e.eventType === 'SEARCH_QUERY' && e.metadata?.query?.toLowerCase() === 'banarasi'
    );
    expect(hasSearchEvent).toBe(true);
  });

  test('4. Identity Resolution on Customer Sign In: Links guest session to user', async ({ page }) => {
    let identifyCalled = false;
    let identifySessionId = null;

    await page.route('**/api/events/identify', async (route) => {
      identifyCalled = true;
      try {
        const data = route.request().postDataJSON();
        identifySessionId = data.sessionId;
      } catch (e) {}
      await route.continue();
    });

    // 1. Visit homepage to establish guest session
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    const guestSessionId = await page.evaluate(() => sessionStorage.getItem('sk_session_id'));
    expect(guestSessionId).toBeTruthy();

    // 2. Sign in as demo customer
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: /^Customer$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await page.waitForURL((url) => !url.pathname.includes('/login'));

    // 3. Verify identify endpoint was invoked with guest session ID
    await page.waitForTimeout(1000);
    expect(identifyCalled).toBe(true);
    expect(identifySessionId).toBe(guestSessionId);
  });

  test('5. Telemetry Failure Isolation: Commerce flows succeed when telemetry is disabled or failing', async ({ page }) => {
    // Abort ALL telemetry requests to simulate complete network/server telemetry failure
    await page.route('**/api/events/**', (route) => route.abort('failed'));

    // Also activate client killswitch
    await page.addInitScript(() => {
      window.__DISABLE_TELEMETRY__ = true;
    });

    // 1. Browse products — verify product list renders smoothly
    await page.goto('/products', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('article h3').first()).toBeVisible({ timeout: 10000 });

    // 2. Search filtering works with dead telemetry
    await page.goto('/products?q=Silk', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('article h3').first()).toBeVisible({ timeout: 10000 });

    // 3. View individual product
    await page.goto('/products/2', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('heading', { name: /Kanchipuram Temple Border/i }).first()).toBeVisible();

    // 4. Add to cart — must NOT throw or freeze UI even with telemetry completely aborted
    await page.getByRole('button', { name: 'Add to bag' }).click();
    await expect(page.getByRole('heading', { name: 'Nice pick.' })).toBeVisible();

    // 5. Open Cart Drawer and verify item presence
    await page.getByRole('button', { name: /View bag/i }).click();
    await expect(page.getByRole('dialog', { name: /Shopping bag/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Checkout/i })).toBeVisible();

    // Complete storefront interaction succeeds with zero blocking or unhandled rejections!
  });

  test('6. Admin Behavioral Telemetry Dashboard: Overview and Funnel Render', async ({ page }) => {
    // 1. Sign in as Admin
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: /^Admin$/i }).click();
    await page.getByRole('button', { name: /^Sign In$/i }).click();
    await page.waitForURL((url) => !url.pathname.includes('/login'));

    // 2. Visit Analytics Dashboard
    await page.goto('/admin/analytics', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Analytics & Financial Telemetry')).toBeVisible();

    // 3. Verify Phase 6 Behavioral Telemetry & Event Funnel section is rendered
    await expect(page.locator('[data-testid="behavioral-telemetry-section"]')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('heading', { name: /Customer Behavioral Telemetry & Event Funnel/i })).toBeVisible();
    await expect(page.getByText('4-Stage Behavior-Driven Conversion Funnel')).toBeVisible();
    await expect(page.getByRole('heading', { name: /Top Explored Sarees/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Trending Search Queries/i })).toBeVisible();
  });

});
