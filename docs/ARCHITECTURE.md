# Architecture

- `MainActivity`: native setup, browser consent, WebView ownership, bottom sheet, inset handling, launch readiness and Android permission/result plumbing. Still the largest class; future work should split upload/lifecycle controllers only with behavioral tests.
- `UrlPolicy`: pure HTTPS navigation/resource decisions. Keep exact navigation separate from supporting hosts.
- `UploadPolicy`: pure chooser-route and generated filename rules. Website action signal disambiguates generic MIME requests.
- `CaptureProvider`: private generated-file access and MIME/display-name/size metadata.
- `CameraActivity`: Camera2 preview, detected lens chips, still capture and confirmation. No external camera app required.
- `TempPhotos`: age-based cleanup of generated attachment files only.
- `ExpressiveLoader` / `WavyLoadingBar`: original lightweight Canvas UI approximations honoring disabled animation settings.
- `assets/gemini-integration.js`: isolated, idempotent website compatibility layer; icon fixes, app-promo hiding, state-tied thinking indicator and known chooser action signal. No native bridge and no network monkeypatches.

The isolated WebView and browser-backed session deliberately do not share cookies. Native settings do not pretend to be Google account preferences. Retained page cancellation returns from mode selection without destroying the chat.

Testing layers: pure JVM assertions, temporary file IO, source-structure guards, JS parse check, Android compilation/signing, and separately documented manual phone tests. Structural guards are intentionally weak evidence, not substitutes for instrumentation.
