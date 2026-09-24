import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.resolve(__dirname, '..');
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..');

test('Point 1 — XSS Sink Audit: dangerouslySetInnerHTML is strictly restricted to static CSS', () => {
  const srcDir = path.join(FRONTEND_ROOT, 'src');
  
  function scanFiles(dir, files = []) {
    for (const item of fs.readdirSync(dir)) {
      const fullPath = path.join(dir, item);
      if (fs.statSync(fullPath).isDirectory()) {
        scanFiles(fullPath, files);
      } else if (/\.(jsx|js)$/.test(item)) {
        files.push(fullPath);
      }
    }
    return files;
  }

  const allFiles = scanFiles(srcDir);
  const sinks = [];

  for (const file of allFiles) {
    const content = fs.readFileSync(file, 'utf-8');
    if (content.includes('dangerouslySetInnerHTML')) {
      const relPath = path.relative(FRONTEND_ROOT, file);
      sinks.push({ file: relPath, content });
    }
  }

  assert.equal(sinks.length, 1, `Expected exactly 1 controlled static usage of dangerouslySetInnerHTML, found: ${sinks.length}`);
  assert.equal(sinks[0].file, path.join('src', 'components', 'NewArrivals.jsx'));
  assert.match(sinks[0].content, /@keyframes marquee/, 'Usage must be pure static CSS keyframes');
  assert.doesNotMatch(sinks[0].content, /props\.|state\.|user\.|input/i, 'Must contain zero user or prop variables');
});

test('Point 2 — Edge Security Headers: vercel.json defines comprehensive security headers', () => {
  const vercelJsonPath = path.join(FRONTEND_ROOT, 'vercel.json');
  assert.ok(fs.existsSync(vercelJsonPath), 'vercel.json must exist');

  const config = JSON.parse(fs.readFileSync(vercelJsonPath, 'utf-8'));
  const allRoutesHeaderRule = config.headers?.find(h => h.source === '/(.*)');
  assert.ok(allRoutesHeaderRule, 'Must define security headers for all routes /(.*)');

  const headerMap = {};
  for (const h of allRoutesHeaderRule.headers) {
    headerMap[h.key] = h.value;
  }

  assert.equal(headerMap['X-Content-Type-Options'], 'nosniff');
  assert.equal(headerMap['X-Frame-Options'], 'DENY');
  assert.equal(headerMap['Referrer-Policy'], 'strict-origin-when-cross-origin');
  assert.ok(headerMap['Strict-Transport-Security']?.includes('max-age=31536000'));
  assert.ok(headerMap['Strict-Transport-Security']?.includes('includeSubDomains'));
  assert.equal(headerMap['X-XSS-Protection'], '1; mode=block');
});

test('Point 3 — Client-Side Secret Scan: frontend src contains zero committed secrets', () => {
  const srcDir = path.join(FRONTEND_ROOT, 'src');
  const sensitivePatterns = [
    /-----BEGIN (RSA|EC|OPENSSH|PGP|PRIVATE) KEY-----/,
    /rzp_live_[a-zA-Z0-9]+/,
    /AIzaSy[a-zA-Z0-9_-]{33}/,
    /EAAB[a-zA-Z0-9]+/,
    /jdbc:mysql:\/\/[a-zA-Z0-9]+:[a-zA-Z0-9]+@/
  ];

  function scanDir(dir) {
    for (const item of fs.readdirSync(dir)) {
      const full = path.join(dir, item);
      if (fs.statSync(full).isDirectory()) {
        scanDir(full);
      } else if (/\.(js|jsx|json|css)$/.test(item)) {
        const text = fs.readFileSync(full, 'utf-8');
        for (const pattern of sensitivePatterns) {
          assert.doesNotMatch(text, pattern, `Potential committed secret found in ${item}`);
        }
      }
    }
  }

  scanDir(srcDir);
});

test('Point 4 — Sensitive Telemetry Redaction: eventTracker redacts PII and credentials', () => {
  const trackerPath = path.join(FRONTEND_ROOT, 'src', 'utils', 'eventTracker.js');
  const code = fs.readFileSync(trackerPath, 'utf-8');

  assert.match(code, /SENSITIVE_KEY_PATTERN/, 'Must define sensitive key redaction pattern');
  assert.match(code, /password/i, 'Must check password');
  assert.match(code, /token/i, 'Must check token');
  assert.match(code, /secret/i, 'Must check secret');
  assert.match(code, /creditcard|cvv|cardnumber/i, 'Must check card data');
  assert.match(code, /\[REDACTED\]/, 'Must replace sensitive values with [REDACTED]');
});

