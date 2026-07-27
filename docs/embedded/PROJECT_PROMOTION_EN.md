# LinguaSign Product Overview

Korean version: [PROJECT_PROMOTION_KO.md](./PROJECT_PROMOTION_KO.md)

Korean social launch copy: [SOCIAL_POSTS_KO.md](../backend/SOCIAL_POSTS_KO.md)

## Project Architecture

The shared runtime architecture and SignGemma-compatible demo flow are documented in [PROJECT_ARCHITECTURE.md](../backend/PROJECT_ARCHITECTURE.md). The Spring Boot + cross-platform app demo guide is [SIGN_GEMMA_APP_DEMO.md](../backend/SIGN_GEMMA_APP_DEMO.md). Korean versions are available in [PROJECT_ARCHITECTURE_KO.md](../backend/PROJECT_ARCHITECTURE_KO.md) and [SIGN_GEMMA_APP_DEMO_KO.md](../backend/SIGN_GEMMA_APP_DEMO_KO.md).

## Demo Guide

The runnable Spring Boot + SignGemma-compatible cross-platform demo is documented
in [SIGN_GEMMA_APP_DEMO.md](../backend/SIGN_GEMMA_APP_DEMO.md). It covers the local mock
model server, Spring Boot profile, Flutter platform commands, API checks, and
troubleshooting.

## One-Line Pitch

LinguaSign is a cross-platform sign language input product powered by the
SignBridge platform. It connects sign input to mobile, web, desktop, and backend
services while leaving room for language-specific SignGemma-compatible model
profiles.

## Promotion Message

- Treat sign input as reusable cross-platform input infrastructure, not a one-off app feature.
- Separate the Flutter SignInputKit, Spring Boot SignBridge, and mock/real model-serving adapters so app and model work can evolve independently.
- Stabilize the product skeleton first: WebSocket protobuf streaming, model profile registry, readiness/metrics, OpenAPI, and T2S/STS playback UX.
- Keep an adapter/profile slot ready for official SignGemma artifacts once they become available.
- The current demo promotes the app-bridge-model contract and cross-platform UX, not final model accuracy.

## Problem

Most digital services already support keyboard and voice input, but natural sign
language input is still rare. A useful sign input experience needs much more
than a model call: camera capture, landmark extraction, realtime inference,
sentence refinement, health checks, metrics, retries, and deployment contracts
must all fit together.

SignBridge splits that problem into the SignInputKit SDK, a backend bridge,
GPU-serving adapters, queue workers, and an optional LLM refinement layer.

## Core Value

- App teams can experiment with sign input by embedding SignInputKit's `SlrInputWidget`.
- Model teams can attach language-specific sign models behind the same SignBridge connection pattern.
- Operations teams get health, readiness, metrics, retry, and DLQ patterns.
- Users receive context-aware natural-language text instead of raw recognition tokens.
- Product teams can start with Korean/KSL and extend to English/ASL or other locale/sign-language combinations with the same structure.

## Current Demo And Product Target

The current local demo uses `DemoLandmarkFrameSource` instead of a real camera
extractor to validate the SignBridge connection, protobuf streaming, idle flush,
mock GPU response, model profile discovery, and language profile echo.

The product target is to replace `DemoLandmarkFrameSource` with a real
camera/MediaPipe-style landmark extractor and attach SignGemma-compatible or
official SignGemma weights to the relevant GPU serving profile.

## Product Flow

1. A user taps the sign input icon next to a chat box, search box, or form field.
2. In production, the Flutter input widget captures hand, pose, and face landmark frames.
3. Landmark batches are sent to SignBridge over WebSocket protobuf.
4. SignBridge buffers frames per session and flushes inference on idle timeout.
5. A GPU serving backend returns sign keywords or a draft sentence.
6. The LLM refinement layer turns the raw output into a natural sentence for the
   current language context.
7. The app inserts the final text into the target input field.

## Language Model Strategy

SignBridge separates locale, sign language, and model profile. Korean service
flows use `locale=ko-KR`, `sign_language=ksl`, and
`model_profile=sign-gemma-ko` as the default profile. English expansion uses
`locale=en-US`, `sign_language=asl`, and `model_profile=sign-gemma` as a
SignGemma-compatible profile.

- The Flutter client passes platform locale or app-provided `SignLanguageContext`
  through WebSocket query parameters.
- SignBridge normalizes the values into `InferenceContext` and forwards the same
  context through HTTP, queue, and future gRPC providers.
- Spring Boot publishes supported profile routes at `/api/v2/model-profiles`,
  and the Flutter sample reflects them in the model profile selector.
- The mock GPU server exposes Korean/KSL and English/ASL metadata, supported
  landmarks, and mock responses through the `sign-gemma-ko` and `sign-gemma`
  profile registries.
- `/health` and `/ready` return profile lists, model load state, LoRA weight
  configuration, and supported landmark contracts.
- `scripts/verify_english_asl_profile.sh` validates the WebSocket-to-bridge-to-
  mock-GPU profile path end to end.

## Supported Platform Samples

| Platform | Example scenario | Sample |
| --- | --- | --- |
| Android | Mobile chat, search, public service apps | `sample/embedded/flutter_app/lib/samples/android_sample.dart` |
| iOS | iPhone accessibility input | `sample/embedded/flutter_app/lib/samples/ios_sample.dart` |
| iPad | Education, kiosk, help desk flows | `sample/embedded/flutter_app/lib/samples/ipad_sample.dart` |
| Web | Browser-based consultation or intake services | `sample/embedded/flutter_app/lib/samples/web_sample.dart` |
| Windows | Desktop kiosks and public counters | `sample/embedded/flutter_app/lib/samples/windows_sample.dart` |
| macOS/OSX | Developer demos and creator workflows | `sample/embedded/flutter_app/lib/samples/macos_sample.dart` |
| Linux | Edge GPU workstation and research rigs | `sample/embedded/flutter_app/lib/samples/linux_sample.dart` |

