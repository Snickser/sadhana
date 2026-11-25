#!/usr/bin/env bash
# Сборка APK без Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLCHAIN="$ROOT/toolchain"
export JAVA_HOME="$TOOLCHAIN/jdk-17.0.21.1+2"
export PATH="$JAVA_HOME/bin:$PATH"
SDK="$TOOLCHAIN/sdk"
BT="$SDK/build-tools/34.0.0"
ANDROID_JAR="$SDK/platforms/android-34/android.jar"
BUILD="$ROOT/build"

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"

echo "== 1/6 aapt2 compile =="
"$BT/aapt2" compile --dir "$ROOT/res" -o "$BUILD/res.zip"

echo "== 2/6 aapt2 link =="
"$BT/aapt2" link \
  -o "$BUILD/app-unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -R "$BUILD/res.zip" \
  --java "$BUILD/gen" \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  --version-code 10 \
  --version-name 1.9 \
  --auto-add-overlay

echo "== 3/6 javac =="
find "$ROOT/src" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
javac -nowarn -Xlint:-options -source 8 -target 8 -bootclasspath "$ANDROID_JAR" \
  -encoding UTF-8 -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "== 4/6 d8 =="
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
"$BT/d8" --release --min-api 21 --lib "$ANDROID_JAR" \
  --output "$BUILD/dex" @"$BUILD/classes.txt"

echo "== 5/6 упаковка dex + zipalign =="
python3 - "$BUILD/app-unsigned.apk" "$BUILD/dex/classes.dex" "$BUILD/app-with-dex.apk" <<'PY'
import shutil, sys, zipfile
src, dex, dst = sys.argv[1], sys.argv[2], sys.argv[3]
shutil.copyfile(src, dst)
with zipfile.ZipFile(dst, "a", zipfile.ZIP_DEFLATED) as z:
    z.write(dex, "classes.dex")
print("dex добавлен:", dex)
PY
"$BT/zipalign" -p -f 4 "$BUILD/app-with-dex.apk" "$BUILD/app-aligned.apk"

echo "== 6/6 подпись =="
# Пароль к хранилищу ключей в репозитории не держим: берём из окружения или из keystore/.ks_pass
KS="${KS_PATH:-$ROOT/keystore/sadhana.keystore}"
ALIAS="${KS_ALIAS:-sadhana}"
if [ -n "${KS_PASS:-}" ]; then
  :
elif [ -f "$ROOT/keystore/.ks_pass" ]; then
  KS_PASS="$(cat "$ROOT/keystore/.ks_pass")"
else
  echo "!! Нет пароля к хранилищу: задайте KS_PASS или создайте keystore/.ks_pass" >&2
  exit 1
fi
mkdir -p "$ROOT/keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -v \
    -keystore "$KS" -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10950 \
    -dname "CN=Sadhana, O=Rolyc, C=RU" >/dev/null 2>&1
  echo "keystore создан: $KS"
fi
VERSION_NAME="$(grep -o 'android:versionName="[^"]*"' "$ROOT/AndroidManifest.xml" | head -1 | cut -d'"' -f2)"
APK="$ROOT/sadhana-$VERSION_NAME.apk"
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$APK" "$BUILD/app-aligned.apk"

echo "== проверка подписи =="
"$BT/apksigner" verify --print-certs "$APK"
ls -lh "$APK"
echo "OK"
