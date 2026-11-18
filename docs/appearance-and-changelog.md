# Appearance and release notes

Approved scope: loading indicators and startup shape only, not web content or menu accents. Use shared light/dark palette with a system accent option on API 31+ and blue fallback. Preview changes live in the Appearance sheet, persist each choice, reset only appearance keys. A straight indicator choice preserves real update percentages; Android's disabled animations always win.

What's new uses bundled notes keyed by app version. On first open of a newer public build, show once unless disabled. Skip fresh installs, local test builds and downgrades. Android install timestamps distinguish upgrades from fresh installs when migrating preferences from older builds. About always opens the bundled notes manually.

Verify via JVM color/appearance and version eligibility tests, existing source guards, manual signed build, and an in-place test install. Keep pending PDF and motion changes on development; no commit, push or release.
