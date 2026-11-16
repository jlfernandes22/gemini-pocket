from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
l=Path('app/src/main/java/com/geminipocket/ExpressiveLoader.java').read_text()
assert 'w.setDecorFitsSystemWindows(false)' in s, 'Sheet must draw through navigation region'
assert 'w.setLayout(-1,-1)' in s and 'sheetHost.addView(scroll,panel)' in s, 'Full-height host anchors sheet at physical bottom, not floating dialog content bounds'
assert 'restoredVisible' in s and 'out.putBoolean("pageVisible",pageVisible)' in s, 'Restored page must not restart launch cover'
assert 'onNewIntent' in s, 'Launcher re-entry must reuse loaded activity'
assert 'drawCircle(x,y' not in l, 'No center dot'
assert '650' in l and '4666' in l, 'Verified M3 timing references'
print('PASS: sheet, resume and expressive-loader structural regression checks')
