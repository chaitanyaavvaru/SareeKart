import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  CANONICAL_DOMAIN,
  getCanonicalUrl,
  truncateDescription,
  toAbsoluteImageUrl,
  isAutoPrivatePath,
} from '../src/utils/seoUtils.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const frontendRoot = path.resolve(__dirname, '..');

test('Point 1 — Canonical Domain Configuration: CANONICAL_DOMAIN is authoritative HTTPS domain', () => {
  assert.equal(CANONICAL_DOMAIN, 'https://sareekart.com', 'Authoritative domain must be https://sareekart.com without trailing slash');
  assert.ok(!CANONICAL_DOMAIN.endsWith('/'), 'Canonical domain must not have trailing slash');
  assert.ok(CANONICAL_DOMAIN.startsWith('https://'), 'Canonical domain must use secure HTTPS scheme');
});

test('Point 2 — Canonical URL Generator: normalizes paths and whitelists canonical query params', () => {
  // Root URL
  assert.equal(getCanonicalUrl('/'), 'https://sareekart.com/', 'Root URL must maintain trailing slash');
  assert.equal(getCanonicalUrl(''), 'https://sareekart.com/', 'Empty path resolves to root URL');

  // Product detail path with trailing slash stripped
  assert.equal(getCanonicalUrl('/products/108/'), 'https://sareekart.com/products/108');
  assert.equal(getCanonicalUrl('/products/108'), 'https://sareekart.com/products/108');

  // Whitelisted category parameter retained, faceted parameters stripped
  const searchParams = new URLSearchParams({
    category: 'kanchipuram-silk',
    fabric: 'Pure Mulberry Silk',
    color: 'Crimson Red',
    price: '25000-50000',
    sort: 'price_desc',
  });
  const canonicalWithCategory = getCanonicalUrl('/products', searchParams, ['category']);
  assert.equal(
    canonicalWithCategory,
    'https://sareekart.com/products?category=kanchipuram-silk',
    'Canonical URL must preserve category while stripping ephemeral facets'
  );

  // When category is 'All', it should be omitted from canonical URL
  const allParams = new URLSearchParams({ category: 'All' });
  assert.equal(
    getCanonicalUrl('/products', allParams, ['category']),
    'https://sareekart.com/products',
    'Category=All must normalize to bare /products'
  );
});

test('Point 3 — Description Truncation: truncates on word boundaries without cutting words', () => {
  const shortText = 'Authentic handwoven Kanchipuram silk saree.';
  assert.equal(truncateDescription(shortText, 160), shortText);

  const longText = 'This breathtaking handwoven Banarasi saree features opulent gold and silver zari work across a rich crimson crimson silk body with intricate floral jaal motifs handcrafted by master weavers in Varanasi India.';
  const truncated = truncateDescription(longText, 100);
  assert.ok(truncated.length <= 103, 'Truncated length must stay within bounds');
  assert.ok(truncated.endsWith('...'), 'Truncated text must end with ellipsis');
  assert.ok(!truncated.includes('  '), 'Truncated text must collapse duplicate whitespace');
});

test('Point 4 — Image URL Normalization: converts relative assets to absolute HTTPS URLs', () => {
  assert.equal(toAbsoluteImageUrl(null), 'https://sareekart.com/favicon.svg');
  assert.equal(toAbsoluteImageUrl(''), 'https://sareekart.com/favicon.svg');
  assert.equal(toAbsoluteImageUrl('/uploads/saree-1.webp'), 'https://sareekart.com/uploads/saree-1.webp');
  assert.equal(toAbsoluteImageUrl('uploads/saree-2.webp'), 'https://sareekart.com/uploads/saree-2.webp');
  assert.equal(
    toAbsoluteImageUrl('https://images.unsplash.com/photo-saree'),
    'https://images.unsplash.com/photo-saree'
  );
});

