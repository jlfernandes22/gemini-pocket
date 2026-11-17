# Architecture

- `MainActivity`: native setup, browser consent, WebView ownership, bottom sheet, inset handling, launch readiness and Android permission/result plumbing. Still owns UI orchestration; transport, trust checks, file preparation and sheet mounting are delegated to small helpers.
- `UpdateClient`: bounded HTTPS transport and package/version/signature verification; no Activity UI state.
- `Transfer`: pure stream copying, byte limits, cancellation, SHA-256 and exact-size verification, exercised by behavioral JVM tests.
- `AttachmentPreparer`: content resolver and private-cache preparation; rolls back partial copies.
- `SheetHost`: shared window, theme, navigation inset and drag-handle mounting.
- `UrlPolicy`: pure HTTPS navigation/resource decisions. Keep exact navigation separate from supporting hosts.
- `UploadPolicy`: pure chooser-route and generated filename rules. Website action signal disambiguates generic MIME requests.
- `CaptureProvider`: private generated-file access and MIME/display-name/size metadata.
- `CameraActivity`: Camera2 preview, detected lens chips, still capture and confirmation. No external camera app required.
- `TempPhotos`: age-based cleanup of generated attachment files only.
- `ExpressiveLoader` / `WavyLoadingBar`: original lightweight Canvas UI approximations honoring disabled animation settings.
- `assets/gemini-integration.js`: isolated, idempotent website compatibility layer; icon fixes, app-promo hiding, known chooser action signal; original Gemini thinking indicator is left untouched. No native bridge and no network monkeypatches.

The isolated WebView and browser-backed session deliberately do not share cookies. Native settings do not pretend to be Google account preferences. Retained page cancellation returns from mode selection without destroying the chat.

Testing layers: pure JVM assertions, temporary file IO, source-structure guards, JS parse check, Android compilation/signing, and separately documented manual phone tests. Structural guards are intentionally weak evidence, not substitutes for instrumentation.
