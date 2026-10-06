#!/usr/bin/env bash
set -euo pipefail

mkdir -p final-preview
# Pull captures before the emulator runner shuts down the device.
trap 'adb pull /sdcard/Android/data/com.metahumanlegacy.game/files/final-preview/. final-preview/ >/dev/null 2>&1 || true' EXIT
gradle --no-daemon connectedDebugAndroidTest
