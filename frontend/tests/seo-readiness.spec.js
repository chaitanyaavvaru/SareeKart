import { test, expect } from '@playwright/test';

test.describe('SEO & Search Engine Readiness (Phase 13 · Stage 6)', () => {

  test('1. Static robots.txt is accessible and disallows private routes with sitemap declaration', async ({ request }) => {
    const response = await request.get('/robots.txt');
    expect(response.ok()).toBe(true);
    const body = await response.text();

    expect(body).toContain('User-agent: *');
    expect(body).toContain('Disallow: /admin');
    expect(body).toContain('Disallow: /checkout');
    expect(body).toContain('Disallow: /cart');
    expect(body).toContain('Disallow: /orders');
    expect(body).toContain('Disallow: /wishlist');
    expect(body).toContain('Disallow: /wallet');
    expect(body).toContain('Disallow: /trousseau/');
    expect(body).toContain('Disallow: /api/');
    expect(body).toContain('Sitemap: https://sareekart.com/sitemap.xml');
  });

  test('2. Homepage contains canonical URL, OpenGraph, Twitter Card, and JSON-LD schema', async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });

    // Canonical link
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://sareekart.com/');

    // Robots meta
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'index, follow');

    // OpenGraph
    await expect(page.locator('meta[property="og:title"]')).toHaveAttribute('content', /SareeKart/);
    await expect(page.locator('meta[property="og:type"]')).toHaveAttribute('content', 'website');
    await expect(page.locator('meta[property="og:url"]')).toHaveAttribute('content', 'https://sareekart.com/');

    // Twitter Card
    await expect(page.locator('meta[name="twitter:card"]')).toHaveAttribute('content', 'summary_large_image');

    // JSON-LD scripts (WebSite & Organization inside @graph)
    const scriptLocator = page.locator('#jsonld-schema');
    await expect(scriptLocator).toBeAttached();
    const schemaText = await scriptLocator.textContent();
    const schemaJson = JSON.parse(schemaText || '{}');
    const graph = schemaJson['@graph'] || [schemaJson];

    const hasWebSite = graph.some((s) => s['@type'] === 'WebSite');
    const hasOrg = graph.some((s) => s['@type'] === 'Organization');
    expect(hasWebSite).toBe(true);
    expect(hasOrg).toBe(true);
  });

  test('3. Category PLP sets canonical and applies noindex when faceted filters are active', async ({ page }) => {
    // Clean category page
    await page.goto('/products?category=kanchipuram', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://sareekart.com/products?category=kanchipuram');
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'index, follow');

    // Faceted filtered query
    await page.goto('/products?category=kanchipuram&color=Crimson&fabric=Silk', { waitUntil: 'domcontentloaded' });
    // Canonical points back to parent category URL
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://sareekart.com/products?category=kanchipuram');
    // Faceted search has noindex, follow to allow crawler discovery without indexing search combinations
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'noindex, follow');
  });

  test('4. Product Details Page (PDP) generates canonical, Product & Breadcrumb JSON-LD schema', async ({ page }) => {
    // Mock product detail API response for reliable test isolation
    await page.route('**/api/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          name: 'Royal Crimson Kanchipuram Pure Silk Saree',
          description: 'Handwoven pure mulberry silk saree with authentic gold zari border.',
          price: 45000,
          originalPrice: 52000,
          stock: 3,
          sku: 'SK-KAN-001',
          categoryName: 'Kanchipuram Silk',
          fabric: 'Pure Silk',
          color: 'Crimson Red',
          images: [{ imageUrl: 'https://sareekart.com/images/kanchi-1.jpg', primary: true }]
        })
      });
    });

    await page.goto('/products/1', { waitUntil: 'domcontentloaded' });

    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://sareekart.com/products/1');

    // Verify Product JSON-LD schema inside @graph
    const scriptLocator = page.locator('#jsonld-schema');
    await expect(scriptLocator).toBeAttached();
    const schemaText = await scriptLocator.textContent();
    const schemaJson = JSON.parse(schemaText || '{}');
    const graph = schemaJson['@graph'] || [schemaJson];

    const productSchema = graph.find((s) => s['@type'] === 'Product');
    expect(productSchema).toBeDefined();
    expect(productSchema.name).toMatch(/Royal.*Saree/i);
    expect(productSchema.offers).toBeDefined();
    expect(Number(productSchema.offers.price)).toBeGreaterThan(0);
    expect(['https://schema.org/InStock', 'https://schema.org/OutOfStock']).toContain(productSchema.offers.availability);

    const breadcrumbSchema = graph.find((s) => s['@type'] === 'BreadcrumbList');
    expect(breadcrumbSchema).toBeDefined();
    expect(breadcrumbSchema.itemListElement.length).toBeGreaterThanOrEqual(2);
  });

  test('5. Non-existent product displays 404 Drape Not Found state with noindex', async ({ page }) => {
    await page.route('**/api/products/999999', async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Product not found' })
      });
    });

    await page.goto('/products/999999', { waitUntil: 'domcontentloaded' });

    // Expect 404 state heading
    await expect(page.getByRole('heading', { name: /Drape Not Found/i })).toBeVisible();

    // Verify robots meta is noindex
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', /noindex/);
  });

  test('6. Unknown routes render custom NotFoundPage with noindex', async ({ page }) => {
    await page.goto('/some-nonexistent-luxury-page-404', { waitUntil: 'domcontentloaded' });

    await expect(page.getByRole('heading', { name: /This Drape Has Moved/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Return Home/i })).toBeVisible();

    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'noindex, nofollow');
  });

  test('7. Cart route enforces noindex', async ({ page }) => {
    await page.goto('/cart', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', /noindex/);
  });

  test('8. Login route enforces noindex', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', /noindex/);
  });

  test('9. Register route enforces noindex', async ({ page }) => {
    await page.goto('/register', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', /noindex/);
  });

  test('10. Shared trousseau token route enforces noindex', async ({ page }) => {
    await page.goto('/trousseau/share/token123', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', /noindex/);
  });

});
