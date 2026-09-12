import { test, expect } from '@playwright/test';

test.describe('Google Gemini AI Saree Stylist & Booking Concierge', () => {

  test('1. Floating AI Stylist button is visible on storefront and opens Gemini assistant', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL('http://localhost:5173/');

    // Locate floating button
    const stylistBtn = page.getByRole('button', { name: /Open AI Saree Stylist/i });
    await expect(stylistBtn).toBeVisible();
    await expect(page.getByText('AI Stylist')).toBeVisible();

    // Click to open slide-over modal
    await stylistBtn.click();

    // Verify modal header and Gemini 2.5 badge
    await expect(page.getByRole('heading', { name: 'SareeKart AI Stylist' })).toBeVisible();
    await expect(page.getByText('Gemini 2.5')).toBeVisible();
    await expect(page.getByText(/Curated recommendations & direct booking/i)).toBeVisible();

    // Verify initial welcoming message
    await expect(page.getByText(/powered by Google Gemini/i)).toBeVisible();
  });

  test('2. Suggestion chip triggers AI stylist query and recommends bookable sarees', async ({ page }) => {
    await page.goto('/');

    // Open assistant
    await page.getByRole('button', { name: /Open AI Saree Stylist/i }).click();
    await expect(page.getByRole('heading', { name: 'SareeKart AI Stylist' })).toBeVisible();

    // Click on suggestion chip
    const chip = page.getByRole('button', { name: 'Wedding silk under ₹20,000' });
    await expect(chip).toBeVisible();
    await chip.click();

    // Verify user message appears in chat (using paragraph selector to avoid strict-mode ambiguity with chip)
    await expect(
      page.locator('.whitespace-pre-line').filter({ hasText: 'Wedding silk under ₹20,000' })
    ).toBeVisible();

    // Wait for Gemini response to appear with bookable sarees (give ample time for LLM response)
    const bookButtons = page.getByRole('button', { name: /Book.*now/i });
    await expect(bookButtons.first()).toBeVisible({ timeout: 25000 });

    // Click "Book Now" on the first recommended saree
    await bookButtons.first().click();

    // Verify "Booked!" feedback and "Go to Checkout" link appears
    await expect(page.getByText(/Booked!/i).first()).toBeVisible();
    const checkoutLink = page.getByRole('button', { name: /Go to Checkout/i });
    await expect(checkoutLink).toBeVisible();

    // Clicking "Go to Checkout" should navigate to checkout
    await checkoutLink.click();
    await expect(page).toHaveURL(/.*checkout/);
  });

  test('3. Custom natural language query allows conversational saree discovery', async ({ page }) => {
    await page.goto('/');

    // Open assistant
    await page.getByRole('button', { name: /Open AI Saree Stylist/i }).click();

    // Type custom query in input
    const input = page.getByPlaceholder(/Ask Gemini: occasion, color, budget/i);
    await input.fill('Looking for pure silk Banarasi saree');
    await page.getByRole('button', { name: /Send message to AI Stylist/i }).click();

    // Verify user question is rendered in chat bubble
    await expect(
      page.locator('.whitespace-pre-line').filter({ hasText: 'Looking for pure silk Banarasi saree' })
    ).toBeVisible();

    // Wait for response and recommendation cards
    const bookButtons = page.getByRole('button', { name: /Book.*now/i });
    await expect(bookButtons.first()).toBeVisible({ timeout: 25000 });
  });

  test('4. Reset chat restores assistant to initial state', async ({ page }) => {
    await page.goto('/');

    // Open assistant
    await page.getByRole('button', { name: /Open AI Saree Stylist/i }).click();

    // Reset button
    const resetBtn = page.getByRole('button', { name: /Reset chat/i });
    await expect(resetBtn).toBeVisible();
    await resetBtn.click();

    // Check that welcoming message is displayed
    await expect(page.getByText(/powered by Google Gemini/i)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Wedding silk under ₹20,000' })).toBeVisible();
  });

});
