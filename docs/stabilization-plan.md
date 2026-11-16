# Stabilization and repository preparation

Approved scope: standalone Gemini Pocket repository, preserve working behavior and travel project. Execute in this session.

1. Audit: build script defaults currently reset version to 1; test.sh omits cleanup and structural checks; README contains outdated claims; MainActivity embeds large debugging/injection script; generated APK/test outputs lack ignore rules.
2. Standard Android layout: app/src/main/{java,res,assets,AndroidManifest.xml}, app/src/test/java; root Gradle Kotlin DSL configuration and portable manual build retained for on-phone ARM tools. Keep tests/ structural checks and scripts/ host runner. Update all paths and make existing root commands self-locating.
3. Extract website integration to assets/gemini-integration.js, remove verbose network/file-read diagnostics and their monkeypatches, retain narrow UI fixes and chooser action signal. No JS native bridge. Preserve URL allowlist, camera and lifecycle behaviors.
4. Add README, architecture, privacy/security, contributing, release instructions, ignore rules, GitHub CI and issue template. No signing keys, local state, generated APK, private images or screenshots in publication archive. Leave license selection to owner; do not grant a license on their behalf.
5. Verify JVM tests + all structural checks + JS syntax + manual compile/DEX/signature. Build a source-only export archive and scan contents. Gradle configuration can be provided but is unverified here until actually executed. No push, commit or release requested.
