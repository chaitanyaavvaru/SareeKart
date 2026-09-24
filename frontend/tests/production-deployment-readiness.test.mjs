import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.resolve(__dirname, '..');
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..');

test('Point 1 — Production Environment Secret Boundary: .env.production.example contains only VITE_ public variables', () => {
  const envExamplePath = path.join(FRONTEND_ROOT, '.env.production.example');
  assert.ok(fs.existsSync(envExamplePath), '.env.production.example must exist');

  const content = fs.readFileSync(envExamplePath, 'utf-8');
  const lines = content.split('\n');

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    assert.ok(
      trimmed.startsWith('VITE_'),
      `Non-VITE variable found in client template: ${trimmed}`
    );
    assert.doesNotMatch(
      trimmed,
      /password|secret|key_secret|token/i,
      `Secret detected in client template: ${trimmed}`
    );
  }
});

test('Point 2 — Vercel Edge Deployment Blueprint: vercel.json defines build command, output dir, SPA rewrites, and headers', () => {
  const vercelPath = path.join(FRONTEND_ROOT, 'vercel.json');
  assert.ok(fs.existsSync(vercelPath), 'vercel.json must exist');

  const config = JSON.parse(fs.readFileSync(vercelPath, 'utf-8'));
  assert.equal(config.buildCommand, 'npm run build');
  assert.equal(config.outputDirectory, 'dist');
  assert.equal(config.framework, 'vite');

  const spaRewrite = config.rewrites?.find(r => r.source === '/(.*)' && r.destination === '/index.html');
  assert.ok(spaRewrite, 'Must define universal SPA rewrite to /index.html');

  const assetHeader = config.headers?.find(h => h.source === '/assets/(.*)');
  assert.ok(assetHeader, 'Must define static asset caching rule');
});

test('Point 3 — Render Container Blueprint Specification: render.yaml specifies free plan, health checks, and 512MB RAM constraints', () => {
  const renderYamlPath = path.join(REPO_ROOT, 'render.yaml');
  assert.ok(fs.existsSync(renderYamlPath), 'render.yaml must exist');

  const content = fs.readFileSync(renderYamlPath, 'utf-8');
  assert.ok(content.includes('plan: free'), 'Must specify free hosting tier');
  assert.ok(content.includes('healthCheckPath: /actuator/health'), 'Must configure health probe');
  assert.ok(content.includes('-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1'), 'Must configure 512MB memory discipline flags');
  assert.ok(content.includes('SPRING_JPA_HIBERNATE_DDL_AUTO'), 'Must configure hibernate ddl-auto');
  assert.ok(content.includes('NEO4J_ENABLED'), 'Must configure neo4j toggle');
});

test('Point 4 — Authoritative DNS Record Configuration: docs/sareekart_dns_configuration.md specifies exact A and CNAME records', () => {
  const dnsDocPath = path.join(REPO_ROOT, 'docs', 'sareekart_dns_configuration.md');
  assert.ok(fs.existsSync(dnsDocPath), 'docs/sareekart_dns_configuration.md must exist');

  const content = fs.readFileSync(dnsDocPath, 'utf-8');
  assert.ok(content.includes('76.76.21.21'), 'Must specify Vercel Anycast IP 76.76.21.21 for apex domain');
  assert.ok(content.includes('cname.vercel-dns.com'), 'Must specify cname.vercel-dns.com for www subdomain');
  assert.ok(content.includes('sareekart.com'), 'Must specify authoritative domain sareekart.com');
});

test('Point 5 — Database Backup & Integrity Manifest Tools: backup and integrity verification scripts exist', () => {
  const backupScriptPath = path.join(REPO_ROOT, 'scripts', 'backup_db.sh');
  const verifyScriptPath = path.join(REPO_ROOT, 'scripts', 'verify_backup_integrity.sh');
  const schemaScriptPath = path.join(REPO_ROOT, 'scripts', 'verify_tidb_schema.sh');

  assert.ok(fs.existsSync(backupScriptPath), 'scripts/backup_db.sh must exist');
  assert.ok(fs.existsSync(verifyScriptPath), 'scripts/verify_backup_integrity.sh must exist');
  assert.ok(fs.existsSync(schemaScriptPath), 'scripts/verify_tidb_schema.sh must exist');

  const backupContent = fs.readFileSync(backupScriptPath, 'utf-8');
  assert.ok(backupContent.includes('sha256sum') || backupContent.includes('shasum -a 256'), 'Backup script must generate SHA-256 checksum manifest');
});

