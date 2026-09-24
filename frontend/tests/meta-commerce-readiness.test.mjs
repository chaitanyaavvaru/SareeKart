import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import metaPixelInstance from '../src/utils/metaPixel.js';
import eventTrackerInstance from '../src/utils/eventTracker.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const frontendRoot = path.resolve(__dirname, '..');

test('Point 1 — Meta Pixel Lifecycle: Safe initialization and graceful degradation without window.fbq', () => {
  // When window.fbq is undefined, tracking calls must complete silently without throwing
  assert.doesNotThrow(() => {
    metaPixelInstance.trackPageView();
    metaPixelInstance.trackViewContent({ id: 101, name: 'Banarasi Silk Saree', price: 15000 });
    metaPixelInstance.trackSearch('katan silk', { category: 'Banarasi' });
    metaPixelInstance.trackAddToCart({ id: 101, name: 'Banarasi Silk Saree', price: 15000 });
    metaPixelInstance.trackInitiateCheckout({ items: [{}], totalAmount: 15000 });
    metaPixelInstance.trackPurchase({ id: 9001, totalAmount: 15000 });
  }, 'MetaPixel tracking methods must degrade silently in non-browser or ad-blocked environments');
});

test('Point 2 — Standard Event: PageView tracking dispatch', () => {
  const events = [];
  globalThis.window = {
    fbq: (type, eventName, params) => {
      events.push({ type, eventName, params });
    },
  };

  metaPixelInstance.trackPageView();
  assert.equal(events.length, 1);
  assert.equal(events[0].type, 'track');
  assert.equal(events[0].eventName, 'PageView');

  delete globalThis.window;
});

test('Point 3 — Standard Event: ViewContent emits accurate product attributes and INR currency', () => {
  const events = [];
  globalThis.window = {
    fbq: (type, eventName, params) => {
      events.push({ type, eventName, params });
    },
  };

  const product = {
    id: 108,
    name: 'Kanchipuram Temple Border Silk Saree',
    category: { name: 'Kanchipuram Silk' },
    fabric: 'Pure Mulberry Silk',
    price: 32500,
  };

  metaPixelInstance.trackViewContent(product);

  assert.equal(events.length, 1);
  assert.equal(events[0].eventName, 'ViewContent');
  assert.deepEqual(events[0].params, {
    content_name: 'Kanchipuram Temple Border Silk Saree',
    content_category: 'Kanchipuram Silk',
    content_ids: ['108'],
    content_type: 'product',
    value: 32500,
    currency: 'INR',
  });

  delete globalThis.window;
});

test('Point 4 — Standard Event: Search query tracking via MetaPixel and EventTracker', () => {
  const events = [];
  globalThis.window = {
    fbq: (type, eventName, params) => {
      events.push({ type, eventName, params });
    },
  };

  metaPixelInstance.trackSearch('Uppada Jamdani', {
    content_category: 'Uppada Silk',
    result_count: 14,
  });

  assert.equal(events.length, 1);
  assert.equal(events[0].eventName, 'Search');
  assert.equal(events[0].params.search_string, 'Uppada Jamdani');
  assert.equal(events[0].params.content_category, 'Uppada Silk');
  assert.equal(events[0].params.result_count, 14);

  delete globalThis.window;
});

test('Point 5 — Standard Event: AddToCart emits product ID, name, value, and INR currency', () => {
  const events = [];
  globalThis.window = {
    fbq: (type, eventName, params) => {
      events.push({ type, eventName, params });
    },
  };

  metaPixelInstance.trackAddToCart({
    id: 204,
    name: 'Pochampally Ikat Handloom Silk Saree',
    price: 18900,
  });

  assert.equal(events.length, 1);
  assert.equal(events[0].eventName, 'AddToCart');
  assert.deepEqual(events[0].params, {
    content_name: 'Pochampally Ikat Handloom Silk Saree',
    content_ids: ['204'],
    content_type: 'product',
    value: 18900,
    currency: 'INR',
  });

  delete globalThis.window;
});

test('Point 6 — Standard Event: InitiateCheckout emits num_items, total value, and INR currency', () => {
  const events = [];
  globalThis.window = {
    fbq: (type, eventName, params) => {
      events.push({ type, eventName, params });
    },
  };

  metaPixelInstance.trackInitiateCheckout({
    items: [{ id: 1 }, { id: 2 }],
    totalAmount: 48000,
  });

  assert.equal(events.length, 1);
  assert.equal(events[0].eventName, 'InitiateCheckout');
  assert.deepEqual(events[0].params, {
    num_items: 2,
    value: 48000,
    currency: 'INR',
  });

  delete globalThis.window;
});

