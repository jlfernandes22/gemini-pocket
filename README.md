<div align="center">

# Gemini Pocket

**Gemini, in a lightweight Android home.**

An unofficial Android wrapper with thoughtful system integration, private file selection, an in-app camera, and optional restricted browsing.

[![Android](https://img.shields.io/badge/Android-8%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Checks](https://github.com/jlfernandes22/gemini-pocket/actions/workflows/check.yml/badge.svg)](https://github.com/jlfernandes22/gemini-pocket/actions/workflows/check.yml)

[Download releases](https://github.com/jlfernandes22/gemini-pocket/releases) · [Report an issue](https://github.com/jlfernandes22/gemini-pocket/issues) · [Privacy](PRIVACY.md)

</div>

---

## A familiar experience, without bundling a browser

Gemini Pocket uses Android System WebView and native Java. No bundled browser engine, analytics SDK, API key, or developer-operated backend.

- **Two connection modes:** a separate app session, or a browser-backed Custom Tab using your browser's existing login after consent.
- **Photos and files:** Android Photo Picker and document picker—no broad gallery permission.
- **In-app camera:** lens-aware zoom controls, front/back switching, pinch zoom and tap focus.
- **Native finishing touches:** edge-to-edge layout, light/dark styling, Monet-compatible launcher icon, and expressive-inspired loading visuals.
- **Browsing controls:** optional HTTPS host restrictions, remembered Gemini camera/microphone consent, and privacy/storage tools.

> **Unofficial project.** Not affiliated with, endorsed by, or maintained by Google. Gemini's website and services remain subject to Google's terms and availability.

## Install and update

Download an APK from [Releases](https://github.com/jlfernandes22/gemini-pocket/releases). Android may ask you to allow installation from your browser or file manager.

Public releases use one persistent release-signing key and increasing version codes, allowing newer releases to install over older public releases while preserving app data. **Development/debug builds use a different key and require a one-time uninstall before moving to public releases.** Back up anything important before uninstalling.

Each successful push build on `main` creates a release named `build-N`, with a signed APK and SHA-256 checksum. A failed build does not publish a release. These are continuous builds, not a promise that every commit has received manual device testing.

## Connection modes

| | Isolated app | Browser session |
|---|---|---|
| Sign-in | Separate WebView cookies | Selected browser's existing session |
| URL restrictions | Optional app-level host filter | Browser rules apply |
| Camera and attachments | Native wrapper integration | Managed by the browser |
| Credentials imported | No | No |

The host filter is **not a firewall**. It covers intercepted WebView HTTPS requests/navigation, not every browser-managed network channel. Google may change endpoints or refuse embedded sign-in; browser mode is an alternative.

## Build locally

### Android Studio / Gradle

Requirements: JDK 17 and Android SDK 35. The wrapper pins Gradle 8.9 and verifies its download checksum. Open this directory in Android Studio, or run:

```sh
sh gradlew :app:assembleDebug :app:lintDebug
```

The wrapper JAR checksum is checked in CI. Release signing reads environment variables; see [Releasing](docs/RELEASING.md). Never commit signing keys or passwords.

### On-device manual build

The ARM-compatible build route is tested in AndroidHarness. Requires Java 17+, `aapt2`, D8, `apksigner`, and an Android API 35 `android.jar` on the configured toolchain.

```sh
sh test.sh
sh build-apk.sh
```

Output: `build/com.geminipocket.apk`. This route defaults to a local **debug key**, unless release-signing variables are supplied.

## Project structure

```text
app/src/main/java/com/geminipocket/  Android UI, camera, file and URL policies
app/src/main/assets/                Narrow Gemini website compatibility layer
app/src/main/res/                   Themes, icons and launcher resources
app/src/test/java/                  Pure JVM tests
tests/                             Structural regression guards
docs/                              Architecture, security and release notes
.github/workflows/                 Tests and signed continuous releases
```

## Quality and known limits

`sh test.sh` runs URL/upload assertions, temporary-file tests, lens-threshold tests, structural regression guards, and JavaScript syntax checking when Node is installed. Structural checks are not instrumented Android tests. Camera and WebView behavior require real-device testing.

- Android may stop the process; background uploads are not resumable native jobs.
- Gemini markup and localized action labels can change independently of this app.
- Some lenses are unavailable to third-party apps; reported zoom ratios are approximations.
- Expressive visuals are lightweight Canvas implementations, not the Compose components.
- Private attachment copies can remain briefly for WebView access; see [Privacy](PRIVACY.md).
- This project has not completed an independent security audit. See [Security](SECURITY.md).

## Why Gemini Pocket exists

This project began with a practical problem: after debloating their phone and uninstalling the Google app, the creator found that the official Gemini experience stopped working on their device. Gemini Pocket was built to access Gemini through its website without reinstalling the Google app, while shaping the experience around a lightweight wrapper and better native integration.

That describes the creator's experience, not a guarantee for every debloated device. Gemini Pocket still depends on Google's Gemini web service, an available Android System WebView (or a supported browser in browser mode), connectivity, and any account requirements imposed by Google. It does not make Gemini an offline or Google-service-free assistant.

## Inspiration & acknowledgements

Gemini Pocket was inspired by **[geminiAssist](https://github.com/AcideFluorhydrique/geminiAssist)** by **AcideFluorhydrique**, a lightweight WebView wrapper for Google Gemini with optional blocking of non-essential URLs. Trying that app motivated this project's creator to build their own take on the idea and explore improvements to camera integration, attachments, native controls, theming, and the overall experience.

Thank you to geminiAssist for the inspiration. Gemini Pocket is a separate project, not an official continuation, endorsed version, or claim of affiliation. This acknowledgement credits the inspiration; it does not imply that geminiAssist's source code was incorporated. geminiAssist is licensed under GPLv3; its license remains separate from this project's MIT license.

The expressive-loading design references consulted for this project are documented in [loader references](docs/loader-references.md).

## Contribute

Bug reports should include Android/WebView version, connection mode, exact reproduction steps, and the error text—**not private photos, credentials or unreviewed logs**.

Read [Contributing](CONTRIBUTING.md) and [Architecture](docs/ARCHITECTURE.md). Licensed under [MIT](LICENSE). Google/Gemini names and other third-party marks belong to their respective owners.
