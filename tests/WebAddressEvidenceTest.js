// Pure recipient-identity regressions. Runs the exact shipping driver without a browser or network.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const sandbox = { window: {}, document: { querySelectorAll: () => [] }, URL };
vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../assets/web-driver.js'), 'utf8'), sandbox);
const identifies = sandbox.window.__morcoolDriver.identifies;
let cases = 0;
function test(expected, target, label, href = '', channel = 'telegram_web') {
  assert.equal(identifies(target, label, href, channel), expected, label);
  cases++;
}
test(true, '@demo_person', 'Demo Person\n@demo_person');
test(true, '@demo_person', 'Demo Person @demo_person');
test(true, '@demo_person', 'Name @DEMO_PERSON');
test(false, '@demo_person', 'Demo Person'); // Imported/display names alone are not proof.
test(false, '@demo_person', '@demo_person_other');
test(false, '@demo_person', '@not_demo_person');
test(false, '@demo_person', 'email@demo_person');
test(true, '@demo_person', 'Open', 'https://t.me/demo_person');
test(false, '@demo_person', 'Open', 'https://t.me.evil.example/demo_person');
test(false, '@demo_person', 'Open', 'https://t.me/demo_person_other');
test(true, '+70000000001', '+7 (000) 000-00-01');
test(false, '+70000000001', '+7 (000) 000-00-02');
test(false, '+70000000001', 'Last seen 7 000 000 00 01');
test(true, 'https://max.ru/u/AbCd', 'Open', '/u/AbCd', 'max_web');
test(false, 'https://max.ru/u/AbCd', 'Open', '/u/abcd', 'max_web');
test(false, '12345', '12345 unread messages', '', 'max_web');
test(true, '12345', 'Open', '/u/12345', 'max_web');
test(true, '@demo_person', 'Demo Person @demo_person', '', 'max_web');
console.log(`PASS: ${cases} exact recipient-address evidence checks`);
