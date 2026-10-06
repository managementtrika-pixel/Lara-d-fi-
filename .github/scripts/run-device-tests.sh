#!/usr/bin/env bash
set -euo pipefail

mkdir -p final-preview
# Pull captures before the emulator runner shuts down the device.
trap 'adb pull /sdcard/Download/metahuman-final-preview final-preview/ || true' EXIT
gradle --no-daemon connectedDebugAndroidTest