test('Point 5 — Private Route Crawl Protection: seoUtils enforces noindex on private paths', async () => {
  const seoUtilsPath = path.join(FRONTEND_ROOT, 'src', 'utils', 'seoUtils.js');
  const seoUtils = await import(`file://${seoUtilsPath}`);

  assert.equal(typeof seoUtils.isAutoPrivatePath, 'function');
  assert.equal(seoUtils.isAutoPrivatePath('/admin'), true);
  assert.equal(seoUtils.isAutoPrivatePath('/admin/wallets'), true);
  assert.equal(seoUtils.isAutoPrivatePath('/orders'), true);
  assert.equal(seoUtils.isAutoPrivatePath('/checkout'), true);
  assert.equal(seoUtils.isAutoPrivatePath('/cart'), true);
  assert.equal(seoUtils.isAutoPrivatePath('/wallet'), true);
  assert.equal(seoUtils.isAutoPrivatePath('/invoices'), true);
  assert.equal(seoUtils.isAutoPrivatePath('/trousseau'), true);

  assert.equal(seoUtils.isAutoPrivatePath('/'), false);
  assert.equal(seoUtils.isAutoPrivatePath('/products'), false);
  assert.equal(seoUtils.isAutoPrivatePath('/heritage-weaves'), false);
});

test('Point 6 — Auth Token Lifecycle: authSlice purges storage on logout', () => {
  const authSlicePath = path.join(FRONTEND_ROOT, 'src', 'redux', 'slices', 'authSlice.js');
  const code = fs.readFileSync(authSlicePath, 'utf-8');

  assert.match(code, /localStorage\.removeItem\('sareekart_token'\)/, 'Must remove token on logout');
  assert.match(code, /localStorage\.removeItem\('sareekart_user'\)/, 'Must remove user profile on logout');
  assert.match(code, /state\.token\s*=\s*null/, 'Must nullify token state in Redux store');
  assert.match(code, /state\.isAuthenticated\s*=\s*false/, 'Must invalidate authenticated flag');
});

test('Point 7 — Axios Error Boundary Sanitization: suppresses stack traces and exposes requestId', () => {
  const axiosPath = path.join(FRONTEND_ROOT, 'src', 'api', 'axiosConfig.js');
  const code = fs.readFileSync(axiosPath, 'utf-8');

  assert.match(code, /X-Request-ID/, 'Must generate and propagate correlation header');
  assert.match(code, /error\.requestId/, 'Must attach correlation ID for UI error tracking');
});

test('Point 8 — Public Environment Variables: .env.production.example contains only VITE_ public variables', () => {
  const envExamplePath = path.join(FRONTEND_ROOT, '.env.production.example');
  assert.ok(fs.existsSync(envExamplePath));

  const lines = fs.readFileSync(envExamplePath, 'utf-8').split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    assert.ok(trimmed.startsWith('VITE_'), `Non-VITE variable found in client env example: ${trimmed}`);
    assert.doesNotMatch(trimmed, /SECRET|PASSWORD|PRIVATE|DATABASE/, 'Client env must not mention backend secrets');
  }
});

test('Point 9 — Static Robots Crawling Restrictions: robots.txt protects private customer areas', () => {
  const robotsPath = path.join(FRONTEND_ROOT, 'public', 'robots.txt');
  const content = fs.readFileSync(robotsPath, 'utf-8');

  assert.match(content, /Disallow:\s*\/admin/);
  assert.match(content, /Disallow:\s*\/orders/);
  assert.match(content, /Disallow:\s*\/checkout/);
  assert.match(content, /Disallow:\s*\/cart/);
  assert.match(content, /Disallow:\s*\/wallet/);
  assert.match(content, /Disallow:\s*\/invoices/);
  assert.match(content, /Disallow:\s*\/trousseau/);
  assert.match(content, /Disallow:\s*\/api\//);
});

test('Point 10 — Backup Hygiene: .gitignore protects database dumps from version control', () => {
  const gitignorePath = path.join(REPO_ROOT, '.gitignore');
  const content = fs.readFileSync(gitignorePath, 'utf-8');

  assert.match(content, /backups\/\*\.sql/, '.gitignore must protect SQL database dumps');
  assert.match(content, /backups\/\*\.sql\.sha256/, '.gitignore must protect backup checksums');
});
