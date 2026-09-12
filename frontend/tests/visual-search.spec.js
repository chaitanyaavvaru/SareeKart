import { test, expect } from '@playwright/test';

test.describe('Google Gemini 2.5 Flash Multimodal Visual Saree Search', () => {

  test('1. Camera icon in Navbar opens AI Visual Saree Search modal', async ({ page }) => {
    await page.goto('/');

    // Locate Camera button in Navbar
    const cameraBtn = page.getByRole('button', { name: /Search by saree image with Gemini/i });
    await expect(cameraBtn).toBeVisible();
    await cameraBtn.click();

    // Verify modal elements
    await expect(page.getByRole('heading', { name: 'AI Visual Saree Search' })).toBeVisible();
    await expect(page.getByText('Gemini 2.5 Vision')).toBeVisible();
    await expect(page.getByText(/Upload or drop any saree photo/i)).toBeVisible();
    await expect(page.getByText(/try instant visual search with sample handlooms/i)).toBeVisible();
  });

  test('2. Instant sample visual search triggers Gemini 2.5 Vision and matches bookable sarees', async ({ page }) => {
    await page.goto('/');

    // Open Visual Search Modal
    await page.getByRole('button', { name: /Search by saree image with Gemini/i }).click();
    await expect(page.getByRole('heading', { name: 'AI Visual Saree Search' })).toBeVisible();

    // Click on Banarasi Bridal sample handloom
    const banarasiSample = page.getByRole('button', { name: /Banarasi Bridal/i });
    await expect(banarasiSample).toBeVisible();
    await banarasiSample.click();

    // Verify image analysis state and wait for Gemini results
    await expect(page.getByText(/Image Analyzed by Gemini|Visual Signature Extracted/i).first()).toBeVisible({ timeout: 25000 });
    await expect(page.getByText(/Visual DNA Extracted by Gemini/i)).toBeVisible({ timeout: 25000 });

    // Verify matching catalog drapes rendered with Book Now buttons
    const bookButtons = page.getByRole('button', { name: /Book.*now/i });
    await expect(bookButtons.first()).toBeVisible({ timeout: 25000 });

    // Click "Book Now" on top matching saree
    await bookButtons.first().click();

    // Verify booking feedback and direct checkout link
    await expect(page.getByText(/Booked!/i).first()).toBeVisible();
    const checkoutLink = page.getByRole('button', { name: /Go to Checkout/i });
    await expect(checkoutLink).toBeVisible();

    // Click checkout
    await checkoutLink.click();
    await expect(page).toHaveURL(/.*checkout/);
  });

  test('3. Reset allows trying another saree image', async ({ page }) => {
    await page.goto('/');

    // Open Visual Search Modal
    await page.getByRole('button', { name: /Search by saree image with Gemini/i }).click();

    // Select Cotton Jamdani sample
    const jamdaniSample = page.getByRole('button', { name: /Cotton Jamdani/i });
    await expect(jamdaniSample).toBeVisible();
    await jamdaniSample.click();

    // Verify analyzed image is shown
    await expect(page.getByText(/Try Another Image/i)).toBeVisible({ timeout: 25000 });

    // Click Try Another Image
    await page.getByRole('button', { name: /Try Another Image/i }).click();

    // Verify dropzone and sample handlooms are back
    await expect(page.getByText(/Click or Drag & Drop Saree Image/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /Kanchipuram Silk/i })).toBeVisible();
  });

});
