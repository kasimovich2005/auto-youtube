#!/usr/bin/env bash
# Installs both APKs on an Android 11 (API 30) emulator, launches the app and
# prints the result, so install/launch problems show up in CI logs.
set -u
status=0
for apk in apks/debug/app-debug.apk apks/release/app-release.apk; do
  echo "::group::install $apk"
  adb uninstall uz.auto.browser >/dev/null 2>&1 || true
  if ! adb install "$apk"; then
    echo "::error::adb install failed for $apk"
    status=1
    echo "::endgroup::"
    continue
  fi
  adb logcat -c
  adb shell am start -W -n uz.auto.browser/.MainActivity
  sleep 15
  if adb shell pidof uz.auto.browser; then
    echo "App process is running after launch ($apk)"
  else
    echo "::error::App is not running after launch ($apk)"
    status=1
  fi
  adb logcat -d -v brief AndroidRuntime:E AutoBrowser:D AndroidAuto:D WebView:D YouTube:D Navigation:D '*:S' | tail -80
  echo "::endgroup::"
done
exit $status
