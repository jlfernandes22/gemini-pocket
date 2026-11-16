from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
j=Path('app/src/main/assets/launch-readiness.js').read_text()
assert '"1".equals(value) && attempt>=8' not in s, 'No composer-only reveal shortcut'
assert 'composer && nav && model' in j and 'getBoundingClientRect' in j
assert 'Open page anyway' in s, 'Bounded manual escape when website changes'
assert 'getBoolean("ready")' in s
print('PASS: visible header/control readiness with explicit slow-load escape')
