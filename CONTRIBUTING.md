# Contributing

1. Discuss behavior changes in an issue; do not weaken URL restrictions to fix an unrelated UI problem.
2. Keep Android implementation in `app/src/main/java`, website adaptations in `app/src/main/assets/gemini-integration.js`, and pure policies independently testable.
3. Add a failing regression test first for bugs; run `sh test.sh`, a manual or Gradle build, and relevant on-device checks.
4. Prefer small, focused changes. Preserve user drafts and sessions on settings navigation and app switching.
5. No secrets, signing keys, private photos, screenshots, logs or generated packages in commits.
6. Document verification honestly: compilation is not proof of sign-in, upload completion or lifecycle correctness.

Contributions are accepted under the project MIT license. A CI definition is present but its first remote run still needs verification.
