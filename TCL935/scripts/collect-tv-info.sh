#!/usr/bin/env bash
# Read-only evidence collection. Usage: ./scripts/collect-tv-info.sh TV_IP[:PORT]
set -euo pipefail
target="${1:?Usage: $0 TV_IP[:PORT]}"
adb connect "$target"
adb -s "$target" wait-for-device
adb -s "$target" shell getprop ro.product.model
adb -s "$target" shell getprop ro.build.version.release
adb -s "$target" shell getprop ro.build.version.sdk
adb -s "$target" shell getprop ro.build.display.id
adb -s "$target" shell getprop ro.build.fingerprint
adb -s "$target" shell cmd package list packages | grep -Ei 'amazon|prime|tcl|tvinput|google.android.tv' || true
adb -s "$target" shell dumpsys tv_input
adb -s "$target" shell dumpsys input
