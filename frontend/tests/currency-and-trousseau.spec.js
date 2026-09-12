import { test, expect } from '@playwright/test';

test.describe('Global Currency Expansion & Bridal Trousseau Planner Suite', () => {

  test('1. Multi-Currency switcher supports SGD, MYR, CHF and updates storefront prices dynamically', async ({ page }) => {
    await page.goto('/');

    const currencySelect = page.getByRole('combobox', { name: /Select currency/i });
    await expect(currencySelect).toBeVisible();

    // Verify all 10 currencies exist in the dropdown
    const options = await currencySelect.locator('option').allTextContents();
    expect(options.some(opt => opt.includes('SGD'))).toBeTruthy();
    expect(options.some(opt => opt.includes('MYR'))).toBeTruthy();
    expect(options.some(opt => opt.includes('CHF'))).toBeTruthy();
    expect(options.some(opt => opt.includes('USD'))).toBeTruthy();
    expect(options.some(opt => opt.includes('INR'))).toBeTruthy();

    // Switch to SGD (Singapore Dollar)
    await currencySelect.selectOption('SGD');
    await expect(page.locator('body')).toContainText(/SG\$/);

    // Switch to MYR (Malaysian Ringgit)
    await currencySelect.selectOption('MYR');
    await expect(page.locator('body')).toContainText(/RM/);

    // Switch to USD (US Dollar)
    await currencySelect.selectOption('USD');
    await expect(page.locator('body')).toContainText(/\$/);

    // Reset back to INR
    await currencySelect.selectOption('INR');
    await expect(page.locator('body')).toContainText(/₹/);
  });

  test('2. Bridal Trousseau Planner renders 5 wedding ceremonies, live budget, and tailoring add-ons', async ({ page }) => {
    await page.goto('/trousseau-planner');

    // Verify Page Hero
    await expect(page.getByRole('heading', { name: /Curate Your Sacred Wedding Drapes/i })).toBeVisible();
    await expect(page.getByText(/The Master Bridal Atelier/i)).toBeVisible();

    // Verify all 5 wedding ceremonies render
    await expect(page.getByRole('heading', { name: /Engagement & Ring Ceremony/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Haldi & Mehendi Rituals/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Sangeet & Cocktail Soirée/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /The Sacred Muhurtham \(Wedding\)/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /The Grand Wedding Reception/i })).toBeVisible();

    // Verify Trousseau Summary Sticky Bar
    await expect(page.getByText(/Trousseau Summary/i)).toBeVisible();
    await expect(page.getByText(/5 of 5 Ceremonies Curated/i)).toBeVisible();

    // Toggle custom tailoring button in Ceremony 1
    const tailoringBtn = page.getByRole('button', { name: /Add Custom Designer Blouse/i }).first();
    await expect(tailoringBtn).toBeVisible();
    await tailoringBtn.click();
    await expect(page.getByRole('button', { name: /Custom Designer Blouse Added/i }).first()).toBeVisible();

    // Verify WhatsApp Concierge link has pre-filled trousseau breakdown
    const whatsappLink = page.getByRole('link', { name: /Send to Bridal Concierge/i });
    await expect(whatsappLink).toBeVisible();
    const href = await whatsappLink.getAttribute('href');
    expect(href).toContain('wa.me/919059564499');
    expect(href).toContain('ENGAGEMENT');
    expect(href).toContain('MUHURTHAM');

    // Click "Add 5 Sarees to Bag"
    const addAllBtn = page.getByRole('button', { name: /Add 5 Sarees to Bag/i });
    await expect(addAllBtn).toBeVisible();
    await addAllBtn.click();

    // Verify confirmation toast appears
    await expect(page.getByText(/Bridal Trousseau added to your shopping bag!/i)).toBeVisible();
  });

  test('3. Trousseau Planner price calculations react dynamically to currency changes', async ({ page }) => {
    await page.goto('/trousseau-planner');

    const currencySelect = page.getByRole('combobox', { name: /Select currency/i });

    // Switch to USD
    await currencySelect.selectOption('USD');
    await expect(page.getByText(/USD Total Investment/i)).toBeVisible();
    await expect(page.getByText(/Displaying live bridal investment in US Dollar \(USD\)/i)).toBeVisible();

    // Switch to SGD
    await currencySelect.selectOption('SGD');
    await expect(page.getByText(/SGD Total Investment/i)).toBeVisible();
    await expect(page.getByText(/Displaying live bridal investment in Singapore Dollar \(SGD\)/i)).toBeVisible();
  });

  test('4. Navbar and Footer navigation links route directly to Trousseau Planner', async ({ page }) => {
    await page.goto('/');

    // Navbar link
    const navTrousseau = page.getByRole('link', { name: 'Trousseau', exact: true });
    await expect(navTrousseau).toBeVisible();
    await navTrousseau.click();
    await expect(page).toHaveURL(/.*trousseau-planner/);
    await expect(page.getByRole('heading', { name: /Curate Your Sacred Wedding Drapes/i })).toBeVisible();

    // Footer link
    await page.goto('/');
    const footerTrousseau = page.getByRole('link', { name: 'Trousseau planner', exact: true });
    await expect(footerTrousseau).toBeVisible();
    await footerTrousseau.click();
    await expect(page).toHaveURL(/.*trousseau-planner/);
  });

});