test('Point 5 — Fail-Safe Private Path Detection: correctly identifies private and public routes', () => {
  // Positive cases (private routes must be noindexed)
  assert.equal(isAutoPrivatePath('/admin'), true);
  assert.equal(isAutoPrivatePath('/admin/orders'), true);
  assert.equal(isAutoPrivatePath('/admin/finance'), true);
  assert.equal(isAutoPrivatePath('/checkout'), true);
  assert.equal(isAutoPrivatePath('/cart'), true);
  assert.equal(isAutoPrivatePath('/orders'), true);
  assert.equal(isAutoPrivatePath('/orders/42/track'), true);
  assert.equal(isAutoPrivatePath('/invoices'), true);
  assert.equal(isAutoPrivatePath('/wallet'), true);
  assert.equal(isAutoPrivatePath('/wishlist'), true);
  assert.equal(isAutoPrivatePath('/profile'), true);
  assert.equal(isAutoPrivatePath('/account'), true);
  assert.equal(isAutoPrivatePath('/trousseau'), true);
  assert.equal(isAutoPrivatePath('/trousseau/share/token-xyz'), true);
  assert.equal(isAutoPrivatePath('/login'), true);
  assert.equal(isAutoPrivatePath('/register'), true);
  assert.equal(isAutoPrivatePath('/forgot-password'), true);
  assert.equal(isAutoPrivatePath('/reset-password'), true);

  // Negative cases (public content must remain indexable)
  assert.equal(isAutoPrivatePath('/'), false);
  assert.equal(isAutoPrivatePath('/products'), false);
  assert.equal(isAutoPrivatePath('/products/108'), false);
  assert.equal(isAutoPrivatePath('/heritage-weaves'), false);
  assert.equal(isAutoPrivatePath('/stores'), false);
  assert.equal(isAutoPrivatePath('/artisans'), false);
  assert.equal(isAutoPrivatePath('/saree-care'), false);
  assert.equal(isAutoPrivatePath('/stylist'), false);
  assert.equal(isAutoPrivatePath('/trousseau-planner'), false, 'Public trousseau-planner marketing page must be indexable');
});

test('Point 6 — 404 & Private Route Directives: verifies noindex and nofollow in source code', () => {
  const notFoundSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/NotFoundPage.jsx'), 'utf8');
  assert.match(notFoundSource, /noindex=\{\s*true\s*\}/, 'NotFoundPage must specify noindex={true}');
  assert.match(notFoundSource, /nofollow=\{\s*true\s*\}/, 'NotFoundPage must specify nofollow={true}');

  const invoicesSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/Invoices.jsx'), 'utf8');
  assert.match(invoicesSource, /noindex=\{\s*true\s*\}/, 'Invoices page must specify noindex={true}');

  const trousseauStudioSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/Trousseau/TrousseauStudioPage.jsx'), 'utf8');
  assert.match(trousseauStudioSource, /noindex=\{\s*true\s*\}/, 'TrousseauStudioPage must specify noindex={true}');

  const cartSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/Cart.jsx'), 'utf8');
  assert.match(cartSource, /noindex=\{\s*true\s*\}/, 'Cart page must specify noindex={true}');

  const myOrdersSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/MyOrders.jsx'), 'utf8');
  assert.match(myOrdersSource, /noindex=\{\s*true\s*\}/, 'MyOrders page must specify noindex={true}');
});

