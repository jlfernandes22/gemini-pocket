from pathlib import Path

activity = Path('app/src/main/java/com/geminipocket/MainActivity.java').read_text()
shield_update = activity.split(' private void updateShield(){', 1)[1].split(' private void showLoadingOverlay', 1)[0]
assert 'shield.setColorFilter(LoaderAppearance.read(this).accent)' in shield_update, 'Shield must use the same selected accent as the loaders'
preview_refresh = activity.split('Runnable refresh=()->{', 1)[1].split('content.addView(text("Loading color"', 1)[0]
assert 'updateShield();' in preview_refresh, 'Appearance changes and reset must update the shield without reloading Gemini'
print('PASS: shield accent and live appearance refresh (source guards)')
