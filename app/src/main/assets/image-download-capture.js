(() => {
  if (location.origin !== 'https://gemini.google.com' || window.__gpTakeDownloadImage) return;
  const limit = 32 * 1024 * 1024;
  const lifetime = 15000;
  const originalFetch = window.fetch;
  let gesture, expiry;
  const blobAddress = /^blob:https:\/\/gemini\.google\.com\/[\da-f]{8}(?:-[\da-f]{4}){3}-[\da-f]{12}$/i;
  const textName = /^(?![. ])[\p{L}\p{M}\p{N}_ .()\-]{1,110}\.(txt|md|markdown)$/iu;
  const create = URL.createObjectURL;
  const anchorClick = HTMLAnchorElement.prototype.click;

  function select(anchor) {
    if (!gesture || Date.now() >= gesture.until || !anchor) return;
    const entry = gesture.blobs.get(anchor.href);
    if (!entry || (anchor.download === '' && !anchor.hasAttribute?.('download'))) return;
    // A code block can suggest a language-based name instead of a text extension.
    entry.name = textName.test(anchor.download || '') ? anchor.download : 'Gemini-document.txt';
  }

  function selectedCode(target) {
    const button = target?.closest?.('button, [role="button"]');
    const download = /^(?:(?:download|transferir|baixar)(?: (?:code|código|ficheiro|arquivo))?|download[-_]code(?:[-_]button)?|code[-_]download(?:[-_]button)?)$/i;
    if (!button || !['aria-label', 'title', 'mattooltip', 'data-test-id'].some(key =>
      download.test((button.getAttribute(key) || '').trim()))) return null;
    for (let parent = button.parentElement, depth = 0; parent && depth < 6; parent = parent.parentElement, depth++) {
      const blocks = parent.querySelectorAll('pre');
      if (!blocks.length) continue;
      if (blocks.length !== 1 || blocks[0].contains(button)) return null;
      const code = blocks[0].querySelector('code') || blocks[0];
      const text = code.textContent;
      return text && text.length <= limit ? text : null;
    }
    return null;
  }

  window.__gpTakePlaintextBlock = size => {
    const text = gesture?.plaintext;
    if (!text || Date.now() >= gesture.until || size <= 0 || size > limit) return null;
    let block = new Blob([text], {type: 'text/plain'});
    // Gemini can trim the rendered code block before exporting it.
    if (block.size !== size) block = new Blob([text.trim()], {type: 'text/plain'});
    if (block.size !== size) return null;
    gesture.plaintext = null;
    return block;
  };

  window.addEventListener('click', event => {
    if (!event.isTrusted) return;
    // A real click can select a link made during the previous gesture.
    const anchor = event.target?.closest?.('a[download]');
    select(anchor);
    if (gesture?.blobs.get(anchor?.href)?.name) return;
    clearTimeout(expiry);
    const current = gesture = {until: Date.now() + lifetime, image: null, ambiguous: false, blobs: new Map(), bytes: 0};
    current.plaintext = selectedCode(event.target);
    expiry = setTimeout(() => { if (gesture === current) gesture = null; }, lifetime);
  }, true);

  URL.createObjectURL = function (blob) {
    const url = create.call(this, blob);
    if (gesture && Date.now() < gesture.until && blob instanceof Blob && blob.size > 0 &&
        gesture.bytes + blob.size <= limit && gesture.blobs.size < 8 && blobAddress.test(url) &&
        /^(?:text\/(?:plain|markdown|x-markdown)(?:;.*)?|application\/octet-stream)?$/i.test(blob.type)) {
      gesture.blobs.set(url, {blob, name: null});
      gesture.bytes += blob.size;
    }
    return url;
  };

  HTMLAnchorElement.prototype.click = function (...args) {
    select(this);
    return anchorClick.apply(this, args);
  };

  window.__gpTakeDownloadBlob = (url, size) => {
    const entry = gesture?.blobs.get(url);
    if (!entry || Date.now() >= gesture.until || (size > 0 && entry.blob.size !== size)) return null;
    gesture.blobs.delete(url);
    gesture.bytes -= entry.blob.size;
    // The exact URL from DownloadListener also identifies downloads without an anchor name.
    const markdown = /^text\/(?:x-)?markdown(?:;|$)/i.test(entry.blob.type);
    return {blob: entry.blob, name: entry.name || (markdown ? 'Gemini-document.md' : 'Gemini-document.txt')};
  };

  window.fetch = function (...args) {
    const current = gesture;
    return originalFetch.apply(this, args).then(response => {
      try {
        if (!current || current !== gesture || Date.now() >= current.until || !response.ok || current.ambiguous) return response;
        const url = new URL(response.url);
        if (url.protocol !== 'https:' || url.port || url.username || url.password) return response;
        const type = (response.headers.get('Content-Type') || '').split(';')[0].trim().toLowerCase();
        const size = Number(response.headers.get('Content-Length'));
        if (!Number.isSafeInteger(size) || size <= 0 || size > limit) return response;
        let name;
        if (/^image\/(jpeg|png|webp|gif)$/.test(type)) {
          if (!/^lh\d+\.googleusercontent\.com$/.test(url.hostname)) return response;
        } else {
          // Only explicit Gemini text attachments, not page content or API responses.
          if (url.origin !== location.origin || !/^(text\/(plain|markdown|x-markdown)|application\/octet-stream)$/.test(type)) return response;
          const disposition = response.headers.get('Content-Disposition') || '';
          const match = /^attachment\s*;\s*filename\s*=\s*(?:"([^"\r\n]+)"|([^;\s]+))\s*$/i.exec(disposition);
          name = match && (match[1] || match[2]);
          if (!name || !/^[\p{L}\p{M}\p{N}_() -][\p{L}\p{M}\p{N}_ .()\-]{0,115}\.(txt|md|markdown)$/iu.test(name) || name.startsWith(' ')) return response;
        }
        if (current.image) {
          current.ambiguous = true;
          current.image = null;
        } else {
          const reader = response.clone().body.getReader();
          current.image = {type, size, name, blob: copy(reader, current, type, size)};
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
  window.__gpTakeDownloadImage = async (type, size, name) => {
    const current = gesture;
    const image = current && current.image;
    if (!image || current.ambiguous || image.type !== type || image.size !== size || (image.name && image.name !== name) || Date.now() >= current.until) return null;
    current.image = null;
    const blob = await image.blob;
    if (current !== gesture || current.ambiguous || current.image || Date.now() >= current.until) return null;
    gesture = null;
    clearTimeout(expiry);
    return blob;
  };
})();
