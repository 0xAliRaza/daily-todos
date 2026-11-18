#!/usr/bin/env bash
# Runs the instrumented tests on the connected phone or emulator.
set -euo pipefail
source "$(dirname "$0")/env.sh"

if ! "$ADB" get-state >/dev/null 2>&1; then
    echo "device-test: no device or emulator connected" >&2
    exit 1
fi
./gradlew --console=plain :app:connectedDebugAndroidTest "$@"
