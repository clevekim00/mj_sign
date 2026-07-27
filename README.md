# SignBridge

SignBridge는 LinguaSign 제품을 구동하는 크로스 플랫폼 수어 입력 플랫폼입니다. SignInputKit SDK가 손/포즈/얼굴 landmark frame을 WebSocket protobuf로 전송하고, Spring Boot SignBridge backend가 세션 버퍼링, idle timeout flush, GPU serving provider 라우팅, queue worker 흐름, LLM 문장 보정을 담당합니다.

> 브랜딩 기준: 프로젝트/플랫폼명은 **SignBridge**, 홍보용 제품명은 **LinguaSign**, SDK 브랜드는 **SignInputKit**입니다. 현재 Flutter package 이름은 호환성을 위해 `slr_input_kit`으로 유지합니다.

- [English README](./README_en.md)
- [한국어 상세 문서](./README_ko.md)
- [프로젝트 홍보 문서 (KO)](./docs/embedded/PROJECT_PROMOTION_KO.md)
- [Project Promotion (EN)](./docs/embedded/PROJECT_PROMOTION_EN.md)
- [프로젝트 아키텍처 (KO)](./docs/backend/PROJECT_ARCHITECTURE_KO.md)
- [Project Architecture (EN)](./docs/backend/PROJECT_ARCHITECTURE.md)
- [SignGemma 앱 예제 가이드 (KO)](./docs/backend/SIGN_GEMMA_APP_DEMO_KO.md)
- [SignGemma App Demo Guide (EN)](./docs/backend/SIGN_GEMMA_APP_DEMO.md)
- [API / SPI Reference](./docs/backend/API_SPI_REFERENCE.md)
- [BE-Model 표준 프로토콜](./docs/backend/MODEL_PROTOCOL.md)
- [T2S / STS Synthesis Design (KO)](./docs/backend/SIGN_SYNTHESIS_DESIGN_KO.md)
- [T2S / STS Synthesis Design (EN)](./docs/backend/SIGN_SYNTHESIS_DESIGN.md)
- [언어별 모델 추가 가이드](./docs/backend/LANGUAGE_MODEL_GUIDE.md)
- [SignGemma 조사 노트 (KO)](./docs/embedded/SIGN_GEMMA_RESEARCH_KO.md)
- [SignGemma Research Notes (EN)](./docs/embedded/SIGN_GEMMA_RESEARCH.md)
- [LLM Integration Prompt](./docs/backend/PROMPT_LLM_INTEGRATION.md)
- [개선 계획 및 리뷰](./docs/backend/REVIEW_AND_ENHANCEMENT_PLAN.md)

## Project Architecture / 프로젝트 아키텍처

공통 런타임 구조와 SignGemma-compatible 예제 흐름은 [PROJECT_ARCHITECTURE_KO.md](docs/backend/PROJECT_ARCHITECTURE_KO.md)에 정리되어 있습니다. 영문판은 [PROJECT_ARCHITECTURE.md](docs/backend/PROJECT_ARCHITECTURE.md)입니다. Spring Boot + cross-platform app 실행 가이드는 [SIGN_GEMMA_APP_DEMO_KO.md](docs/backend/SIGN_GEMMA_APP_DEMO_KO.md), 영문판은 [SIGN_GEMMA_APP_DEMO.md](docs/backend/SIGN_GEMMA_APP_DEMO.md)를 참고하세요.

## 왜 SignBridge인가

- 수어 입력을 일반 텍스트 필드처럼 붙일 수 있는 SignInputKit의 `SlrInputWidget` 중심 구조입니다.
- Android, iOS, iPad, Web, Windows, macOS/OSX, Linux 샘플 흐름을 한 예제 앱에서 비교할 수 있습니다.
- 실제 GPU serving 전까지 `sign_gemma_mock`으로 WebSocket, queue, serializer/converter, metrics 경로를 검증할 수 있습니다.
- `http`, `grpc`, `queue` provider 분기와 Kafka/RabbitMQ transport 골격이 있어 운영 구조로 확장하기 쉽습니다.
- Gemma 2 기반 LLM 보정 레이어로 원시 수어 키워드를 자연스러운 한국어 문장으로 다듬는 방향을 포함합니다.

## 현재 아키텍처

