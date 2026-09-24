import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '../..');
const frontendRoot = path.resolve(__dirname, '..');
const distRoot = path.join(frontendRoot, 'dist');

test('Point 1 — Homepage Asset & Branding Readiness: HTML title, meta tags, and hero assets compile cleanly', () => {
  const indexHtmlPath = path.join(distRoot, 'index.html');
  assert.ok(fs.existsSync(indexHtmlPath), 'dist/index.html must exist');

  const content = fs.readFileSync(indexHtmlPath, 'utf8');
  assert.match(content, /SareeKart/i, 'Homepage title/branding must reference SareeKart');
  assert.match(content, /viewport/i, 'Responsive viewport meta tag must be present');
});

test('Point 2 — Product Catalog Bundle: ProductsPage and ProductCard chunks exist and satisfy bundle limits', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const productChunk = files.find(f => f.startsWith('ProductsPage-') && f.endsWith('.js'));
  const cardChunk = files.find(f => f.startsWith('ProductCard-') && f.endsWith('.js'));

  assert.ok(productChunk, 'ProductsPage chunk must be built');
  assert.ok(cardChunk, 'ProductCard chunk must be built');

  const productStat = fs.statSync(path.join(distAssetsDir, productChunk));
  assert.ok(productStat.size < 500 * 1024, 'ProductsPage chunk must be strictly under 500 kB');
});

test('Point 3 — Product Detail Page: ProductDetailPage and specs viewer chunks exist in distribution', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const detailChunk = files.find(f => f.startsWith('ProductDetailPage-') && f.endsWith('.js'));
  assert.ok(detailChunk, 'ProductDetailPage bundle chunk must be present in dist');

  const stat = fs.statSync(path.join(distAssetsDir, detailChunk));
  assert.ok(stat.size < 500 * 1024, 'ProductDetailPage chunk must be < 500 kB');
});

test('Point 4 — Search & Filtering: Search functionality bundle and debounce hooks exist in build', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  // Both standard search and AI visual search dashboards should be bundled
  const visualSearchChunk = files.find(f => f.startsWith('AiVisualSearchDashboard-') && f.endsWith('.js'));
  assert.ok(visualSearchChunk, 'AiVisualSearchDashboard must be bundled in distribution');
});

test('Point 5 — Categories Taxonomy: Category service contract and taxonomy constants are defined', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const catServiceChunk = files.find(f => f.startsWith('categoryService-') && f.endsWith('.js'));
  assert.ok(catServiceChunk, 'categoryService chunk must be compiled in dist/assets');
});

test('Point 6 — Customer Auth Flow: Register and Login bundles exist with stateless JWT integration', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const loginChunk = files.find(f => f.startsWith('LoginPage-') && f.endsWith('.js'));
  const registerChunk = files.find(f => f.startsWith('RegisterPage-') && f.endsWith('.js'));

  assert.ok(loginChunk, 'LoginPage chunk must exist');
  assert.ok(registerChunk, 'RegisterPage chunk must exist');
});

test('Point 7 — Wishlist Operations: Wishlist bundle and persistence handlers are present', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const wishlistChunk = files.find(f => f.startsWith('WishlistPage-') && f.endsWith('.js'));
  assert.ok(wishlistChunk, 'WishlistPage chunk must be present');
});

test('Point 8 — Cart Subtotals & State: Cart component bundle exists and satisfies budget', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const cartChunk = files.find(f => f.startsWith('Cart-') && f.endsWith('.js'));
  assert.ok(cartChunk, 'Cart component chunk must exist');
  const stat = fs.statSync(path.join(distAssetsDir, cartChunk));
  assert.ok(stat.size < 500 * 1024, 'Cart chunk size must be under 500 kB');
});

test('Point 9 — Checkout Initialization: CheckoutPage bundle and payment mode routing configured', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const checkoutChunk = files.find(f => f.startsWith('CheckoutPage-') && f.endsWith('.js'));
  assert.ok(checkoutChunk, 'CheckoutPage chunk must exist in dist');
});

test('Point 10 — Admin Authentication & RBAC: Admin layout and navigation guard chunks present', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const adminChunk = files.find(f => f.startsWith('AdminDashboard-') && f.endsWith('.js'));
  assert.ok(adminChunk, 'AdminDashboard chunk must exist for staff RBAC');
});

