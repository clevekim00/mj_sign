# SLR Input Kit (Sign Language Recognition)

Sign Language AI Implementation & Deployment Guide.

- [🇰🇷 한국어 설명 (README_ko.md)](./README_ko.md)
- [🇺🇸 English Description (README_en.md)](./README_en.md)

---

## Architecture Overview / 아키텍처 개요

This system uses **Flutter + Dart FFI + C++ (MediaPipe & TFLite)** to provide high-performance, real-time sign language recognition across platforms.

본 시스템은 **Flutter + Dart FFI + C++ (MediaPipe & TFLite)** 기술 스택을 사용하여 다양한 플랫폼에서 고성능 실시간 수어 인식 기능을 제공합니다.

1. **Frontend**: Flutter WebRTC (Camera)
2. **Bridge**: Dart FFI (Native Binding)
3. **Engine**: MediaPipe Holistic & SignFormer-GCN (TFLite)
