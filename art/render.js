// Renders the artwork: assets/cat.webp (everyday) and assets/xmas.webp (Christmas season).
// Usage: node art/render.js [previewDir]
const { chromium } = require('playwright'); const fs = require('fs'); const path = require('path');
const dir = __dirname, out = path.join(dir, '..', 'assets'), preview = process.argv[2];
async function paint(page, file, hash) {
  await page.goto('about:blank');
  await page.goto('file://' + path.join(dir, file) + (hash || ''));
  await page.waitForFunction(() => window.done, null, { timeout: 180000 });
}
const save = (url, file) => fs.writeFileSync(file, Buffer.from(url.split(',')[1], 'base64'));
(async () => {
  fs.mkdirSync(out, { recursive: true });
  const b = await chromium.launch({ args: ['--allow-file-access-from-files'] }); const p = await b.newPage({ viewport: { width: 1200, height: 900 } });
  await paint(p, 'paint-cat.html');
  save(await p.evaluate(() => document.getElementById('c').toDataURL('image/webp', 0.86)), path.join(out, 'cat.webp'));
  await paint(p, 'paint-cat.html', '#cut');
  save(await p.evaluate(() => document.getElementById('c').toDataURL('image/png')), path.join(dir, 'cat-cut.png'));
  await paint(p, 'paint-reindeer.html');
  save(await p.evaluate(() => document.getElementById('c').toDataURL('image/png')), path.join(dir, 'reindeer-cut.png'));
  if (preview) await p.locator('#c').screenshot({ path: path.join(preview, 'reindeer.png'), omitBackground: true });
  await paint(p, 'paint-xmas.html');
  save(await p.evaluate(() => document.getElementById('c').toDataURL('image/webp', 0.86)), path.join(out, 'xmas.webp'));
  if (preview) await p.locator('#c').screenshot({ path: path.join(preview, 'xmas.png') });
  await b.close();
})();
