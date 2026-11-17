from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
h=Path('app/src/main/java/com/geminipocket/SheetHost.java').read_text()
d=Path('app/src/main/java/com/geminipocket/DraggableSheetScrollView.java').read_text()
a=Path('app/src/main/java/com/geminipocket/AnimatedSheetDialog.java').read_text()
assert 'dispatchTouchEvent' in d and 'super.dispatchTouchEvent(cancel)' in d
assert 'getRawX()' in d and 'getRawY()' in d
assert 'translationY(close?getHeight()' not in d, 'Only dialog owns exit animation'
assert 'private void addSheetBack(' in s and 'setContentDescription("Back to browsing controls")' in s
assert 'sheet.backAction(()->settings())' in s
assert 'new LinearLayout.LayoutParams(-1,dp(activity,40))' in h, 'Handle needs large touch surface'
assert 'onBackPressed()' in a and 'backAction' in a
print('PASS: sheet stream ownership, single exit animation and explicit child navigation')
