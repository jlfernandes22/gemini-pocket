const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const script = fs.readFileSync('app/src/main/assets/image-download.js', 'utf8');
const origin = 'https://gemini.google.com';
const url = 'blob:' + origin + '/d1a4c320-70fa-4720-82b6-ad2523dd02dd';

function receive({address = url, sender = '', size = 5, eventKey = 'one-time-key', captured = true, retained, plaintext, type = 'image/jpeg'} = {}) {
  const payload = Buffer.alloc(Math.min(size, 65536), 71);
  const messages = [];
  let port, listener, fetches = 0;
  const window = {
    __gpTakePlaintextBlock(length) {
      assert.equal(type, 'text/plain');
      assert.equal(length, size);
      return plaintext;
    },
    __gpTakeDownloadBlob(address, length) {
      assert.equal(address, url);
      assert.equal(length, size);
      return retained;
    },
    async __gpTakeDownloadImage(type, length) {
      assert.equal(length, size);
      return captured ? new Blob([payload], {type}) : null;
    },
    addEventListener(_, callback) { listener = callback; },
    removeEventListener(_, callback) { if (listener === callback) listener = null; }
  };
  let settle;
  const settled = new Promise(resolve => { settle = resolve; });
  port = {
    postMessage(value) {
      messages.push(value);
      if (value === 'done' || value === 'error') settle();
    },
    close() {},
    start() {}
  };
  vm.runInNewContext(script.replace('/* REQUEST */ null', JSON.stringify({url: address, key: 'one-time-key', limit: 65536, type, size})), {
    window, location: {origin}, URL, AbortSignal, AbortController, setTimeout: () => 1, clearTimeout() {},
    fetch: async (_, options) => {
      fetches++;
      assert.equal(options.credentials, 'same-origin');
      assert.equal(options.redirect, 'error');
      return {ok: true, body: {getReader() {
        let left = size;
        return {async read() {
          const n = Math.min(left, payload.length);
          left -= n;
          return n ? {value: payload.subarray(0, n), done: false} : {done: true};
        }};
      }}};
    },
    btoa: str => Buffer.from(str, 'latin1').toString('base64')
  });
  if (listener) listener({source: sender ? window : null, origin: sender, data: eventKey, ports: [port]});
  return {messages, settled, fetches: () => fetches, next: () => port.onmessage({data:'next'})};
}

// A stalled mocked transfer must fail, not let Node exit with unresolved promises.
const watchdog = setTimeout(() => {
  console.error('Image transfer test did not settle');
  process.exitCode = 1;
}, 3000);

(async () => {
  for (const options of [{sender: 'https://evil.test'}, {sender: origin}, {eventKey: 'wrong'}, {address: 'https://evil.test/image'}, {address: origin + '/app'}, {address: 'blob:null/not-a-uuid'}]) {
    const rejected = receive(options);
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(rejected.fetches(), 0);
    assert.deepEqual(rejected.messages, []);
  }
  const valid = receive({size: 50000});
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(valid.messages.length, 1, 'only one chunk until Android acknowledges it');
  valid.next();
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(valid.messages.length, 2);
  valid.next();
  await valid.settled;
  assert.equal(valid.messages.pop(), 'done');
  assert.deepEqual(Buffer.concat(valid.messages.map(value => Buffer.from(value, 'base64'))), Buffer.alloc(50000, 71));
  const documentBlob = new Blob(['# Notes\n'], {type: 'application/octet-stream'});
  const document = receive({size: documentBlob.size, type: documentBlob.type, retained: {blob: documentBlob, name: 'notes.md'}});
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(document.fetches(), 0, 'Do not refetch a retained URL');
  assert.equal(document.messages[0], 'text-name:notes.md');
  document.next();
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(Buffer.from(document.messages[1], 'base64').toString(), '# Notes\n');
  document.next();
  await document.settled;
  assert.equal(document.messages[2], 'done');
  const opaque = receive({address: 'blob:null/d1a4c320-70fa-4720-82b6-ad2523dd02dd'});
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(opaque.fetches(), 0, 'do not fetch an opaque blob from the top page');
  assert.equal(opaque.messages.length, 1, 'transfer the matched captured bytes');
  opaque.next();
  await opaque.settled;
  assert.equal(opaque.messages.at(-1), 'done');
  const unmatched = receive({address: 'blob:null/d1a4c320-70fa-4720-82b6-ad2523dd02dd', captured:false});
  await unmatched.settled;
  assert.deepEqual(unmatched.messages, ['error']);
  const block = new Blob(['olá\n  indented\n'], {type: 'text/plain'});
  const text = receive({address: 'blob:null/d1a4c320-70fa-4720-82b6-ad2523dd02dd', captured: false,
    type: 'text/plain', size: block.size, plaintext: block});
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(text.fetches(), 0);
  assert.equal(Buffer.from(text.messages[0], 'base64').toString(), await block.text());
  text.next();
  await text.settled;
  assert.equal(text.messages[1], 'done');
  const oversized = receive({size: 80000});
  await new Promise(resolve => setImmediate(resolve));
  oversized.next();
  await new Promise(resolve => setImmediate(resolve));
  oversized.next();
  await oversized.settled;
  assert.equal(oversized.messages.at(-1), 'error');
  console.log('PASS: scoped image transfer, byte limit, and native acknowledgement');
})().then(() => clearTimeout(watchdog)).catch(error => {
  clearTimeout(watchdog);
  console.error(error);
  process.exitCode = 1;
});
