import { test, expect } from '@playwright/test';

test.describe('Order Tracking System Suite', () => {

  test('1. Public Order Tracking portal allows tracking by Order ID without login', async ({ page }) => {
    await page.goto('/track-order');

    // Verify page header
    await expect(page.getByRole('heading', { name: /Track your saree in motion/i })).toBeVisible();
    await expect(page.getByText(/Real-Time Express Courier Tracking/i)).toBeVisible();

    // Fill Order ID '37'
    const orderInput = page.getByPlaceholder(/Order Number/i);
    await expect(orderInput).toBeVisible();
    await orderInput.fill('37');

    // Click Track button
    await page.getByRole('button', { name: 'Track', exact: true }).click();

    // Verify Order details appear
    await expect(page.getByRole('heading', { name: /Order #SK-37/i })).toBeVisible();
    await expect(page.getByText('BlueDart Express').first()).toBeVisible();
    await expect(page.getByText(/Estimated Doorstep Delivery/i)).toBeVisible();

    // Verify 6-stage milestone stepper titles
    await expect(page.getByText('Order Confirmed').first()).toBeVisible();
    await expect(page.getByText('Silk Mark & GI Inspection').first()).toBeVisible();
    await expect(page.getByText('Tailoring & Finishing Studio').first()).toBeVisible();
    await expect(page.getByText('Keepsake Box Packaging').first()).toBeVisible();
    await expect(page.getByText('Dispatched & In Transit').first()).toBeVisible();
    await expect(page.getByText('Delivered').first()).toBeVisible();

    // Verify transit scans activity log
    await expect(page.getByText(/Transit Scans & Activity Log/i)).toBeVisible();

    // Verify package items
    await expect(page.getByText(/Package Items/i)).toBeVisible();
    await expect(page.getByText(/Royal Banarasi/i).first()).toBeVisible();

    // Verify WhatsApp Concierge link
    const whatsappLink = page.getByRole('link', { name: /WhatsApp Concierge/i });
    await expect(whatsappLink).toBeVisible();
    const href = await whatsappLink.getAttribute('href');
    expect(href).toContain('wa.me/919059564499');
  });

  test('2. Public Order Tracking portal allows tracking by Courier AWB Number', async ({ page }) => {
    await page.goto('/track-order?awb=SK-BD-1028749');

    // Tracking details should auto-load via URL param
    await expect(page.getByRole('heading', { name: /Order #SK-37/i })).toBeVisible();
    await expect(page.getByText('SK-BD-1028749').first()).toBeVisible();
    await expect(page.getByText('BlueDart Express').first()).toBeVisible();
  });

  test('3. Footer link navigates directly to the public Track Order portal', async ({ page }) => {
    await page.goto('/');

    const trackLink = page.getByRole('link', { name: 'Track order' }).first();
    await expect(trackLink).toBeVisible();
    await trackLink.click();

    await expect(page).toHaveURL(/.*track-order/);
    await expect(page.getByRole('heading', { name: /Track your saree in motion/i })).toBeVisible();
  });

  test('4. My Orders page renders "Track Shipment" button and opens interactive tracking modal', async ({ page }) => {
    // Sign in as customer via quick access or form
    await page.goto('/login?redirect=/orders');
    await page.getByRole('button', { name: 'Customer', exact: true }).click();
    await page.getByRole('button', { name: /Sign In/i }).click();

    // Verify redirected to Orders page
    await expect(page).toHaveURL(/.*orders/);
    await expect(page.getByRole('heading', { name: /Your sarees, in motion/i })).toBeVisible();

    // Verify Order card has Track Shipment button
    const trackShipmentBtn = page.getByRole('button', { name: /Track Shipment/i }).first();
    await expect(trackShipmentBtn).toBeVisible();
    await trackShipmentBtn.click();

    // Verify Tracking Modal opens
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Shipment Tracker' })).toBeVisible();
    await expect(page.getByText('BlueDart Express')).toBeVisible();
    await expect(page.getByText(/Transit Progression/i)).toBeVisible();

    // Verify Full Tracking Portal link is present
    await expect(page.getByRole('link', { name: /Full Tracking Portal/i })).toBeVisible();

    // Close modal
    await page.getByRole('button', { name: /Close tracking modal/i }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });

});
