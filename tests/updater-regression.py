from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
p=Path('app/src/main/java/com/geminipocket/UpdateProvider.java').read_text()
m=Path('app/src/main/AndroidManifest.xml').read_text()
assert 'setChecked(prefs.getBoolean("autoUpdates",false))' in s
assert '24L*60*60*1000' in s and 'Download update' in s
assert 'verifyUpdate(part)' in s and 'current.signatures,candidate.signatures' in s
assert 'received!=expected' in s and 'sha.equalsIgnoreCase' in s
assert 'cancelUpdate=true;if(updateConnection!=null)updateConnection.disconnect();destroyWeb()' in s
assert 'MODE_READ_ONLY' in p and '"/update.apk".equals' in p
assert 'android:authorities="com.geminipocket.updates" android:exported="false"' in m
print('PASS: opt-in, integrity, lifecycle and private installer-provider guards')