```mermaid
graph TD
    A["SignInputKit SDK / slr_input_kit"] -->|"protobuf landmark frames over WebSocket"| B["Spring Boot SignBridge backend"]
    B --> C["Session buffer + idle timeout flush"]
    C --> D["Async inference dispatcher"]
    D --> E{"Inference provider"}
    E -->|"http"| F["HTTP GPU serving client"]
    E -->|"grpc"| G["gRPC extension point"]
    E -->|"queue"| H["Queue inference gateway"]
    H --> I{"Queue transport"}
    I -->|"in-memory"| J["Local worker"]
    I -->|"kafka"| K["Kafka worker consumer"]
    I -->|"rabbitmq"| L["RabbitMQ worker consumer"]
    F --> M["Mock GPU or real serving backend"]
    J --> M
    K --> M
    L --> M
    B --> N["LLM refinement / Gemma 2 via Ollama"]
    N --> O["Natural language output"]
```

## 저장소 구성

```text
sign/
├── embedded/       # 앱에 내장되는 SignInputKit Flutter SDK
├── backend/        # 원격 인식용 Spring SignBridge
└── common/         # protobuf 원본과 공통 평가 fixture
sample/
├── embedded/       # Flutter SDK 사용 예제
└── backend/        # 모델 서버·웹 앱·Docker 통합 예제
docs/
├── embedded/       # 내장형 인식 및 접근성 문서
└── backend/        # 스트리밍·API·운영 문서
```

각 폴더의 README에 책임, 의존 방향과 실행 방법을 정리했습니다.

## 구현된 주요 기능

- `/ws/sign` WebSocket binary protobuf landmark intake
- 세션 단위 buffering, frame window aggregation, idle timeout 자동 flush
- 세션별 in-flight 보호가 있는 async inference dispatch
- `sign.gpu.provider=http|grpc|queue` 기반 provider routing
- HTTP GPU serving contract: `GpuInferenceRequest`, `GpuInferenceResponse`
- 언어 독립 BE SPI: `InferenceContext` 기반 `locale`, `sign_language`, `model_profile` 표준화
- Queue worker contract: `QueueInferenceTask`, `QueueInferenceResult`, `QueueInferenceTransport`, `QueueWorkerBackend`
- Kafka/RabbitMQ serializer, converter, worker consumer, result publication path
- 운영 endpoint: `GET /internal/healthz`, `GET /internal/readyz`, `GET /internal/metrics`
- Gemma 2/Ollama 기반 `POST /api/v2/translate` LLM 문장 보정
- T2S/STS 1차 계약: `POST /api/v2/sign/synthesize`, `POST /api/v2/speech/sign`, mock `SignPlan + landmark motion`
- T2S/STS SPI: `SpeechToTextAdapter`, `SignPlanner`, `SignMotionGenerator`, `SignSynthesisProvider`
- Flutter `SignOutputWidget` 및 Web `SignSynthesisPreview` 기반 synthesis playback stub

## 빠른 시작

Mock GPU 서버:

```bash
cd sample/backend/model_server
python3 -m pip install -r requirements.txt
python3 main.py
```

Spring bridge:

```bash
cd sign/backend/bridge
./gradlew bootRun
```

Flutter 샘플 앱:

```bash
cd sample/embedded/flutter_app
flutter run
```

샘플 앱은 기본적으로 deterministic demo landmark source를 사용합니다. 실제 카메라 landmark extractor가 없어도 bridge 연결, protobuf streaming, 최종 결과 이벤트 흐름을 바로 확인할 수 있습니다.

## 플랫폼별 샘플

플랫폼 샘플은 [sample/embedded/flutter_app/lib/samples](./sample/embedded/flutter_app/lib/samples)에 분리되어 있고, [sample/embedded/flutter_app/lib/main.dart](./sample/embedded/flutter_app/lib/main.dart)에서 갤러리 형태로 실행됩니다.

| Platform | Sample profile | Run command | 기본 bridge URL |
| --- | --- | --- | --- |
| Android | `android_sample.dart` | `flutter run -d android` | `ws://10.0.2.2:8080/ws/sign` |
| iOS | `ios_sample.dart` | `flutter run -d ios` | `ws://127.0.0.1:8080/ws/sign` |
| iPad | `ipad_sample.dart` | `flutter run -d <ipad-device-id>` | `ws://127.0.0.1:8080/ws/sign` |
| Web | `web_sample.dart` | `flutter run -d chrome` | `ws://localhost:8080/ws/sign` |
| Windows | `windows_sample.dart` | `flutter run -d windows` | `ws://127.0.0.1:8080/ws/sign` |
| macOS/OSX | `macos_sample.dart` | `flutter run -d macos` | `ws://127.0.0.1:8080/ws/sign` |
| Linux | `linux_sample.dart` | `flutter run -d linux` | `ws://127.0.0.1:8080/ws/sign` |

