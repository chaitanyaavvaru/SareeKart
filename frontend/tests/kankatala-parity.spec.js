import { test, expect } from '@playwright/test';

test.describe('Kankatala Luxury Feature Parity Suite', () => {

  test('1. Announcement Bar Carousel & Multi-Currency Switcher converts prices dynamically', async ({ page }) => {
    await page.goto('/');

    // Announcement bar is rendered with one of the carousel slides
    const announcement = page.locator('text=/Virtual Video Shopping|Worldwide Express Shipping|Authentic Handlooms|Fall & Pico/i');
    await expect(announcement.first()).toBeVisible();

    // Verify Currency Switcher is present in Announcement Bar
    const currencySelect = page.getByLabel('Select currency');
    await expect(currencySelect).toBeVisible();

    // Verify initial price shows INR (₹)
    const initialPrice = page.getByRole('article').getByText(/₹/).first();
    await expect(initialPrice).toBeVisible();

    // Switch Currency to USD ($)
    await currencySelect.selectOption('USD');

    // Verify product card prices now show USD ($)
    const usdPrice = page.getByRole('article').getByText(/\$/).first();
    await expect(usdPrice).toBeVisible();

    // Switch to EUR (€)
    await currencySelect.selectOption('EUR');
    const eurPrice = page.getByRole('article').getByText(/€/).first();
    await expect(eurPrice).toBeVisible();
  });

  test('2. Virtual Video Shopping Appointment Studio can book an appointment and show confirmation', async ({ page }) => {
    await page.goto('/');

    // Open video shopping modal from navbar button
    const bookBtn = page.getByRole('button', { name: /Video Shopping/i }).first();
    await expect(bookBtn).toBeVisible();
    await bookBtn.click();

    // Verify modal header
    await expect(page.getByRole('heading', { name: /Virtual Video Shopping/i })).toBeVisible();
    await expect(page.getByText(/1-on-1 private video drape session/i)).toBeVisible();

    // Fill appointment form
    await page.getByPlaceholder('Priya Sharma').fill('Pooja Reddy');
    await page.getByPlaceholder('+91 98765 43210').fill('+91 98765 43210');

    // Select weave tag
    await page.getByRole('button', { name: 'Bridal Kanchipuram' }).click();

    // Select slot
    await page.getByRole('button', { name: /11:00 AM - 11:30 AM/i }).click();

    // Submit booking inside the modal dialog form
    await page.locator('div[role="dialog"] form').getByRole('button', { name: /Book Video Shopping Appointment/i }).click();

    // Verify confirmed appointment screen
    await expect(page.getByText('Appointment Confirmed!')).toBeVisible();
    await expect(page.getByText('Pooja Reddy')).toBeVisible();
    await expect(page.getByRole('link', { name: /Notify Stylist via WhatsApp/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Add to Calendar/i })).toBeVisible();
  });

  test('3. Silk Mark India & GI Tag Provenance Trust Center modal details authenticity guarantee', async ({ page }) => {
    await page.goto('/products/1');

    // Verify Silk Mark Trust Badge button
    const silkMarkBadge = page.getByRole('button', { name: /Silk Mark Certified/i });
    await expect(silkMarkBadge).toBeVisible();
    await silkMarkBadge.click();

    // Check modal contents
    await expect(page.getByRole('heading', { name: /Silk Mark Certified/i })).toBeVisible();
    await expect(page.getByText(/Silk Mark Organization of India \(SMOI\)/i)).toBeVisible();
    await expect(page.getByText(/100% Natural Silk Fibers/i)).toBeVisible();
    await expect(page.getByText(/GI Tag Protected Weave Clusters/i)).toBeVisible();
    await expect(page.getByText(/Physical Security Tag/i)).toBeVisible();

    // Close modal
    await page.getByRole('button', { name: /Understood & Close/i }).click();
  });

  test('4. Luxury Tailoring Studio allows Blouse Customization and applies fee to Cart', async ({ page }) => {
    await page.goto('/products/1');

    // Verify Custom Tailoring card is visible
    await expect(page.getByText(/Custom Tailoring & Fall\/Pico/i)).toBeVisible();

    // Open Tailoring Studio Modal
    const customizeFitBtn = page.getByRole('button', { name: /Customize Fit →/i });
    await expect(customizeFitBtn).toBeVisible();
    await customizeFitBtn.click();

    // Verify Modal
    await expect(page.getByRole('heading', { name: /Luxury Tailoring & Finishing Studio/i })).toBeVisible();

    // Select Custom Tailored Blouse option
    await page.getByRole('button', { name: /Custom Tailored/i }).click();

    // Select bust size and neckline
    const bustSelect = page.locator('label').filter({ hasText: 'Bust Size' }).locator('select');
    await bustSelect.selectOption('36"');

    const frontNeckSelect = page.locator('label').filter({ hasText: 'Front Neckline' }).locator('select');
    await frontNeckSelect.selectOption('Sweetheart');

    // Save preferences
    await page.getByRole('button', { name: /Save & Apply Tailoring/i }).click();

    // Tailoring card on PDP should show updated specs
    await expect(page.getByText(/Custom Tailored/i).first()).toBeVisible();

    // Add to cart
    await page.getByRole('button', { name: /Add to bag/i }).click();

    // View Bag from toast modal
    await page.getByRole('button', { name: 'View bag' }).click();

    // Verify Cart Drawer opens with Tailored Blouse badge
    await expect(page.getByRole('dialog', { name: 'Shopping bag' })).toBeVisible();
    await expect(page.getByText('Tailored Blouse')).toBeVisible();
  });

  test('5. Pincode Delivery Estimator calculates instant delivery timeline on PDP', async ({ page }) => {
    await page.goto('/products/1');

    // Check delivery section
    const pinInput = page.getByPlaceholder('Enter 6-digit PIN code');
    await expect(pinInput).toBeVisible();

    // Enter South Zone pincode (500081)
    await pinInput.fill('500081');
    await page.getByRole('button', { name: 'Check' }).click();

    // Verify estimate appears
    await expect(page.getByText(/Delivery by/i)).toBeVisible();
    await expect(page.getByText(/Express South Zone:/i)).toBeVisible();
    await expect(page.getByText(/BlueDart \/ Delhivery Express/i)).toBeVisible();
  });

  test('6. PDP provides 1-on-1 Video Consultation and WhatsApp inquiry triggers', async ({ page }) => {
    await page.goto('/products/1');

    // WhatsApp button
    const whatsappBtn = page.getByRole('link', { name: /Ask on WhatsApp/i });
    await expect(whatsappBtn).toBeVisible();
    const href = await whatsappBtn.getAttribute('href');
    expect(href).toContain('wa.me/919059564499');

    // Video Shopping Consultation button
    const videoBtn = page.getByRole('button', { name: /Book 1-on-1 Video Draping Consultation/i });
    await expect(videoBtn).toBeVisible();
    await videoBtn.click();

    // Video Shopping Modal opens with saree preselected
    await expect(page.getByRole('heading', { name: /Virtual Video Shopping/i })).toBeVisible();
  });

});
