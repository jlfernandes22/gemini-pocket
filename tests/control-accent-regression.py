from pathlib import Path

activity = Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
assert 'styleAction(b,primary,24)' in activity, 'Native actions, including Reload and Home, share the selected palette'
assert 'row.setBackground(accentBackground(tonal,20))' in activity, 'Settings rows use the tonal accent surface'
assert 'styleAction(retryLaunch,true,28)' in activity and 'styleAction(openIncomplete,false,28)' in activity
assert 'styleAction((Button)child,false,24)' in activity, 'Appearance button refreshes without restarting the app'
print('PASS: native action and settings-row accent wiring (source guards)')
