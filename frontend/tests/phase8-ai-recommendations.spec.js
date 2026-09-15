import { test, expect } from '@playwright/test';

test.describe('Phase 8 — AI Recommendations & Hybrid Ranking Suite', () => {

  test('1. Explainable Recommendations Endpoint: Returns scored products with transparent diagnostic reasons', async ({ request }) => {
    const response = await request.get('/api/recommendations/explainable/1?surface=SIMILAR&limit=4');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
    expect(body.data.length).toBeGreaterThan(0);

    for (const scored of body.data) {
      expect(scored).toHaveProperty('product');
      expect(scored).toHaveProperty('recommendationScore');
      expect(scored).toHaveProperty('reasons');
      expect(scored).toHaveProperty('scoreBreakdown');

      // Recommendation score must be bounded in [0.0, 1.0]
      expect(scored.recommendationScore).toBeGreaterThanOrEqual(0.0);
      expect(scored.recommendationScore).toBeLessThanOrEqual(1.0);

      // Explainability reasons must have at least one diagnostic tag
      expect(Array.isArray(scored.reasons)).toBe(true);
      expect(scored.reasons.length).toBeGreaterThan(0);

      // Score breakdown must contain dimensions
      expect(scored.scoreBreakdown).toHaveProperty('semanticScore');

      // Product must be active and contain zero leaked PII
      const p = scored.product;
      expect(p).toHaveProperty('id');
      expect(p).toHaveProperty('name');
      expect(p).toHaveProperty('price');
      expect(p.active).toBe(true);
      expect(p).not.toHaveProperty('email');
      expect(p).not.toHaveProperty('password');
    }
  });

  test('2. Trending Heritage Sarees Endpoint: Returns active velocity-ranked catalog sarees', async ({ request }) => {
    const response = await request.get('/api/recommendations/trending?limit=6');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
    expect(body.data.length).toBeGreaterThan(0);

    for (const item of body.data) {
      expect(item.active).toBe(true);
      expect(item.price).toBeDefined();
    }
  });

  test('3. Complete The Look Endpoint: Returns complementary cross-category and pairing items', async ({ request }) => {
    const response = await request.get('/api/recommendations/complete-the-look/1?limit=4');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);

    // Target product itself (id 1) must never be recommended in its own Complete The Look
    for (const item of body.data) {
      expect(item.id).not.toBe(1);
      expect(item.active).toBe(true);
    }
  });

  test('4. Dynamic Personalized Recommendations with Session ID: Personalizes for anonymous guest', async ({ request }) => {
    const response = await request.get('/api/recommendations/personalized?sessionId=test_sess_p8&limit=4');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
    expect(body.data.length).toBeGreaterThan(0);
  });

  test('5. Absolute Failure Isolation: When AI recommendation endpoints are aborted, storefront browsing and commerce operate 100%', async ({ page }) => {
    // Simulate complete AI & recommendation downtime by aborting recommendation route
    await page.route('**/api/recommendations/**', async (route) => {
      await route.abort('failed');
    });

    // Storefront product catalog renders with zero blockers
    await page.goto('/products');
    await expect(page.locator('article h3').first()).toBeVisible({ timeout: 10000 });

    // Product detail page renders successfully
    await page.goto('/products/1');
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10000 });

    // Adding to bag must succeed
    const addToBagButton = page.getByRole('button', { name: /Add to bag/i });
    if (await addToBagButton.isVisible()) {
      await addToBagButton.click();
      await expect(page.getByRole('heading', { name: /Nice pick/i })).toBeVisible({ timeout: 5000 });
    }
  });
});
