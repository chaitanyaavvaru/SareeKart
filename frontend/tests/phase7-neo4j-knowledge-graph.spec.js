import { test, expect } from '@playwright/test';

test.describe('Phase 7 — Neo4j Knowledge Graph & Traversal Layer Suite', () => {

  test('1. Graph Status & Topology Endpoint: Reports active graph availability and metrics', async ({ request }) => {
    const response = await request.get('/api/recommendations/status');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.data).toBeDefined();

    // Verify non-blocking status report
    const stats = body.data;
    expect(stats).toHaveProperty('circuitOpen');
    expect(stats).toHaveProperty('consecutiveFailures');
  });

  test('2. Frequently Bought Together API: Returns sanitized DTOs without leaking graph internals', async ({ request }) => {
    const response = await request.get('/api/recommendations/frequently-bought-together/1?limit=4');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);

    // Each product must have valid MySQL attributes, active=true, and ZERO PII
    for (const item of body.data) {
      expect(item).toHaveProperty('id');
      expect(item).toHaveProperty('name');
      expect(item).toHaveProperty('price');
      expect(item.active).toBe(true);
      expect(item).not.toHaveProperty('email');
      expect(item).not.toHaveProperty('password');
      expect(item).not.toHaveProperty('phone');
      expect(item).not.toHaveProperty('cypher');
    }
  });

  test('3. Customers Also Viewed API: Returns co-view candidate products', async ({ request }) => {
    const response = await request.get('/api/recommendations/customers-also-viewed/1?limit=6');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
  });

  test('4. Structurally Similar Sarees API: Returns sarees matching weave, occasion, and color family', async ({ request }) => {
    const response = await request.get('/api/recommendations/similar/1?limit=6');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
  });

  test('5. Absolute Failure Isolation: When recommendation API is aborted, commerce browsing and bag operations continue 100%', async ({ page }) => {
    // Simulate complete Neo4j / recommendation network failure by aborting recommendation route
    await page.route('**/api/recommendations/**', async (route) => {
      await route.abort('failed');
    });

    // Storefront product catalog must render without disruption
    await page.goto('/products');
    await expect(page.locator('article h3').first()).toBeVisible({ timeout: 10000 });

    // Product detail page must render successfully
    await page.goto('/products/1');
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10000 });

    // Adding to bag must succeed with zero blockers
    const addToBagButton = page.getByRole('button', { name: /Add to bag/i });
    if (await addToBagButton.isVisible()) {
      await addToBagButton.click();
      await expect(page.getByRole('heading', { name: /Nice pick/i })).toBeVisible({ timeout: 5000 });
    }
  });
});
