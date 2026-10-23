// test-phase7.js - Automated validation tests for Phase 7 static site
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { sanitize, validateEmail, validatePhone, validatePincode, validateBloodGroup } = require('./js/validate');

console.log('--- Testing Phase 7: Static Site & Client-Side Validation ---');

// 1. Verify all 4 HTML files exist and contain semantic elements
const requiredPages = ['index.html', 'about.html', 'donate.html', 'contact.html'];
requiredPages.forEach((page) => {
  const filePath = path.join(__dirname, page);
  assert.ok(fs.existsSync(filePath), `${page} must exist`);
  const content = fs.readFileSync(filePath, 'utf8');

  // Verify semantic HTML5 tags
  ['<header>', '<nav', '<main>', '<section>', '<footer>'].forEach((tag) => {
    assert.ok(content.includes(tag), `${page} must contain semantic tag ${tag}`);
  });
  console.log(`✔ Verified ${page} contains semantic HTML structure.`);
});

// 2. Verify donate.html contains required native HTML validation attributes
const donateHtml = fs.readFileSync(path.join(__dirname, 'donate.html'), 'utf8');
assert.ok(donateHtml.includes('required'), 'donate.html must contain required attributes');
assert.ok(donateHtml.includes('pattern='), 'donate.html must contain pattern attributes');
assert.ok(donateHtml.includes('min='), 'donate.html must contain min attribute');
assert.ok(donateHtml.includes('max='), 'donate.html must contain max attribute');
assert.ok(donateHtml.includes('type="email"'), 'donate.html must have type="email"');
assert.ok(donateHtml.includes('type="tel"'), 'donate.html must have type="tel"');
assert.ok(donateHtml.includes('type="date"'), 'donate.html must have type="date"');
console.log('✔ donate.html verified for native HTML validation attributes (required, pattern, min/max, email/tel/date).');

// 3. Verify CSS file exists and has media queries & Flexbox/Grid
const css = fs.readFileSync(path.join(__dirname, 'css/style.css'), 'utf8');
assert.ok(css.includes('display: flex') || css.includes('display:flex'), 'style.css must use Flexbox');
assert.ok(css.includes('display: grid') || css.includes('display:grid'), 'style.css must use Grid');
const mediaQueries = css.match(/@media\s*\([^)]+\)/g);
assert.ok(mediaQueries && mediaQueries.length >= 2, 'style.css must contain at least 2 media queries');
console.log(`✔ style.css verified with Flexbox, Grid, and ${mediaQueries.length} Media Queries.`);

// 4. Test sanitization function
const dirtyXSS = '<script>alert("hacked")</script><b>Hello</b>';
const cleaned = sanitize(dirtyXSS);
assert.strictEqual(cleaned.includes('<'), false, 'sanitize must remove <');
assert.strictEqual(cleaned.includes('>'), false, 'sanitize must remove >');
console.log('✔ Sanitize function strips < and > tags properly.');

// 5. Test RegExp validation helpers
assert.strictEqual(validateEmail('test@hospital.org'), true);
assert.strictEqual(validateEmail('invalid-email'), false);

assert.strictEqual(validatePhone('9822012345'), true);
assert.strictEqual(validatePhone('12345'), false);

assert.strictEqual(validatePincode('411001'), true);
assert.strictEqual(validatePincode('41100'), false);

assert.strictEqual(validateBloodGroup('O+'), true);
assert.strictEqual(validateBloodGroup('X-'), false);
console.log('✔ Regular expression validators (Email, Phone, Pincode, BloodGroup) verified.');

console.log('\n--- ALL PHASE 7 TESTS PASSED SUCCESSFULLY ---');
