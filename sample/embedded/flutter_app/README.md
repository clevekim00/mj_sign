# LinguaSign SignInputKit Samples

This example app is a platform sample gallery for the SignInputKit SDK
(`slr_input_kit`). It shows how the same sign-language input widget can be
prepared for Android, iPhone, iPad, Web, Windows, macOS/OSX, and Linux while
keeping the SignBridge backend contract identical.

## Project Architecture

The shared runtime architecture and SignGemma-compatible demo flow are documented in [PROJECT_ARCHITECTURE.md](../../../docs/backend/PROJECT_ARCHITECTURE.md). The Spring Boot + cross-platform app demo guide is [SIGN_GEMMA_APP_DEMO.md](../../../docs/backend/SIGN_GEMMA_APP_DEMO.md). Korean versions are available in [PROJECT_ARCHITECTURE_KO.md](../../../docs/backend/PROJECT_ARCHITECTURE_KO.md) and [SIGN_GEMMA_APP_DEMO_KO.md](../../../docs/backend/SIGN_GEMMA_APP_DEMO_KO.md).

## What the Sample Demonstrates

- `SlrInputWidget` connected to the Spring SignBridge backend over WebSocket.
- Platform-specific bridge URLs and execution commands.
- A deterministic demo landmark source that exercises the protobuf streaming
  contract without requiring a real camera extractor.
- `SignOutputWidget` and `SignSynthesisResult.fromJson` for phase 1
  Text/Speech-to-Sign landmark playback.
- Production notes for replacing the demo source with camera or MediaPipe style
  landmark extraction.
- Responsive UI behavior for phone, tablet, desktop, and browser layouts.

## Start the Local Backend

Run the mock GPU server and Spring bridge from the repository root:

```bash
cd sample/backend/model_server
python main.py
```

```bash
cd sign/backend/bridge
./gradlew bootRun
```

For queue-backed local validation, use the integrated Kafka or RabbitMQ stacks
documented in the root README.

For the HTTP mock stack, the repository root includes a single smoke script:

```bash
./scripts/verify_docker_http_stack.sh
```

It verifies Docker startup, Spring readiness, model profile discovery,
Text-to-Sign, and WebSocket protobuf streaming.

## Run by Platform

Android emulator:

```bash
flutter run -d android
```

iPhone simulator:

```bash
flutter run -d ios
```

iPad device or simulator:

```bash
flutter run -d <ipad-device-id>
```

Web:

```bash
flutter run -d chrome
```

Windows:

```bash
flutter run -d windows
```

macOS / OSX:

```bash
flutter run -d macos
```

Linux:

```bash
flutter run -d linux
```

## Bridge URL Tips

- Android emulator should use `ws://10.0.2.2:8080/ws/sign`.
- iOS Simulator, desktop, and local Web can use `ws://127.0.0.1:8080/ws/sign`
  or `ws://localhost:8080/ws/sign`.
- Physical Android, iPhone, and iPad devices should use the host machine LAN IP.
- Production deployments should use WSS and short-lived session identifiers.

## Language Context

`SignGemmaClient` derives a default `SignLanguageContext` from the Flutter
platform locale and appends it to the WebSocket URL. English locales become
`locale=en-US`, `sign_language=asl`, and `model_profile=sign-gemma` by default.

The sample also includes a `Model profile` selector. It calls
`GET /api/v2/model-profiles` through `SignModelProfileHttpClient`, then applies
the selected `SignModelProfile` to WebSocket, Text-to-Sign, and Speech-to-Sign
requests. If the bridge is not running, the selector keeps the bundled demo
profiles so the UI remains usable. Discovery calls use a short timeout so
emulator-only addresses such as `10.0.2.2` do not leave the Web UI in a long
loading state.

For a Web smoke test:

1. Run Spring Boot on `localhost:8080`.
2. Run `flutter run -d chrome --web-port 5173`.
3. Select the `Web` platform sample or set the bridge URL to
   `ws://127.0.0.1:8080/ws/sign`.
4. Confirm `Model profile` shows `en-US · asl · sign-gemma`.
5. Press `Text to Sign` and confirm the motion preview renders frames.

The backend API/SPI and model protocol are documented in the repository root:

- `API_SPI_REFERENCE.md`
- `MODEL_PROTOCOL.md`
- `LANGUAGE_MODEL_GUIDE.md`
- `SIGN_SYNTHESIS_DESIGN.md`
- `SIGN_SYNTHESIS_DESIGN_KO.md`

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the
bridge is running, with example payloads for profile discovery, readiness, T2S,
and STS.

If your host app can read the active keyboard or input-method language, pass it
explicitly:

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

## Replacing the Demo Landmark Source

The gallery uses `DemoLandmarkFrameSource` so every platform can exercise the
bridge immediately. For production, replace it with:

```dart
CameraLandmarkFrameSource(
  extractor: (image, camera) async {
    // Run your hand, pose, and face landmark model here.
    // Return List<LandmarkFrame> that follows sign/common/schema/landmark.proto.
    return <LandmarkFrame>[];
  },
)
```

Keep frame batches small, throttle to a stable FPS, and let the bridge idle
flush collect enough context before final inference.

## MediaPipe / ML Kit Extractor Skeleton

The SDK also exposes `MediaPipeLandmarkFrameSource`, a production-facing wrapper
around `CameraLandmarkFrameSource`. It fixes the camera lifecycle, throttling,
batch limit, and protobuf output contract while letting the host app provide the
actual MediaPipe, ML Kit, native, or WASM extraction call.
The wrapper now applies front-camera x-coordinate mirroring and the configured
hands/pose/face inclusion flags before frames are sent to SignBridge.

```dart
final source = MediaPipeLandmarkFrameSource(
  config: const MediaPipeLandmarkExtractionConfig(
    includeHands: true,
    includePose: true,
    includeFaceContour: true,
    maxFramesPerBatch: 3,
  ),
  extractor: (image, camera, config) async {
    // 1. Convert CameraImage planes into the runtime input format.
    // 2. Run MediaPipe Holistic, MediaPipe Hands + Pose, ML Kit, or a native model.
    // 3. Normalize coordinates into 0.0..1.0 x/y/z values.
    // 4. Return LandmarkFrame values that match sign/common/schema/landmark.proto.
    return const MediaPipeLandmarkExtraction(frames: <LandmarkFrame>[]);
  },
);

SlrInputWidget(
  landmarkFrameSource: source,
  disposeLandmarkFrameSource: true,
  onSignRecognized: (text) {},
)
```

Extractor implementations should preserve these rules:

- Emit at least one `LandmarkFrame` per accepted camera image.
- Keep batches at or below the bridge `sign.window.max-buffered-frames` limit.
- Prefer 8-12 FPS until latency and battery use are measured.
- Fill `timestamp_ms` with camera or wall-clock milliseconds.
- Normalize x/y coordinates to the visible image coordinate system.
- Separate left and right hands when the underlying model provides handedness.
- Use `pose` and `face_contour` only when configured and available.

## Text/Speech-to-Sign Playback

The SDK now exports a lightweight playback stub for synthesis results:

```dart
final result = SignSynthesisResult.fromJson(responseJson);

SignOutputWidget(
  frames: result.frames,
  placeholder: 'T2S/STS motion을 기다리는 중입니다.',
)
```

The current backend returns mock `SignPlan + landmark motion` through
`POST /api/v2/sign/synthesize` and `POST /api/v2/speech/sign`. A production app
can keep this widget while replacing the backend mock with an ASR adapter,
language-specific sign planner, and real motion generator.
