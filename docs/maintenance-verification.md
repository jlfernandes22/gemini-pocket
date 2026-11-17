# Maintenance verification

Development changes remain uncommitted; main unchanged. Existing app-promotion filter preserved.

Verified: pure JVM policy tests, real byte transfer/limit/cancel/hash/truncation tests, temp-file cleanup, JS syntax/recognition checks, structural guards, official wrapper JAR SHA-256 and Gradle distribution SHA-256, Gradle 8.9 --version, manual ARM Java/D8/aapt2 signed APK and expected release certificate. Installed 1.12.3-maintenance-test (300013) without clearing data. VScreen reached Gemini and grouped browsing controls; no private account screens or media used.

Local Gradle lint attempted: configuration reached Android dependency resolution but failed because required SDK licenses/build-tool components are absent; no licenses silently accepted. Revised pinned workflow has not been pushed/run remotely. No independent review result: delegate was interrupted, with no surviving task. These are unresolved verification limits, not passed checks.

Still needed: fresh real camera/upload and updater replacement regression, light/dark and main-display keyboard/gesture checks, remote CI. MainActivity is smaller in responsibility, not yet split into a complete lifecycle/controller architecture. Avoid claiming an audit or guaranteed website compatibility.
