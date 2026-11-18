# Shared environment for the scripts in this folder. Sourced, not run.
# There is no system Java: Gradle runs on the JDK bundled with Android Studio.
: "${JAVA_HOME:=/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
: "${ANDROID_HOME:=$HOME/Library/Android/sdk}"
export JAVA_HOME ANDROID_HOME
ADB="$ANDROID_HOME/platform-tools/adb"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [ ! -f local.properties ]; then
    printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
fi
