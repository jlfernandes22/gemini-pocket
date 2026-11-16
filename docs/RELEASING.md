# Release checklist

1. Select a license and add LICENSE; confirm branding and third-party/reference obligations. This unofficial app is not endorsed by Google.
2. Run `sh test.sh`, manual build and signature verification. Run Gradle build/lint on CI; fix actual results before claiming they pass.
3. Exercise fresh setup, browser consent, isolated sign-in, text response, photo/file picker, camera capture, camera/mic denial, theme/keyboard/insets, restricted external link, mode selection + Back, Home/reopen, renderer/process death and attachment cleanup. Use non-private sample data.
4. Update `version.properties`. Release signing: pass KEYSTORE, KS_ALIAS, KS_PASS via environment to `build-apk.sh`; do not put them in repository files. The script's default is DEBUG signing, not production signing. Never share the keystore. Existing installations need the same signing key for updates.
5. Scan the source-only export contents. Never upload `.harness`, build outputs, screenshots, local.properties or debug/release keys.
6. Publish source first; inspect GitHub Actions results. Add an APK to Releases only after verifying its exact signature and checksum. Gradle release signing/AAB publication is not configured.

Current CI uses pinned major action versions and Gradle 8.9/AGP 8.7.3; consider pinning action commit SHAs and adding dependency update automation before a public release.

## Continuous public releases

`release.yml` runs on pushes to main and publishes only after tests, Gradle compilation/lint and APK signature verification succeed. Configure repository Actions secrets RELEASE_KEYSTORE_BASE64 and RELEASE_STORE_PASSWORD before the first push. Key alias is release. Never rotate/delete the key for routine builds. Android versionCode is 100000 + the workflow run number; preserve workflow numbering or deliberately increase the offset when migrating repositories. Re-running an already published build will fail tag creation rather than replace assets. GitHub concurrency may coalesce pending runs during rapid pushes; this workflow releases each successful executed run, not guaranteed intermediate commits in a burst.

Signing files are maintained outside the repository. Back up the key and password securely; never add them as public release assets. New public release signatures are intentionally different from historic debug builds.
