from pathlib import Path
s=Path('app/src/main/assets/gemini-integration.js').read_text()
assert 'GP_MENU' not in s and 'menuSnapshot' not in s
assert 'outerHTML' not in s and 'document.body.innerText' not in s
print('PASS: temporary menu diagnostics removed')
