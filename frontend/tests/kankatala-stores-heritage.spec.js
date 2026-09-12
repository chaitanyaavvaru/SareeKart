import { test, expect } from '@playwright/test';

test.describe('Kankatala Parity Phase 2: Boutiques, Heritage Weaves & Saree Care Suite', () => {

  test('1. Flagship Boutiques Locator renders 5 showrooms and supports city filtering', async ({ page }) => {
    await page.goto('/stores');

    // Verify Page Header
    await expect(page.getByRole('heading', { name: /Where Indian Heritage Comes Alive/i })).toBeVisible();
    await expect(page.getByText(/5 Flagship Boutiques Across India/i)).toBeVisible();

    // Verify all 5 boutique showrooms appear
    await expect(page.getByText('Jubilee Hills Flagship')).toBeVisible();
    await expect(page.getByText('Jayanagar Experience Salon')).toBeVisible();
    await expect(page.getByText('South Extension Heritage House')).toBeVisible();
    await expect(page.getByText('V Square Heritage Flagship')).toBeVisible();
    await expect(page.getByText('MG Road Bridal Emporium')).toBeVisible();

    // Test city filter: Hyderabad
    const hydFilter = page.getByRole('button', { name: 'Hyderabad', exact: true });
    await expect(hydFilter).toBeVisible();
    await hydFilter.click();

    // Jubilee Hills should be visible, others should be hidden
    await expect(page.getByText('Jubilee Hills Flagship')).toBeVisible();
    await expect(page.getByText('Jayanagar Experience Salon')).not.toBeVisible();
    await expect(page.getByText('South Extension Heritage House')).not.toBeVisible();

    // Reset filter to All
    await page.getByRole('button', { name: 'All', exact: true }).click();
    await expect(page.getByText('Jayanagar Experience Salon')).toBeVisible();

    // Verify Directions link
    const directionsLink = page.getByRole('link', { name: /Directions/i }).first();
    await expect(directionsLink).toBeVisible();
    const href = await directionsLink.getAttribute('href');
    expect(href).toContain('maps.google.com');
  });

  test('2. In-Store VIP Drape Appointment booking flow generates official VIP pass and WhatsApp link', async ({ page }) => {
    await page.goto('/stores');

    // Click "Book VIP Drape" on first boutique
    const bookBtn = page.getByRole('button', { name: /Book VIP Drape/i }).first();
    await expect(bookBtn).toBeVisible();
    await bookBtn.click();

    // Verify modal is open
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('heading', { name: /Book VIP In-Store Drape Salon/i })).toBeVisible();

    // Select Styling Focus
    await page.getByRole('button', { name: /Bridal Trousseau/i }).click();

    // Fill Client Info
    await page.getByPlaceholder(/Deepika Rao/i).fill('Radhika Iyer');
    await page.getByPlaceholder(/\+91 98480 12345/i).fill('+91 99887 76655');

    // Submit Appointment
    await page.getByRole('button', { name: /Reserve In-Store VIP Drape Appointment/i }).click();

    // Verify Confirmed VIP Pass
    await expect(page.getByText(/Official VIP Salon Invitation/i)).toBeVisible();
    await expect(page.getByRole('heading', { name: /VIP Drape Pass Confirmed/i })).toBeVisible();
    await expect(page.getByText(/Pass #SK-VIP-/i)).toBeVisible();
    await expect(page.getByText('Radhika Iyer')).toBeVisible();

    // Verify WhatsApp confirmation link
    const whatsappBtn = page.getByRole('link', { name: /Notify Boutique via WhatsApp/i });
    await expect(whatsappBtn).toBeVisible();
    const waHref = await whatsappBtn.getAttribute('href');
    expect(waHref).toContain('wa.me/919059564499');

    // Close modal
    await page.getByRole('button', { name: /Done & Return to Boutiques/i }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });

  test('3. Heritage Weaves of India Explorer showcases 8 GI clusters, craft details, and motif guide', async ({ page }) => {
    await page.goto('/heritage-weaves');

    // Verify Header
    await expect(page.getByRole('heading', { name: /The Heritage Weaves of India/i })).toBeVisible();
    await expect(page.getByText(/Living Handloom Encyclopedia/i)).toBeVisible();

    // Verify Initial Active Weave (Kanchipuram Silk)
    await expect(page.getByRole('heading', { name: 'Kanchipuram Silk' })).toBeVisible();
    await expect(page.getByText('GI-01 (Certified 2005)')).toBeVisible();
    await expect(page.getByText(/Three-Shuttle Korvai & Petni Interlocking/i)).toBeVisible();

    // Switch Tab to Patan Patola
    const patolaTab = page.getByRole('button', { name: /Patan Patola/i });
    await expect(patolaTab).toBeVisible();
    await patolaTab.click();

    // Verify Patan Patola details loaded
    await expect(page.getByRole('heading', { name: /Patan Patola Double Ikat/i })).toBeVisible();
    await expect(page.getByText('GI-232 (Certified 2013)')).toBeVisible();
    await expect(page.getByText(/Double Ikat Warp & Weft Resist Dyeing/i)).toBeVisible();

    // Verify Catalog Deep Link
    const shopLink = page.getByRole('link', { name: /Explore Patan Patola Double Ikat Sarees/i });
    await expect(shopLink).toBeVisible();
    const href = await shopLink.getAttribute('href');
    expect(href).toContain('/products?search=Patola');

    // Verify Motif Symbolism section
    await expect(page.getByRole('heading', { name: /The Language of Indian Motifs/i })).toBeVisible();
    await expect(page.getByText(/Mayil \(The Sacred Peacock\)/i)).toBeVisible();
    await expect(page.getByText(/Temple Gopuram \(Pyramidal Spire\)/i)).toBeVisible();
    await expect(page.getByText(/Shikargah \(Royal Forest Hunt\)/i)).toBeVisible();
  });

  test('4. Pure Silk & Heirloom Saree Care Guide renders 6 commandments and emergency stain advice', async ({ page }) => {
    await page.goto('/saree-care');

    // Verify Header
    await expect(page.getByRole('heading', { name: /Preserving Heirloom Drapes/i })).toBeVisible();
    await expect(page.getByText(/Artisanal Longevity Standard/i)).toBeVisible();

    // Verify Key Commandments
    await expect(page.getByText('Wrap Exclusively in Pure Muslin or Cotton')).toBeVisible();
    await expect(page.getByText('The Quarterly Refolding Ritual')).toBeVisible();
    await expect(page.getByText('Shield Zari from Perfumes & Deodorants')).toBeVisible();
    await expect(page.getByText('Professional Petrol Dry Cleaning Only')).toBeVisible();
    await expect(page.getByText('Natural Neem Leaves Over Naphthalene')).toBeVisible();
    await expect(page.getByText('Gentle Airing in Morning Shade')).toBeVisible();

    // Verify Emergency Stain Guide
    await expect(page.getByRole('heading', { name: /Emergency Stain Response Guide/i })).toBeVisible();
    await expect(page.getByText(/Haldi \/ Turmeric \/ Curry/i)).toBeVisible();
    await expect(page.getByText(/Tea, Coffee & Beverages/i)).toBeVisible();

    // Verify Restoration WhatsApp Concierge
    const restorationBtn = page.getByRole('link', { name: /Contact Textile Restoration Concierge/i });
    await expect(restorationBtn).toBeVisible();
    const waHref = await restorationBtn.getAttribute('href');
    expect(waHref).toContain('wa.me/919059564499');
  });

  test('5. Footer and Navbar navigation links route seamlessly to new luxury portals', async ({ page }) => {
    await page.goto('/');

    // Test Footer: Store locator
    const storeLink = page.getByRole('link', { name: 'Store locator' }).first();
    await expect(storeLink).toBeVisible();
    await storeLink.click();
    await expect(page).toHaveURL(/.*stores/);
    await expect(page.getByRole('heading', { name: /Where Indian Heritage Comes Alive/i })).toBeVisible();

    // Test Footer: Saree care
    await page.goto('/');
    const careLink = page.getByRole('link', { name: 'Saree care' }).first();
    await expect(careLink).toBeVisible();
    await careLink.click();
    await expect(page).toHaveURL(/.*saree-care/);
    await expect(page.getByRole('heading', { name: /Preserving Heirloom Drapes/i })).toBeVisible();

    // Test Footer: Heritage weaves
    await page.goto('/');
    const heritageLink = page.getByRole('link', { name: 'Heritage weaves' }).first();
    await expect(heritageLink).toBeVisible();
    await heritageLink.click();
    await expect(page).toHaveURL(/.*heritage-weaves/);
    await expect(page.getByRole('heading', { name: /The Heritage Weaves of India/i })).toBeVisible();

    // Test Navbar: Boutiques
    const navBoutique = page.getByRole('link', { name: 'Boutiques' }).first();
    await expect(navBoutique).toBeVisible();
    await navBoutique.click();
    await expect(page).toHaveURL(/.*stores/);
  });

});
