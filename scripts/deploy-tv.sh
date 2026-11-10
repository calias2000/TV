#!/usr/bin/env bash
# Build and install the app on the TV, then start it.
#   scripts/deploy-tv.sh            release build (the real thing, fast)
#   scripts/deploy-tv.sh debug      debug build (slow, prints player errors in logcat)
# Another TV: TV_ADDR=192.168.1.50:5555 scripts/deploy-tv.sh
# Not ./gradlew installDebug: it installs on every connected device, the TV included.
set -euo pipefail
cd "$(dirname "$0")/.."

TV_ADDR="${TV_ADDR:-192.168.1.230:5555}"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
variant="${1:-release}"

case "$variant" in
  release) task=:app:assembleRelease ;;
  debug)   task=:app:assembleDebug ;;
  *) echo "usage: $0 [release|debug]" >&2; exit 1 ;;
esac

"$ADB" connect "$TV_ADDR"
./gradlew "$task" -q
"$ADB" -s "$TV_ADDR" install -r "app/build/outputs/apk/$variant/app-$variant.apk"
# Release only: compile it fully now, instead of waiting for Android's idle job (smoother at first start).
[ "$variant" = release ] && "$ADB" -s "$TV_ADDR" shell cmd package compile -m speed -f com.calias.tv
"$ADB" -s "$TV_ADDR" shell monkey -p com.calias.tv -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1
echo "$variant build is on the TV"
