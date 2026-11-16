# Privacy

Gemini Pocket itself has no analytics SDK or developer-operated backend. Gemini and the selected browser communicate with Google under their own terms and policies.

Isolated mode stores website cookies/storage in the Android app sandbox. Browser mode uses the chosen browser's session and does not copy credentials into the app. Camera and microphone require Android permission plus a website-origin check. No broad gallery/storage permission is requested.

Chosen attachments are copied to private cache (100 MB per-file preparation limit). Camera originals are deleted after successful copy; remaining generated attachments older than 24 hours are cleaned on a later activity creation. Cleanup does not promise immediate secure erasure or resumable background uploads, and does not delete gallery originals. Android may remove cache itself.

Verbose request, file-content-read and JavaScript-console diagnostics were removed during stabilization. Some local lifecycle/error and host-only blocked-request logs remain for troubleshooting. They should not contain credentials, URL query strings or image data. Do not publish raw logcat/screenshots without reviewing them for unrelated device data.

Android Clear storage resets the isolated session and settings. Clear cache does not intentionally sign you out. Browsing controls → Privacy & storage provides temporary-attachment cleanup, website-cache clearing and confirmed isolated-session reset. Do not run cleanup while a draft or upload is in use.
