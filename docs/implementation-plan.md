# Gemini Pocket implementation plan

Approved execution: implement in this session, preserving the travel application.

1. Policy: add JVM tests for Gemini/auth navigation, resource allowlist, HTTPS, deceptive suffixes, credentials, ports and unrestricted-mode behavior. Run red, then implement UrlPolicy.java and run green.
2. Android shell: create manifest, native theme/icon and MainActivity.java. First-run mode consent and browser selection; discover Custom Tabs providers via service queries and launch explicit selected package with session extra. No browser-cookie copying. Settings persist mode/restriction preference.
3. WebView: safe settings, progress/header, same-policy popup transport, HTTPS-only allowlist requests/navigation, explicit external link confirmation, cookie flush and restore state on process recreation. Preserve activity across configuration changes and avoid pausing timers on background transitions.
4. Uploads: Photo Picker for images, document fallback for arbitrary accepted MIME types, multiple select, persisted grants. Camera provider serves only cache/captures files and capture output is explicitly granted to camera. Resolve/cancel callbacks once; retain pending capture URI in saved state. Origin-scoped camera/audio permission prompts.
5. Build: reuse root build script without editing it. Run JVM suite and aapt2/javac/D8/apksigner; install APK and inspect first run using Agent-VScreen. Review source with a fresh read-only reviewer and fix findings; document unverified account/upload paths.

Files: src/com/geminipocket/{MainActivity,UrlPolicy,CaptureProvider}.java; AndroidManifest.xml; res/values/styles.xml; res/drawable/ic_launcher.xml; tests/UrlPolicyTest.java; test.sh; README.md. Output build/com.geminipocket.apk.
