/* Narrow promotion matching; failure restores UI rather than trapping an invisible modal. */
(function () {
  function normalize(value) { return (value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/\s+/g, ' ').trim().toLowerCase(); }
  const pairs = [
    ['do more with the gemini app','continue in browser'], ['get more with the gemini app','continue in your browser'],
    ['faca mais com a app gemini','continuar no navegador'], ['faca mais com o app gemini','continuar no browser'],
    ['haz mas con la app de gemini','continuar en el navegador'], ['haz mas con la aplicacion gemini','continuar en el navegador'],
    ['profitez davantage de l’application gemini','continuer dans le navigateur'],
    ['mit der gemini app mehr erledigen','im browser fortfahren']
  ];
  function recognized(title, action) {
    return pairs.some(p => normalize(p[0]) === normalize(title) && normalize(p[1]) === normalize(action));
  }
  function temporarilyHide(nodes) {
    const saved = nodes.filter(Boolean).map(node => ({ node, visibility: node.style.getPropertyValue('visibility'), vp: node.style.getPropertyPriority('visibility'), pointer: node.style.getPropertyValue('pointer-events'), pp: node.style.getPropertyPriority('pointer-events') }));
    saved.forEach(s => { s.node.style.setProperty('visibility','hidden','important'); s.node.style.setProperty('pointer-events','none','important'); });
    return () => saved.forEach(s => {
      if(s.visibility) s.node.style.setProperty('visibility',s.visibility,s.vp); else s.node.style.removeProperty('visibility');
      if(s.pointer) s.node.style.setProperty('pointer-events',s.pointer,s.pp); else s.node.style.removeProperty('pointer-events');
    });
  }
  if(typeof module !== 'undefined') module.exports = { recognized, temporarilyHide };
  if(typeof document === 'undefined' || window.__gpPromotion || !document.documentElement) return;
  window.__gpPromotion = true;
  const handled = new WeakSet();
  function scan() {
    for(const button of document.querySelectorAll('button,a,[role=button]')) {
      const label = button.textContent || button.getAttribute('aria-label') || '';
      if(!pairs.some(p => normalize(p[1]) === normalize(label))) continue;
      let node = button.parentElement;
      for(let depth=0; node && depth<10 && node!==document.body; depth++,node=node.parentElement) {
        if(!Array.from(node.querySelectorAll('h1,h2,h3,[role=heading]')).some(h => recognized(h.textContent,label))) continue;
        const sheet=node.closest('[role=dialog],mat-bottom-sheet-container,mat-dialog-container') || node;
        if(handled.has(sheet)) break;
        handled.add(sheet);
        const pane=sheet.closest('.cdk-overlay-pane');
        const candidate=pane && pane.previousElementSibling;
        const backdrop=candidate && candidate.classList.contains('cdk-overlay-backdrop') ? candidate : null;
        const restore=temporarilyHide([sheet,backdrop]);
        try { button.click(); } catch(error) { restore(); break; }
        setTimeout(() => {
          // Fail open when the expected dismissal did not remove the modal.
          // Never remove a generic overlay or change body scroll/focus locks ourselves.
          if(sheet.isConnected) restore();
          if(backdrop && backdrop.isConnected && !sheet.isConnected) restore();
        },750);
        break;
      }
    }
  }
  new MutationObserver(scan).observe(document.documentElement,{subtree:true,childList:true,characterData:true});
  scan();
})();
