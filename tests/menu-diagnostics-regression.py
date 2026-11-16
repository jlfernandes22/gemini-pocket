from pathlib import Path
s=Path('app/src/main/assets/gemini-integration.js').read_text()
assert 'GP_MENU ' in s and 'menuSnapshots >= 12' in s
assert 'getBoundingClientRect' in s and 'overflow' in s
assert 'outerHTML' not in s and 'document.body.innerText' not in s
assert 'case "menu-diagnostic"' not in s, 'Diagnostics must not invoke native actions'
print('PASS: bounded geometry-only menu diagnostics')
