# SignBridge Model Protocol

This document defines the stable contract between the SignBridge backend and
model-serving backends. The goal is to keep the backend SPI identical across
spoken languages and sign languages while allowing each model backend to select
the correct recognition profile.

## Design Goals

- The Flutter/WebSocket client may provide a language hint, but the backend owns
  normalization and model profile selection.
- The backend SPI remains provider-neutral for `http`, `grpc`, and `queue`.
- Model servers receive the same envelope regardless of transport.
- English-oriented input resolves to `asl` and the `sign-gemma` model profile by
  default.
- The protobuf landmark payload remains stable; language metadata is carried in
  the model request envelope.
- API/SPI boundaries are documented in [API_SPI_REFERENCE.md](./API_SPI_REFERENCE.md).
- New language model onboarding is documented in [LANGUAGE_MODEL_GUIDE.md](./LANGUAGE_MODEL_GUIDE.md).

## Client to Bridge Language Hint

`slr_input_kit` appends language context to the WebSocket URL:

```text
ws://127.0.0.1:8080/ws/sign?locale=en-US&sign_language=asl&model_profile=sign-gemma&protocol_version=signbridge-model-v1
```

The default Flutter client derives `locale` from `PlatformDispatcher.locale`.
Flutter does not expose the active keyboard layout consistently on every
platform, so host apps that can read the active keyboard/input language should
pass an explicit `SignLanguageContext`.

```dart
SlrInputWidget(
  languageContext: const SignLanguageContext(
    locale: 'en-US',
    signLanguage: 'asl',
    modelProfile: 'sign-gemma',
  ),
  onSignRecognized: (text) {},
)
```

## Backend Normalization

The backend converts query parameters into an `InferenceContext`.

Default mapping:

| Locale language | Sign language | Model profile |
| --- | --- | --- |
| `ko` | `ksl` | `sign-gemma-ko` |
| `en` | `asl` | `sign-gemma` |
| `ja` | `jsl` | `sign-gemma-ja` |
| `zh` | `csl` | `sign-gemma-zh` |
| `fr` | `lsf` | `sign-gemma-fr` |
| `de` | `dgs` | `sign-gemma-de` |
| `es` | `lse` | `sign-gemma-es` |

These values are configurable under `sign.language` in
`sign_bridge/src/main/resources/application.yml`.

## Backend SPI

Every inference provider receives the same logical inputs:

```java
TranslationResult sendForInference(ClientStreamChunk chunk, InferenceContext context)
```

This keeps the BE SPI stable across language, model, and transport changes.

SPI ownership:

| SPI | Owner | Implementations |
| --- | --- | --- |
| `InferenceGateway` | SignBridge | HTTP, gRPC, queue |
| `GpuServingClient` | SignBridge model adapter | HTTP model serving |
| `QueueInferenceTransport` | SignBridge queue adapter | in-memory, Kafka, RabbitMQ |
| `QueueWorkerBackend` | Worker process | HTTP-backed worker backend |

See [API_SPI_REFERENCE.md](./API_SPI_REFERENCE.md) for the full API/SPI map.

## Model Request Envelope

HTTP, Kafka, RabbitMQ, and future gRPC adapters should preserve this envelope.

```json
{
  "session_id": "sample-en",
  "protobuf_b64": "BASE64_PROTOBUF_CLIENT_STREAM_CHUNK",
  "frame_count": 12,
  "transport": "protobuf-b64",
  "client_schema_version": "v1",
  "protocol_version": "signbridge-model-v1",
  "locale": "en-US",
  "sign_language": "asl",
  "model_profile": "sign-gemma"
}
```

Field notes:

- `session_id`: stable stream/session identifier from the client.
- `protobuf_b64`: base64-encoded `ClientStreamChunk`.
- `frame_count`: number of landmark frames in the request.
- `transport`: payload encoding. Current value is `protobuf-b64`.
- `client_schema_version`: landmark protobuf schema version from the client.
- `protocol_version`: BE-model protocol version. `signbridge-model-v1` is canonical; legacy `mj-sign-model-v1` is accepted and normalized at the SignBridge boundary.
- `locale`: BCP-47 style locale hint such as `ko-KR` or `en-US`.
- `sign_language`: normalized sign language code such as `ksl` or `asl`.
- `model_profile`: backend-selected model profile. English defaults to
  `sign-gemma`.

## Model Response Envelope

Model backends should return:

```json
{
  "session_id": "sample-en",
  "text": "Nice to meet you.",
  "is_final": true,
  "confidence": 0.94,
  "processing_time_ms": 128,
  "model_version": "sign-gemma",
  "protocol_version": "signbridge-model-v1",
  "locale": "en-US",
  "sign_language": "asl",
  "model_profile": "sign-gemma",
  "error": null
}
```

