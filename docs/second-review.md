# Second self-review

Resolved: update cancellation is checked after APK verification and on the main-thread install handoff; Cancel/Back/scrim cancel immediately rather than waiting for exit animation. Each download owns its connection, so an older worker cannot disconnect a subsequent retry. Attachment operations have cancellation/one-shot gates and originating WebView identity; stale copies are removed and callbacks complete once.

Release builds publish without claiming latest. A serialized promotion job selects the highest numeric published build. Updater independently scans release pages by numeric version, excluding drafts/prereleases; it fails with an explicit error after 100 pages rather than silently choosing a partial history.

Promotion matching remains exact and conservative, with additional localized pairs; unrecognized translations fail open. Recognized sheet/backdrop styles are restored after failed/thrown dismissal. DOM fixtures exercise removal, retained-modal recovery, exceptions and unrelated-dialog safety. No guarantee for all languages or pre-first-paint injection; early WebView lifecycle injection is best effort.

Verification: full test runner, git diff --check, manual ARM compile/DEX/signing, expected release certificate and in-place installation succeeded for 1.12.3-review-test (300014). No further high-severity issue found in this focused self-review. This is not independent review or exhaustive audit. Remote CI, real installer cancellation at final verification, configuration changes during attachment preparation and broader website/device regressions remain to be exercised. Local Gradle lint still blocked by SDK license/components; licenses were not accepted silently. Changes remain uncommitted on development; main unchanged.
