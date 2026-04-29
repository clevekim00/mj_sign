# SignBridge 프로젝트 아키텍처

이 문서는 LinguaSign / SignBridge / SignInputKit 예제와 운영 확장 구조를
한 장의 기준 아키텍처로 정리합니다. 현재 SignGemma 공식 checkpoint와 입력
spec이 공개적으로 확정되지 않았기 때문에, 실행 가능한 예제는
`sign-gemma` model profile을 SignGemma-compatible ASL mock serving 계약으로
사용합니다.

## 전체 구성

```mermaid
graph TD
    A["Cross-platform app<br/>Web / iOS / iPad / Android / Windows / macOS / Linux"] --> B["SignInputKit Flutter SDK<br/>slr_input_kit"]
    B -->|"S2T: protobuf landmark frames<br/>WebSocket /ws/sign"| C["Spring Boot SignBridge"]
    C --> D["Session buffer<br/>window aggregation<br/>idle timeout flush"]
    D --> E["Async inference dispatcher"]
    E --> F{"Inference provider"}
    F -->|"http"| G["HTTP GPU serving client"]
    F -->|"grpc"| H["gRPC extension point"]
    F -->|"queue"| I["Queue inference gateway"]
    I --> J{"Queue transport"}
    J -->|"in-memory"| K["Local worker"]
    J -->|"Kafka"| L["Kafka worker"]
    J -->|"RabbitMQ"| M["RabbitMQ worker"]
    G --> N["sign_gemma_mock FastAPI<br/>or real SignGemma-compatible serving"]
    K --> N
    L --> N
    M --> N
    N -->|"recognized text + metadata"| C
    C -->|"result event"| B
    B --> A
    C --> O["Optional LLM refinement<br/>Gemma/Ollama"]
    O -->|"natural sentence"| C
```

## SignGemma 예제 흐름

```mermaid
sequenceDiagram
    participant App as Cross-platform Flutter app
    participant SDK as SignInputKit
    participant Bridge as Spring Boot SignBridge
    participant Mock as sign_gemma_mock

    App->>SDK: Select platform sample
    SDK->>Bridge: Connect ws://.../ws/sign?locale=en-US&sign_language=asl&model_profile=sign-gemma
    SDK->>Bridge: Send ClientStreamChunk protobuf frames
    Bridge->>Bridge: Buffer frames and flush window
    Bridge->>Mock: POST /api/v2/recognize GpuInferenceRequest
    Mock->>Bridge: GpuInferenceResponse with ASL/English result
    Bridge->>SDK: WebSocket result event
    SDK->>App: Render recognized text
```

## Text/Speech-to-Sign 흐름

```mermaid
sequenceDiagram
    participant App as Cross-platform Flutter app
    participant Bridge as Spring Boot SignBridge
    participant Planner as SignPlanner
    participant Motion as SignMotionGenerator
    participant Player as SignOutputWidget

    App->>Bridge: POST /api/v2/sign/synthesize text
    App->>Bridge: or POST /api/v2/speech/sign transcript
    Bridge->>Planner: Normalize locale/sign_language/model_profile
    Planner->>Motion: SignPlan glosses + markers
    Motion->>Bridge: landmark motion frames
    Bridge->>App: SignSynthesisResult
    App->>Player: Play SignPlan + landmark motion
```

## 런타임 책임

| 영역 | 책임 | 현재 구현 | 교체/확장 지점 |
| --- | --- | --- | --- |
| App | 플랫폼별 UI, 카메라 권한, 입력/재생 UX | Flutter sample gallery | 제품 앱, kiosk, accessibility input |
| SignInputKit | WebSocket client, protobuf frame 전송, playback widget | `slr_input_kit` | 실제 camera landmark extractor |
| SignBridge | 세션, buffering, routing, API/SPI, readiness/metrics | Spring Boot | 인증, tenant routing, 운영 정책 |
| Model serving | S2T inference | `sign_gemma_mock` | 공식 SignGemma-compatible server |
| Synthesis | T2S/STS mock motion | `SignPlanner`, `MockSignMotionGenerator` | 실제 planner, avatar/skeleton renderer |
| Queue | 비동기 inference transport | in-memory/Kafka/RabbitMQ skeleton | 운영 broker, DLQ, retry |
| LLM refinement | raw keyword 보정 | Gemma/Ollama SPI | 언어별 prompt/model policy |

## 배포 관점

```mermaid
graph LR
    A["Mobile/Desktop/Web app"] -->|"WSS / HTTPS"| B["SignBridge API tier"]
    B -->|"HTTP/gRPC or queue"| C["Model serving tier"]
    B -->|"metrics/readiness"| D["Observability"]
    B -->|"Kafka/RabbitMQ optional"| E["Broker"]
    E --> C
    C -->|"GPU/CPU runtime"| F["SignGemma-compatible model"]
```

개발 환경에서는 앱, Spring Boot, `sign_gemma_mock`을 한 머신에서 실행할 수
있습니다. 운영 환경에서는 SignBridge와 model serving을 분리하고, GPU model
서버나 broker worker를 독립적으로 확장하는 구성이 안전합니다.

## 검증 훅

- `./scripts/verify_docker_http_stack.sh`는 HTTP compose stack을 build하고
  readiness, profile discovery, T2S, WebSocket protobuf streaming을 검증합니다.
- `./scripts/regenerate_protobuf.sh`는 `schema/landmark.proto`에서 Python과
  Flutter protobuf 산출물을 재생성합니다. Spring Java 출력은 Gradle build에서
  생성합니다.
- `/swagger-ui.html`과 `/v3/api-docs`는 profile discovery, synthesis,
  readiness, health, metrics 예제를 포함한 OpenAPI 문서를 제공합니다.
