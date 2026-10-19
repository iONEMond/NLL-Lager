#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME=/workspace/android-tools/sdk
export ANDROID_USER_HOME=/workspace/android-tools/home
export ANDROID_EMULATOR_HOME=/workspace/android-tools/home
export ANDROID_AVD_HOME=/workspace/android-tools/home/avd
# A dedicated test device, no production data. Software emulation works without /dev/kvm.
# No wipe: subsequent starts preserve the disposable test device and boot faster.
exec "$ANDROID_HOME/emulator/emulator" -avd nll-test30 \
    -no-window -no-audio -no-boot-anim -no-snapshot -accel off \
    -gpu swiftshader_indirect -memory 1536 -cores 2 "$@"
