# Pleos Speech 실험: 세션 계약과 실패 복구

이 기능은 **Pleos TTS/STT SDK 실험용**이다. 향후 AI 팟캐스트의 오디오 플레이어와 생성 엔진은 별도 기능으로 설계한다.

## 계층별 책임

- `domain`: `PlaybackPlan`은 순수 상태 전이와 대기 턴을 단일 불변 값으로 보유한다. `VoiceSession`은 재생 계약, `VoiceSessionOwner`는 UI에 종속되지 않는 lease 계약이다.
- `voice-data`: `VoiceSessionImpl`은 ActivityRetained 범위의 실제 런타임이다. 명령·SDK 콜백·타이머를 하나의 mailbox에서 순차 처리하고 SDK 연결을 생성·폐기한다.
- `voice-local`: Pleos SDK 메서드 호출과 콜백 변환. 중요 제어 이벤트는 `VoiceEventInbox` 큐, RMS는 최신값 처리.
- `presentation`: ViewModel은 lease를 획득하고 세션 StateFlow를 Orbit으로 투영한다. 재생·중지 조건을 새로 구현하지 않는다.
- `data-resource`: 차량 데이터처럼 로딩·성공·분류 가능한 실패를 갖는 Flow에 적용한다. `onStart/onDone`을 `DataResource.Success`로 바꾸지 않는다.

## 강제되는 규칙

1. `PlaybackPlan.begin()`은 Idle에서만 허용된다. 여러 boolean이나 별도 큐를 조합할 수 없다.
2. `onDone`은 `Playing` 상태에서만 성공으로 간주한다. `Starting`/`Stopping`에서 온 `onDone`은 성공으로 처리하지 않는다.
3. 한국어 A/B 시연은 `VoiceSessionState.canPreview()`를 통과해야 한다. UI에서도 필터링하지만 UI를 우회해도 외국어 화자 선택이 허용되지 않는다.
4. stop 이후에는 SDK 연결을 재활용하지 않는다. 세대(generation)가 다른 늦은 콜백은 모두 무시한다.
5. 타임아웃/실패 시 B 대사는 절대 자동 시작하지 않는다. Failed 상태에서 사용자가 재연결을 요청해야 한다.
6. 화면을 떠나도 진행 중 재생은 유지되고, 마지막 발화 완료 및 lease 0에서 SDK를 해제한다. ActivityRetained 자체는 OS 백그라운드 재생 보장을 의미하지 않는다.

## 방어용 제한시간 (Pleos SDK 계약이 아님)

| 단계 | 임시 제한시간 | 실패 처리 |
|---|---:|---|
| TTS 초기화 | 10초 | SDK 연결 해제 후 Failed |
| speak → onStart | 5초 | SDK 연결 해제 후 Failed |
| onStart → onDone | `15초 + 350ms × 문자수` (최대 180초) | SDK 연결 해제 후 Failed |
| stop → onStop | 3초 | SDK 연결 해제 후 Failed |
| STT 시작 → onStartedRecognition | 5초 | 연결 해제 및 재연결 UI 안내 |
| STT 중지 → onEndedRecognition | 3초 | 연결 해제 및 재연결 UI 안내 |

SDK가 발화 ID를 제공하지 않는 한 같은 연결에서 지연된 중복 `onDone`을 **완벽히 식별할 수 없다**. 이를 숨기지 않는다. 현재 정책은 B가 `onStart`를 받아야 `onDone`을 수락하고, 중지 후에는 연결을 교체하는 방식으로 위험을 줄인다. 실제 콜백 순서는 에뮬레이터에서 로그로 확인해야 한다.

## 검증 명령어

```bash
./gradlew :domain:test :voice-data:testDebugUnitTest :presentation:testDebugUnitTest :app:assembleDebug
```

여기에 실제 Pleos 에뮬레이터의 `onStart/onDone/onStop/onError` 순서 검증, 오디오 출력, 접근 권한 검증이 추가되어야 한다.
