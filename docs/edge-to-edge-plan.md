# Approved edge-to-edge update

User chose edge-to-edge, not hidden system bars. Preserve uploads, camera and URL policy. Execute inline.

1. Transparent system bars; window draws edge-to-edge. Root surface paints behind bars; content receives system/cutout/IME insets, using max rather than summing keyboard and navigation heights. API 26 fallback remains available.
2. Remove wrapper toolbar. Overlay a 44dp accessible shield control near the upper right without covering Gemini's profile/menu controls. Native rounded bottom settings sheet follows system palette, offers restricted/open HTTPS radio choices, explicit Apply, reload/home/change mode. Changing restriction does not reload or discard prompt; applies to future requests.
3. Compile and run existing 52 regression assertions and signature check. Install and inspect Gemini viewport and settings sheet on Agent-VScreen. Report any unverified keyboard/theme paths.

Alternatives: HTML account-menu injection would be fragile and misleading; hidden bars were explicitly rejected. Native overlay and native bottom sheet avoid exposing a JS bridge or pretending these are Google account settings.
