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
  let expires, disposition = '';
  const window = {
    addEventListener(name, fn) { events[name] = fn; },
    async fetch(url) {
      return {url, ok: true, headers: new Headers({'Content-Type': type, 'Content-Length': '4', 'Content-Disposition': disposition}), clone() {
        clones++;
        return new Response(Uint8Array.from(bytes));
      }};
    }
  };
  class PageURL extends URL {
    static createObjectURL(blob) {
      const address = `blob:${origin}/d1a4c320-70fa-4720-82b6-${String(blobs.size + 1).padStart(12, '0')}`;
      blobs.set(address, blob);
      return address;
    }
    static revokeObjectURL(address) { blobs.set(address, null); }
  }
  class Anchor { click() {} }
  const blobs = new Map();
  vm.runInNewContext(source, {window, location:{origin}, URL: PageURL, HTMLAnchorElement: Anchor, Blob, Date, setTimeout(fn) { expires = fn; }, clearTimeout() {}});
  const take = (type = 'image/jpeg', size = 4, name) => window.__gpTakeDownloadImage(type, size, name);
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
  events.click({isTrusted:true});
  type = 'text/plain';
  bytes = [35, 32, 97, 10];
  const before = clones;
  await window.fetch(origin + '/download');
  disposition = 'attachment; filename="notes.md"';
  await window.fetch(cdn + '/download');
  assert.equal(clones, before, 'Do not capture ordinary responses or CDN text');
  await window.fetch(origin + '/download');
  assert.equal(await take('text/plain', 4, 'other.md'), null, 'Reject mismatched filename');
  assert.equal(await (await take('text/plain', 4, 'notes.md')).text(), '# a\n');
  const blob = new Blob(['# Notes\n'], {type: 'application/octet-stream'});
  events.click({isTrusted:true});
  const anchor = new Anchor();
  anchor.href = PageURL.createObjectURL(blob);
  anchor.download = 'notes.md';
  anchor.click(); // A detached download link does not bubble to window.
  PageURL.revokeObjectURL(anchor.href);
  assert.equal(blobs.get(anchor.href), null, 'Do not delay Gemini’s revocation');
  assert.equal(window.__gpTakeDownloadBlob(anchor.href, blob.size + 1), null, 'Reject wrong length');
  const retained = window.__gpTakeDownloadBlob(anchor.href, blob.size);
  assert.equal(retained.name, 'notes.md');
  assert.equal(await retained.blob.text(), '# Notes\n');
  assert.equal(window.__gpTakeDownloadBlob(anchor.href, blob.size), null, 'Only consume the exact URL once');
  for (const name of ['snippet', 'snippet.plaintext', 'snippet.text', 'file.html']) {
    events.click({isTrusted:true});
    anchor.href = PageURL.createObjectURL(blob);
    anchor.download = name;
    anchor.click();
    PageURL.revokeObjectURL(anchor.href);
    const saved = window.__gpTakeDownloadBlob(anchor.href, blob.size);
    assert.equal(saved?.name, 'Gemini-document.txt', `Use a safe text name for ${name}`);
    assert.equal(await saved.blob.text(), '# Notes\n');
  }
  events.click({isTrusted:true});
  anchor.href = PageURL.createObjectURL(blob);
  anchor.download = '';
  anchor.click();
  // This lookup represents WebView's DownloadListener, not a page click. It
  // must work when the site dispatches a download without calling anchor.click().
  PageURL.revokeObjectURL(anchor.href);
  assert.equal(window.__gpTakeDownloadBlob(anchor.href, blob.size + 1), null);
  assert.equal(window.__gpTakeDownloadBlob(anchor.href, blob.size)?.name, 'Gemini-document.txt');
  assert.equal(window.__gpTakeDownloadBlob(anchor.href, blob.size), null);
  events.click({isTrusted:true});
  const markdown = new Blob(['# Notes\n'], {type: 'text/markdown; charset=utf-8'});
  const markdownUrl = PageURL.createObjectURL(markdown);
  PageURL.revokeObjectURL(markdownUrl);
  assert.equal(window.__gpTakeDownloadBlob(markdownUrl, markdown.size)?.name, 'Gemini-document.md');
  events.click({isTrusted:true});
  const html = new Blob(['<script>never export as HTML</script>'], {type: 'text/html'});
  anchor.href = PageURL.createObjectURL(html);
  anchor.download = 'snippet.txt';
  anchor.click();
  assert.equal(window.__gpTakeDownloadBlob(anchor.href, html.size), null, 'Do not capture active MIME types');
  const plain = 'olá\n  indented\n';
  const plainSize = new Blob([plain]).size;
  const code = {textContent: plain};
  const pre = {contains: () => false, querySelector: () => code};
  const container = {querySelectorAll: () => [pre]};
  const button = {getAttribute: key => key === 'aria-label' ? 'Transferir código' : null, parentElement: container};
  const target = {closest: selector => selector.includes('button') ? button : null};
  events.click({isTrusted:false, target});
  assert.equal(window.__gpTakePlaintextBlock?.(plainSize) ?? null, null, 'Synthetic clicks cannot select chat text');
  events.click({isTrusted:true, target});
  assert.equal(window.__gpTakePlaintextBlock(plainSize - 2), null, 'Reject unrelated byte counts');
  assert.equal(await window.__gpTakePlaintextBlock(plainSize - 1)?.text(), plain.trim(), 'Match Gemini’s trimmed UTF-8 download');
  assert.equal(window.__gpTakePlaintextBlock(plainSize), null, 'Consume only once');
  events.click({isTrusted:true, target});
  assert.equal(await window.__gpTakePlaintextBlock(plainSize)?.text(), plain, 'Keep whitespace when the original byte count matches');
  button.getAttribute = () => 'Copiar código';
  events.click({isTrusted:true, target});
  assert.equal(window.__gpTakePlaintextBlock(plainSize), null, 'Copy is not a download');
  console.log('PASS: bounded response capture, revoked document blobs and selected plaintext blocks');
})().catch(error => { console.error(error); process.exitCode = 1; });
