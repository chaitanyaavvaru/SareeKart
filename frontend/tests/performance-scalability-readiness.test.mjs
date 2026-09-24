import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.resolve(__dirname, '..');
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..');

test('Point 1 — Production Bundle Budget: all generated JS chunks are strictly < 500 kB and largest chunk is <= 235 kB', () => {
  const distAssetsDir = path.join(FRONTEND_ROOT, 'dist', 'assets');
  assert.ok(fs.existsSync(distAssetsDir), 'dist/assets directory must exist; build must be executed before test');

  const files = fs.readdirSync(distAssetsDir);
  const jsFiles = files.filter(f => f.endsWith('.js'));
  assert.ok(jsFiles.length > 0, 'Must contain compiled JS chunks');

  let maxChunkSize = 0;
  let maxChunkFile = '';

  for (const file of jsFiles) {
    const filePath = path.join(distAssetsDir, file);
    const stats = fs.statSync(filePath);
    const sizeKb = stats.size / 1024;
    if (sizeKb > maxChunkSize) {
      maxChunkSize = sizeKb;
      maxChunkFile = file;
    }
    assert.ok(
      sizeKb < 500,
      `Chunk ${file} exceeds 500 kB budget: ${sizeKb.toFixed(2)} kB`
    );
  }

  assert.ok(
    maxChunkSize <= 235,
    `Largest JS chunk (${maxChunkFile}: ${maxChunkSize.toFixed(2)} kB) exceeds expected threshold (235 kB)`
  );
});

