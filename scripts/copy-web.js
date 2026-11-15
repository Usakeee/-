// Copies the web app files into www/, which Capacitor packages into the Android app.
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
const out = path.join(root, 'www');
fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out);
for (const f of ['index.html', 'manifest.webmanifest', 'icon.svg', 'sw.js']) {
  fs.copyFileSync(path.join(root, f), path.join(out, f));
}
console.log('copied web files to www/');
