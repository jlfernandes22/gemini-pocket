#!/bin/sh
set -eu

PROJECT=${1:-$(dirname "$0")}
cd "$PROJECT"
[ ! -f version.properties ] || . ./version.properties

MIN_SDK=${MIN_SDK:-26}
TARGET_SDK=${TARGET_SDK:-35}
VERSION_CODE=${VERSION_CODE:-$APP_VERSION_CODE}
VERSION_NAME=${VERSION_NAME:-$APP_VERSION_NAME}
KEYSTORE=${KEYSTORE:-$HOME/.android/debug.keystore}
KS_ALIAS=${KS_ALIAS:-androiddebugkey}
KS_PASS=${KS_PASS:-android}
export KS_PASS

fail() { echo "build-apk: $*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || fail "$1 is not installed. Run android_sdk_install first."; }

for tool in aapt2 javac jar d8 apksigner keytool; do need "$tool"; done
[ -f app/src/main/AndroidManifest.xml ] || fail "app/src/main/AndroidManifest.xml not found in $(pwd)"

if [ -z "${ANDROID_JAR:-}" ]; then
    ANDROID_JAR=$(ls -d "$HOME"/android-sdk/platforms/android-*/android.jar 2>/dev/null | sort | tail -n 1 || true)
fi
[ -n "${ANDROID_JAR:-}" ] && [ -f "$ANDROID_JAR" ] || fail "android.jar not found. Run android_sdk_install, or set ANDROID_JAR."

PACKAGE=com.geminipocket
# AGP 8 owns namespace in Gradle; inject package only into the manual link manifest.
sed 's/<manifest /<manifest package="com.geminipocket" /' app/src/main/AndroidManifest.xml > build-manifest.tmp.xml
trap 'rm -f build-manifest.tmp.xml' EXIT HUP INT TERM

rm -rf build
mkdir -p build/classes build/dex build/gen

LINK_INPUTS=""
if [ -d app/src/main/res ] && [ -n "$(ls -A app/src/main/res 2>/dev/null)" ]; then
    aapt2 compile --dir app/src/main/res -o build/res.zip
    LINK_INPUTS="build/res.zip"
fi
ASSET_ARGS=""
[ -d app/src/main/assets ] && ASSET_ARGS="-A app/src/main/assets"

# shellcheck disable=SC2086
aapt2 link -I "$ANDROID_JAR" --manifest build-manifest.tmp.xml $ASSET_ARGS \
    --java build/gen --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
    -o build/base.apk $LINK_INPUTS

CLASSPATH_JARS="$ANDROID_JAR"
LIB_JARS=""
if [ -d libs ]; then
    for lib in libs/*.jar; do
        [ -f "$lib" ] || continue
        CLASSPATH_JARS="$CLASSPATH_JARS:$lib"
        LIB_JARS="$LIB_JARS $lib"
    done
fi

find app/src/main/java build/gen -name '*.java' > build/sources.txt
[ -s build/sources.txt ] || fail "no .java sources under src/"
javac -source 8 -target 8 -classpath "$CLASSPATH_JARS" -Xlint:-options \
    -d build/classes @build/sources.txt

jar cf build/classes.jar -C build/classes .
# shellcheck disable=SC2086
d8 --min-api "$MIN_SDK" --lib "$ANDROID_JAR" --output build/dex build/classes.jar $LIB_JARS

cp build/base.apk build/unsigned.apk
for dex in build/dex/classes*.dex; do
    jar --update --file build/unsigned.apk -C build/dex "$(basename "$dex")"
done

if [ ! -f "$KEYSTORE" ]; then
    [ "$KEYSTORE" = "$HOME/.android/debug.keystore" ] || fail "keystore $KEYSTORE does not exist"
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

OUT="build/$PACKAGE.apk"
apksigner sign --ks "$KEYSTORE" --ks-key-alias "$KS_ALIAS" \
    --ks-pass env:KS_PASS --key-pass env:KS_PASS --out "$OUT" build/unsigned.apk
apksigner verify "$OUT"

echo "SUCCESS_APK: $(pwd)/$OUT"
echo "Package: $PACKAGE"
