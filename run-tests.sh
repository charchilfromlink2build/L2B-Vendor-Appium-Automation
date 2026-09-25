#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

UDID=emulator-5554
AVD=Medium_Phone_API_36.1
PKG=com.l2b.app.qa
APPIUM_URL=http://127.0.0.1:4723
QA_URL=https://qa.waardian.com
REPORT_HTML=target/site/allure-maven-plugin/index.html

cd "$(dirname "$0")"

echo "--- JDK ---"
java -version 2>&1 | head -1
java -version 2>&1 | grep -q '"17' || { echo "FAIL: not JDK 17"; exit 1; }

echo "--- Emulator ---"
if ! adb devices | grep -q "^${UDID}[[:space:]]*device$"; then
  STATE=$(adb devices | grep "^${UDID}" || true)
  if [ -z "$STATE" ]; then
    echo "Booting ${AVD}..."
    (emulator -avd "$AVD" >/dev/null 2>&1 &)
    adb wait-for-device
    until [ "$(adb -s $UDID shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
      sleep 2
    done
  else
    echo "FAIL: device state -> $STATE"
    echo "  unauthorized: accept the USB debugging dialog on the emulator"
    echo "  or: adb kill-server && adb start-server"
    echo "  or cold boot: emulator -avd $AVD -no-snapshot-load"
    exit 1
  fi
fi
echo "OK: $UDID ready"

echo "--- App ---"
APK="$(pwd)/apk/app-qa-release.apk"
if [ -f "$APK" ]; then
  echo "Installing from project APK: $APK"
  adb -s "$UDID" install -r "$APK"
elif adb -s "$UDID" shell pm list packages | grep -q "$PKG"; then
  echo "WARN: $APK missing — using already-installed $PKG"
else
  echo "FAIL: put QA APK at apk/app-qa-release.apk then re-run"
  echo "  (or: adb install -r apk/app-qa-release.apk)"
  exit 1
fi
echo "OK: $PKG ready"

echo "--- Appium ---"
curl -sf "${APPIUM_URL}/status" >/dev/null \
  || { echo "FAIL: Appium not running. Start: appium --address 127.0.0.1 --port 4723"; exit 1; }
echo "OK: Appium up"

echo "--- QA backend ---"
HTTP_CODE=$(curl -sf -o /dev/null -w "%{http_code}" --connect-timeout 5 "$QA_URL/" || true)
if [ "$HTTP_CODE" != "200" ]; then
  echo "FAIL: $QA_URL returned '${HTTP_CODE:-unreachable}'"
  exit 1
fi
echo "OK: $QA_URL HTTP 200"

echo "--- Tests ---"
set +e
mvn test 2>&1 | tee run.log
MVN_EXIT=${PIPESTATUS[0]}
set -e

echo "--- Results ---"
grep -E "PASS |FAIL |SKIP |Tests run|permission dialog|BUILD" run.log || true

if [ "$MVN_EXIT" -ne 0 ]; then
  echo "FAIL: mvn test exit $MVN_EXIT — see run.log"
  exit "$MVN_EXIT"
fi

echo "--- Allure ---"
mvn -q allure:report
echo "OK: local preview -> $REPORT_HTML"
echo "--- Company pack ---"
bash scripts/publish-allure.sh
