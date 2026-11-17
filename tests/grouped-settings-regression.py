from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
h=Path('app/src/main/java/com/geminipocket/SheetHost.java').read_text()
d=Path('app/src/main/java/com/geminipocket/DraggableSheetScrollView.java').read_text()
assert 'connectionControls();' in s and 'editor.setVisibility' not in s
assert 'PRIVACY & PERMISSIONS' in s and 'About & updates' in s
assert 'DraggableSheetScrollView scroll=' in h
assert '!canScrollVertically(-1)' in d and 'getRawY()' in d
assert 'ACTION_CANCEL' in d and 'ValueAnimator.areAnimatorsEnabled()' in d
print('PASS: grouped settings and scroll-safe dismissal structural guards')
