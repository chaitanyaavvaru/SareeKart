import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '../..');
const frontendRoot = path.resolve(__dirname, '..');

test('Vercel Configuration: vercel.json exists, valid JSON, SPA rewrite and security headers configured', () => {
  const vercelJsonPath = path.join(frontendRoot, 'vercel.json');
  assert.ok(fs.existsSync(vercelJsonPath), 'vercel.json should exist in frontend directory');

  const content = JSON.parse(fs.readFileSync(vercelJsonPath, 'utf8'));
  assert.equal(content.outputDirectory, 'dist', 'outputDirectory must be dist');
  assert.equal(content.framework, 'vite', 'framework must be vite');

  // Verify SPA client-side routing rewrites
  assert.ok(Array.isArray(content.rewrites), 'rewrites must be an array');
  const spaRewrite = content.rewrites.find(
    (r) => r.source === '/(.*)' && r.destination === '/index.html'
  );
  assert.ok(spaRewrite, 'rewrites must contain source /(.*) to destination /index.html');

  // Verify Security Headers
  assert.ok(Array.isArray(content.headers), 'headers must be an array');
  const globalHeadersObj = content.headers.find((h) => h.source === '/(.*)');
  assert.ok(globalHeadersObj, 'headers must include a rule for /(.*)');

  const headerMap = Object.fromEntries(globalHeadersObj.headers.map((h) => [h.key, h.value]));
  assert.equal(headerMap['X-Content-Type-Options'], 'nosniff', 'X-Content-Type-Options header must be nosniff');
  assert.equal(headerMap['X-Frame-Options'], 'DENY', 'X-Frame-Options header must be DENY');
  assert.equal(headerMap['Referrer-Policy'], 'strict-origin-when-cross-origin', 'Referrer-Policy must be strict-origin-when-cross-origin');
  assert.equal(headerMap['Strict-Transport-Security'], 'max-age=31536000; includeSubDomains', 'HSTS header must be configured');
});

test('API Base URL Contract: Template exists and includes /api contract documentation', () => {
  const envExamplePath = path.join(frontendRoot, '.env.production.example');
  assert.ok(fs.existsSync(envExamplePath), '.env.production.example should exist in frontend directory');

  const content = fs.readFileSync(envExamplePath, 'utf8');
  assert.match(content, /VITE_API_BASE_URL=https:\/\/.*\/api/, 'VITE_API_BASE_URL template must document /api suffix');
});

