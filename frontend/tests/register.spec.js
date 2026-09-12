import { test, expect } from '@playwright/test';

const registrationData = {
  firstName: '  Test User  ',
  lastName: '  Generated  ',
  email: '  registration-flow@example.test  ',
  mobile: ' 9876543210 ',
  password: 'test1234',
  confirmPassword: 'test1234',
};

async function openRegisterPage(page) {
  await page.goto('http://127.0.0.1:5173/register?redirect=/orders');
  await expect(page.getByRole('heading', { name: 'Make room for color.' })).toBeVisible();
}

async function fillRegistrationForm(page, data = registrationData) {
  await page.getByRole('textbox', { name: 'First name *' }).fill(data.firstName);
  await page.getByRole('textbox', { name: 'Last name' }).fill(data.lastName);
  await page.getByRole('textbox', { name: 'Email address *' }).fill(data.email);
  await page.getByRole('textbox', { name: 'Mobile number *' }).fill(data.mobile);
  await page.getByRole('textbox', { name: 'Password *' }).fill(data.password);
  await page.getByRole('textbox', { name: 'Confirm *' }).fill(data.confirmPassword);
}

test.describe('Registration', () => {
  test('validates required fields and password confirmation', async ({ page }) => {
    await openRegisterPage(page);

    await page.getByRole('button', { name: 'Create Account' }).click();
    await expect(page.getByRole('alert')).toContainText('Please fill in all required fields.');

    await fillRegistrationForm(page, { ...registrationData, confirmPassword: 'different123' });
    await page.getByRole('button', { name: 'Create Account' }).click();
    await expect(page.getByRole('alert')).toContainText('Passwords do not match.');
  });

  test('sends only normalized registration fields and redirects to orders', async ({ page }) => {
    let requestBody;

    await page.route('**/api/auth/register', async (route) => {
      requestBody = route.request().postDataJSON();
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'User registered successfully',
          data: {
            token: 'generated-test-token',
            id: 999,
            firstName: 'Test User',
            lastName: 'Generated',
            email: 'registration-flow@example.test',
            role: 'CUSTOMER',
          },
        }),
      });
    });

    await page.route('**/api/orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] }),
      });
    });

    await openRegisterPage(page);
    await fillRegistrationForm(page);
    await page.getByRole('button', { name: 'Create Account' }).click();

    await expect(page).toHaveURL(/\/orders$/);
    expect(requestBody).toEqual({
      firstName: 'Test User',
      lastName: 'Generated',
      email: 'registration-flow@example.test',
      mobile: '9876543210',
      password: 'test1234',
    });
    expect(requestBody).not.toHaveProperty('confirmPassword');
  });

  test('shows the backend duplicate-email message and stays on registration', async ({ page }) => {
    await page.route('**/api/auth/register', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Email address already in use.' }),
      });
    });

    await openRegisterPage(page);
    await fillRegistrationForm(page);
    await page.getByRole('button', { name: 'Create Account' }).click();

    await expect(page).toHaveURL(/\/register\?redirect=\/orders$/);
    await expect(page.getByRole('alert')).toContainText('Email address already in use.');
  });

  test('shows a clear backend-unavailable message', async ({ page }) => {
    await page.route('**/api/auth/register', async (route) => {
      await route.abort('failed');
    });

    await openRegisterPage(page);
    await fillRegistrationForm(page);
    await page.getByRole('button', { name: 'Create Account' }).click();

    await expect(page.getByRole('alert')).toContainText('Cannot connect to server');
  });

  test('preserves the redirect query on the sign-in link', async ({ page }) => {
    await openRegisterPage(page);
    await expect(page.getByRole('link', { name: 'Sign In', exact: true })).toHaveAttribute('href', '/login?redirect=/orders');
  });
});
