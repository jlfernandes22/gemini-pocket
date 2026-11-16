# Gemini Pocket — approved design

A separate lightweight Java Android app, minimum API 26, target API 35. No external runtime libraries, analytics, JS bridge, or cookie extraction. Existing travel app remains unchanged.

First run explains two modes: browser-backed Custom Tabs reuses the selected browser's session after explicit consent; isolated WebView has separate cookies and app-controlled HTTPS restrictions. Browser mode cannot enforce URL restrictions. Settings allow mode/browser changes and restricted-mode toggle.

Isolated mode uses the Android 13+ Photo Picker, document fallback, persistable URI grants, full-resolution camera capture through a private ContentProvider, and origin-scoped web camera/microphone permission prompts. No broad storage permission. WebView is not paused on app switching; configuration changes preserve it, and process recreation restores navigation where possible. Background survival is best effort, not guaranteed; no permanent foreground service or wakelock.

Restricted mode validates exact HTTPS navigation hosts and curated supporting resource hosts; no arbitrary intents or HTTP. Blocked main-frame links require explicit external-browser confirmation. Camera/microphone requests only accepted from Gemini itself. SSL errors never bypassed. Popups remain subject to the same navigation policy.

Verification: JVM policy tests, APK signature check, installation and first-run/restricted-navigation UI checks. Private account sign-in and photo contents require user participation; no claim they pass without testing.
