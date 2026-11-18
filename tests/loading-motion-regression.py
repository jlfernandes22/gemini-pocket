from pathlib import Path

sources = Path('app/src/main/java/com/geminipocket')
bar = (sources / 'WavyLoadingBar.java').read_text().split('void onDraw', 1)[1]
condition = 'if (!AppearancePolicy.drawWave(appearance.wave,animate))'
assert condition in bar, 'Startup loader needs a straight rendering path'
assert 'boolean animate=ValueAnimator.areAnimatorsEnabled();' in bar
assert 'AppearancePolicy.lineStart(phase,animate)' in bar and 'AppearancePolicy.lineEnd(phase,animate)' in bar
before, reduced = bar.split(condition, 1)
fallback, animated = reduced.split('return;', 1)
assert 'drawLine(' in fallback and 'drawLine(' not in before + animated, 'Draw a straight line only in reduced motion, never under the wave'
assert 'drawPath(' in animated, 'Keep the animated wave when motion is enabled'
for view in ('UpdateCheckView', 'UpdateProgressView'):
    draw = (sources / f'{view}.java').read_text().split('void onDraw', 1)[1]
    assert 'ValueAnimator.areAnimatorsEnabled()' in draw and 'drawArc(' in draw, f'{view} needs a smooth reduced-motion arc'
print('PASS: single startup stroke and smooth reduced-motion indicators (source guards)')
