import { test, expect } from '@playwright/test';

test.describe('AI Complete the Look, Draping Guide & WhatsApp Concierge', () => {

  test('1. Product detail page renders AI Complete The Look & Draping Guide', async ({ page }) => {
    await page.goto('/products/1');

    // Verify heading and Gemini badge
    await expect(page.getByRole('heading', { name: /AI Complete The Look & Draping Guide/i })).toBeVisible();
    await expect(page.getByText('Gemini Curated')).toBeVisible();

    // Verify recommendations
    await expect(page.getByText('Blouse Pairing')).toBeVisible();
    await expect(page.getByText('Jewelry Accents')).toBeVisible();
    await expect(page.getByText('Draping Style')).toBeVisible();

    // Verify dynamic WhatsApp inquiry link contains product details
    const whatsappLink = page.getByRole('link', { name: /Ask on WhatsApp/i });
    await expect(whatsappLink).toBeVisible();
    const href = await whatsappLink.getAttribute('href');
    expect(href).toContain('wa.me/919059564499');
    expect(href).toContain('Royal%20Banarasi');
  });

  test('2. "Style with AI" button on product page triggers Gemini AI Stylist consultation', async ({ page }) => {
    await page.goto('/products/1');

    // Locate "Style with AI" button
    const styleBtn = page.getByRole('button', { name: /Style.*AI/i });
    await expect(styleBtn).toBeVisible();
    await styleBtn.click();

    // Verify AI Stylist modal opens with the preloaded question
    await expect(page.getByRole('heading', { name: 'SareeKart AI Stylist' })).toBeVisible();
    await expect(page.getByText(/I am viewing the "Royal Banarasi/i)).toBeVisible({ timeout: 10000 });

    // Wait for Gemini response to arrive
    await expect(page.getByText(/Gemini 2.5/i)).toBeVisible();
  });

});
