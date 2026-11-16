from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
assert 'Connection mode' in s and 'RadioGroup modes=' in s
assert 'sheet.dismiss();beginModeSelection();' not in s
assert 'Change connection mode",' not in s
assert 'if(browserSelected){chooseBrowser();return;}' in s
assert 'if(web==null)isolated();' in s, 'Same isolated mode must not reload the page'
print('PASS: connection mode integrated in sheet with explicit apply')