실제 제품에서는 `DemoLandmarkFrameSource`를 `CameraLandmarkFrameSource` 또는 MediaPipe/ML Kit/온디바이스 모델 기반 extractor로 교체하면 됩니다.

## 언어 및 수어 모델 라우팅

Flutter client는 기본적으로 현재 platform locale을 기준으로 WebSocket URL에 `locale`, `sign_language`, `model_profile`, `protocol_version`을 붙입니다. 플랫폼별 active keyboard layout은 Flutter에서 일관되게 노출되지 않으므로, 앱에서 키보드 언어를 알 수 있는 경우 `SignLanguageContext`로 명시 override할 수 있습니다.

영어 locale은 BE에서 기본적으로 `asl`과 `sign-gemma` model profile로 정규화됩니다. BE 내부 SPI는 언어와 무관하게 `InferenceContext`를 함께 받는 동일한 형태이며, model backend에는 [MODEL_PROTOCOL.md](./docs/backend/MODEL_PROTOCOL.md)에 정의된 표준 JSON envelope가 전달됩니다.

Spring Boot는 `GET /api/v2/model-profiles`로 지원하는 locale/sign-language/model profile registry를 공개합니다. Flutter 샘플의 `Model profile` selector는 이 endpoint를 읽어 WebSocket, T2S, STS 요청에 같은 profile을 적용하며, bridge가 꺼져 있으면 bundled demo profile로 fallback합니다.

명시적으로 요청한 `sign_language` 또는 `model_profile`이 registry에 없거나 서로 맞지 않으면 SignBridge는 unsupported profile로 거절합니다. REST synthesis API는 HTTP 400을 반환하고, WebSocket은 `unsupported-profile` event를 보낸 뒤 연결을 닫습니다.

API/SPI 경계는 [API_SPI_REFERENCE.md](./docs/backend/API_SPI_REFERENCE.md)에 정리되어 있고, 새 언어 모델을 추가하는 절차와 Sign Gemma 호환 model spec은 [LANGUAGE_MODEL_GUIDE.md](./docs/backend/LANGUAGE_MODEL_GUIDE.md)를 기준으로 따르면 됩니다.

SignGemma 공개 정보와 landmark 지원 범위는 [SIGN_GEMMA_RESEARCH_KO.md](./docs/embedded/SIGN_GEMMA_RESEARCH_KO.md)와 [SIGN_GEMMA_RESEARCH.md](./docs/embedded/SIGN_GEMMA_RESEARCH.md)에 따로 정리했습니다.

## Provider 설정

주요 설정은 [sign/backend/bridge/src/main/resources/application.yml](./sign/backend/bridge/src/main/resources/application.yml)에 있습니다.

- `sign.gpu.provider`
- `sign.language.default-locale`
- `sign.language.sign-language-by-locale-language`
- `sign.language.model-profile-by-sign-language`
- `sign.gpu.base-url`
- `sign.gpu.infer-path`
- `sign.gpu.health-path`
- `sign.gpu.grpc-target`
- `sign.gpu.queue-transport`
- `sign.gpu.queue-request-topic`
- `sign.gpu.queue-result-topic`
- `sign.gpu.queue-consumer-group`
- `sign.gpu.queue-exchange`
- `sign.gpu.queue-routing-key`
- `sign.gpu.queue-timeout-ms`
- `sign.window.min-frames`
- `sign.window.idle-timeout-ms`
- `sign.async.core-pool-size`

## Broker 로컬 실행

Kafka:

```bash
docker compose -f sample/backend/docker-compose.kafka.yml up -d
cd sign/backend/bridge
./gradlew bootRun --args='--spring.profiles.active=kafka'
```

RabbitMQ:

```bash
docker compose -f sample/backend/docker-compose.rabbitmq.yml up -d
cd sign/backend/bridge
./gradlew bootRun --args='--spring.profiles.active=rabbitmq'
```

Mock GPU와 bridge까지 포함한 통합 스택:

```bash
docker compose -f sample/backend/docker-compose.stack.http.yml up -d --build
docker compose -f sample/backend/docker-compose.stack.kafka.yml up -d --build
docker compose -f sample/backend/docker-compose.stack.rabbitmq.yml up -d --build
```