test('Point 6 — Production Incident Runbook Readiness: docs/PRODUCTION_INCIDENT_RUNBOOK.md exists and covers critical failures', () => {
  const runbookPath = path.join(REPO_ROOT, 'docs', 'PRODUCTION_INCIDENT_RUNBOOK.md');
  assert.ok(fs.existsSync(runbookPath), 'docs/PRODUCTION_INCIDENT_RUNBOOK.md must exist');

  const content = fs.readFileSync(runbookPath, 'utf-8');
  assert.ok(content.includes('Backend Unavailable'), 'Runbook must document backend outage diagnosis');
  assert.ok(content.includes('Database Outage'), 'Runbook must document database outage diagnosis');
  assert.ok(content.includes('Payment Failures'), 'Runbook must document payment failure diagnosis');
  assert.ok(content.includes('WhatsApp'), 'Runbook must document WhatsApp failure diagnosis');
});

test('Point 7 — Rollback Procedure Readiness: rollback architecture documented for frontend, backend, and database', () => {
  const runbookPath = path.join(REPO_ROOT, 'docs', 'PRODUCTION_INCIDENT_RUNBOOK.md');
  assert.ok(fs.existsSync(runbookPath), 'docs/PRODUCTION_INCIDENT_RUNBOOK.md must exist');

  const content = fs.readFileSync(runbookPath, 'utf-8');
  assert.ok(content.includes('Rollback Procedures'), 'Must document rollback procedures');
  assert.ok(content.includes('Vercel Instant Edge Rollback'), 'Must cover frontend rollback');
  assert.ok(content.includes('Backend Container Rollback'), 'Must cover backend rollback');
  assert.ok(content.includes('Database Disaster Recovery & Rollback'), 'Must cover database restore drill');
});

test('Point 8 — Zero Hardcoded Localhost URLs in Production Config: production yaml and render blueprints use env vars', () => {
  const prodYamlPath = path.join(REPO_ROOT, 'backend', 'backend', 'src', 'main', 'resources', 'application-prod.yaml');
  assert.ok(fs.existsSync(prodYamlPath), 'application-prod.yaml must exist');

  const content = fs.readFileSync(prodYamlPath, 'utf-8');
  assert.ok(!content.includes('localhost:3306'), 'Must not hardcode localhost MySQL in production profile');
  assert.ok(!content.includes('localhost:8081'), 'Must not hardcode localhost server port in production profile');
  assert.ok(content.includes('${SPRING_DATASOURCE_URL}'), 'Must bind database URL to environment variable');
});

test('Point 9 — Production Asset Hygiene: .gitignore excludes database dumps and secret files', () => {
  const gitignorePath = path.join(REPO_ROOT, '.gitignore');
  assert.ok(fs.existsSync(gitignorePath), '.gitignore must exist');

  const content = fs.readFileSync(gitignorePath, 'utf-8');
  assert.ok(content.includes('backups/*.sql'), 'Must exclude database sql dumps');
  assert.ok(content.includes('backups/*.sql.sha256'), 'Must exclude checksum files');
  assert.ok(content.includes('.env*'), 'Must exclude .env files');
});

test('Point 10 — Storefront 15-Point Smoke Test Suite: smoke-15-points.test.mjs covers all critical commerce journeys', () => {
  const smokePath = path.join(FRONTEND_ROOT, 'tests', 'smoke-15-points.test.mjs');
  assert.ok(fs.existsSync(smokePath), 'frontend/tests/smoke-15-points.test.mjs must exist');

  const content = fs.readFileSync(smokePath, 'utf-8');
  assert.ok(content.includes('Point 1'), 'Must cover Point 1 Homepage');
  assert.ok(content.includes('Point 2'), 'Must cover Point 2 Catalog');
  assert.ok(content.includes('Point 6'), 'Must cover Point 6 Auth Flow');
  assert.ok(content.includes('Point 8'), 'Must cover Point 8 Cart');
  assert.ok(content.includes('Point 9'), 'Must cover Point 9 Checkout');
  assert.ok(content.includes('Point 15'), 'Must cover Point 15 Neo4j Fallback');
});
