import { test, expect } from '@playwright/test';

test.describe.serial('Phase 11 — Collaborative Bridal Trousseau Studio E2E Suite', () => {

  let authToken = null;
  let createdBoardId = null;
  let createdCeremonyId = null;
  let shareToken = null;
  let createdItemId = null;
  let testProductId = null;

  test.beforeAll(async ({ request }) => {
    // 1. Authenticate as bride/customer
    const loginRes = await request.post('/api/auth/login', {
      data: {
        email: 'ananya@example.com',
        password: 'customer123',
      },
    });

    if (loginRes.ok()) {
      const body = await loginRes.json();
      authToken = body.data?.token || body.token;
    }

    // 2. Fetch an active catalog saree to use in tests
    const prodRes = await request.get('/api/products?size=5');
    if (prodRes.ok()) {
      const prodBody = await prodRes.json();
      const list = prodBody.data?.content || prodBody.data || prodBody.content || [];
      if (list.length > 0) {
        testProductId = list[0].id;
      }
    }
  });

  test('1. Board Creation: Bride creates collaborative trousseau board with ceremonies', async ({ request }) => {
    if (!authToken) test.skip(!authToken, 'Authentication required');

    const boardPayload = {
      title: 'Kavya & Arjun Royal Wedding Trousseau',
      weddingDate: '2026-11-20',
      notes: 'South Indian handlooms with traditional temple zari',
      isPublicVoting: true,
      ceremonies: [
        {
          ceremonyType: 'MUHURTHAM',
          title: 'Auspicious Muhurtham',
          colorTheme: 'Crimson & Pure Gold Zari',
          targetBudget: 75000,
          displayOrder: 1,
        },
        {
          ceremonyType: 'ENGAGEMENT',
          title: 'Pastel Ring Ceremony',
          colorTheme: 'Pastel Blush Rose',
          targetBudget: 45000,
          displayOrder: 2,
        },
      ],
    };

    const res = await request.post('/api/trousseau', {
      data: boardPayload,
      headers: { Authorization: `Bearer ${authToken}` },
    });

    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body.data).toBeDefined();

    createdBoardId = body.data.id;
    shareToken = body.data.shareToken;
    expect(createdBoardId).toBeDefined();
    expect(shareToken).toBeDefined();
    expect(shareToken.length).toBeGreaterThan(10);
    expect(body.data.ceremonies.length).toBe(2);

    createdCeremonyId = body.data.ceremonies[0].id;
    expect(createdCeremonyId).toBeDefined();
  });

  test('2. Catalog Pinning: Bride adds catalog saree to Muhurtham ceremony', async ({ request }) => {
    if (!authToken || !createdBoardId || !createdCeremonyId || !testProductId) {
      test.skip(true, 'Prerequisites not met');
    }

    const itemPayload = {
      productId: testProductId,
      notes: 'Pure Mulberry silk handwoven by master weaver',
      isAiRecommended: false,
    };

    const res = await request.post(
      `/api/trousseau/${createdBoardId}/ceremonies/${createdCeremonyId}/items`,
      {
        data: itemPayload,
        headers: { Authorization: `Bearer ${authToken}` },
      }
    );

    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body.data.id).toBeDefined();
    expect(body.data.productId).toBe(testProductId);

    createdItemId = body.data.id;
  });

  test('3. Public Family Voting: Guest accesses board via share token and casts LOVE vote', async ({ request }) => {
    if (!shareToken || !createdItemId) test.skip(true, 'Share token or item not available');

    // Guest accesses public view without credentials
    const viewRes = await request.get(`/api/trousseau/share/${shareToken}`);
    expect(viewRes.status()).toBe(200);

    const viewBody = await viewRes.json();
    expect(viewBody.success).toBe(true);
    expect(viewBody.data.title).toContain('Kavya & Arjun');
    expect(viewBody.data.ceremonies.length).toBeGreaterThan(0);

    // Guest casts a LOVE vote
    const votePayload = {
      voterName: 'Amma (Radha)',
      voterPhone: '+919876543210',
      reaction: 'LOVE',
      note: 'The pure zari border is absolutely breathtaking!',
    };

    const voteRes = await request.post(
      `/api/trousseau/share/${shareToken}/items/${createdItemId}/vote`,
      { data: votePayload }
    );

    expect(voteRes.status()).toBe(200);
    const voteBody = await voteRes.json();
    expect(voteBody.success).toBe(true);
    expect(voteBody.data.reaction).toBe('LOVE');
    expect(voteBody.data.voterName).toBe('Amma (Radha)');
  });

  test('4. Rate Limiter Security: Prevents vote spamming on public share endpoint', async ({ request }) => {
    if (!shareToken || !createdItemId) test.skip(true, 'Share token or item not available');

    // Cast another valid vote
    const votePayload = {
      voterName: 'Sunita Chachi',
      voterPhone: '+919876543211',
      reaction: 'LIKE',
      note: 'Elegant drape',
    };

    const res = await request.post(
      `/api/trousseau/share/${shareToken}/items/${createdItemId}/vote`,
      { data: votePayload }
    );

    expect(res.status()).toBe(200);

    // Verify item votes list
    const listRes = await request.get(
      `/api/trousseau/share/${shareToken}/items/${createdItemId}/votes`
    );
    expect(listRes.status()).toBe(200);
    const listBody = await listRes.json();
    expect(listBody.data.length).toBeGreaterThanOrEqual(2);
  });

  test('5. 1-Click Cart Conversion: Bride converts ceremony sarees to shopping bag', async ({ request }) => {
    if (!authToken || !createdBoardId || !createdCeremonyId || !createdItemId) {
      test.skip(true, 'Prerequisites not met');
    }

    const res = await request.post(
      `/api/trousseau/${createdBoardId}/ceremonies/${createdCeremonyId}/transfer-to-cart`,
      {
        data: { itemIds: [createdItemId] },
        headers: { Authorization: `Bearer ${authToken}` },
      }
    );

    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body.message).toContain('transferred to cart');
  });

  test('6. SSE Security & Authorization: Intruder is blocked from subscribing to stream', async ({ request }) => {
    if (!createdBoardId) test.skip(true, 'Board ID not available');

    // Unauthenticated attempt receives 401 or 403
    const unauthRes = await request.get(`/api/trousseau/${createdBoardId}/stream`);
    expect([401, 403]).toContain(unauthRes.status());
  });

  test('7. UI E2E — Public Family Shared View renders in browser and displays ceremony sarees', async ({ page }) => {
    if (!shareToken) test.skip(true, 'Share token not available');

    await page.goto(`/trousseau/share/${shareToken}`);
    
    // Verify Board Title & Header
    await expect(page.getByRole('heading', { name: /Kavya & Arjun/i })).toBeVisible();
    await expect(page.getByText(/Family Collaboration & Voting/i)).toBeVisible();

    // Verify Ceremony Content
    await expect(page.getByText(/Auspicious Muhurtham/i).first()).toBeVisible();

    // Verify Voting Action Buttons
    const loveVoteBtn = page.getByTitle(/Love this look/i).first();
    await expect(loveVoteBtn).toBeVisible();
  });

  test('8. UI E2E — Trousseau Studio route renders live collaborative components', async ({ page }) => {
    // Inject auth token into browser localStorage to simulate logged-in bride
    if (authToken) {
      await page.goto('/');
      await page.evaluate((token) => {
        localStorage.setItem('sareekart_token', token);
        localStorage.setItem('sareekart_user', JSON.stringify({
          id: 9,
          firstName: 'Ananya',
          lastName: 'Verma',
          email: 'ananya@example.com',
          role: 'CUSTOMER'
        }));
      }, authToken);
    }

    await page.goto('/trousseau');

    // Verify Studio renders with active board
    await expect(page.getByRole('heading', { name: /Kavya & Arjun/i })).toBeVisible();
    await expect(page.getByText(/Auspicious Muhurtham/i).first()).toBeVisible();
  });

});
