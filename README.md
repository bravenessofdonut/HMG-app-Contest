# DrivingApplication 개발 가이드

## 차량 신호를 추가할 때

속도 구독은 AaosSpeedSource → VehicleRepositoryImpl → ObserveSpeedUseCase → VehicleViewModel 순으로 따라간다. 화면은 권한 변화를 알려 주고 ViewModel은 신호별 구독과 재시도를 소유한다. local은 SDK 객체와 리스너를 같은 구독 안에서 획득하고 정리한다. 등록 후 최초 읽기에 실패하는 경우도 정리 대상이다.

| 조건 | 계약 |
| --- | --- |
| 첫 콜백 대기 | DataResource.Loading |
| 응답을 받았지만 도어 값이 없음 | Success(DoorPosition.Unavailable) |
| 권한 거부 | PermissionRequired. 권한 상태를 확인한다. |
| SDK 클래스 또는 메서드를 사용할 수 없음 | Unsupported. 환경의 지원 여부를 안내한다. |
| 서비스 연결이나 구독이 끊김 | ServiceUnavailable. 현재 Flow는 종료되고 재연결은 새 구독을 시작한다. |
| 작업 취소 | 자원 정리 후 취소를 전파한다. |
| 계약에 없는 예외 | 상위로 전파한다. 매퍼 결함을 차량 연결 오류로 바꾸지 않는다. |

Repository의 asDataResourceFlow 분류 함수는 복구 방법을 아는 예외만 변환하고 원인을 보존한다. 매핑은 그 경계 밖에서 수행한다. flatMapDataResource는 성공한 값마다 후속 Flow를 순차 수집한다. 끝나지 않는 구독을 교체해야 하는 기능에는 별도의 최신값 전환 정책이 필요하다.

## 화면 사이에서 음성 세션 공유하기

VoiceSession이 현재 턴과 대기 중인 다음 턴을 소유한다. 명령과 이벤트 처리는 메인 스레드에 한정한다. SDK 어댑터는 여러 스레드에서 콜백을 전달할 수 있다. 화면은 Orbit을 통해 읽기 전용 상태를 관찰한다. 대시보드는 ObservePlaybackUseCase로 현재 진행자만 관찰하며 엔진을 열지 않는다.

- 음성 화면 진입 시 SDK를 연결한다. 화면을 닫으면 음성 인식을 중단하고, 진행 중인 발화는 완료까지 유지한다.
- 음성 화면이 없는 상태에서 마지막 턴이 끝나면 엔진과 리스너를 해제한다. Activity 최종 종료 시에도 세션을 정리한다.
- 완료·중단 신호는 순서가 필요한 제어 이벤트다. 큐 용량은 32개이고 포화 시 연결 실패를 알린다. 음량 샘플은 최신값 한 개만 보관한다.
- 이 범위는 단일 Activity 안의 화면 전환이다. OS 미디어 서비스, 프로세스 재생성 후 복원과 메모리 예산 검증은 별도의 통합 범위다.

## 모듈별 검증

| 모듈 | 기준 사례 |
| --- | --- |
| data-resource | 로딩, 성공, 복구 가능한 실패, 취소, 예상하지 못한 예외 |
| domain | 세션 상태 전이, 구독자 재진입, 중단 이후 완료 |
| vehicle-data | 원시 값의 도메인 매핑, 권한 오류 원인 보존 |
| vehicle-local | 등록 직후 실패와 취소에서 해제 순서·횟수 |
| voice-data | 음성 명령과 이벤트 위임 |
| voice-local | 화자 목록, 음량 폭주, 제어 큐 포화 |
| presentation | 권한 변화와 재시도, 독립 신호, 세션 상태의 Orbit 반영 |
| app | 화면 조립, Activity 재생성 시 같은 세션 유지 |
| build-logic | 실제 버전 카탈로그로 컨벤션을 적용하는 최소 소비자 프로젝트 |

실제 AAOS 권한, SDK의 콜백 순서, 재생 중 메모리 사용량은 에뮬레이터 또는 차량에서 측정한다.
