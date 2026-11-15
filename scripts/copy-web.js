// Copies the web app files into www/, which Capacitor packages into the Android app.
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
const out = path.join(root, 'www');
fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out);
for (const f of ['index.html', 'manifest.webmanifest', 'icon.png', 'sw.js']) {
  fs.copyFileSync(path.join(root, f), path.join(out, f));
}
fs.cpSync(path.join(root, 'assets'), path.join(out, 'assets'), { recursive: true });
console.log('copied web files to www/');
