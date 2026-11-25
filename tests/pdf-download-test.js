const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app/src/main/assets/pdf-download.js', 'utf8');
const receiver = fs.readFileSync('app/src/main/assets/image-download.js', 'utf8');
const origin = 'https://gemini.google.com';
const url = 'blob:null/12345678-1234-4321-abcd-123456789abc';
const pdf = new Blob(['%PDF-1.7\n%%EOF\n'], {type: 'application/pdf'});

function frame(parentOrigin = origin) {
  const topListeners = [], childListeners = [], native = [];
  const top = {addEventListener: (_, fn) => topListeners.push(fn),
    removeEventListener: (_, fn) => topListeners.splice(topListeners.indexOf(fn), 1),
    postMessage(data, target) {
      assert.equal(target, origin);
      for (const fn of [...topListeners]) fn({source: child, origin: 'null', data, ports: []});
    }};
  top.top = top;
  const child = {top, addEventListener: (_, fn) => childListeners.push(fn),
    postMessage(data, target, ports) {
      assert.equal(target, '*'); // Only opaque destinations require a wildcard target.
      child.send(data, origin, top, ports);
    }, send(data, sender, from, ports) {
      for (const fn of childListeners) fn({source: from, origin: sender, data, ports});
    }};
  child.origin = 'null';
  class FrameURL extends URL { static createObjectURL() { return url; } }
  function context(window, location, URLClass) {
    return vm.createContext({window, location, URL: URLClass, Blob, AbortController,
      setTimeout: () => 1, clearTimeout() {}, btoa: s => Buffer.from(s, 'latin1').toString('base64')});
  }
  const topContext = context(top, {origin}, URL);
  vm.runInContext(source, topContext);
  vm.runInContext(source, context(child, {ancestorOrigins: [parentOrigin]}, FrameURL));
  const port = {postMessage(data) { native.push(data); if (data !== 'done') this.onmessage?.({data: 'next'}); }, close() {}};
  return {top, child, native, create: blob => FrameURL.createObjectURL(blob), port, request(size) {
    const request = {url, size, type: 'application/pdf', key: 'native-only', limit: 65536};
    vm.runInContext(receiver.replace('/* REQUEST */ null', JSON.stringify(request)), topContext);
    for (const fn of [...topListeners]) fn({source: null, origin: '', data: request.key, ports: [port]});
  }};
}

(async () => {
  const fixture = frame();
  fixture.create(pdf);
  assert.equal(fixture.top.__gpForwardPdf?.(url, pdf.size + 1, fixture.port), false, 'Wrong length cannot receive the port');
  fixture.child.send({kind: 'gp-pdf-read', url, size: pdf.size}, 'https://evil.example', fixture.top, [fixture.port]);
  assert.equal(fixture.native.length, 0, 'Untrusted sender cannot read the PDF');
  fixture.request(pdf.size);
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(fixture.native.pop(), 'done');
  assert.deepEqual(Buffer.concat(fixture.native.map(s => Buffer.from(s, 'base64'))), Buffer.from(await pdf.arrayBuffer()));
  assert.equal(fixture.top.__gpForwardPdf(url, pdf.size, fixture.port), false, 'Download is one-shot');
  const unrelated = frame('https://evil.example');
  unrelated.create(pdf);
  assert.equal(unrelated.top.__gpForwardPdf(url, pdf.size, unrelated.port), false, 'Unrelated top origin cannot capture files');
  const nonPdf = frame();
  nonPdf.create(new Blob(['not a PDF'], {type: 'text/plain'}));
  assert.equal(nonPdf.top.__gpForwardPdf(url, 9, nonPdf.port), false, 'Other file types do not enter this path');
  console.log('PASS: opaque PDF reaches Android only for an exact Canvas URL and length');
})().catch(e => {console.error(e); process.exitCode = 1;});
