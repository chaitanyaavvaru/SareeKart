import { test, expect } from '@playwright/test';

test.describe('Phase 9 — AI Luxury Saree Stylist & Drape Concierge Suite', () => {

  test('1. Conversational Stylist API: Returns grounded sarees, ensemble look, and consultationId', async ({ request }) => {
    const payload = {
      message: 'I need a royal bridal saree for my wedding reception',
      sessionId: 'test_sess_p9_01',
      occasion: 'Wedding'
    };

    const response = await request.post('/api/ai/stylist/chat', { data: payload });
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.data).toBeDefined();

    const data = body.data;
    expect(data.reply).toBeDefined();
    expect(typeof data.reply).toBe('string');
    expect(data.reply.length).toBeGreaterThan(20);

    // Primary ensemble look validation
    expect(data.primaryLook).toBeDefined();
    expect(data.primaryLook.blouse).toBeDefined();
    expect(data.primaryLook.blouse.contrastColor).toBeDefined();
    expect(data.primaryLook.blouse.frontNeck).toBeDefined();
    expect(data.primaryLook.blouse.sleeve).toBeDefined();
    expect(data.primaryLook.jewelry).toBeDefined();
    expect(data.primaryLook.drapingTechnique).toBeDefined();

    // Consultation ID for Tailoring Studio conversion
    expect(data.consultationId).toBeDefined();
    expect(typeof data.consultationId).toBe('number');

    // Grounded saree candidates
    expect(Array.isArray(data.recommendedSarees)).toBe(true);
    expect(data.recommendedSarees.length).toBeGreaterThan(0);

    for (const scored of data.recommendedSarees) {
      expect(scored).toHaveProperty('product');
      const p = scored.product;
      expect(p.id).toBeDefined();
      expect(p.name).toBeDefined();
      expect(p.price).toBeDefined();
      expect(p.active).toBe(true);
    }
  });

  test('2. Dual-Gate Grounding: Strict budget filtering under ₹20,000', async ({ request }) => {
    const payload = {
      message: 'Show me wedding sarees under 20000 in pink',
      sessionId: 'test_sess_p9_02'
    };

    const response = await request.post('/api/ai/stylist/chat', { data: payload });
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);

    const data = body.data;
    expect(data.recommendedSarees.length).toBeGreaterThan(0);

    for (const scored of data.recommendedSarees) {
      const price = Number(scored.product.price);
      expect(price).toBeLessThanOrEqual(20000);
      expect(scored.product.active).toBe(true);
    }
  });

  test('3. Tailoring Studio Handoff: Records conversion telemetry for consultation', async ({ request }) => {
    // Generate styling consultation first
    const chatRes = await request.post('/api/ai/stylist/chat', {
      data: { message: 'Kanchipuram silk drape for Diwali puja' }
    });
    const chatBody = await chatRes.json();
    const consultationId = chatBody.data.consultationId;
    expect(consultationId).toBeDefined();

    // Record tailoring conversion
    const convertRes = await request.post(`/api/ai/stylist/track-tailoring/${consultationId}`);
    expect(convertRes.status()).toBe(200);

    const convertBody = await convertRes.json();
    expect(convertBody.success).toBe(true);
  });

  test('4. Stylist Studio UI: Interactive Concierge Chat & Quick Prompts', async ({ page }) => {
    await page.goto('/stylist');

    // Verify Studio Hero & Mode Tabs
    await expect(page.locator('h1')).toContainText('AI Luxury Saree Stylist');
    const chatTab = page.locator('button:has-text("Conversational Stylist Concierge")');
    await expect(chatTab).toBeVisible();

    // Verify online status pill
    await expect(page.locator('text=Online • Grounded in authentic catalog inventory')).toBeVisible();

    // Verify quick prompt chips
    const weddingChip = page.locator('button:has-text("Wedding saree under ₹20,000 with contrast blouse")');
    await expect(weddingChip).toBeVisible();

    // Click quick prompt chip to trigger chat
    await weddingChip.click();

    // Wait for assistant response to appear
    await expect(page.locator('text=Haute Couture Stylist Ensemble').or(page.locator('text=Stylist Ensemble Breakdown')).or(page.locator('text=Atelier Stylist'))).toBeVisible({ timeout: 15000 });

    // Verify grounded recommendation cards rendered
    const viewButtons = page.locator('button:has-text("Ensemble")');
    await expect(viewButtons.first()).toBeVisible();

    // Verify tailoring handoff button rendered
    const customizeBtn = page.locator('button:has-text("Customize Blouse")');
    await expect(customizeBtn.first()).toBeVisible();
  });

  test('5. Multi-Tier Failure Isolation: System remains resilient when chat is requested with empty query', async ({ request }) => {
    // Empty message should be rejected cleanly with 400 Bad Request
    const response = await request.post('/api/ai/stylist/chat', {
      data: { message: '' }
    });
    expect([400, 422]).toContain(response.status());
  });

});
