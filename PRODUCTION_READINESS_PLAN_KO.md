# SignBridge Production-Readiness 개발 계획

이 계획은 공식 SignGemma 모델 artifact가 준비되기 전에도 진행할 수 있는
제품/플랫폼 개발 항목을 정리합니다. 목표는 실제 모델이 나중에 들어와도 앱,
Spring Boot bridge, model server, queue worker, 운영 계층의 계약이 흔들리지
않게 만드는 것입니다.

영문 아키텍처: [PROJECT_ARCHITECTURE.md](./PROJECT_ARCHITECTURE.md)  
한국어 아키텍처: [PROJECT_ARCHITECTURE_KO.md](./PROJECT_ARCHITECTURE_KO.md)  
실행 예제: [SIGN_GEMMA_APP_DEMO_KO.md](./SIGN_GEMMA_APP_DEMO_KO.md)

## 개발 원칙

- 모델 성능과 플랫폼 계약을 분리합니다.
- 공식 SignGemma weight가 없어도 API/SPI, envelope, routing, readiness,
  metrics, playback, eval harness는 먼저 고정합니다.
- mock 구현은 버리는 코드가 아니라 공식 모델 adapter가 따라야 할 계약 검증
  기준으로 사용합니다.
- 각 단계는 테스트 가능한 산출물을 남깁니다.

## 단계별 계획

| 단계 | 목표 | 산출물 | 검증 |
| --- | --- | --- | --- |
| 1 | Spring Boot ↔ model server 계약 고정 | request/response envelope validation, mismatch error handling, contract tests | `./gradlew test` |
| 2 | WebSocket protobuf streaming 안정화 | frame window 정책, empty/oversized payload 처리, status/error event 일관화 | WebSocket handler tests |
| 3 | Flutter cross-platform sample 완성도 개선 | platform별 URL/default profile, model profile selector, discovery timeout/stale guard, T2S/STS UX, web runner, smoke tests | `flutter analyze`, `flutter test` |
| 4 | Camera landmark extractor SPI 준비 | `LandmarkFrameSource` production contract, MediaPipe/ML Kit adapter skeleton, profile-aware source swap | Dart analyzer, sample source swap test |
| 5 | T2S/STS API와 playback UX 개선 | request validation, motion metadata, pause/replay controls | Spring + Flutter tests |
| 6 | Queue/Kafka/RabbitMQ worker 안정화 | retry/DLQ policy, result correlation, timeout metrics | queue transport tests |
| 7 | Health/readiness/metrics 운영 계층 | model profile readiness, contract error counters, latency summary | controller tests, curl smoke |
| 8 | Model profile registry와 언어 라우팅 | profile metadata endpoint, client discovery, unsupported profile handling, route mismatch rejection, alias normalization | resolver/profile tests |
| 9 | 평가용 dataset/eval harness | fixture dataset format, batch runner, metrics report | script smoke test |
| 10 | 공식 SignGemma adapter 자리 | official model adapter interface, config keys, migration checklist | mock-to-real adapter contract test |

## 1차 개발 범위

이번 개발 slice는 가장 아래 계약부터 단단히 합니다.

- `GpuInferenceRequest`가 model server로 보낼 envelope를 안정적으로 유지합니다.
- `GpuInferenceResponse`가 echo한 `protocol_version`, `locale`, `sign_language`,
  `model_profile`이 요청 context와 다르면 bridge가 오류로 처리합니다.
- model response의 `confidence` 범위를 `0.0..1.0`으로 clamp합니다.
- 관련 계약 테스트를 추가합니다.

## 다음 slice 후보

- WebSocket empty frame / oversized frame 정책 추가. (진행: error event 처리와 테스트 추가)
- `/internal/metrics`에 model contract mismatch counter 추가. (완료: `model_protocol_errors`)
- Flutter `SignOutputWidget`에 replay/pause/speed controls 추가. (완료)
- `MediaPipeLandmarkFrameSource` skeleton 추가. (완료)
- `scripts/run_eval_fixtures.py`로 fixture 기반 S2T 회귀 테스트 추가. (완료: offline validation, optional model endpoint)
- Docker HTTP 통합 스택 검증 스크립트 추가. (완료: build/readiness/profile/T2S/WebSocket probe)
- OpenAPI 예제 request/response 보강. (완료: model profiles, T2S/STS, readiness/health/metrics)
- GitHub Actions toolchain pinning. (완료: Java 21, Flutter 3.38.5/Dart 3.10.4, Python 3.11)
- Protobuf 재생성 스크립트 추가. (완료: Python mock, Flutter generated model, Spring proto source)
