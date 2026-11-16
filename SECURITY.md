# Security and reporting

Do not put passwords, cookies, account details or private photos into a public issue. Until a private reporting contact is configured, use GitHub private vulnerability reporting if the repository owner enables it.

## Boundaries

- HTTPS only; certificate errors are cancelled, never bypassed.
- Restricted navigation uses explicit hosts; supporting resources use curated Google hosts/families. Browser-backed mode cannot enforce these restrictions.
- No JavaScript-to-native object bridge; chooser routing receives a small console signal and only accepts known actions. Unknown console messages are not logged by the wrapper.
- The capture provider is non-exported and accepts only narrow generated filenames. Camera activity is non-exported.
- Android permissions and current Gemini origin are checked for website camera/microphone requests.
- Backups are disabled. Signing keys must stay outside source control.

## Known risks / unverified paths

The allowlist is not a firewall: service workers, WebSockets and browser-managed traffic require a separate threat-model review. Third-party cookies are enabled for site compatibility. Website DOM hooks can change. Camera/session race handling and long-running background upload survival need instrumented multi-device testing. No independent security review has completed; interrupted reviewer attempts are not reviews.

Before public release, run Gradle lint, instrumented tests, dependency/build-action review, and a fresh device regression pass. Do not advertise the project as audited or hardened.
