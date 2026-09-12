import { test, expect, chromium, webkit } from '@playwright/test';

test.describe('Cross-Browser Booking & Order Persistence', () => {
  // Allow sufficient time for all 3 actual browser launches, LLM booking, and backend order persistence
  test.setTimeout(120000);

  test('Book saree in Google Chrome, verify booking in Safari and Brave Browser', async () => {
    let bookedOrderId;
    const baseURL = 'http://localhost:5173';

    // =========================================================================
    // STEP 1: Book Saree in GOOGLE CHROME
    // =========================================================================
    console.log('[1/3] Launching Google Chrome...');
    const chromeBrowser = await chromium.launch({
      channel: 'chrome',
      headless: true,
    });
    const chromeContext = await chromeBrowser.newContext({ baseURL });
    const chromePage = await chromeContext.newPage();

    try {
      // 1.1 Sign in as Customer in Google Chrome
      console.log('Logging in as Customer in Google Chrome...');
      await chromePage.goto('/login');
      await chromePage.getByRole('button', { name: /^Customer$/i }).click();
      await chromePage.getByRole('button', { name: /^Sign In$/i }).click();
      await expect(chromePage).not.toHaveURL(/.*login/);

      // 1.2 Open AI Saree Stylist modal
      console.log('Opening AI Saree Stylist in Google Chrome...');
      const stylistBtn = chromePage.getByRole('button', { name: /Open AI Saree Stylist/i });
      await expect(stylistBtn).toBeVisible();
      await stylistBtn.click();
      await expect(chromePage.getByRole('heading', { name: 'SareeKart AI Stylist' })).toBeVisible();

      // 1.3 Ask AI Stylist for wedding silk under ₹20,000
      console.log('Querying AI Stylist...');
      const chip = chromePage.getByRole('button', { name: 'Wedding silk under ₹20,000' });
      await expect(chip).toBeVisible();
      await chip.click();

      // 1.4 Wait for AI Stylist recommendations and click "Book Now"
      const bookButtons = chromePage.getByRole('button', { name: /Book.*now/i });
      await expect(bookButtons.first()).toBeVisible({ timeout: 30000 });
      console.log('Booking recommended saree...');
      await bookButtons.first().click();

      // 1.5 Click "Go to Checkout" link
      const checkoutLink = chromePage.getByRole('button', { name: /Go to Checkout/i });
      await expect(checkoutLink).toBeVisible();
      await checkoutLink.click();
      await expect(chromePage).toHaveURL(/.*checkout/);

      // 1.6 Fill shipping address details on Checkout
      console.log('Filling shipping details in Google Chrome...');
      await expect(chromePage.getByText(/Delivery details/i)).toBeVisible({ timeout: 15000 });
      await chromePage.getByPlaceholder('Recipient name').fill('Priya Sharma');
      await chromePage.getByPlaceholder('10-digit mobile').fill('9876543210');
      await chromePage.getByPlaceholder('Flat, house, street').fill('42 Heritage Loom Lane, Indiranagar');
      await chromePage.getByPlaceholder('City').fill('Bengaluru');
      await chromePage.getByPlaceholder('State').fill('Karnataka');
      await chromePage.getByPlaceholder('6 digits').fill('560038');

      await chromePage.getByRole('button', { name: /Continue to payment/i }).click();

      // 1.7 Select Cash on Delivery and Place Order
      console.log('Placing order with Cash on Delivery in Google Chrome...');
      await expect(chromePage.getByText(/Payment method/i)).toBeVisible();
      await chromePage.getByRole('button', { name: /Place order/i }).click();

      // 1.8 Verify Order Confirmation and capture Order ID
      await expect(chromePage.getByText(/Order placed/i)).toBeVisible({ timeout: 20000 });
      await expect(chromePage.getByText(/Thank you for shopping/i)).toBeVisible();

      const orderIdElement = chromePage.locator('text=/#SK-\\d+/');
      await expect(orderIdElement).toBeVisible();
      bookedOrderId = (await orderIdElement.textContent()).trim();
      console.log(`[Google Chrome] Successfully booked saree! Created Order ID: ${bookedOrderId}`);
      expect(bookedOrderId).toMatch(/^#SK-\d+$/);
    } finally {
      await chromeContext.close();
      await chromeBrowser.close();
      console.log('[Google Chrome] Browser session closed.');
    }

    // =========================================================================
    // STEP 2: Verify Booking in SAFARI (WebKit Engine)
    // =========================================================================
    console.log('[2/3] Launching Safari (WebKit)...');
    const safariBrowser = await webkit.launch({
      headless: true,
    });
    const safariContext = await safariBrowser.newContext({ baseURL });
    const safariPage = await safariContext.newPage();

    try {
      // 2.1 Sign in with the exact same customer account in Safari
      console.log('Logging in as Customer in Safari...');
      await safariPage.goto('/login');
      await safariPage.getByRole('button', { name: /^Customer$/i }).click();
      await safariPage.getByRole('button', { name: /^Sign In$/i }).click();
      await expect(safariPage).not.toHaveURL(/.*login/);

      // 2.2 Navigate to My Orders in Safari
      console.log(`Navigating to /orders in Safari to check for ${bookedOrderId}...`);
      await safariPage.goto('/orders');
      await expect(safariPage.getByRole('heading', { name: /Your sarees, in motion/i })).toBeVisible({ timeout: 15000 });

      // 2.3 Assert that the exact Order ID booked in Google Chrome is rendered in Safari
      const orderHeadingInSafari = safariPage.getByRole('heading', { name: `Order ${bookedOrderId}` });
      await expect(orderHeadingInSafari).toBeVisible({ timeout: 15000 });
      console.log(`[Safari] Verified: Order ${bookedOrderId} booked in Google Chrome is visible in Safari!`);
    } finally {
      await safariContext.close();
      await safariBrowser.close();
      console.log('[Safari] Browser session closed.');
    }

    // =========================================================================
    // STEP 3: Verify Booking in BRAVE BROWSER
    // =========================================================================
    console.log('[3/3] Launching Brave Browser...');
    const braveBrowser = await chromium.launch({
      executablePath: '/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',
      headless: true,
    });
    const braveContext = await braveBrowser.newContext({ baseURL });
    const bravePage = await braveContext.newPage();

    try {
      // 3.1 Sign in with the exact same customer account in Brave Browser
      console.log('Logging in as Customer in Brave Browser...');
      await bravePage.goto('/login');
      await bravePage.getByRole('button', { name: /^Customer$/i }).click();
      await bravePage.getByRole('button', { name: /^Sign In$/i }).click();
      await expect(bravePage).not.toHaveURL(/.*login/);

      // 3.2 Navigate to My Orders in Brave Browser
      console.log(`Navigating to /orders in Brave Browser to check for ${bookedOrderId}...`);
      await bravePage.goto('/orders');
      await expect(bravePage.getByRole('heading', { name: /Your sarees, in motion/i })).toBeVisible({ timeout: 15000 });

      // 3.3 Assert that the exact Order ID booked in Google Chrome is rendered in Brave
      const orderHeadingInBrave = bravePage.getByRole('heading', { name: `Order ${bookedOrderId}` });
      await expect(orderHeadingInBrave).toBeVisible({ timeout: 15000 });
      console.log(`[Brave Browser] Verified: Order ${bookedOrderId} booked in Google Chrome is visible in Brave Browser!`);
    } finally {
      await braveContext.close();
      await braveBrowser.close();
      console.log('[Brave Browser] Browser session closed.');
    }

    console.log('ALL CROSS-BROWSER VERIFICATIONS PASSED: Google Chrome -> Safari -> Brave Browser!');
  });
});