## Technical Differentiators

- WebSocket protobuf keeps frame payloads compact and explicit.
- Session buffering and idle-timeout flush group short gesture fragments into
  inference windows.
- `InferenceContext` standardizes locale, sign language, and model profile so new
  language models can be added through the same connection pattern.
- The mock GPU server separates `sign-gemma-ko` and `sign-gemma` profiles so real
  model IDs, checkpoints, or LoRA paths can be introduced per profile later.
- HTTP is ready for quick mock or real serving checks, while Kafka/RabbitMQ queue
  paths are ready for asynchronous worker expansion.
- Readiness checks distinguish "the app process is up" from "the model backend
  is ready."
- The phase 1 T2S/STS contract and `SpeechToTextAdapter`, `SignPlanner`,
  `SignMotionGenerator`, and `SignSynthesisProvider` SPI return `SignPlan +
  landmark motion`, allowing text/speech-to-sign playback validation before a
  production generator is ready.

## Developer Notes

- API/SPI references and the language model onboarding guide are separated so new
  sign models can be attached without reshaping provider or transport code.
- Queue workers are structured so request consumption and result publication can
  be validated in local integration flows.
- The HTTP compose stack has a single verification script for readiness, profile
  discovery, T2S, and WebSocket protobuf streaming.
- OpenAPI examples cover profile discovery, synthesis, readiness, health, and
  metrics so app and backend teams can share the same contract.
- Keeping the BE-model envelope and adapter layer stable lets the app and
  SignBridge API remain steady even when model weights or serving backends change.
- T2S/STS architecture is documented separately in `SIGN_SYNTHESIS_DESIGN.md`
  and `SIGN_SYNTHESIS_DESIGN_KO.md`.

## Landmark Contract

The exact official SignGemma input landmark specification has not been verified
from a public model card yet. During the current mock/profile contract validation
stage, SignBridge uses a MediaPipe-style protobuf contract at the adapter
boundary.

| Input field | Current support | Purpose |
| --- | --- | --- |
| `left_hand` | 21 3D hand landmarks recommended | Left-hand shape and movement |
| `right_hand` | 21 3D hand landmarks recommended | Right-hand shape and movement |
| `pose` | Selected upper-body pose landmarks | Shoulder, arm, and body orientation context |
| `face_contour` | Selected mouth, jaw, and face landmarks | Non-manual markers and facial context |

When official SignGemma weights and input schemas become available, SignBridge can
adapt this contract inside the model adapter while preserving the backend-model
envelope. Until then, the `sign-gemma` profile is treated as a
SignGemma-compatible serving contract validation profile.

## Demo

Run the mock GPU server:

```bash
cd sample/backend/model_server
../scripts/setup_mock_venv.sh
.venv/bin/python main.py
```

Run the Spring bridge:

```bash
cd sign/backend/bridge
./gradlew bootRun
```

Run the Flutter sample gallery:

```bash
cd sample/embedded/flutter_app
flutter run
```

For a quick HTTP integrated stack check:

```bash
./scripts/setup_mock_venv.sh
MOCK_PORT=18000 ./scripts/verify_spring_openapi_smoke.sh
```

In an environment with Docker installed, the HTTP/Kafka/RabbitMQ integrated
stacks can be verified as well.

Validate Kafka or RabbitMQ worker flows:

```bash
./scripts/verify_kafka_stack.sh
./scripts/verify_rabbitmq_stack.sh
```

## Use Cases

- Public-service kiosks.
- Hospital and pharmacy reception.
- Sign language learning apps.
- Accessibility input for messengers and social apps.
- Assisted input for contact centers.
- Local GPU research and pilot environments.

## Current Maturity And Gaps

SignBridge currently includes the bridge, provider routing, mock GPU server,
Korean/KSL `sign-gemma-ko` and English/ASL `sign-gemma` profile registries,
profile-aware health/readiness, queue worker contract, broker serializer/converter
settings, a platform sample gallery, a mock T2S/STS synthesis contract, ASR/T2S
HTTP provider extension points, and playback stubs.

To reach production readiness, the project still needs a real landmark
extractor, official SignGemma or SignGemma-compatible ASL model serving,
real ASR/T2S model serving, authenticated WSS endpoints, privacy policy work,
operational dashboards, and dataset-backed evaluation.

## Next Priorities

1. Replace `DemoLandmarkFrameSource` with a real camera landmark extractor.
2. Connect official SignGemma or SignGemma-compatible ASL weights to the
   `sign-gemma` profile.
3. Attach an STS ASR adapter and a real T2S sign generation provider behind
   `signbridge-synthesis-v1`.
4. Attach a real GPU serving backend behind HTTP or queue workers.
5. Harden Web, iOS, and Android camera permission and lifecycle handling.
6. Add TLS, authentication, session policy, and metrics dashboards.
7. Document datasets, evaluation metrics, and user feedback loops.

## Message

LinguaSign is not just a sign-recognition model demo. Its goal is to make the
whole path usable: sign input on real devices, stable SignBridge routing,
swappable model serving, and natural text output that applications can adopt.
