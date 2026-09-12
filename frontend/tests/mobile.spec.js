import { test, expect } from '@playwright/test';

test.describe('Mobile Viewport & Navigation Experience', () => {
  test.use({ viewport: { width: 390, height: 844 } }); // iPhone 13 dimensions

  test('should render mobile bottom navigation bar with all 5 tabs', async ({ page }) => {
    await page.goto('/');

    const mobileNav = page.locator('nav[aria-label="Mobile Navigation"]');
    await expect(mobileNav).toBeVisible();

    // Verify all 5 touch tabs
    await expect(mobileNav.getByRole('link', { name: 'Home' })).toBeVisible();
    await expect(mobileNav.getByRole('link', { name: 'Explore' })).toBeVisible();
    await expect(mobileNav.getByRole('link', { name: 'Search' })).toBeVisible();
    await expect(mobileNav.getByRole('button', { name: /Open Shopping Bag/i })).toBeVisible();
    await expect(mobileNav.getByRole('link', { name: /Sign In|Orders/i })).toBeVisible();
  });

  test('should open cart drawer when tapping Bag tab on mobile', async ({ page }) => {
    await page.goto('/');

    const bagButton = page.locator('nav[aria-label="Mobile Navigation"]').getByRole('button', { name: /Open Shopping Bag/i });
    await bagButton.click();

    // Cart drawer dialog should slide in
    const cartDialog = page.getByRole('dialog', { name: 'Shopping bag' });
    await expect(cartDialog).toBeVisible();
    await expect(cartDialog.getByText(/Your edit/i)).toBeVisible();
  });

  test('should have zero horizontal scroll overflow on mobile screens', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const hasHorizontalScroll = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });

    expect(hasHorizontalScroll).toBe(false);
  });

  test('should display sticky purchase action bar on mobile PDP', async ({ page }) => {
    await page.goto('/products/11');

    // Sticky bottom action bar should be visible on mobile
    const stickyBar = page.locator('div.fixed.bottom-14');
    await expect(stickyBar).toBeVisible();
    await expect(stickyBar.getByRole('button', { name: /Add to Bag/i })).toBeVisible();
  });

  test('should render mobile touch gallery with counter badge and thumbnail tabs', async ({ page }) => {
    await page.goto('/products/11');

    // Thumbnail tablist
    const thumbnails = page.locator('div[role="tablist"][aria-label="Product thumbnails"]');
    await expect(thumbnails).toBeVisible();

    // Floating counter badge (e.g. 1 / 4)
    const counter = page.locator('text=/\\d+ \\/ \\d+/');
    await expect(counter.first()).toBeVisible();

    // Switch image via second thumbnail tab
    const secondThumb = thumbnails.getByRole('tab').nth(1);
    if (await secondThumb.isVisible()) {
      await secondThumb.click();
      await expect(secondThumb).toHaveAttribute('aria-selected', 'true');
    }
  });

  test('should open and close full-screen zari inspection lightbox', async ({ page }) => {
    await page.goto('/products/11');

    // Tap zoom button
    const zoomButton = page.getByRole('button', { name: /Open high-resolution zari inspection lightbox/i });
    await expect(zoomButton).toBeVisible();
    await zoomButton.click();

    // Lightbox should open with zoom controls
    const lightbox = page.getByRole('dialog', { name: /High-resolution zari inspection lightbox/i });
    await expect(lightbox).toBeVisible();
    await expect(lightbox.getByText(/Zari & Weave Inspection/i)).toBeVisible();

    // Test 2.2x zoom toggle
    const zoomToggle = lightbox.getByRole('button', { name: /2\.2x Zoom/i });
    await expect(zoomToggle).toBeVisible();
    await zoomToggle.click();
    await expect(lightbox.getByRole('button', { name: /Reset/i })).toBeVisible();

    // Close lightbox
    const closeBtn = lightbox.getByRole('button', { name: /Close inspection lightbox/i });
    await closeBtn.click();
    await expect(lightbox).not.toBeVisible();
  });

  test('should open mobile bottom-sheet filter drawer on /products', async ({ page }) => {
    await page.goto('/products');

    const filterBtn = page.getByRole('button', { name: /Filters/i });
    await expect(filterBtn).toBeVisible();
    await filterBtn.click();

    // Mobile filter drawer dialog should slide up
    const drawer = page.getByRole('dialog', { name: /Filter sarees mobile drawer/i });
    await expect(drawer).toBeVisible();
    await expect(drawer.getByText('Filter Sarees')).toBeVisible();

    // Close via close button
    const closeBtn = drawer.getByRole('button', { name: /Close filters/i });
    await closeBtn.click();
    await expect(drawer).not.toBeVisible();
  });

  test('should select fabric chip and apply filters in mobile drawer', async ({ page }) => {
    await page.goto('/products');

    const filterBtn = page.getByRole('button', { name: /Filters/i });
    await filterBtn.click();

    const drawer = page.getByRole('dialog', { name: /Filter sarees mobile drawer/i });
    await expect(drawer).toBeVisible();

    // Select Silk fabric chip
    const silkChip = drawer.getByRole('button', { name: /^Silk$/i });
    if (await silkChip.isVisible()) {
      await silkChip.click();
    }

    // Apply filters
    const applyBtn = drawer.getByRole('button', { name: /Apply Filters/i });
    await expect(applyBtn).toBeVisible();
    await applyBtn.click();

    // Drawer should dismiss and filters button should reflect active state
    await expect(drawer).not.toBeVisible();
  });

  test('should have valid PWA manifest link and theme color meta in HTML head', async ({ page }) => {
    await page.goto('/');

    const manifestLink = page.locator('link[rel="manifest"]');
    await expect(manifestLink).toHaveAttribute('href', '/manifest.json');

    const themeColorMeta = page.locator('meta[name="theme-color"]');
    await expect(themeColorMeta).toHaveAttribute('content', '#3A0F1F');

    const appleMeta = page.locator('meta[name="apple-mobile-web-app-capable"]');
    await expect(appleMeta).toHaveAttribute('content', 'yes');
  });

  test('should serve valid PWA manifest and service worker script', async ({ page }) => {
    const manifestRes = await page.request.get('/manifest.json');
    expect(manifestRes.ok()).toBe(true);
    const manifestJson = await manifestRes.json();
    expect(manifestJson.short_name).toBe('SareeKart');
    expect(manifestJson.theme_color).toBe('#3A0F1F');

    const swRes = await page.request.get('/sw.js');
    expect(swRes.ok()).toBe(true);
    const swText = await swRes.text();
    expect(swText).toContain('sareekart-pwa-v1');
  });

  test('should render offline notification badge when network goes offline', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Simulate offline event
    await page.evaluate(() => {
      window.dispatchEvent(new Event('offline'));
    });

    const offlineBadge = page.getByText(/Offline Mode Active/i);
    await expect(offlineBadge).toBeVisible();

    // Simulate online recovery event
    await page.evaluate(() => {
      window.dispatchEvent(new Event('online'));
    });
    await expect(offlineBadge).not.toBeVisible();
  });
});

test.describe('Desktop Viewport Regression', () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test('should hide mobile bottom navigation bar on desktop screens', async ({ page }) => {
    await page.goto('/');

    const mobileNav = page.locator('nav[aria-label="Mobile Navigation"]');
    await expect(mobileNav).not.toBeVisible();
  });
});