HTTP provider 통합 스택은 아래 스크립트로 build, readiness, profile discovery,
T2S, WebSocket protobuf streaming까지 한 번에 확인할 수 있습니다.

```bash
./scripts/verify_docker_http_stack.sh
```

`sign_gemma_mock`은 [requirements.txt](./sample/backend/model_server/requirements.txt)와
[Dockerfile](./sample/backend/model_server/Dockerfile)로 Python/protobuf runtime을 고정합니다.
로컬 Python에 오래된 `protobuf`가 설치되어 있으면 mock 서버가 schema import 단계에서
실패할 수 있으므로 requirements 또는 Docker 실행을 사용하세요.

Flutter 샘플 앱의 `Bridge diagnostics` 패널은 현재 WebSocket URL에서 계산한
HTTP base 기준으로 `/internal/healthz`, `/internal/readyz`,
`/api/v2/model-profiles`, `/swagger-ui.html`, `/v3/api-docs`를 바로 확인할 수
있게 합니다. Bridge가 꺼져 있어도 fallback profile은 유지하고, 실패 원인은
패널에 표시합니다.
`SignBridge Stream` 위젯은 샘플에서 자동 재연결을 켜 두었고, 연결 실패 시
지수 backoff로 최대 6회까지 재시도합니다.

## Protobuf 재생성

공용 schema를 수정한 뒤에는 Python mock, Flutter SDK, Spring proto source를 같은
계약으로 맞춰야 합니다.

```bash
./scripts/regenerate_protobuf.sh
```

이 스크립트는 `protoc`와 `protoc-gen-dart`를 사용합니다. Spring Java protobuf
출력은 Gradle build에서 생성되며, 필요하면 `RUN_GRADLE=1 ./scripts/regenerate_protobuf.sh`
로 함께 생성할 수 있습니다.

Python mock schema만 다시 만들 때는 Dart 플러그인 없이 아래 스크립트를 사용합니다.

```bash
./scripts/regenerate_mock_protobuf.sh
```

## 통합 검증

Kafka queue flow:

```bash
./scripts/verify_kafka_stack.sh
```

RabbitMQ queue flow:

```bash
./scripts/verify_rabbitmq_stack.sh
```

English/ASL `sign-gemma` profile flow:

```bash
./scripts/verify_english_asl_profile.sh
```

백엔드 단위 테스트:

```bash
cd sign/backend/bridge
./gradlew test
```

Flutter 패키지 분석:

```bash
dart analyze sign/embedded
```

CI는 GitHub Actions의 [ci.yml](./.github/workflows/ci.yml)에서 Spring Boot test,
Flutter analyze/test, Python mock import, eval fixture smoke를 실행합니다.

## OpenAPI 문서

Bridge 실행 후 Swagger UI는 `http://localhost:8080/swagger-ui.html`,
OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 확인합니다.
문서에는 model profile discovery, T2S/STS synthesis, readiness/health/metrics의
예제 request/response가 포함되어 있습니다.
Prometheus text metrics는 `http://localhost:8080/internal/metrics.prometheus`에서
확인할 수 있습니다.

Docker 없이 Spring Boot와 mock model server만 검증하려면 mock 전용 venv를
만든 뒤 smoke script를 사용합니다.

```bash
./scripts/setup_mock_venv.sh
```

```bash
./scripts/verify_spring_openapi_smoke.sh
```

이 smoke script는 `sample/backend/model_server/.venv`가 있으면 자동으로 사용합니다.

## 운영 준비 체크포인트

- 실제 GPU serving backend의 `/healthz`, `/infer` contract를 mock과 동일하게 맞춥니다.
- WebSocket은 운영에서 WSS로 노출하고 session ID 수명과 인증 정책을 정합니다.
- Kafka/RabbitMQ profile에는 DLQ, retry, backoff, consumer group 정책을 환경별로 튜닝합니다.
- `/internal/metrics`를 Prometheus 또는 운영 대시보드로 연결합니다.
- LLM 보정은 final/high-confidence 결과에만 적용해 latency와 비용을 관리합니다.

## 현재 상태

SignBridge는 단순 로컬 FFI 실험이 아니라, 크로스 플랫폼 수어 입력 UI와 클라우드/edge GPU serving bridge를 연결하는 구조로 발전했습니다. 다음 단계는 실제 landmark extractor와 실제 GPU 모델 serving을 붙여 demo landmark source를 LinguaSign 제품 수준 입력 파이프라인으로 교체하는 것입니다.
