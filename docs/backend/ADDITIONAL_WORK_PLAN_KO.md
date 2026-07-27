# 추가 작업 계획

이 문서는 현재 SignBridge + SignInputKit 데모가 production-ready 골격으로
가기 위해 남은 작업을 계속 추적하기 위한 실행 계획입니다.

## 우선순위

| 순서 | 작업 | 산출물 | 상태 |
| --- | --- | --- | --- |
| 1 | Flutter Web 샘플의 bridge 진단 UX 개선 | health/readiness/profile/OpenAPI endpoint 상태 패널 | 완료 |
| 2 | WebSocket 연결 실패/재시도 UX 개선 | 연결 상태, 자동 재연결/backoff, fallback 안내 정리 | 완료 |
| 3 | OpenAPI/Swagger 실제 실행 검증 루틴 정리 | Spring 실행 후 browser/manual smoke 절차 문서화 | 완료 |
| 4 | Protobuf 재생성 환경 보강 | `protoc-gen-dart` 설치 전/후 경로와 mock-only 재생성 분리 | 완료 |
| 5 | Docker 통합 스택 실제 기동 검증 | Docker 있는 환경에서 `verify_docker_http_stack.sh` 실행 | 환경 대기 |
| 6 | 운영 metrics 확장 | Prometheus-friendly exporter 또는 JSON metric contract 보강 | 완료 |
| 7 | 실제 camera landmark extractor 연결 | MediaPipe/ML Kit adapter 구현체 1차 연결 | 준비 완료 |

## 추가 완료 작업

- `scripts/verify_spring_openapi_smoke.sh`를 추가해 Docker 없이 mock server와
  Spring Boot를 띄운 뒤 readiness, profile discovery, OpenAPI, Swagger UI,
  Prometheus metrics, T2S synthesis를 한 번에 검증합니다.
- Kafka/RabbitMQ/English-ASL 검증 스크립트의 metrics assertion을 현재
  `BridgeMetricsService` 필드명에 맞게 갱신했습니다.
- Flutter `Bridge diagnostics` 패널에 Prometheus endpoint 링크를 추가했습니다.
- `MediaPipeLandmarkFrameSource`가 front camera mirroring과 hands/pose/face
  include flag를 적용하도록 보강해 실제 extractor 구현체를 붙일 준비를
  마쳤습니다. 실제 MediaPipe/ML Kit runtime 선택과 네이티브 권한 처리는 별도
  환경 작업입니다.
- mock server Python/protobuf runtime을 전역 Python이 아니라
  `sample/backend/model_server/.venv`로 격리하는 `scripts/setup_mock_venv.sh`를 추가했고,
  Docker-free Spring/OpenAPI smoke script가 이 venv를 자동 사용하도록 보강했습니다.

## 이번 작업 slice

1. `slr_input_kit`에 SignBridge 운영 상태 조회 client를 추가한다. (완료)
2. Flutter example에 bridge diagnostics card를 추가한다. (완료)
3. README와 demo guide에 diagnostics 흐름을 업데이트한다. (완료)
4. Flutter analyze/test와 Spring/Python smoke를 다시 확인한다. (완료)

## 완료 기준

- `http://localhost:5173/` 샘플에서 현재 bridge URL 기준의 health/readiness
  상태와 Swagger/OpenAPI endpoint를 확인할 수 있다.
- Bridge가 꺼져 있어도 앱은 fallback profile을 유지하고, 실패 원인을
  더 구체적으로 보여준다.
- SignBridge Stream 위젯은 연결 실패 후 지수 backoff로 자동 재연결을 시도한다.
- `/internal/metrics.prometheus`는 bridge gauge/counter를 Prometheus text
  exposition 형식으로 제공한다.
- 기존 `flutter analyze`, `flutter test`, `./gradlew test`,
  `scripts/run_eval_fixtures.py`가 계속 통과한다.
- Docker가 없는 환경에서도 `./scripts/setup_mock_venv.sh` 이후
  `MOCK_PORT=18000 ./scripts/verify_spring_openapi_smoke.sh`로 Spring/OpenAPI
  smoke를 재현할 수 있다.
