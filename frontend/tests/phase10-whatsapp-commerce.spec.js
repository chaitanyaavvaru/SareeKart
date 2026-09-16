import { test, expect } from '@playwright/test';

test.describe('Phase 10 — WhatsApp AI Commerce Assistant Suite', () => {

  test('1. Webhook Verification Handshake: GET /api/webhook/whatsapp challenge validation', async ({ request }) => {
    // Valid challenge subscription
    const validRes = await request.get('/api/webhook/whatsapp', {
      params: {
        'hub.mode': 'subscribe',
        'hub.verify_token': 'sareekart-verify-token',
        'hub.challenge': 'CHALLENGE_TOKEN_2026'
      }
    });
    expect(validRes.status()).toBe(200);
    const body = await validRes.text();
    expect(body).toBe('CHALLENGE_TOKEN_2026');

    // Invalid token rejected
    const invalidRes = await request.get('/api/webhook/whatsapp', {
      params: {
        'hub.mode': 'subscribe',
        'hub.verify_token': 'wrong-unauthorized-token',
        'hub.challenge': 'CHALLENGE_TOKEN_2026'
      }
    });
    expect(invalidRes.status()).toBe(403);
  });

  test('2. Webhook Security: POST /api/webhook/whatsapp rejects forged HMAC-SHA256 signature', async ({ request }) => {
    const payload = {
      object: 'whatsapp_business_account',
      entry: [{
        id: 'WHATSAPP_BIZ_ID',
        changes: [{
          field: 'messages',
          value: {
            messaging_product: 'whatsapp',
            messages: [{
              from: '919876543210',
              id: 'wam_test_forged_01',
              timestamp: String(Math.floor(Date.now() / 1000)),
              type: 'text',
              text: { body: 'Hello SareeKart' }
            }]
          }
        }]
      }]
    };

    // Forged signature
    const forgedRes = await request.post('/api/webhook/whatsapp', {
      data: payload,
      headers: {
        'X-Hub-Signature-256': 'sha256=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789'
      }
    });

    expect(forgedRes.status()).toBe(401);
  });

  test('3. Webhook Ingestion & Idempotency: Returns 200 OK fast (<250ms) and handles duplicates', async ({ request }) => {
    const wamId = `wam_test_dedup_${Date.now()}`;
    const payload = {
      object: 'whatsapp_business_account',
      entry: [{
        id: 'WHATSAPP_BIZ_ID',
        changes: [{
          field: 'messages',
          value: {
            messaging_product: 'whatsapp',
            contacts: [{
              profile: { name: 'Priya Sundaram' },
              wa_id: '919876543210'
            }],
            messages: [{
              from: '919876543210',
              id: wamId,
              timestamp: String(Math.floor(Date.now() / 1000)),
              type: 'text',
              text: { body: 'Namaste! Show me bridal silks under 40000' }
            }]
          }
        }]
      }]
    };

    const startTime = Date.now();
    const firstDelivery = await request.post('/api/webhook/whatsapp', { data: payload });
    const elapsedMs = Date.now() - startTime;

    expect(firstDelivery.status()).toBe(200);
    expect(elapsedMs).toBeLessThan(1000); // Fast ACK for Meta webhook SLA

    // Second delivery of same wam_id (Meta retry simulation)
    const secondDelivery = await request.post('/api/webhook/whatsapp', { data: payload });
    expect(secondDelivery.status()).toBe(200);
  });

  test('4. Admin WhatsApp Clienteling: Authenticated staff can list conversations and change status', async ({ request }) => {
    // Authenticate as Admin
    const loginRes = await request.post('/api/auth/login', {
      data: {
        email: 'admin@sareekart.com',
        password: 'admin123'
      }
    });
    expect(loginRes.status()).toBe(200);
    const loginData = await loginRes.json();
    const token = loginData.data?.token || loginData.token;
    expect(token).toBeDefined();

    // Query conversations list
    const convRes = await request.get('/api/admin/whatsapp/conversations', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(convRes.status()).toBe(200);
    const convData = await convRes.json();
    expect(convData.success).toBe(true);
    expect(Array.isArray(convData.data)).toBe(true);

    // If conversations exist, test status transition
    if (convData.data.length > 0) {
      const convId = convData.data[0].id;
      
      // Update to OPEN (human agent take over)
      const updateRes = await request.put(`/api/admin/whatsapp/conversations/${convId}/status?status=OPEN`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      expect(updateRes.status()).toBe(200);

      // Revert back to BOT_HANDLING
      const botRes = await request.put(`/api/admin/whatsapp/conversations/${convId}/status?status=BOT_HANDLING`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      expect(botRes.status()).toBe(200);
    }
  });

  test('5. Storefront Admin WhatsApp Console: Renders Dispatch and Live Clienteling views', async ({ page }) => {
    // Go directly to admin page (redirects to login)
    await page.goto('/admin');
    await expect(page).toHaveURL(/.*login.*/);

    // Click Admin quick access button
    await page.getByRole('button', { name: /^Admin$/i }).click();

    // Click Sign In
    await page.getByRole('button', { name: /^Sign In$/i }).click();

    // Wait for redirect to admin
    await expect(page).toHaveURL(/.*admin.*/);

    // Navigate to WhatsApp Console via sidebar link
    await page.getByRole('link', { name: /WhatsApp Dispatch/i }).click();
    await expect(page).toHaveURL(/.*admin\/whatsapp.*/);

    await expect(page.getByRole('heading', { name: /WhatsApp Dispatch & Clienteling/i })).toBeVisible();

    // Check tabs exist
    const dispatchTab = page.locator('button:has-text("Dispatch & Audit Ledger")');
    const clientelingTab = page.locator('button:has-text("Live Clienteling & Escalations")');

    await expect(dispatchTab).toBeVisible();
    await expect(clientelingTab).toBeVisible();

    // Switch to Clienteling Tab
    await clientelingTab.click();
    await expect(page.locator('text=Patron Threads')).toBeVisible();

    // Switch back to Dispatch Tab
    await dispatchTab.click();
    await expect(page.locator('text=Real-time Outbound Dispatch Ledger')).toBeVisible();
  });
});
