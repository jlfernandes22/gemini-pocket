from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
assert 'root.setPadding(left,top,right,bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:i.consumeSystemWindowInsets();' in s
assert 'bottom=web!=null?ime.bottom:Math.max(bars.bottom,ime.bottom);' in s, 'WebView extends through gesture area, keyboard still resizes it'
assert 'GradientDrawable.Orientation.TOP_BOTTOM' not in s, 'No painted gradient masquerading as page content'
print('PASS: inset ownership and real edge-to-edge viewport regression checks')
