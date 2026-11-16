from pathlib import Path
c=Path('app/src/main/java/com/geminipocket/CameraActivity.java').read_text()
r=Path('app/src/main/java/com/geminipocket/PhotoReviewView.java')
assert 'new PhotoReviewView(' in c and 'setTitle("Use this photo?")' not in c
assert 'reviewVisible' in c and 'out.putString("reviewFile"' in c
assert 'if(reviewVisible)return;' in c, 'Do not reopen live camera behind review'
s=r.read_text()
assert 'ValueAnimator.areAnimatorsEnabled()' in s and 'setDuration(350)' in s
assert 'ExifInterface' in s and 'dispose()' in s
assert 'onBackPressed()' in c and 'discardReview' in c
print('PASS: camera review transition, lifecycle and reduced-motion structural guards')
