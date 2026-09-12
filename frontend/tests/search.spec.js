import { test, expect } from '@playwright/test';

test.describe('Search', () => {
  test('passes the selected sort to the search API', async ({ page }) => {
    let sortedRequestUrl = '';
    page.on('request', (request) => {
      if (request.url().includes('/api/products/search') && request.url().includes('sortBy=price')) {
        sortedRequestUrl = request.url();
      }
    });

    await page.goto('http://localhost:5173/products?search=silk');
    await expect(page.getByRole('heading', { name: 'Handloom, with a point of view.' })).toBeVisible();
    await page.getByRole('combobox', { name: 'Sort products' }).selectOption('price-asc');

    await expect.poll(() => sortedRequestUrl).toContain('sortDir=asc');
  });
});
