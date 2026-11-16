from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
c=Path('app/src/main/java/com/geminipocket/CameraActivity.java').read_text()
j=Path('app/src/main/assets/gemini-integration.js').read_text()
assert 'Privacy & storage' in s and 'Reset website session' in s
assert 'upload!=null || preparingAttachment' in s, 'Cleanup must reject in-progress preparation'
assert 'launchError(' in s and 'Could not open Gemini' in s
assert 'ScaleGestureDetector' in c and 'CONTROL_AF_REGIONS' in c
assert 'transformPreview()' in c and 'if(!delivered && output!=null && !isChangingConfigurations())output.delete()' in c
assert 'XMLHttpRequest.prototype' not in j and 'arrayBuffer' not in j, 'No content-read/network diagnostics'
print('PASS: privacy, attachment, camera and launch error structural checks')
