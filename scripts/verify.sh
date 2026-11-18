#!/usr/bin/env bash
# Runs every check: formatting and static analysis, lint, all JVM tests and a debug build.
# Green here is the bar for committing logic.
set -euo pipefail
source "$(dirname "$0")/env.sh"

./gradlew --console=plain \
    detekt \
    :core:test \
    :app:testDebugUnitTest \
    :app:lintDebug \
    :app:assembleDebug \
    "$@"
echo "verify: all checks passed"
