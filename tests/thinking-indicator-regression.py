from pathlib import Path
s=Path('app/src/main/assets/gemini-integration.js').read_text()
assert 'gpDots' not in s and 'markDots' not in s, 'Keep only Gemini original thinking indicator'
assert '@keyframes gp-dots' not in s, 'No obsolete fallback animation'
assert 'thinking-dots-animation' not in s, 'Do not alter native thinking dots'
print('PASS: original Gemini thinking indicator only')
