#!/usr/bin/env bash
# Pleos 에뮬레이터 전용 통합 테스트임. 앱의 Mock Location 허용 설정을 변경함.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ADB="${ADB:-${ANDROID_HOME:-${HOME}/Library/Android/sdk}/platform-tools/adb}"
if [[ ! -x "$ADB" ]]; then ADB="$(command -v adb || true)"; fi
if [[ -z "$ADB" || ! -x "$ADB" ]]; then
  echo "adb not found. Set ADB=/path/to/adb or ANDROID_HOME." >&2
  exit 1
fi

PACKAGE="com.braveberry.drivingapplication"
PERMISSION="pleos.car.permission.FUSED_LOCATION"
TEST_CLASS="com.braveberry.drivingapplication.location.PleosFusedLocationIntegrationTest"

"$ADB" get-state >/dev/null || { echo "Start the Pleos emulator first." >&2; exit 1; }

# 앱 설치 후 권한과 app-op 설정을 적용함.
./gradlew :app:installDebug

if ! "$ADB" shell pm grant "$PACKAGE" "$PERMISSION"; then
  echo "Could not grant FUSED_LOCATION via adb. Approve it in the Pleos app permission UI." >&2
  echo "The integration test will fail with a clear message if permission is still denied." >&2
fi

# Mock Location 허용과 Pleos 위치 권한은 별개임.
echo "Permission before instrumentation (may be reset during APK install):"
"$ADB" shell dumpsys package "$PACKAGE" | grep -F -B 1 -A 2 "$PERMISSION" || true

"$ADB" shell appops set --user current "$PACKAGE" android:mock_location allow
"$ADB" logcat -c 2>/dev/null || true

echo "Running real Pleos FusedLocation integration tests..."
# 테스트 실패 시에도 SDK 콜백과 GPS 주입 로그를 출력함.
status=0
./gradlew :app:connectedDebugAndroidTest \
  "-Pandroid.testInstrumentationRunnerArguments.class=$TEST_CLASS" || status=$?
echo "=== FusedLocation instrumentation log ==="
"$ADB" logcat -d -s PleosLocationIT:I '*:S' | tail -n 160 || true

echo "Permission after instrumentation:"
"$ADB" shell dumpsys package "$PACKAGE" | grep -F -B 1 -A 2 "$PERMISSION" || true

echo "Result: $([[ "$status" -eq 0 ]] && echo PASS || echo FAIL)"
echo "Mock-location app-op is still enabled for $PACKAGE. To revoke:"
echo "  $ADB shell appops set --user current $PACKAGE android:mock_location deny"
exit "$status"
