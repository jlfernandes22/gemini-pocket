#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build-tests
javac -d build-tests app/src/main/java/com/geminipocket/ChangelogPolicy.java app/src/main/java/com/geminipocket/AppearancePolicy.java app/src/main/java/com/geminipocket/Operation.java app/src/main/java/com/geminipocket/Transfer.java app/src/main/java/com/geminipocket/GoogleDomains.java app/src/main/java/com/geminipocket/UrlPolicy.java app/src/main/java/com/geminipocket/ImageDownloadPolicy.java app/src/main/java/com/geminipocket/UploadPolicy.java app/src/main/java/com/geminipocket/TempPhotos.java app/src/main/java/com/geminipocket/CameraZoom.java app/src/main/java/com/geminipocket/UpdatePolicy.java app/src/test/java/*.java
for test in UrlPolicyTest UploadPolicyTest TempPhotoTest CameraZoomTest UpdatePolicyTest TransferTest OperationTest AppearancePolicyTest ChangelogPolicyTest ImageDownloadPolicyTest; do java -cp build-tests "$test"; done
for test in tests/*-regression.py; do python3 "$test"; done
if command -v node >/dev/null 2>&1; then node --check app/src/main/assets/gemini-integration.js; node --check app/src/main/assets/app-promotion.js; node tests/app-promotion-test.js; node --check app/src/main/assets/image-download.js; node tests/image-download-test.js; node tests/image-download-capture-test.js; fi
