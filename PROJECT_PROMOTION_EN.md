# MJ Sign Project Overview

Korean version: [PROJECT_PROMOTION_KO.md](./PROJECT_PROMOTION_KO.md)

## One-Line Pitch

MJ Sign is a cross-platform sign language recognition bridge that connects sign
input to mobile, web, desktop, and backend services while leaving room for
language-specific Sign Gemma-compatible model profiles.

## Problem

Most digital services already support keyboard and voice input, but natural sign
language input is still rare. A useful sign input experience needs much more
than a model call: camera capture, landmark extraction, realtime inference,
sentence refinement, health checks, metrics, retries, and deployment contracts
must all fit together.

MJ Sign splits that problem into a reusable input widget, a backend bridge,
GPU-serving adapters, queue workers, and an optional LLM refinement layer.

## Core Value

- App teams can experiment with sign input by embedding `SlrInputWidget`.
- Model teams can swap GPU serving behind HTTP, gRPC, or queue providers.
- Operations teams get health, readiness, metrics, retry, and DLQ patterns.
- Users receive refined natural-language text instead of raw recognition tokens.
- Product teams can separate locale, sign language, and model profile, such as
  Korean/KSL or English/ASL, while keeping the backend SPI stable.

## Product Flow

1. A user taps the sign input icon next to a chat box, search box, or form field.
2. The Flutter input widget captures hand, pose, and face landmark frames.
3. Landmark batches are sent to Sign Bridge over WebSocket protobuf.
4. Sign Bridge buffers frames per session and flushes inference on idle timeout.
5. A GPU serving backend returns sign keywords or a draft sentence.
6. The LLM refinement layer turns the raw output into a natural sentence for the
   current language context.
7. The app inserts the final text into the target input field.

## English/ASL Sign Gemma Profile

MJ Sign standardizes English sign input around
`locale=en-US`, `sign_language=asl`, and `model_profile=sign-gemma`.

- The Flutter client passes platform locale or app-provided `SignLanguageContext`
  through WebSocket query parameters.
- Sign Bridge normalizes the values into `InferenceContext` and forwards the same
  context through HTTP, queue, and future gRPC providers.
- The mock GPU server exposes English/ASL metadata, supported landmarks, and mock
  responses through the `sign-gemma` profile registry.
- `/health` and `/ready` return profile lists, model load state, LoRA weight
  configuration, and supported landmark contracts.
- `scripts/verify_english_asl_profile.sh` validates the WebSocket-to-bridge-to-
  mock-GPU profile path end to end.

## Supported Platform Samples

| Platform | Example scenario | Sample |
| --- | --- | --- |
| Android | Mobile chat, search, public service apps | `slr_input_kit/example/lib/samples/android_sample.dart` |
| iOS | iPhone accessibility input | `slr_input_kit/example/lib/samples/ios_sample.dart` |
| iPad | Education, kiosk, help desk flows | `slr_input_kit/example/lib/samples/ipad_sample.dart` |
| Web | Browser-based consultation or intake services | `slr_input_kit/example/lib/samples/web_sample.dart` |
| Windows | Desktop kiosks and public counters | `slr_input_kit/example/lib/samples/windows_sample.dart` |
| macOS/OSX | Developer demos and creator workflows | `slr_input_kit/example/lib/samples/macos_sample.dart` |
| Linux | Edge GPU workstation and research rigs | `slr_input_kit/example/lib/samples/linux_sample.dart` |

## Technical Differentiators

- WebSocket protobuf keeps frame payloads compact and explicit.
- Session buffering and idle-timeout flush group short gesture fragments into
  inference windows.
- `InferenceContext` standardizes locale, sign language, and model profile while
  keeping the backend SPI language independent.
- English input is routed to ASL and the `sign-gemma` profile by default.
- The mock GPU server separates `sign-gemma` and `sign-gemma-ko` profiles so real
  model IDs, checkpoints, or LoRA paths can be introduced per profile later.
- HTTP is ready for quick mock or real serving integration.
- Queue provider support is ready for Kafka/RabbitMQ-style asynchronous workers.
- Worker consumers cover request consumption and result publication paths for
  local integration testing.
- Readiness checks reflect provider health, making it easier to distinguish
  "app process is up" from "model backend is ready."

## Landmark Contract

The exact official SignGemma input landmark specification has not been verified
from a public model card yet. MJ Sign therefore uses a MediaPipe-style protobuf
contract at the adapter boundary.

| Input field | Current support | Purpose |
| --- | --- | --- |
| `left_hand` | 21 3D hand landmarks recommended | Left-hand shape and movement |
| `right_hand` | 21 3D hand landmarks recommended | Right-hand shape and movement |
| `pose` | Selected upper-body pose landmarks | Shoulder, arm, and body orientation context |
| `face_contour` | Selected mouth, jaw, and face landmarks | Non-manual markers and facial context |

When official SignGemma weights and input schemas become available, MJ Sign can
adapt this contract inside the model adapter while preserving the backend-model
envelope.

## Demo

Run the mock GPU server:

```bash
cd sign_gemma_mock
python main.py
```

Run the Spring bridge:

```bash
cd sign_bridge
./gradlew bootRun
```

Run the Flutter sample gallery:

```bash
cd slr_input_kit/example
flutter run
```

Validate profile and broker flows:

```bash
./scripts/verify_english_asl_profile.sh
./scripts/verify_kafka_stack.sh
./scripts/verify_rabbitmq_stack.sh
```

For a quick English/ASL HTTP-stack check, use the local stack described by
`docker-compose.stack.http.yml`.

## Use Cases

- Public-service kiosks.
- Hospital and pharmacy reception.
- Sign language learning apps.
- Accessibility input for messengers and social apps.
- Assisted input for contact centers.
- Local GPU research and pilot environments.

## Current Maturity And Gaps

MJ Sign currently includes the bridge, provider routing, mock GPU server,
English/ASL `sign-gemma` profile registry, profile-aware health/readiness,
queue worker contract, broker serializer/converter settings, and a platform
sample gallery.

To reach production readiness, the project still needs a real landmark
extractor, official SignGemma or SignGemma-compatible ASL model serving,
authenticated WSS endpoints, privacy policy work, operational dashboards, and
dataset-backed evaluation.

## Next Priorities

1. Replace `DemoLandmarkFrameSource` with a real camera landmark extractor.
2. Connect official SignGemma or SignGemma-compatible ASL weights to the
   `sign-gemma` profile.
3. Attach a real GPU serving backend behind HTTP or queue workers.
4. Harden Web, iOS, and Android camera permission and lifecycle handling.
5. Add TLS, authentication, session policy, and metrics dashboards.
6. Document datasets, evaluation metrics, and user feedback loops.

## Message

MJ Sign is not just a sign-recognition model demo. Its goal is to make the whole
path usable: sign input on real devices, stable backend routing, swappable model
serving, and natural text output that applications can adopt.