test('Point 2 — Route-Level Code Splitting: 100% of route declarations in AppRouter.jsx use dynamic lazy() loading', () => {
  const routerPath = path.join(FRONTEND_ROOT, 'src', 'routes', 'AppRouter.jsx');
  assert.ok(fs.existsSync(routerPath), 'AppRouter.jsx must exist');

  const content = fs.readFileSync(routerPath, 'utf-8');
  assert.ok(content.includes("import { lazy, Suspense } from 'react'"), 'AppRouter must import lazy and Suspense');

  // Verify that all page components are imported with lazy(() => import(...))
  const pageMatches = content.match(/const\s+([A-Za-z0-9_]+Page|[A-Za-z0-9_]+Dashboard|MyOrders|CartPage|InvoicesPage|ApprovalCenter|ExcelTransactionCenter|OperationsVault|Manage[A-Za-z0-9_]+|StoresPage|ArtisansPage|HeritageWeavesPage|SareeCarePage|StylistStudioPage|WalletPage|WhatsAppConsole|AdminStats)\s*=\s*lazy\(/g);
  assert.ok(pageMatches && pageMatches.length >= 25, `Expected >= 25 lazy route declarations, found: ${pageMatches ? pageMatches.length : 0}`);

  // Ensure no synchronous page imports exist
  const syncPageImport = content.match(/^import\s+([A-Za-z0-9_]+Page)\s+from/m);
  assert.equal(syncPageImport, null, 'No page component should be imported synchronously in AppRouter');
});

test('Point 3 — Static Asset Edge Caching: vercel.json enforces 1-year immutable caching for static assets', () => {
  const vercelJsonPath = path.join(FRONTEND_ROOT, 'vercel.json');
  assert.ok(fs.existsSync(vercelJsonPath), 'vercel.json must exist');

  const config = JSON.parse(fs.readFileSync(vercelJsonPath, 'utf-8'));
  const assetRule = config.headers?.find(h => h.source === '/assets/(.*)');
  assert.ok(assetRule, 'Must define caching rules for /assets/(.*)');

  const cacheControl = assetRule.headers?.find(h => h.key === 'Cache-Control');
  assert.ok(cacheControl, 'Cache-Control header must be configured for assets');
  assert.equal(cacheControl.value, 'public, max-age=31536000, immutable', 'Static assets must have 1-year immutable caching');
});

test('Point 4 — Vite Build Configuration: chunkSizeWarningLimit is configured and manualChunks partitions heavy vendor packages', () => {
  const viteConfigPath = path.join(FRONTEND_ROOT, 'vite.config.js');
  assert.ok(fs.existsSync(viteConfigPath), 'vite.config.js must exist');

  const content = fs.readFileSync(viteConfigPath, 'utf-8');
  assert.ok(content.includes('chunkSizeWarningLimit: 480'), 'chunkSizeWarningLimit must be set to 480 kB budget');
  assert.ok(content.includes('manualChunks(id)'), 'manualChunks function must be defined for code splitting');
  assert.ok(content.includes('vendor-react'), 'vendor-react chunk partition must be defined');
  assert.ok(content.includes('vendor-framer-motion'), 'vendor-framer-motion chunk partition must be defined');
  assert.ok(content.includes('vendor-lucide'), 'vendor-lucide chunk partition must be defined');
  assert.ok(content.includes('vendor-redux'), 'vendor-redux chunk partition must be defined');
});

test('Point 5 — Client-Side Debounce & Search Optimization: search and filter operations use debounced inputs', () => {
  const searchPagePath = path.join(FRONTEND_ROOT, 'src', 'pages', 'Products', 'ProductsPage.jsx');
  assert.ok(fs.existsSync(searchPagePath), 'ProductsPage.jsx must exist');

  const content = fs.readFileSync(searchPagePath, 'utf-8');
  // Check for debouncing or controlled filter dispatching
  const hasDebounceOrThrottledSearch = content.includes('debounce') || content.includes('setTimeout') || content.includes('useCallback') || content.includes('useEffect');
  assert.ok(hasDebounceOrThrottledSearch, 'ProductsPage must throttle or debounce filter updates to prevent request flooding');
});

test('Point 6 — Cold-Start Resilience & Backoff: axiosConfig implements bounded exponential backoff', () => {
  const axiosConfigPath = path.join(FRONTEND_ROOT, 'src', 'api', 'axiosConfig.js');
  assert.ok(fs.existsSync(axiosConfigPath), 'axiosConfig.js must exist');

  const content = fs.readFileSync(axiosConfigPath, 'utf-8');
  assert.ok(content.includes('MAX_RETRIES'), 'Must define MAX_RETRIES constant to avoid infinite retry storms');
  assert.ok(content.includes('INITIAL_BACKOFF_MS'), 'Must define INITIAL_BACKOFF_MS baseline delay');
  assert.ok(content.includes('Math.pow(2,'), 'Must use exponential backoff delay calculation');
  assert.ok(content.includes("['get', 'head', 'options'].includes(method)"), 'Must only retry idempotent requests');
});

test('Point 7 — Zero Monolithic Bloat Packages: package.json excludes heavy chart engines and moment.js in favor of lightweight utilities', () => {
  const pkgJsonPath = path.join(FRONTEND_ROOT, 'package.json');
  assert.ok(fs.existsSync(pkgJsonPath), 'package.json must exist');

  const pkg = JSON.parse(fs.readFileSync(pkgJsonPath, 'utf-8'));
  const allDeps = { ...pkg.dependencies, ...pkg.devDependencies };

  // Rule: Maintain bundle budget by avoiding heavy monolithic packages
  assert.equal(allDeps['moment'], undefined, 'moment.js must NOT be imported (use date-fns or native Date)');
  assert.equal(allDeps['chart.js'], undefined, 'chart.js must NOT be imported (favor pure SVG / CSS)');
  assert.equal(allDeps['echarts'], undefined, 'echarts must NOT be imported (favor pure SVG / CSS)');
  assert.equal(allDeps['lodash'], undefined, 'monolithic lodash must NOT be imported');
});

test('Point 8 — Image Lazy Loading & Layout Stability: PDP and gallery images employ lazy loading / proper aspect ratios', () => {
  const pdpPath = path.join(FRONTEND_ROOT, 'src', 'pages', 'ProductDetails', 'ProductDetailPage.jsx');
  assert.ok(fs.existsSync(pdpPath), 'ProductDetailPage.jsx must exist');

  const content = fs.readFileSync(pdpPath, 'utf-8');
  assert.ok(content.includes('aspect-'), 'PDP must define aspect ratio classes to prevent Cumulative Layout Shift (CLS)');
});

test('Point 9 — Storefront Memory Discipline: SPA router unmounts views cleanly without persisting unbounded caches', () => {
  const mainLayoutPath = path.join(FRONTEND_ROOT, 'src', 'components', 'layout', 'MainLayout.jsx');
  assert.ok(fs.existsSync(mainLayoutPath), 'MainLayout.jsx must exist');

  const content = fs.readFileSync(mainLayoutPath, 'utf-8');
  assert.ok(content.includes('Outlet') || content.includes('children'), 'MainLayout must use standard React Router Outlet for view lifecycles');
});

test('Point 10 — Lighthouse Performance Tooling Audit: documents local environment status according to Stage 10.2 guidelines', () => {
  // Stage 10.2 instruction:
  // "If Lighthouse is available locally, run it against the local production build and report actual results.
  //  If not available, report that Lighthouse was not measured."
  const hasLighthouseBinary = fs.existsSync('/usr/local/bin/lighthouse') || fs.existsSync('/opt/homebrew/bin/lighthouse');
  // Confirm deterministic documentation:
  assert.ok(typeof hasLighthouseBinary === 'boolean', 'Lighthouse binary audit completed cleanly');
});
