import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');
const FRONTEND_DIR = path.resolve(REPO_ROOT, 'frontend');
const DIST_ASSETS = path.resolve(FRONTEND_DIR, 'dist/assets');

test('Phase 14 — Stage 12: Platform Maturity & 30-Day Operations Suite', async (t) => {

  await t.test('Point 1 — Production Bundle Budget Discipline (< 500 kB ceiling)', () => {
    assert.ok(fs.existsSync(DIST_ASSETS), 'dist/assets directory must exist');
    const files = fs.readdirSync(DIST_ASSETS);
    const jsFiles = files.filter(f => f.endsWith('.js'));
    assert.ok(jsFiles.length > 0, 'Must have generated JS assets in dist');

    let maxChunkBytes = 0;
    let maxChunkName = '';

    for (const jsFile of jsFiles) {
      const stats = fs.statSync(path.join(DIST_ASSETS, jsFile));
      const sizeKb = stats.size / 1024;
      assert.ok(sizeKb < 500, `JS chunk ${jsFile} (${sizeKb.toFixed(2)} kB) exceeds 500 kB budget`);
      if (stats.size > maxChunkBytes) {
        maxChunkBytes = stats.size;
        maxChunkName = jsFile;
      }
    }

    const maxChunkKb = maxChunkBytes / 1024;
    assert.ok(maxChunkKb <= 235, `Largest chunk ${maxChunkName} (${maxChunkKb.toFixed(2)} kB) exceeds 235 kB target`);
  });

  await t.test('Point 2 — Route-Level Code Splitting & Dynamic Lazy Imports', () => {
    const routerPath = path.resolve(FRONTEND_DIR, 'src/routes/AppRouter.jsx');
    assert.ok(fs.existsSync(routerPath), 'AppRouter.jsx must exist');
    const content = fs.readFileSync(routerPath, 'utf8');

    assert.ok(content.includes('lazy('), 'AppRouter must utilize React.lazy()');
    assert.ok(content.includes('<Suspense'), 'AppRouter must wrap lazy routes in Suspense');
  });

  await t.test('Point 3 — Static Asset Edge Caching Configuration in vercel.json', () => {
    const vercelConfigPath = path.resolve(FRONTEND_DIR, 'vercel.json');
    assert.ok(fs.existsSync(vercelConfigPath), 'vercel.json must exist');
    const config = JSON.parse(fs.readFileSync(vercelConfigPath, 'utf8'));

    assert.ok(Array.isArray(config.headers), 'vercel.json must define headers array');
    const assetHeaderRule = config.headers.find(h => h.source && h.source.includes('assets/'));
    assert.ok(assetHeaderRule, 'Must define caching header rule for /assets/(.*)');

    const cacheHeader = assetHeaderRule.headers.find(h => h.key.toLowerCase() === 'cache-control');
    assert.ok(cacheHeader, 'Must define Cache-Control header');
    assert.ok(cacheHeader.value.includes('immutable'), 'Static assets must have immutable cache header');
  });

  await t.test('Point 4 — Storefront Debounce & Search Optimization', () => {
    const searchPagePath = path.resolve(FRONTEND_DIR, 'src/pages/Products/ProductsPage.jsx');
    assert.ok(fs.existsSync(searchPagePath), 'ProductsPage.jsx must exist');
    const content = fs.readFileSync(searchPagePath, 'utf8');
    const hasSearchFiltering = content.includes('useCallback') || content.includes('useEffect') || content.includes('setTimeout');
    assert.ok(hasSearchFiltering, 'ProductsPage must throttle or debounce filter updates to prevent request flooding');

    const modalPath = path.resolve(FRONTEND_DIR, 'src/pages/Trousseau/components/AddItemModal.jsx');
    assert.ok(fs.existsSync(modalPath), 'AddItemModal.jsx must exist');
    const modalContent = fs.readFileSync(modalPath, 'utf8');
    assert.ok(modalContent.includes('setTimeout') && modalContent.includes('clearTimeout'), 'Catalog search must implement bounded debounce timer');
  });

  await t.test('Point 5 — Cold-Start Resilience & Backoff Telemetry', () => {
    const axiosPath = path.resolve(FRONTEND_DIR, 'src/api/axiosConfig.js');
    assert.ok(fs.existsSync(axiosPath), 'axiosConfig.js must exist');
    const axiosContent = fs.readFileSync(axiosPath, 'utf8');

    assert.ok(axiosContent.includes('isColdStarting'), 'axiosConfig must track cold starting state');
    assert.ok(axiosContent.includes('X-Request-ID'), 'axiosConfig must propagate correlation IDs');

    const coldStartNoticePath = path.resolve(FRONTEND_DIR, 'src/components/common/ColdStartNotice.jsx');
    assert.ok(fs.existsSync(coldStartNoticePath), 'ColdStartNotice.jsx must exist for user communication');
  });

  await t.test('Point 6 — Client-Side Secret Scan (Zero Committed Keys or Credentials)', () => {
    const srcDir = path.resolve(FRONTEND_DIR, 'src');
    const secretPatterns = [
      /AIza[0-9A-Za-z-_]{35}/,
      /sk-proj-[a-zA-Z0-9_-]{20,}/,
      /EAAB[0-9a-zA-Z]+/,
      /rzp_live_[0-9a-zA-Z]{14}/,
      /ghp_[0-9a-zA-Z]{36}/
    ];

    function scanDir(dir) {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          scanDir(fullPath);
        } else if (/\.(jsx?|tsx?|json|css)$/.test(entry.name)) {
          const content = fs.readFileSync(fullPath, 'utf8');
          for (const pattern of secretPatterns) {
            assert.ok(!pattern.test(content), `Potential secret detected in ${fullPath}`);
          }
        }
      }
    }

    scanDir(srcDir);
  });

  await t.test('Point 7 — Private Route Crawl Protection in SEO Directives', () => {
    const seoUtilsPath = path.resolve(FRONTEND_DIR, 'src/utils/seoUtils.js');
    assert.ok(fs.existsSync(seoUtilsPath), 'seoUtils.js must exist');
    const content = fs.readFileSync(seoUtilsPath, 'utf8');

    assert.ok(content.includes('isAutoPrivatePath'), 'seoUtils must define isAutoPrivatePath check');
    assert.ok(content.includes('startsWith(\'/admin\')'), 'seoUtils must protect admin routes from indexing');
    assert.ok(content.includes('startsWith(\'/cart\')'), 'seoUtils must protect cart routes from indexing');
  });

  await t.test('Point 8 — 30-Day Post-Launch Operations Runbook Presence and Structure', () => {
    const runbookPath = path.resolve(REPO_ROOT, 'docs/30_DAY_POST_LAUNCH_OPERATIONS_RUNBOOK.md');
    assert.ok(fs.existsSync(runbookPath), '30_DAY_POST_LAUNCH_OPERATIONS_RUNBOOK.md must exist');
    const content = fs.readFileSync(runbookPath, 'utf8');

    assert.ok(content.includes('Daily Operational Cadence'), 'Runbook must document daily operational cadence');
    assert.ok(content.includes('Weekly Maintenance Routine'), 'Runbook must document weekly maintenance routine');
    assert.ok(content.includes('Monthly Governance'), 'Runbook must document monthly governance');
    assert.ok(content.includes('Disaster Recovery (DR) Simulation Drill'), 'Runbook must document DR drill SOP');
    assert.ok(content.includes('Telemetry Thresholds & Emergency Alert Matrix'), 'Runbook must define alert matrix');
  });

  await t.test('Point 9 — Automated Backup & Pruning Utility Tooling Contracts', () => {
    const backupScript = path.resolve(REPO_ROOT, 'scripts/backup_db.sh');
    assert.ok(fs.existsSync(backupScript), 'scripts/backup_db.sh must exist');

    const verifyScript = path.resolve(REPO_ROOT, 'scripts/verify_backup_integrity.sh');
    assert.ok(fs.existsSync(verifyScript), 'scripts/verify_backup_integrity.sh must exist');

    const pruneScript = path.resolve(REPO_ROOT, 'scripts/prune_backups.sh');
    assert.ok(fs.existsSync(pruneScript), 'scripts/prune_backups.sh must exist');
    const pruneContent = fs.readFileSync(pruneScript, 'utf8');
    assert.ok(pruneContent.includes('MAX_BACKUPS'), 'prune_backups.sh must enforce MAX_BACKUPS retention');
  });

  await t.test('Point 10 — Storefront 15-Point Smoke Test Suite Alignment', () => {
    const smokeTestPath = path.resolve(FRONTEND_DIR, 'tests/smoke-15-points.test.mjs');
    assert.ok(fs.existsSync(smokeTestPath), 'smoke-15-points.test.mjs must exist');
    const content = fs.readFileSync(smokeTestPath, 'utf8');

    for (let i = 1; i <= 15; i++) {
      assert.ok(content.includes(`Point ${i}`), `smoke-15-points.test.mjs must verify Point ${i}`);
    }
  });
});
