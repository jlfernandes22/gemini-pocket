from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
assert 'root.setPadding(left,top,right,bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:i.consumeSystemWindowInsets();' in s
assert 'WindowInsets.Type.tappableElement()' in s
assert 'int navigationBottom=tappable.bottom>0?bars.bottom:0;' in s, 'Button navigation protected, gesture-only space draws edge to edge'
assert 'bottom=Math.max(navigationBottom,ime.bottom);' in s, 'Keyboard stays protected without adding duplicate bottom space'
assert 'GradientDrawable.Orientation.TOP_BOTTOM' not in s
print('PASS: tappable navigation vs gesture space and keyboard inset ownership')