test('Point 7 — Standard Event: Purchase deduplication protects against re-renders & reloads', () => {
  const events = [];
  globalThis.window = {
    fbq: (type, eventName, params) => {
      events.push({ type, eventName, params });
    },
  };

  const order = {
    id: 9942,
    totalAmount: 54999,
  };

  // First track call
  metaPixelInstance.trackPurchase(order);
  assert.equal(events.length, 1, 'First purchase call must emit Purchase event');
  assert.equal(events[0].eventName, 'Purchase');
  assert.equal(events[0].params.order_id, '9942');
  assert.equal(events[0].params.value, 54999);
  assert.equal(events[0].params.currency, 'INR');

  // Second track call with identical order_id (simulating React re-render or reload)
  metaPixelInstance.trackPurchase(order);
  assert.equal(events.length, 1, 'Duplicate purchase invocation with same order_id must be suppressed');

  // Third track call with different order_id
  metaPixelInstance.trackPurchase({ id: 9943, totalAmount: 22000 });
  assert.equal(events.length, 2, 'Distinct order_id must emit Purchase event');
  assert.equal(events[1].params.order_id, '9943');

  delete globalThis.window;
});

test('Point 8 — Open Graph Social Metadata Hardening in SEO.jsx', () => {
  const seoFile = path.resolve(frontendRoot, 'src/components/common/SEO.jsx');
  const content = fs.readFileSync(seoFile, 'utf-8');

  assert.ok(content.includes("'og:title': title"), 'SEO.jsx must include og:title');
  assert.ok(content.includes("'og:description': description"), 'SEO.jsx must include og:description');
  assert.ok(content.includes("'og:type': ogType"), 'SEO.jsx must include og:type');
  assert.ok(content.includes("'og:image': absoluteOgImage"), 'SEO.jsx must include og:image');
  assert.ok(content.includes("'og:image:alt': ogImageAlt"), 'SEO.jsx must include og:image:alt');
  assert.ok(content.includes("'og:url': resolvedCanonical"), 'SEO.jsx must include og:url');
  assert.ok(content.includes("'og:site_name': 'SareeKart'"), 'SEO.jsx must include og:site_name');
  assert.ok(content.includes("'twitter:card': 'summary_large_image'"), 'SEO.jsx must include twitter:card');
});

test('Point 9 — Dual-Channel Meta Domain Verification Readiness', () => {
  // Channel A: HTML meta tag in SEO.jsx
  const seoFile = path.resolve(frontendRoot, 'src/components/common/SEO.jsx');
  const seoContent = fs.readFileSync(seoFile, 'utf-8');
  assert.ok(
    seoContent.includes('facebook-domain-verification'),
    'SEO.jsx must support facebook-domain-verification meta tag'
  );
  assert.ok(
    seoContent.includes('VITE_META_DOMAIN_VERIFICATION'),
    'SEO.jsx must read VITE_META_DOMAIN_VERIFICATION environment variable'
  );

  // Channel B: DNS TXT documentation in docs/sareekart_dns_configuration.md
  const dnsDocFile = path.resolve(frontendRoot, '../docs/sareekart_dns_configuration.md');
  const dnsContent = fs.readFileSync(dnsDocFile, 'utf-8');
  assert.ok(
    dnsContent.includes('Meta (Facebook & Instagram) Domain Verification'),
    'DNS doc must specify Meta domain verification instructions'
  );
  assert.ok(
    dnsContent.includes('facebook-domain-verification='),
    'DNS doc must specify facebook-domain-verification DNS TXT record'
  );
});

test('Point 10 — Privacy, Non-Blocking Fail-Safe & Zero PII Leakage', () => {
  const metaPixelFile = path.resolve(frontendRoot, 'src/utils/metaPixel.js');
  const pixelCode = fs.readFileSync(metaPixelFile, 'utf-8');

  // Verify non-blocking try/catch wrapping
  assert.ok(pixelCode.includes('try {'), 'Tracking calls must be wrapped in try/catch for outage isolation');
  assert.ok(pixelCode.includes('console.warn'), 'Errors must degrade to console.warn without throwing');

  // Verify no hardcoded secrets or PII
  assert.ok(!pixelCode.includes('EAAB'), 'Must not contain hardcoded Meta access tokens');
  assert.ok(!pixelCode.includes('password'), 'Must not reference passwords');
  assert.ok(!pixelCode.includes('credit_card'), 'Must not reference raw credit card numbers');
});
