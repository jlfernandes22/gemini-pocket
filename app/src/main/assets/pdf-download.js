(() => {
  const origin = 'https://gemini.google.com';
  const address = /^blob:null\/[\da-f]{8}(?:-[\da-f]{4}){3}-[\da-f]{12}$/i;
  const limit = 32 * 1024 * 1024;
  const lifetime = 60000;
  const valid = (url, size) => typeof url === 'string' && address.test(url) &&
    Number.isSafeInteger(size) && size > 0 && size <= limit;

  if (window === window.top) {
    if (location.origin !== origin) return;
    const offers = new Map();
    window.addEventListener('message', event => {
      const offer = event.data;
      if (event.origin !== 'null' || !event.source || event.source === window ||
          offer?.kind !== 'gp-pdf-offer' || !valid(offer.url, offer.size)) return;
      if (offers.size >= 8) offers.delete(offers.keys().next().value);
      offers.set(offer.url, {frame: event.source, size: offer.size, expires: Date.now() + lifetime});
    });
    window.__gpForwardPdf = (url, size, port) => {
      const offer = offers.get(url);
      if (!offer || offer.expires <= Date.now() || offer.size !== size) return false;
      offers.delete(url);
      // Opaque frames require '*'. Transfer only the port, no data or credentials.
      offer.frame.postMessage({kind: 'gp-pdf-read', url, size}, '*', [port]);
      return true;
    };
    return;
  }

  // Only a sandboxed descendant of Gemini can offer its own generated PDF.
  const ancestors = location.ancestorOrigins;
  if (window.origin !== 'null' || ancestors?.[ancestors.length - 1] !== origin) return;
  const create = URL.createObjectURL;
  let latest, expiry;
  URL.createObjectURL = function (object) {
    const url = create.call(this, object);
    if (object instanceof Blob && object.type === 'application/pdf' && valid(url, object.size)) {
      clearTimeout(expiry);
      const offer = latest = {url, blob: object};
      expiry = setTimeout(() => { if (latest === offer) latest = undefined; }, lifetime);
      window.top.postMessage({kind: 'gp-pdf-offer', url, size: object.size}, origin);
    }
    return url;
  };
  window.addEventListener('message', async event => {
    const request = event.data;
    if (event.origin !== origin || event.source !== window.top || request?.kind !== 'gp-pdf-read' ||
        event.ports.length !== 1) return;
    const port = event.ports[0];
    if (!latest || request.url !== latest.url || request.size !== latest.blob.size) {
      port.postMessage('error'); port.close(); return;
    }
    const blob = latest.blob;
    latest = undefined;
    clearTimeout(expiry);
    try {
      for (let offset = 0; offset < blob.size; offset += 32768) {
        const bytes = new Uint8Array(await blob.slice(offset, offset + 32768).arrayBuffer());
        let text = '';
        for (const byte of bytes) text += String.fromCharCode(byte);
        await new Promise((resolve, reject) => {
          const timer = setTimeout(reject, 5000);
          port.onmessage = reply => {
            clearTimeout(timer);
            port.onmessage = null;
            if (reply.data === 'next') resolve();
            else reject();
          };
          port.postMessage(btoa(text));
        });
      }
      port.postMessage('done');
    } catch (_) {
      port.postMessage('error');
    } finally {
      port.close();
    }
  });
})();
