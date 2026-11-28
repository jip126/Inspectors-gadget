#!/usr/bin/env bash
# Builds Inspector's Gadget without Gradle/AGP:
#   aapt (resources) -> javac (Java 8 bytecode) -> dx (dex) -> zipalign -> apksigner
set -euo pipefail
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS
cd "$(dirname "$0")"
ANDROID_JAR="${ANDROID_JAR:-$PWD/sdk/android35.jar}"   # compile classpath (API 35 framework + core libs)
RES_JAR="${RES_JAR:-$PWD/sdk/android23-res.jar}"   # framework resources for aapt (IDs are stable)
OUT=build
rm -rf "$OUT"; mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

aapt package -f -m -J "$OUT/gen" -M AndroidManifest.xml -S res -I "$RES_JAR" \
  --min-sdk-version 26 --target-sdk-version 34 -0 arsc -F "$OUT/app.unsigned.apk"

javac -nowarn -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID_JAR" -Xlint:-options \
  -d "$OUT/classes" $(find src "$OUT/gen" -name '*.java')

dalvik-exchange --dex --min-sdk-version=26 --output="$OUT/dex/classes.dex" "$OUT/classes"
(cd "$OUT/dex" && zip -q -u ../app.unsigned.apk classes.dex)

zipalign -f -p 4 "$OUT/app.unsigned.apk" "$OUT/app.aligned.apk"
KS=${KEYSTORE:-$PWD/demo.keystore}
if [ ! -f "$KS" ]; then
  # A new key means the APK can't install over the current app: the phone has to uninstall it,
  # which deletes accounts and inspections. Use the existing demo.keystore unless told otherwise.
  if [ "${ALLOW_NEW_KEY:-}" != 1 ]; then
    echo "Signing key not found: $KS. Copy demo.keystore here (or set ALLOW_NEW_KEY=1 for a new key)." >&2
    exit 1
  fi
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias demo \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Inspector's Gadget Demo" >/dev/null 2>&1
fi
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/InspectorsGadget.apk" "$OUT/app.aligned.apk"
apksigner verify "$OUT/InspectorsGadget.apk" && echo "Built $OUT/InspectorsGadget.apk"
