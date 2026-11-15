// Renders art/paint-cat.html to assets/cat.webp (and a PNG preview path if given).
const { chromium } = require('playwright'); const fs = require('fs'); const path = require('path');
(async () => {
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 1200, height: 900 } });
  await p.goto('file://' + path.join(__dirname, 'paint-cat.html'));
  await p.waitForFunction(() => window.done, null, { timeout: 120000 });
  const url = await p.evaluate(() => document.getElementById('c').toDataURL('image/webp', 0.86));
  fs.mkdirSync(path.join(__dirname, '..', 'assets'), { recursive: true });
  fs.writeFileSync(path.join(__dirname, '..', 'assets', 'cat.webp'), Buffer.from(url.split(',')[1], 'base64'));
  if (process.argv[2]) await p.locator('#c').screenshot({ path: process.argv[2] });
  await b.close();
})();
