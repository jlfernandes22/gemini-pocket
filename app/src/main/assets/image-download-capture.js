(() => {
  if (location.origin !== 'https://gemini.google.com' || window.__gpTakeDownloadImage) return;
  const limit = 32 * 1024 * 1024;
  const lifetime = 15000;
  const originalFetch = window.fetch;
  let gesture, expiry;

  window.addEventListener('click', event => {
    if (!event.isTrusted) return;
    clearTimeout(expiry);
    const current = gesture = {until: Date.now() + lifetime, image: null, ambiguous: false};
    expiry = setTimeout(() => { if (gesture === current) gesture = null; }, lifetime);
  }, true);

  window.fetch = function (...args) {
    const current = gesture;
    return originalFetch.apply(this, args).then(response => {
      try {
        if (!current || current !== gesture || Date.now() >= current.until || !response.ok || current.ambiguous) return response;
        const url = new URL(response.url);
        if (url.protocol !== 'https:' || url.port || !/^lh\d+\.googleusercontent\.com$/.test(url.hostname)) return response;
        const type = (response.headers.get('Content-Type') || '').split(';')[0].trim().toLowerCase();
        const size = Number(response.headers.get('Content-Length'));
        if (!/^image\/(jpeg|png|webp|gif)$/.test(type) || !Number.isSafeInteger(size) || size <= 0 || size > limit) return response;
        if (current.image) {
          current.ambiguous = true;
          current.image = null;
        } else {
          const reader = response.clone().body.getReader();
          current.image = {type, size, blob: copy(reader, current, type, size)};
        }
      } catch (_) { /* Never interfere with Gemini's own request. */ }
      return response;
    });
  };

  async function copy(reader, current, type, size) {
    const chunks = [];
    let received = 0;
    try {
      while (gesture === current && !current.ambiguous && Date.now() < current.until) {
        const {done, value} = await reader.read();
        if (done) return received === size ? new Blob(chunks, {type}) : null;
        received += value.length;
        if (received > size) break;
        chunks.push(value);
      }
    } catch (_) { /* An interrupted response cannot be exported. */ }
    reader.cancel().catch(() => {});
    return null;
  }

  // ponytail: no inference when multiple image responses overlap. Revisit only if Gemini changes its download flow.
  window.__gpTakeDownloadImage = async (type, size) => {
    const current = gesture;
    const image = current && current.image;
    if (!image || current.ambiguous || image.type !== type || image.size !== size || Date.now() >= current.until) return null;
    current.image = null;
    const blob = await image.blob;
    if (current !== gesture || current.ambiguous || current.image || Date.now() >= current.until) return null;
    gesture = null;
    clearTimeout(expiry);
    return blob;
  };
})();
