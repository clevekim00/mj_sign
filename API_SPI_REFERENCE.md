# MJ Sign API / SPI Reference

이 문서는 MJ Sign에서 외부 client, Spring bridge, inference provider, queue worker, model backend가 만나는 API/SPI 경계를 정리합니다. 목표는 언어가 늘어나도 BE 내부 SPI와 BE-model protocol이 흔들리지 않도록 하는 것입니다.

## 용어

- API: 외부 시스템이나 client가 호출하는 공개 계약입니다.
- SPI: 구현체를 교체하기 위해 내부에서 사용하는 확장 계약입니다.
- Model protocol: BE와 model backend 사이의 표준 request/response envelope입니다.
- Inference context: `locale`, `sign_language`, `model_profile`, `protocol_version`을 묶은 언어/모델 라우팅 context입니다.

## Public Client API

### Flutter Widget API

파일:

- `slr_input_kit/lib/src/slr_input_widget.dart`
- `slr_input_kit/lib/src/sign_gemma_client.dart`
- `slr_input_kit/lib/src/landmark_frame_source.dart`

주요 사용 형태:

```dart
SlrInputWidget(
  bridgeUrl: 'ws://127.0.0.1:8080/ws/sign',
  languageContext: const SignLanguageContext(
    locale: 'en-US',
    signLanguage: 'asl',
    modelProfile: 'sign-gemma',
  ),
  landmarkFrameSource: myFrameSource,
  onSignRecognized: (text) {},
)
```

핵심 파라미터:

| 이름 | 역할 |
| --- | --- |
| `bridgeUrl` | Spring Sign Bridge WebSocket endpoint |
| `languageContext` | locale/sign language/model profile 힌트 |
| `landmarkFrameStream` | 외부에서 만든 landmark batch stream |
| `landmarkFrameSource` | start/stop/dispose lifecycle을 가진 landmark source |
| `onSignRecognized` | final result text callback |

### LandmarkFrameSource SPI

Flutter 앱은 실제 카메라/MediaPipe/ML Kit/온디바이스 모델을 이 SPI 뒤에 숨길 수 있습니다.

```dart
abstract interface class LandmarkFrameSource {
  Stream<List<LandmarkFrame>> get frames;
  Future<void> start();
  Future<void> stop();
  Future<void> dispose();
}
```

운영 구현체 요구사항:

- frame batch는 `schema/landmark.proto`의 `LandmarkFrame`을 따라야 합니다.
- timestamp는 ms 단위로 채웁니다.
- 손 landmark는 가능한 한 좌/우를 분리합니다.
- 과도한 FPS는 BE queue와 GPU latency를 악화시키므로 8-12 FPS부터 튜닝합니다.

## WebSocket API

Endpoint:

```text
GET /ws/sign
```

Query parameters:

| 이름 | 예시 | 필수 | 설명 |
| --- | --- | --- | --- |
| `locale` | `en-US` | 아니오 | BCP-47 style locale hint |
| `sign_language` | `asl` | 아니오 | 정규화된 수어 코드 |
| `model_profile` | `sign-gemma` | 아니오 | 사용할 model profile |
| `protocol_version` | `mj-sign-model-v1` | 아니오 | BE-model protocol version |

Client to BE payload:

- Binary protobuf
- Message: `mj.sign.ClientStreamChunk`
- Schema: `schema/landmark.proto`

BE to client event:

```json
{
  "session_id": "sample-en",
  "event_type": "result",
  "result_text": "Nice to meet you.",
  "text": "Nice to meet you.",
  "is_final": true,
  "confidence": 0.94
}
```

Event types:

| `event_type` | 의미 |
| --- | --- |
| `status` | buffering, processing, idle flush, busy 등 진행 상태 |
| `result` | inference 결과 |
| `error` | payload parse, inference, provider 오류 |

## REST API

### LLM Translation API

Endpoint:

```text
POST /api/v2/translate
```

목적:

- 수어 인식 모델이 반환한 키워드 배열을 자연어 문장으로 보정합니다.
- 현재 구현은 Spring AI/Ollama 기반 Gemma 계열 LLM을 사용합니다.

주의:

- 실시간 inference flow에서는 final/high-confidence/non-system 결과에만 LLM refinement가 적용됩니다.
- 언어별 prompt는 `InferenceContext`의 `locale`, `sign_language`를 기준으로 선택됩니다.

