(() => {
  const request = /* REQUEST */ null;
  const origin = 'https://gemini.google.com';
  if (location.origin !== origin || !request) return false;
  const url = new URL(request.url);
  if (url.protocol !== 'blob:' || (url.origin !== origin &&
      !/^blob:null\/[\da-f]{8}(?:-[\da-f]{4}){3}-[\da-f]{12}$/i.test(url.href))) return false;
  const opaque = url.origin === 'null';
  // Opaque blobs cannot be read from this document. Use the matched response instead.

  const receive = async event => {
    // Android's native WebMessage has no sending window or web origin.
    if (event.source !== null || (event.origin !== '' && event.origin !== 'null') ||
        event.data !== request.key || event.ports.length !== 1) return;
    clearTimeout(expiry);
    window.removeEventListener('message', receive);
    const port = event.ports[0];
    const abort = new AbortController();
    const deadline = setTimeout(() => abort.abort(), 45000);
    const send = data => new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('File transfer timed out')), 5000);
      port.onmessage = reply => {
        clearTimeout(timeout);
        port.onmessage = null;
        if (reply.data === 'next') resolve();
        else reject(new Error('File transfer cancelled'));
      };
      port.postMessage(data);
    });
    try {
      let stream;
      if (opaque) {
        const file = (request.type === 'text/plain' ? window.__gpTakePlaintextBlock?.(request.size) : null)
          || await window.__gpTakeDownloadImage?.(request.type, request.size, request.name);
        if (!file || file.size !== request.size || file.type !== request.type) throw new Error('Unmatched download');
        stream = file.stream();
      } else {
        const retained = window.__gpTakeDownloadBlob?.(url.href, request.size);
        if (retained) {
          await send('text-name:' + retained.name);
          stream = retained.blob.stream();
        } else {
          const response = await fetch(url.href, {
            mode: 'same-origin', credentials: 'same-origin', redirect: 'error', signal: abort.signal
          });
          if (!response.ok || !response.body) throw new Error('Unreadable download');
          stream = response.body;
        }
      }
      const reader = stream.getReader();
      let total = 0;
      for (;;) {
        const {value, done} = await reader.read();
        if (done) break;
        total += value.length;
        if (total > request.limit) throw new Error('File too large');
        for (let offset = 0; offset < value.length; offset += 32768) {
          const chunk = value.subarray(offset, offset + 32768);
          let text = '';
          for (const byte of chunk) text += String.fromCharCode(byte);
          await send(btoa(text));
        }
      }
      port.postMessage('done');
    } catch (_) {
      port.postMessage('error');
    } finally {
      clearTimeout(deadline);
      abort.abort();
      port.close();
    }
  };
  const expiry = setTimeout(() => window.removeEventListener('message', receive), 5000);
  window.addEventListener('message', receive);
  return true;
})();
