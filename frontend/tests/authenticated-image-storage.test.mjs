import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_DIR = path.resolve(__dirname, '..');

test('Milestone 3: S3 Storage Adapter & Authenticated Media Serving Frontend Suite', async (t) => {

  await t.test('Point 1 — AuthenticatedImage.jsx exists and exports component', () => {
    const compPath = path.resolve(FRONTEND_DIR, 'src/components/common/AuthenticatedImage.jsx');
    assert.ok(fs.existsSync(compPath), 'AuthenticatedImage.jsx must exist');
    const content = fs.readFileSync(compPath, 'utf8');

    assert.ok(content.includes('export default function AuthenticatedImage'), 'Must export AuthenticatedImage as default');
  });

  await t.test('Point 2 — Header-based Bearer authentication (No JWT in URL)', () => {
    const compPath = path.resolve(FRONTEND_DIR, 'src/components/common/AuthenticatedImage.jsx');
    const content = fs.readFileSync(compPath, 'utf8');

    // Verifies axios with responseType blob is used
    assert.ok(content.includes("responseType: 'blob'") || content.includes('responseType: "blob"'),
      'Must request image as blob to preserve Authorization header');

    // Verifies no query-parameter JWT token concatenation
    assert.ok(!content.includes('?token='), 'Must not use query parameter ?token= for authentication');
    assert.ok(!content.includes('&token='), 'Must not use query parameter &token= for authentication');
    assert.ok(!content.includes('?jwt='), 'Must not use query parameter ?jwt= for authentication');
  });

  await t.test('Point 3 — Lifecycle hygiene: AbortController and URL.revokeObjectURL cleanup', () => {
    const compPath = path.resolve(FRONTEND_DIR, 'src/components/common/AuthenticatedImage.jsx');
    const content = fs.readFileSync(compPath, 'utf8');

    // Verifies AbortController usage
    assert.ok(content.includes('new AbortController()'), 'Must initialize AbortController for request cancellation');
    assert.ok(content.includes('abortController.abort()'), 'Must abort in-flight requests on unmount or src change');

    // Verifies Object URL creation and revocation
    assert.ok(content.includes('URL.createObjectURL'), 'Must create Blob Object URL for memory rendering');
    assert.ok(content.includes('URL.revokeObjectURL'), 'Must revoke Object URL in cleanup to prevent memory leaks');
  });

  await t.test('Point 4 — ReturnRequestModal uses AuthenticatedImage for private defect photos', () => {
    const modalPath = path.resolve(FRONTEND_DIR, 'src/components/orders/ReturnRequestModal.jsx');
    assert.ok(fs.existsSync(modalPath), 'ReturnRequestModal.jsx must exist');
    const content = fs.readFileSync(modalPath, 'utf8');

    assert.ok(content.includes('AuthenticatedImage'), 'ReturnRequestModal must import and render AuthenticatedImage');
  });

  await t.test('Point 5 — ReturnStatusDrawer uses AuthenticatedImage for customer review & lightbox', () => {
    const drawerPath = path.resolve(FRONTEND_DIR, 'src/components/orders/ReturnStatusDrawer.jsx');
    assert.ok(fs.existsSync(drawerPath), 'ReturnStatusDrawer.jsx must exist');
    const content = fs.readFileSync(drawerPath, 'utf8');

    assert.ok(content.includes('AuthenticatedImage'), 'ReturnStatusDrawer must import and render AuthenticatedImage');
  });

  await t.test('Point 6 — ManageReturns uses AuthenticatedImage for staff inspection console', () => {
    const adminPath = path.resolve(FRONTEND_DIR, 'src/pages/Admin/ManageReturns.jsx');
    assert.ok(fs.existsSync(adminPath), 'ManageReturns.jsx must exist');
    const content = fs.readFileSync(adminPath, 'utf8');

    assert.ok(content.includes('AuthenticatedImage'), 'ManageReturns must import and render AuthenticatedImage');
  });

  await t.test('Point 7 — Media security hygiene: Zero JWT query parameters in media URLs', () => {
    // Check all return/order/media related frontend files
    const targetDirs = [
      path.resolve(FRONTEND_DIR, 'src/components/orders'),
      path.resolve(FRONTEND_DIR, 'src/components/common'),
      path.resolve(FRONTEND_DIR, 'src/pages/Admin'),
      path.resolve(FRONTEND_DIR, 'src/services')
    ];

    for (const dir of targetDirs) {
      const files = fs.readdirSync(dir);
      for (const file of files) {
        if (file.endsWith('.js') || file.endsWith('.jsx')) {
          const content = fs.readFileSync(path.join(dir, file), 'utf8');
          assert.ok(!content.includes('/uploads/') || (!content.includes('?token=') && !content.includes('&token=')),
            `File ${file} must not append token query parameter to media URLs`);
          assert.ok(!content.includes('/uploads/') || !content.includes('?jwt='),
            `File ${file} must not append jwt query parameter to media URLs`);
        }
      }
    }
  });
});
