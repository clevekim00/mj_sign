# SLR Input Kit (Sign Language Recognition)

This project is a high-performance sign language recognition input tool that combines Flutter and C++ FFI to enable real-time inference on multiple platforms including mobile, desktop, and web.

## System Architecture

The system is designed with a 3-layer architecture to balance high-performance AI inference with a flexible cross-platform UI.

### 1. Frontend Layer (Flutter/Dart)
- **Camera Control:** Uses `flutter_webrtc` for low-latency camera frame capture.
- **UI Rendering:** Real-time overlay of recognized sign language results using Flutter's high-performance engine.
- **State Management:** Handles user interactions and processing of recognition results/text.

### 2. Bridge Layer (Dart FFI)
- **Native Invocation:** Uses `dart:ffi` to directly access C++ memory from Dart.
- **Binding Automation:** Automatically generates Dart classes from C headers via `ffigen` to ensure type safety.

### 3. Native Inference Layer (C/C++)
- **Landmark Extraction:** Extracts 543 key points using the MediaPipe Holistic C++ API.
- **AI Inference:** Executes the SignFormer-GCN (TensorFlow Lite) model with 8-bit quantization for real-time translation even on low-end devices.
- **Optimization:** Maximizes CPU/GPU acceleration for real-time processing at 30~60 FPS.

## Project Structure

- `slr_input_kit/`: Core Flutter FFI plugin source
  - `src/`: C++ native source code and CMake configuration
  - `lib/`: Dart API and FFI binding code
- `example/`: Demo application demonstrating plugin features

## Getting Started

1. Install the Flutter SDK.
2. Install dependencies in the `slr_input_kit` folder: `flutter pub get`
3. Generate native bindings: `dart run ffigen --config ffigen.yaml`
4. Run the example app: `cd example && flutter run`

## Tech Stack
- **Framework:** Flutter (Dart)
- **Native Bridge:** Dart FFI
- **AI Engine:** MediaPipe Holistic, TensorFlow Lite (SignFormer-GCN)
- **Streaming:** WebRTC