## Operations API

Endpoint:

| Method | Path | 목적 |
| --- | --- | --- |
| `GET` | `/internal/healthz` | process liveness |
| `GET` | `/internal/readyz` | selected provider/model readiness |
| `GET` | `/internal/metrics` | runtime counters and gauges |

운영 지표 예:

- active WebSocket sessions
- buffered sessions and frames
- in-flight inferences
- received messages
- payload parse errors
- accepted/rejected dispatches
- completed inferences
- idle flush count

## Backend SPI

### InferenceGateway

파일:

- `sign_bridge/src/main/java/com/mj/sign/InferenceGateway.java`
- 구현체: `HttpInferenceGateway`, `GrpcInferenceGateway`, `QueueInferenceGateway`, `RoutingInferenceGateway`

표준 SPI:

```java
TranslationResult sendForInference(ClientStreamChunk chunk, InferenceContext context)
```

규칙:

- 모든 provider는 같은 `ClientStreamChunk`와 `InferenceContext`를 받습니다.
- 언어별 분기는 provider 구현체가 아니라 `InferenceContext`와 model profile에서 처리합니다.
- 기존 `sendForInference(ClientStreamChunk chunk)`는 backward-compatible default path입니다.

### InferenceContext

파일:

- `sign_bridge/src/main/java/com/mj/sign/InferenceContext.java`
- `sign_bridge/src/main/java/com/mj/sign/SignLanguageResolver.java`
- `sign_bridge/src/main/java/com/mj/sign/SignLanguageProperties.java`

필드:

| 필드 | 예시 | 설명 |
| --- | --- | --- |
| `locale` | `en-US` | spoken/written output locale hint |
| `sign_language` | `asl` | normalized sign language code |
| `model_profile` | `sign-gemma` | model serving profile name |
| `protocol_version` | `mj-sign-model-v1` | BE-model protocol version |

### GpuServingClient

파일:

- `sign_bridge/src/main/java/com/mj/sign/GpuServingClient.java`
- 구현체: `HttpGpuServingClient`

역할:

- `GpuInferenceRequest`를 model backend로 전송합니다.
- `GpuInferenceResponse`를 provider가 `TranslationResult`로 변환할 수 있게 반환합니다.

### QueueInferenceTransport

파일:

- `sign_bridge/src/main/java/com/mj/sign/QueueInferenceTransport.java`
- 구현체: `InMemoryQueueInferenceTransport`, `KafkaQueueInferenceTransport`, `RabbitMqQueueInferenceTransport`, `RoutingQueueInferenceTransport`

표준 SPI:

```java
QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout)
```

규칙:

- transport는 `GpuInferenceRequest` envelope를 변경하지 않습니다.
- correlation은 `QueueInferenceTask.requestId` 기준으로 잡습니다.
- Kafka/RabbitMQ serializer/converter는 envelope field name을 유지해야 합니다.

### QueueWorkerBackend

파일:

- `sign_bridge/src/main/java/com/mj/sign/QueueWorkerBackend.java`
- 구현체: `HttpQueueWorkerBackend`

역할:

- queue worker가 받은 `GpuInferenceRequest`를 실제 model backend로 전달합니다.
- worker consumer는 결과를 `GpuInferenceResponse`로 다시 publish합니다.

## BE to Model API

표준 request/response는 [MODEL_PROTOCOL.md](./MODEL_PROTOCOL.md)를 따릅니다.

현재 HTTP endpoint 기본값:

```text
POST /api/v2/recognize
```

HTTP, Kafka, RabbitMQ, future gRPC 모두 같은 logical envelope를 유지해야 합니다.

## Extension Rules

- 새 provider를 추가할 때는 `InferenceGateway`를 구현하고 `InferenceProvider`에 enum 값을 추가합니다.
- 새 queue transport를 추가할 때는 `QueueInferenceTransport`를 구현하고 `QueueTransportKind`에 enum 값을 추가합니다.
- 새 언어 모델을 추가할 때는 provider/SPI를 수정하지 말고 `sign.language.*` mapping과 model backend profile을 추가합니다.
- model request field 이름은 transport별로 바꾸지 않습니다.
- breaking change가 필요하면 `protocol_version`을 올리고 compatibility adapter를 둡니다.
