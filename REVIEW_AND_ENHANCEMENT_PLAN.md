# SignBridge Review And Enhancement Plan

## Review Summary

The current project direction is valid for a V2 sign-recognition bridge, but the implementation is still closer to an architecture prototype than a production-ready sign input feature.

Strengths:

- clear separation between client, bridge, provider routing, and worker transport
- solid buffering and idle-flush foundation for streamed sign input
- operational endpoints and local integration tooling already exist
- queue transport seams are prepared for Kafka and RabbitMQ expansion

Gaps:

- the Flutter client still streams mock landmarks instead of real camera or MediaPipe output
- UI connection state is more optimistic than the actual WebSocket state
- LLM refinement is applied too broadly and can rewrite status or error messages
- readiness was originally HTTP-only and did not reflect the active queue provider path

## Priority Fixes

### P1. Make the client honest and safe

- do not default to mock landmark auto-streaming
- reflect connecting, connected, disconnected, and error states explicitly
- avoid calling the recognition callback for transient status text
- present the widget as a bridge client until a real landmark source is connected

### P1. Restrict LLM refinement to true final recognition output

- only refine final inference results
- require a non-trivial confidence threshold
- never send buffering, busy, timeout, or error strings to the LLM layer
- fall back to the raw GPU result if refinement fails

### P1. Make readiness provider-aware

- `http` provider should probe the configured HTTP serving backend
- `queue` provider should reflect the selected transport and whether the required broker adapter exists
- queue readiness should also verify the downstream HTTP GPU dependency used by the current worker backend
- `grpc` should remain explicitly marked as not ready until a real gRPC path exists

## Next Product-Grade Work

These items still remain after the current patch set.

### P2. Replace mock landmarks with a real landmark source

- connect camera capture or MediaPipe extraction on the client
- batch real frames into `ClientStreamChunk`
- expose start, stop, mute, and reconnect controls in the UI

Status: implemented as an integration contract in `feature/enhance`.

The Flutter widget now accepts `Stream<List<LandmarkFrame>>` through `landmarkFrameStream`, so a camera or MediaPipe adapter can feed real landmark batches into the existing WebSocket bridge without using mock frame generation.

The package also includes `CameraLandmarkFrameSource`, which owns camera image streaming and delegates landmark extraction to a supplied `LandmarkFrameBatchExtractor`. This keeps camera lifecycle, throttling, and backpressure handling in the package while allowing the actual MediaPipe/native extractor to remain platform-specific.

Example:

```dart
final source = CameraLandmarkFrameSource(
  extractor: (image, camera) async {
    return mediaPipeExtractor.extract(image, camera);
  },
);

SlrInputWidget(
  landmarkFrameSource: source,
  disposeLandmarkFrameSource: true,
  onSignRecognized: (text) {
    // Use final recognized text here.
  },
)
```

The remaining implementation step is the platform-specific MediaPipe/native extractor that converts each `CameraImage` into one or more `LandmarkFrame` values.

### P2. Separate status payloads from recognition payloads

Status: implemented in `feature/enhance`.

The WebSocket response contract now sends explicit JSON events:

- `event_type=status`
- `event_type=result`
- `event_type=error`

Status events use `status` and `status_text`, recognition events use `result_text`, and errors use `error_code` plus `status_text`. The client parses these through `SignGemmaBridgeEvent`, while the legacy `onTranslation` callback remains available for result events.

### P3. Strengthen broker health verification

- add actual Kafka and RabbitMQ connectivity probes
- validate request-topic publish and result-topic consumption at readiness time
- expose broker health separately from downstream GPU health in ops endpoints

## Patch Scope In This Branch

The `feature/enhance` branch implements the highest-priority safe fixes:

- review document added
- Flutter client default behavior made less misleading
- WebSocket connection state handling improved
- camera-backed landmark frame source contract added
- `SlrInputWidget` can now start a managed `LandmarkFrameSource`
- LLM refinement narrowed to final high-confidence recognition output
- readiness updated to respect the selected provider and queue transport
- regression tests updated for the new rules