The current bridge requires `session_id`, `text`, `is_final`, `confidence`, and
`error`. The protocol metadata fields may be echoed by the model backend for
observability and debugging.

## Sign Gemma-Compatible Model Spec

Every language-specific Sign Gemma profile should publish metadata equivalent to:

```yaml
model_profile: sign-gemma
model_family: sign-gemma
model_version: sign-gemma-asl-v1
sign_language: asl
locale: en-US
output_language: en
output_mode: sentence
input_schema: mj.sign.ClientStreamChunk
input_schema_version: v1
protocol_version: signbridge-model-v1
transport: protobuf-b64
min_frames: 8
max_frames: 24
recommended_fps: 8-12
supports_partial: false
supports_final: true
```

Compatibility requirements:

- Decode `protobuf_b64` as `mj.sign.ClientStreamChunk`.
- Respect `locale`, `sign_language`, and `model_profile`.
- Return `confidence` in the `0.0` to `1.0` range.
- Return a stable `model_version` for rollout and rollback.
- Use `error` for model-side failures instead of changing the response shape.

Detailed onboarding steps for new language models are in
[LANGUAGE_MODEL_GUIDE.md](./LANGUAGE_MODEL_GUIDE.md).

## Sign Synthesis Protocol

T2S(Text-to-Sign) and STS(Speech-to-Sign) use a separate protocol version:

```text
signbridge-synthesis-v1
```

The S2T recognition protocol remains `signbridge-model-v1`. Synthesis responses
return `SignPlan + motion` instead of sentence text only.

Synthesis request:

```json
{
  "session_id": "t2s-ko-demo",
  "source_type": "text",
  "text": "내일 병원에 가야 합니다.",
  "transcript": null,
  "audio_b64": null,
  "locale": "ko-KR",
  "sign_language": "ksl",
  "model_profile": "sign-gemma-ko",
  "output_format": "landmarks",
  "protocol_version": "signbridge-synthesis-v1"
}
```

Synthesis response:

```json
{
  "session_id": "t2s-ko-demo",
  "event_type": "synthesis_result",
  "source_type": "text",
  "text": "내일 병원에 가야 합니다.",
  "locale": "ko-KR",
  "sign_language": "ksl",
  "model_profile": "sign-gemma-ko",
  "protocol_version": "signbridge-synthesis-v1",
  "sign_plan": {
    "glosses": ["내일", "병원", "가다"],
    "non_manual_markers": ["neutral"],
    "grammar_note": "Mock KSL-compatible gloss order. Replace with a language-specific planner before production."
  },
  "motion": {
    "format": "landmark-frames",
    "fps": 12,
    "frame_count": 24,
    "frames": []
  },
  "is_final": true,
  "confidence": 0.82,
  "error": null
}
```

See [SIGN_SYNTHESIS_DESIGN.md](./SIGN_SYNTHESIS_DESIGN.md) and
[SIGN_SYNTHESIS_DESIGN_KO.md](./SIGN_SYNTHESIS_DESIGN_KO.md) for the full
T2S/STS architecture.

External SignGemma-compatible T2S providers can be attached through the
HTTP synthesis SPI:

```properties
sign.synthesis.provider=http
sign.synthesis.base-url=http://localhost:8010
sign.synthesis.synthesize-path=/api/v2/sign/synthesize
```

External ASR providers can be attached independently:

```properties
sign.synthesis.asr-provider=http
sign.synthesis.asr-base-url=http://localhost:8011
sign.synthesis.asr-path=/api/v2/speech/transcribe
```

The HTTP T2S provider should accept the synthesis request envelope above and
return the synthesis response envelope unchanged. The HTTP ASR provider should
return:

```json
{
  "session_id": "sts-en-demo",
  "transcript": "I need help tomorrow.",
  "confidence": 0.91,
  "provider": "external-asr",
  "error": null
}
```

## LLM Refinement

The LLM refinement layer uses the same `InferenceContext`.

- `ko`/`ksl` prompts refine sign keywords into natural Korean.
- `en`/`asl` prompts refine sign keywords into natural English.
- Refinement is still limited to final, high-confidence, non-system messages.

## Versioning Rules

- Add new optional fields rather than changing existing names.
- Keep `protocol_version` stable for compatible changes.
- Bump `protocol_version` when payload semantics or required fields change.
- Keep `mj-sign-model-v1` as a temporary legacy alias during the SignBridge rebrand window.
- Provider-specific transports must not rename the envelope fields.
