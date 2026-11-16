from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
assert '"1".equals(value) && attempt>=8' in s, 'Visible composer must dismiss cover after bounded grace even if menu detection fails'
print('PASS: visible composer cannot be blocked indefinitely by menu detection')
