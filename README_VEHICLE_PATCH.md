# Pleos Guide Architecture (develope/guide)

기준 커밋: `09e15da85795c4953f252a31b541df76dd0db86b` (리팩터링 전). 이 변경은 차량 신호 처리의 기존 동작을 보존하는 구조 리팩터링이다.

## Gradle 모듈과 의존 방향

- `app` → `presentation`, `vehicle-data`, `vehicle-local` : **composition root**. app은 앱 빌드에 구현을 포함하지만 SDK 로직을 사용하지 않음
- `presentation` → `domain`, `data-resource`
- `domain` → `data-resource`
- `vehicle-data` → `domain`, `data-resource` (JVM 모듈)
- `vehicle-local` → `vehicle-data`, `data-resource` (Android 모듈, Pleos/AAOS SDK 경계)
- `assembly` 모듈은 제거함. 각 구현 모듈 `di` 패키지가 자체 바인딩을 소유함

## 소스 패키지 규칙

- domain: `repository`, `usecase/vehicle`, `model` (미래 모델 확장 공간)
- vehicle-data: `datasource`, `impl`, `di`, `model`, `mapper`
- vehicle-local: `impl`, `source/aaos`, `source/pleos`, `di`, `model`, `mapper`
- presentation: `vehicle/screen` (Route + Screen), `vehicle/viewmodel` (Orbit state + VM)
- 인터페이스: `VehicleRepository`, `VehicleDataSource`. 구현체: `VehicleRepositoryImpl`, `VehicleDataSourceImpl`.
- UseCase는 도메인 흐름을 대표하므로 위임만 하더라도 `ObserveSpeedUseCase`, `ObserveDriverDoorUseCase` 유지.
- 아직 미구현인 모델/매퍼 패키지는 `.gitkeep`만 둠. 의미 없는 DTO/Mapper 클래스를 만들지 않음.

## 데이터 흐름

- AAOS: `PERF_VEHICLE_SPEED` → `AaosSpeedSource` → `VehicleDataSourceImpl` → `VehicleRepositoryImpl` → `ObserveSpeedUseCase` → Orbit VM → Compose.
- Pleos: SDK Door → `PleosDoorSource` → 나머지는 동일.
- 구독 각각 독립 실행, 미수신과 오류를 숫자 0으로 대체하지 않음.
- `CAR_SPEED` 권한 거부 문제는 이 구조 리팩터링에서 수정하지 않음; 실환경 검증 필요.
- 도어 정수 코드 0/1의 의미도 SDK 문서/차량별 검증 전까지 그대로 유지함.

## 자동 테스트

- `:domain:test` : UseCase 위임, 오류 흐름 보존
- `:vehicle-data:test` : Loading/Success/Error, 상호 독립된 데이터 스트림
- `:vehicle-local:testDebugUnitTest` : 데이터소스 위임 및 Flow 라이프사이클(MockK; 실제 SDK 미호출)
- `:presentation:testDebugUnitTest` : ViewModel Orbit 상태 전이, 독립성 및 재시도
- `:data-resource:test` : 성공/오류/취소 전파
- `:presentation:connectedDebugAndroidTest` : Compose Screen 렌더링
- `:app:connectedDebugAndroidTest` : Hilt 앱 조립과 화면 smoke test (Pleos 에뮬레이터 등 실제 기기 필요)

개별 JVM 테스트와 실제 Vehicle SDK callback/권한 검증은 **서로 다른 검증**이다. 실제 SDK 테스트는 기기 연결과 적합한 권한이 필요함.

## 환경 정보

- `android.car`는 일반 Android SDK에 없어 해당 reflection은 local 모듈에 유지. 차량에 공개된 API/권한/실제 값이 있는지 별도 검증.
- Pleos Vehicle SDK 2.3.2와 `DoorArea(ROW_1_LEFT)`는 기존 브랜치 그대로 사용.
- `local.properties`의 계정 키와 인증값을 커밋하지 말 것.
