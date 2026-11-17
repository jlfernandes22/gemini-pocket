# Maintenance and release hardening plan

Goal: reduce MainActivity responsibilities without changing UI, signing or update compatibility; make release tooling reproducible. Preserve uncommitted promotion fix. Work on development; no automatic main push.

1. Extract sheet mounting to SheetHost, update transport/integrity into UpdateClient, attachment copying into AttachmentPreparer. Keep Activity-owned permission/UI callbacks explicit. Remove bounded menu diagnostics. Add JVM behavioral tests for integrity and stream limits/cancellation, JS fixture tests for readiness/promotion where possible. Structural checks must follow ownership, not retain old source strings.
2. Add official Gradle 8.9 wrapper with distribution checksum and validate wrapper JAR against Gradle's published checksum. Pin GitHub Actions to verified commit SHAs and current supported majors, use wrapper in CI, restrict release permissions per job, verify expected signing certificate on each release. Preserve build-N mapping and signing key. Document updater/version constraints and release notes.
3. Run tests, JS checks, wrapper validation, manual signed build and check version/certificate compatibility. Inspect harmless About/settings on VScreen only; do not inspect chats or record media without consent. Report Gradle/remote CI separately if not exercised. No merge or public release until approval.

Main risks: callbacks after Activity destruction; cleanup during file copying; update partial download cancellation; certificate/version regression; sheet animation/navigation regression. Pure helpers get behavioral fixtures; UI paths get smoke checks plus explicit untested cases.
