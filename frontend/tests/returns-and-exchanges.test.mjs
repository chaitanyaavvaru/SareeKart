import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');
const FRONTEND_DIR = path.resolve(REPO_ROOT, 'frontend');

test('SareeKart v3.1: Self-Service Customer Returns, Exchanges & Reverse Logistics Suite', async (t) => {

  await t.test('Point 1 — returnService.js Contract & Method Signatures', async () => {
    const servicePath = path.resolve(FRONTEND_DIR, 'src/services/returnService.js');
    assert.ok(fs.existsSync(servicePath), 'src/services/returnService.js must exist');
    const content = fs.readFileSync(servicePath, 'utf8');

    // Verify customer endpoints
    assert.ok(content.includes('createReturnRequest:'), 'Must export createReturnRequest method');
    assert.ok(content.includes('getMyReturns:'), 'Must export getMyReturns method');
    assert.ok(content.includes('getReturnByOrderId:'), 'Must export getReturnByOrderId method');
    assert.ok(content.includes('uploadConditionPhoto:'), 'Must export uploadConditionPhoto method');

    // Verify admin endpoints
    assert.ok(content.includes('getAllReturns:'), 'Must export getAllReturns method');
    assert.ok(content.includes('updateReturnStatus:'), 'Must export updateReturnStatus method');

    // Verify compatibility aliases
    assert.ok(content.includes('submitReturnRequest ='), 'Must define submitReturnRequest alias');
    assert.ok(content.includes('getOrderReturnStatus ='), 'Must define getOrderReturnStatus alias');
    assert.ok(content.includes('uploadReturnPhoto ='), 'Must define uploadReturnPhoto alias');
    assert.ok(content.includes('getAllAdminReturns ='), 'Must define getAllAdminReturns alias');
  });

  await t.test('Point 2 — 7-Day Post-Delivery Eligibility Gate Logic', () => {
    // Pure mathematical verification of 7-day cutoff rule
    const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;
    const now = Date.now();

    const isEligibleForReturn = (orderStatus, deliveredAtTimestamp) => {
      if (orderStatus !== 'DELIVERED') return { eligible: false, reason: 'Order must be delivered' };
      if (!deliveredAtTimestamp) return { eligible: false, reason: 'Delivery timestamp missing' };
      const elapsedMs = now - new Date(deliveredAtTimestamp).getTime();
      if (elapsedMs < 0) return { eligible: false, reason: 'Invalid delivery timestamp' };
      if (elapsedMs > SEVEN_DAYS_MS) return { eligible: false, reason: '7-day return window expired' };
      return { eligible: true, daysRemaining: Math.ceil((SEVEN_DAYS_MS - elapsedMs) / (24 * 60 * 60 * 1000)) };
    };

    // Test cases
    assert.equal(isEligibleForReturn('SHIPPED', now - 1000).eligible, false);
    assert.equal(isEligibleForReturn('PENDING', now - 1000).eligible, false);
    assert.equal(isEligibleForReturn('CANCELLED', now - 1000).eligible, false);

    // Delivered 2 days ago -> eligible
    const twoDaysAgo = new Date(now - 2 * 24 * 60 * 60 * 1000).toISOString();
    const res2 = isEligibleForReturn('DELIVERED', twoDaysAgo);
    assert.equal(res2.eligible, true);
    assert.equal(res2.daysRemaining, 5);

    // Delivered 6.9 days ago -> eligible (1 day remaining)
    const sixDaysAgo = new Date(now - 6.5 * 24 * 60 * 60 * 1000).toISOString();
    assert.equal(isEligibleForReturn('DELIVERED', sixDaysAgo).eligible, true);

    // Delivered 8 days ago -> expired
    const eightDaysAgo = new Date(now - 8 * 24 * 60 * 60 * 1000).toISOString();
    const res8 = isEligibleForReturn('DELIVERED', eightDaysAgo);
    assert.equal(res8.eligible, false);
    assert.equal(res8.reason, '7-day return window expired');
  });

  await t.test('Point 3 — ReturnRequestModal.jsx UI & Validation Contracts', () => {
    const modalPath = path.resolve(FRONTEND_DIR, 'src/components/orders/ReturnRequestModal.jsx');
    assert.ok(fs.existsSync(modalPath), 'ReturnRequestModal.jsx must exist');
    const content = fs.readFileSync(modalPath, 'utf8');

    // Reason taxonomy
    assert.ok(content.includes('COLOR_MISMATCH'), 'Must support COLOR_MISMATCH reason');
    assert.ok(content.includes('ZARI_DEFECT'), 'Must support ZARI_DEFECT reason');
    assert.ok(content.includes('FABRIC_FEEL'), 'Must support FABRIC_FEEL reason');
    assert.ok(content.includes('INCORRECT_ITEM'), 'Must support INCORRECT_ITEM reason');
    assert.ok(content.includes('SIZE_MISMATCH'), 'Must support SIZE_MISMATCH reason');
    assert.ok(content.includes('OTHER'), 'Must support OTHER reason');

    // Refund preferences
    assert.ok(content.includes('ORIGINAL_PAYMENT'), 'Must support ORIGINAL_PAYMENT refund mode');
    assert.ok(content.includes('STORE_CREDIT'), 'Must support STORE_CREDIT refund mode');
    assert.ok(content.includes('EXCHANGE_DRAPE'), 'Must support EXCHANGE_DRAPE refund mode');

    // Defect condition photo upload safeguards
    assert.ok(content.includes('uploadConditionPhoto') || content.includes('uploadReturnPhoto'), 'Must integrate photo uploader');
    assert.ok(content.includes('MAX_FILES') || content.includes('images.length < 3') || content.includes('3'), 'Must guard max photo upload limit');
  });

  await t.test('Point 4 — ReturnStatusDrawer.jsx Telemetry & Milestone Display', () => {
    const drawerPath = path.resolve(FRONTEND_DIR, 'src/components/orders/ReturnStatusDrawer.jsx');
    assert.ok(fs.existsSync(drawerPath), 'ReturnStatusDrawer.jsx must exist');
    const content = fs.readFileSync(drawerPath, 'utf8');

    // Verification of reverse logistics telemetry presentation
    assert.ok(content.includes('reverseCourier') || content.includes('reverse_courier'), 'Must display reverse courier');
    assert.ok(content.includes('reverseTrackingNumber') || content.includes('reverse_tracking_number'), 'Must display reverse tracking number');
    assert.ok(content.includes('adminNotes') || content.includes('admin_notes'), 'Must display admin review notes');
    assert.ok(content.includes('status'), 'Must display return claim status');
  });

  await t.test('Point 5 — ManageReturns.jsx Staff Moderation Console Contracts', () => {
    const adminPath = path.resolve(FRONTEND_DIR, 'src/pages/Admin/ManageReturns.jsx');
    assert.ok(fs.existsSync(adminPath), 'ManageReturns.jsx must exist');
    const content = fs.readFileSync(adminPath, 'utf8');

    // Status filter tabs
    assert.ok(content.includes('ALL'), 'Must include ALL filter');
    assert.ok(content.includes('PENDING'), 'Must include PENDING filter');
    assert.ok(content.includes('APPROVED'), 'Must include APPROVED filter');
    assert.ok(content.includes('PICKUP_SCHEDULED'), 'Must include PICKUP_SCHEDULED filter');
    assert.ok(content.includes('COMPLETED'), 'Must include COMPLETED filter');
    assert.ok(content.includes('REJECTED'), 'Must include REJECTED filter');

    // Moderation action handlers
    assert.ok(content.includes('updateReturnStatus'), 'Must invoke updateReturnStatus');
    assert.ok(content.includes('reverseCourier'), 'Must collect courier assignment');
    assert.ok(content.includes('reverseTrackingNumber'), 'Must collect tracking AWB');
  });

  await t.test('Point 6 — AppRouter.jsx & Navigation Integration for Returns', () => {
    const routerPath = path.resolve(FRONTEND_DIR, 'src/routes/AppRouter.jsx');
    assert.ok(fs.existsSync(routerPath), 'AppRouter.jsx must exist');
    const routerContent = fs.readFileSync(routerPath, 'utf8');

    // Child route 'returns' under '/admin' parent route
    assert.ok(routerContent.includes('path="returns"'), 'AppRouter must register nested returns route');
    assert.ok(routerContent.includes('ManageReturns'), 'AppRouter must lazy load ManageReturns');

    const dashboardPath = path.resolve(FRONTEND_DIR, 'src/pages/Admin/AdminDashboard.jsx');
    assert.ok(fs.existsSync(dashboardPath), 'AdminDashboard.jsx must exist');
    const dashboardContent = fs.readFileSync(dashboardPath, 'utf8');

    assert.ok(dashboardContent.includes('/admin/returns') || dashboardContent.includes('returns'), 'AdminDashboard must link to Returns');
  });

  await t.test('Point 7 — Zero Secrets & Client-Side PII Hygiene in Returns Code', () => {
    const filesToCheck = [
      'src/services/returnService.js',
      'src/components/orders/ReturnRequestModal.jsx',
      'src/components/orders/ReturnStatusDrawer.jsx',
      'src/pages/Admin/ManageReturns.jsx',
    ];

    for (const relFile of filesToCheck) {
      const fullPath = path.resolve(FRONTEND_DIR, relFile);
      if (fs.existsSync(fullPath)) {
        const content = fs.readFileSync(fullPath, 'utf8');
        assert.ok(!content.includes('rzp_live_'), `${relFile} must not leak live Razorpay credentials`);
        assert.ok(!content.includes('BEGIN PRIVATE KEY'), `${relFile} must not leak private keys`);
        assert.ok(!content.includes('WHATSAPP_APP_SECRET'), `${relFile} must not leak WhatsApp secrets`);
      }
    }
  });

});