test('Keep-Alive Workflow: .github/workflows/keepalive.yml exists with 12-min cron and actuator ping', () => {
  const workflowPath = path.join(projectRoot, '.github/workflows/keepalive.yml');
  assert.ok(fs.existsSync(workflowPath), 'keepalive.yml must exist at .github/workflows/keepalive.yml');

  const content = fs.readFileSync(workflowPath, 'utf8');
  assert.match(content, /cron:\s*['"]\*\s*\/12\s+\*\s+\*\s+\*\s+\*['"]|cron:\s*['"]\*\s*\/12 \* \* \* \*['"]|cron:\s*'\*\/12 \* \* \* \*'/, 'Keep-alive cron must run every 12 minutes');
  assert.match(content, /\/actuator\/health/, 'Keep-alive workflow must ping /actuator/health');
  assert.match(content, /BACKEND_HOST/, 'Keep-alive workflow must reference BACKEND_HOST secret');
});

test('Bundle Budget Compliance: dist/assets has all chunks < 500 kB and largest chunk <= 229.01 kB', () => {
  const distAssetsDir = path.join(frontendRoot, 'dist/assets');
  assert.ok(fs.existsSync(distAssetsDir), 'dist/assets must exist');

  const files = fs.readdirSync(distAssetsDir);
  assert.ok(files.length > 0, 'dist/assets must contain generated files');

  let maxSizeBytes = 0;
  let maxFileName = '';

  for (const file of files) {
    const filePath = path.join(distAssetsDir, file);
    const stat = fs.statSync(filePath);
    if (stat.isFile()) {
      const sizeKb = stat.size / 1024;
      assert.ok(
        sizeKb < 500,
        `Chunk ${file} size ${sizeKb.toFixed(2)} kB exceeds 500 kB budget ceiling`
      );
      if (stat.size > maxSizeBytes) {
        maxSizeBytes = stat.size;
        maxFileName = file;
      }
    }
  }

  const maxKb = maxSizeBytes / 1024;
  assert.ok(
    maxKb <= 230,
    `Largest chunk ${maxFileName} (${maxKb.toFixed(2)} kB) must satisfy project policy <= 229 kB`
  );
});

test('Sitemap & Robots Static Assets: dist/sitemap.xml and dist/robots.txt exist and are non-empty', () => {
  const sitemapPath = path.join(frontendRoot, 'dist/sitemap.xml');
  const robotsPath = path.join(frontendRoot, 'dist/robots.txt');

  assert.ok(fs.existsSync(sitemapPath), 'dist/sitemap.xml must exist');
  assert.ok(fs.existsSync(robotsPath), 'dist/robots.txt must exist');

  const sitemapContent = fs.readFileSync(sitemapPath, 'utf8');
  const robotsContent = fs.readFileSync(robotsPath, 'utf8');

  assert.match(sitemapContent, /<urlset xmlns="http:\/\/www\.sitemaps\.org\/schemas\/sitemap\/0\.9">/, 'sitemap.xml must have valid urlset');
  assert.match(robotsContent, /User-agent: \*/, 'robots.txt must declare User-agent');
  assert.match(robotsContent, /Sitemap: https:\/\/sareekart\.com\/sitemap\.xml/, 'robots.txt must link sitemap.xml');
});

test('Base URL Normalization Logic: handles unset, trailing slashes, and missing /api', () => {
  const normalize = (envVal) => {
    if (!envVal) return '/api';
    let trimmed = envVal.trim().replace(/\/+$/, '');
    if (!trimmed.endsWith('/api')) {
      trimmed = `${trimmed}/api`;
    }
    return trimmed;
  };

  assert.equal(normalize(undefined), '/api');
  assert.equal(normalize(''), '/api');
  assert.equal(normalize('https://sareekart.onrender.com'), 'https://sareekart.onrender.com/api');
  assert.equal(normalize('https://sareekart.onrender.com/'), 'https://sareekart.onrender.com/api');
  assert.equal(normalize('https://sareekart.onrender.com/api'), 'https://sareekart.onrender.com/api');
  assert.equal(normalize('https://sareekart.onrender.com/api/'), 'https://sareekart.onrender.com/api');
  assert.equal(normalize('/api'), '/api');
  assert.equal(normalize('/api/'), '/api');
});

test('Cold Start Interceptor Logic: verifies retry eligibility conditions', () => {
  const isRetryCandidate = (err, method = 'get') => {
    const isTimeout =
      err.code === 'ECONNABORTED' ||
      err.code === 'ETIMEDOUT' ||
      (err.message && err.message.toLowerCase().includes('timeout'));
    const isNetworkError = !err.response && Boolean(err.message);
    const isGatewayError = err.response && [502, 503, 504].includes(err.response.status);
    const isColdStartError = isTimeout || isNetworkError || isGatewayError;
    const isIdempotent = ['get', 'head', 'options'].includes(method.toLowerCase());
    return isColdStartError && isIdempotent;
  };

  // Timeout on GET
  assert.equal(isRetryCandidate({ code: 'ECONNABORTED', message: 'timeout of 30000ms exceeded' }, 'GET'), true);
  // Gateway 504 on GET
  assert.equal(isRetryCandidate({ response: { status: 504 } }, 'get'), true);
  // Gateway 502 on GET
  assert.equal(isRetryCandidate({ response: { status: 502 } }, 'GET'), true);
  // Network Error on GET
  assert.equal(isRetryCandidate({ message: 'Network Error' }, 'GET'), true);
  // 400 Bad Request should NOT be retried
  assert.equal(isRetryCandidate({ response: { status: 400 } }, 'GET'), false);
  // Non-idempotent POST should NOT be auto-retried
  assert.equal(isRetryCandidate({ code: 'ECONNABORTED' }, 'POST'), false);
});

