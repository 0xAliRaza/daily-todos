#!/usr/bin/env bash
# Stops what development sessions start: emulators, the adb server and the Gradle and Kotlin daemons.
set -uo pipefail
source "$(dirname "$0")/env.sh"

if [ -x "$ADB" ]; then
    for serial in $("$ADB" devices 2>/dev/null | awk '/^emulator-/{print $1}'); do
        "$ADB" -s "$serial" emu kill && echo "stopped emulator $serial"
    done
    "$ADB" kill-server 2>/dev/null && echo "stopped adb server"
fi
./gradlew --stop -q && echo "stopped Gradle daemons"
pkill -f 'KotlinCompileDaemon' && echo "stopped Kotlin daemon"
exit 0
