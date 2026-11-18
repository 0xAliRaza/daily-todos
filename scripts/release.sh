#!/usr/bin/env bash
# Builds the signed release APK into build/release/, after every check passes.
# The key lives outside the repo (~/.android-signing/daily-tasks-release.jks); its password is read
# from the macOS Keychain item "daily-tasks-release-key" and handed to Gradle through the environment.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh

export DT_KEYSTORE="${DT_KEYSTORE:-$HOME/.android-signing/daily-tasks-release.jks}"
[ -f "$DT_KEYSTORE" ] || { echo "release: no keystore at $DT_KEYSTORE" >&2; exit 1; }
DT_KEYSTORE_PASSWORD="$(security find-generic-password -s daily-tasks-release-key -w)"
export DT_KEYSTORE_PASSWORD

./scripts/verify.sh
./gradlew -q :app:assembleRelease

version="$(sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts)"
out="$ROOT/build/release/daily-tasks-$version.apk"
mkdir -p "$ROOT/build/release"
cp app/build/outputs/apk/release/app-release.apk "$out"
apksigner="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)/apksigner"
"$apksigner" verify --print-certs "$out" 2>/dev/null | grep -E "^Verifies|certificate DN|certificate SHA-256"
(cd "$ROOT/build/release" && shasum -a 256 "$(basename "$out")" > "$(basename "$out").sha256")
echo "release: $out"
