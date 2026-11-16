(function () {
  // Gemini identifiers are not a public API. Require visible, enabled controls
  // and geometry rather than trusting existence of an ID or translated text.
  function visible(n) {
    if (!n || n.disabled || n.getAttribute('aria-disabled') === 'true') return false;
    const r = n.getBoundingClientRect(), s = getComputedStyle(n);
    return r.width >= 16 && r.height >= 16 && r.bottom > 0 && r.right > 0 &&
      r.top < innerHeight && r.left < innerWidth && s.display !== 'none' &&
      s.visibility !== 'hidden' && s.opacity !== '0';
  }
  const composer = Array.from(document.querySelectorAll('[contenteditable=true],textarea')).some(visible);
  const controls = Array.from(document.querySelectorAll('button,[role=button]')).filter(visible);
  const top = controls.filter(n => n.getBoundingClientRect().bottom < Math.min(180, innerHeight * .25));
  const nav = top.some(n => /menu|navigation|navega|expand.*menu/i.test(
    (n.getAttribute('aria-label') || '') + ' ' + (n.getAttribute('data-test-id') || '') + ' ' + (n.getAttribute('mattooltip') || '') + ' ' + n.textContent));
  const model = top.some(n => /gemini/i.test(n.textContent || '') && !/google account|conta google/i.test(n.getAttribute('aria-label') || ''));
  // Attachment controls may be lazy-created: only validate them when rendered.
  const attachments = controls.filter(n => /^(camera|câmara|câmera|photos|fotos|files|ficheiros|arquivos|drive)$/i.test((n.innerText || n.getAttribute('aria-label') || '').trim())).length;
  return {ready: composer && nav && model, composer: composer, nav: nav, model: model,
    headerControls: top.length, attachments: attachments};
})()
