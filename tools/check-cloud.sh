#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1 -Djavax.net.ssl.trustStore=/workspace/android-tools/java-cacerts"
export JAVA_HOME=/workspace/android-tools/jdk-21.0.8+9
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_EMULATOR_HOME=/workspace/android-tools/home
export ANDROID_HOME=/workspace/android-tools/sdk
export ANDROID_USER_HOME=/workspace/android-tools/home
export GRADLE_USER_HOME=/workspace/android-tools/gradle-cache
exec /workspace/android-tools/gradle-8.13/bin/gradle --daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug "$@"