test('Point 7 — Structured Data Product Schema: verified against PDP component code', () => {
  const pdpSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/ProductDetails/ProductDetailPage.jsx'), 'utf8');

  // Verify Product Schema
  assert.match(pdpSource, /"@type":\s*"Product"/, 'Product schema must declare @type: Product');
  assert.match(pdpSource, /"priceCurrency":\s*"INR"/, 'Product schema must use INR currency');
  assert.match(pdpSource, /"availability":\s*\(product\.stockQuantity\s*>\s*0\)/, 'Product availability must depend on real stockQuantity');
  assert.match(pdpSource, /"itemCondition":\s*"https:\/\/schema\.org\/NewCondition"/, 'Item condition must be NewCondition');
  assert.match(pdpSource, /"sku":\s*`SK-\${product\.id}`/, 'SKU must format as SK-{id}');
  assert.match(pdpSource, /reviews\.length\s*>\s*0\s*&&\s*\{[\s\S]*"aggregateRating"/, 'aggregateRating must ONLY be present when reviews.length > 0');
});

test('Point 8 — Structured Data BreadcrumbList: verified against PDP and Catalog code', () => {
  const pdpSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/ProductDetails/ProductDetailPage.jsx'), 'utf8');
  assert.match(pdpSource, /"@type":\s*"BreadcrumbList"/, 'PDP must emit BreadcrumbList schema');
  assert.match(pdpSource, /"name":\s*"Home"/, 'BreadcrumbList must include Home');
  assert.match(pdpSource, /"name":\s*"Catalog"/, 'BreadcrumbList must include Catalog');

  const productsSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/Products/ProductsPage.jsx'), 'utf8');
  assert.match(productsSource, /categoryBreadcrumbSchema/, 'ProductsPage must supply category breadcrumb schema');
  assert.match(productsSource, /hasFacetedFilters/, 'ProductsPage must track hasFacetedFilters');
  assert.match(productsSource, /noindex=\{hasFacetedFilters\}/, 'ProductsPage must set noindex={hasFacetedFilters}');
});

test('Point 9 — Structured Data WebSite & Organization: verified against HomePage code', () => {
  const homeSource = fs.readFileSync(path.join(frontendRoot, 'src/pages/Home/HomePage.jsx'), 'utf8');
  assert.match(homeSource, /"@type":\s*"WebSite"/, 'HomePage must emit WebSite schema');
  assert.match(homeSource, /"@type":\s*"SearchAction"/, 'WebSite must specify SearchAction');
  assert.match(homeSource, /target":\s*`\${getCanonicalUrl\('\/products'\)}\?search=\{search_term_string\}`/, 'SearchAction target must format correctly');
  assert.match(homeSource, /"@type":\s*"Organization"/, 'HomePage must emit Organization schema');
  assert.match(homeSource, /"name":\s*"SareeKart Handlooms"/, 'Organization name must be SareeKart Handlooms');
});

test('Point 10 — Static Robots & Sitemap Assets: public files contain expected production rules', () => {
  const robotsPath = path.join(frontendRoot, 'public/robots.txt');
  const sitemapPath = path.join(frontendRoot, 'public/sitemap.xml');

  assert.ok(fs.existsSync(robotsPath), 'frontend/public/robots.txt must exist');
  assert.ok(fs.existsSync(sitemapPath), 'frontend/public/sitemap.xml must exist');

  const robots = fs.readFileSync(robotsPath, 'utf8');
  assert.match(robots, /User-agent:\s*\*/, 'robots.txt must specify User-agent: *');
  assert.match(robots, /Allow:\s*\/products/, 'robots.txt must allow /products');
  assert.match(robots, /Disallow:\s*\/invoices/, 'robots.txt must disallow /invoices');
  assert.match(robots, /Disallow:\s*\/admin/, 'robots.txt must disallow /admin');
  assert.match(robots, /Disallow:\s*\/checkout/, 'robots.txt must disallow /checkout');
  assert.match(robots, /Disallow:\s*\/trousseau/, 'robots.txt must disallow /trousseau');
  assert.match(robots, /Sitemap:\s*https:\/\/sareekart\.com\/sitemap\.xml/, 'robots.txt must reference sitemap');

  const sitemap = fs.readFileSync(sitemapPath, 'utf8');
  assert.match(sitemap, /<\?xml version="1.0" encoding="UTF-8"\?>/, 'sitemap must have standard xml declaration');
  assert.match(sitemap, /<urlset xmlns="http:\/\/www\.sitemaps\.org\/schemas\/sitemap\/0\.9">/, 'sitemap must have standard namespace');
  assert.match(sitemap, /<loc>https:\/\/sareekart\.com\/<\/loc>/, 'sitemap must include homepage');
  assert.match(sitemap, /<loc>https:\/\/sareekart\.com\/products<\/loc>/, 'sitemap must include /products');
  assert.match(sitemap, /<loc>https:\/\/sareekart\.com\/heritage-weaves<\/loc>/, 'sitemap must include /heritage-weaves');

  // Negative assertion: no private URLs in static sitemap
  assert.ok(!sitemap.includes('/admin'), 'sitemap must not leak /admin');
  assert.ok(!sitemap.includes('/checkout'), 'sitemap must not leak /checkout');
  assert.ok(!sitemap.includes('/invoices'), 'sitemap must not leak /invoices');
});
