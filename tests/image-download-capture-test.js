const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app/src/main/assets/image-download-capture.js', 'utf8');
const origin = 'https://gemini.google.com';
const cdn = 'https://lh3.googleusercontent.com';

(async () => {
  const events = {};
  let clones = 0;
  let type = 'image/jpeg';
  let bytes = [255, 216, 255, 1];
  let expires;
  const window = {
    addEventListener(name, fn) { events[name] = fn; },
    async fetch(url) {
      return {url, ok: true, headers: new Headers({'Content-Type': type, 'Content-Length': '4'}), clone() {
        clones++;
        return new Response(Uint8Array.from(bytes));
      }};
    }
  };
  vm.runInNewContext(source, {window, location:{origin}, URL, Blob, Date, setTimeout(fn) { expires = fn; }, clearTimeout() {}});
  const take = (type = 'image/jpeg', size = 4) => window.__gpTakeDownloadImage(type, size);
  await window.fetch(cdn + '/image');
  events.click({isTrusted:false});
  await window.fetch(cdn + '/image');
  assert.equal(clones, 0);
  events.click({isTrusted:true});
  await window.fetch(cdn + '.evil.test/image');
  await window.fetch('https://lh3.google.com/metadata');
  type = 'text/plain';
  await window.fetch(cdn + '/metadata');
  assert.equal(clones, 0);
  type = 'image/jpeg';
  await window.fetch(cdn + '/image');
  assert.equal(await take('image/png'), null);
  assert.equal(await take(type, 5), null);
  assert.deepEqual([...new Uint8Array(await (await take()).arrayBuffer())], bytes);
  assert.equal(await take(), null, 'one shot');
  events.click({isTrusted:true});
  await window.fetch(cdn + '/image-1');
  await window.fetch(cdn + '/image-2');
  assert.equal(await take(), null, 'ambiguous match must not save another image');
  events.click({isTrusted:true});
  await window.fetch(cdn + '/image');
  expires();
  assert.equal(await take(), null);
  events.click({isTrusted:true});
  bytes = [255,216,255,1,2];
  await window.fetch(cdn + '/image');
  assert.equal(await take(), null, 'reject a response larger than its declared size');
  console.log('PASS: gesture-gated image capture, exact match, expiry, ambiguity and byte bounds');
})().catch(error => { console.error(error); process.exitCode = 1; });
