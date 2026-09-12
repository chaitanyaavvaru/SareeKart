import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('BROWSER CONSOLE:', msg.text()));
  page.on('pageerror', exception => console.log(`BROWSER ERROR: "${exception}"`));
  
  console.log('Navigating to localhost:5173...');
  await page.goto('http://localhost:5173/');
  
  await page.waitForTimeout(2000);
  
  console.log('Root HTML:');
  const rootHtml = await page.evaluate(() => document.getElementById('root').innerHTML);
  console.log(rootHtml.substring(0, 5000));
  
  await browser.close();
})();
