from pathlib import Path
s=Path('app/src/main/java/com/geminipocket/PhotoReviewView.java').read_text()
assert 'image.setElevation(dp(13))' in s, 'Photo must draw above 12dp elevated review card'
assert 'card.setElevation(dp(12))' in s
print('PASS: review photo layer draws above elevated card')