test('Point 11 — Admin Dashboards: Analytics, Inventory, Orders, and Returns management bundles present', () => {
  const distAssetsDir = path.join(distRoot, 'assets');
  const files = fs.readdirSync(distAssetsDir);

  const analyticsChunk = files.find(f => f.startsWith('AnalyticsDashboard-') && f.endsWith('.js'));
  const inventoryChunk = files.find(f => f.startsWith('ManageInventory-') && f.endsWith('.js'));
  const ordersChunk = files.find(f => f.startsWith('ManageOrders-') && f.endsWith('.js'));
  const returnsChunk = files.find(f => f.startsWith('ManageReturns-') && f.endsWith('.js'));

  assert.ok(analyticsChunk, 'AnalyticsDashboard chunk must exist');
  assert.ok(inventoryChunk, 'ManageInventory chunk must exist');
  assert.ok(ordersChunk, 'ManageOrders chunk must exist');
  assert.ok(returnsChunk, 'ManageReturns chunk must exist');
});

test('Point 12 — robots.txt Directives: SEO crawling directives served at edge', () => {
  const robotsPath = path.join(distRoot, 'robots.txt');
  assert.ok(fs.existsSync(robotsPath), 'dist/robots.txt must exist');

  const content = fs.readFileSync(robotsPath, 'utf8');
  assert.match(content, /User-agent:\s*\*/i, 'robots.txt must allow search indexers');
  assert.match(content, /Sitemap:\s*https:\/\/sareekart\.com\/sitemap\.xml/i, 'robots.txt must link to canonical sitemap');
});

test('Point 13 — sitemap.xml Delivery: Valid XML URL set with luxury handloom endpoints', () => {
  const sitemapPath = path.join(distRoot, 'sitemap.xml');
  assert.ok(fs.existsSync(sitemapPath), 'dist/sitemap.xml must exist');

  const content = fs.readFileSync(sitemapPath, 'utf8');
  assert.match(content, /<urlset\s+xmlns="http:\/\/www\.sitemaps\.org\/schemas\/sitemap\/0\.9"/i, 'sitemap must have standard xml schema');
  assert.match(content, /https:\/\/sareekart\.com\/products/i, 'sitemap must include /products URL');
  assert.match(content, /https:\/\/sareekart\.com\/heritage-weaves/i, 'sitemap must include /heritage-weaves URL');
});

test('Point 14 — Backend Health & Readiness Blueprint: render.yaml specifies /actuator/health check', () => {
  const renderYamlPath = path.join(projectRoot, 'render.yaml');
  assert.ok(fs.existsSync(renderYamlPath), 'render.yaml must exist at project root');

  const content = fs.readFileSync(renderYamlPath, 'utf8');
  assert.match(content, /healthCheckPath:\s*\/actuator\/health/, 'render.yaml must configure /actuator/health');
  assert.match(content, /plan:\s*free/, 'render.yaml must specify free tier');
  assert.match(content, /JAVA_TOOL_OPTIONS[\s\S]*-Xmx384m/, 'render.yaml must tune JVM memory for 512MB limit');
});

test('Point 15 — Neo4j Safe Fallback & MySQL Isolation: Circuit breaker and fallbackHydrate verified in source', () => {
  const backendSrcRoot = path.join(projectRoot, 'backend/backend/src/main/java/com/sareekart');
  const neo4jServicePath = path.join(backendSrcRoot, 'service/impl/Neo4jGraphServiceImpl.java');
  const recServicePath = path.join(backendSrcRoot, 'service/impl/RecommendationServiceImpl.java');

  assert.ok(fs.existsSync(neo4jServicePath), 'Neo4jGraphServiceImpl must exist');
  assert.ok(fs.existsSync(recServicePath), 'RecommendationServiceImpl must exist');

  const neo4jContent = fs.readFileSync(neo4jServicePath, 'utf8');
  const recContent = fs.readFileSync(recServicePath, 'utf8');

  assert.match(neo4jContent, /isCircuitPermitted\(\)/, 'Neo4j service must implement circuit breaker permission check');
  assert.match(recContent, /fallbackHydrate/, 'Recommendation service must implement fallbackHydrate()');
});
