package com.braveberry.drivingapplication

/**
 * release 빌드: 디버그 provider를 설치하지 않음.
 * 운영용 provider(예: Play Integrity)는 차량 환경 지원 여부를 팀에서 결정한 뒤 추가해야 함.
 * Firebase 콘솔에서 App Check 적용(enforcement)이 켜져 있으면 그 전까지 release의 Gemini 호출은 거부됨.
 */
internal object AppCheckSetup {
    fun install() = Unit
}
