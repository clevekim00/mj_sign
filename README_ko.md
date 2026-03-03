# 수어 인식 AI 배포 키트 (SLR Input Kit)

본 프로젝트는 Flutter와 C++ FFI를 결합하여 모바일, 데스크톱, 웹 등 멀티 플랫폼에서 실시간 수어 인식을 가능하게 하는 고성능 입력 도구입니다.

## 시스템 아키텍처 (System Architecture)

이 시스템은 고성능 AI 추론과 유연한 크로스 플랫폼 UI를 결합하기 위해 3단계 계층 구조로 설계되었습니다.

### 1. 프론트엔드 레이어 (Flutter/Dart)
- **카메라 제어:** `flutter_webrtc`를 사용하여 저지연 카메라 프레임을 캡처합니다.
- **UI 렌더링:** Flutter의 고성능 엔진을 통해 인식된 수어를 화면에 실시간으로 오버레이합니다.
- **상태 관리:** 사용자 상호작용 및 인식 결과 시트/텍스트 처리를 담당합니다.

### 2. 브릿지 레이어 (Dart FFI)
- **네이티브 호출:** `dart:ffi`를 사용하여 Dart 언어에서 직접 C++ 메모리 영역에 접근합니다.
- **바인딩 자동화:** `ffigen`을 통해 C 헤더 파일로부터 Dart 클래스를 자동으로 생성하여 타입 안정성을 보장합니다.

### 3. 네이티브 추론 레이어 (C/C++)
- **랜드마크 추출:** MediaPipe Holistic C++ API를 사용하여 영상에서 543개의 주요 관절 포인트를 추출합니다.
- **AI 추론:** SignFormer-GCN (TensorFlow Lite) 모델을 8비트 양자화하여 저사양 기기에서도 실시간 번역이 가능하도록 실행합니다.
- **최적화:** CPU/GPU 가속을 최대한 활용하여 초당 30~60프레임의 실시간 처리를 수행합니다.

## 프로젝트 구조

- `slr_input_kit/`: 핵심 Flutter FFI 플러그인 소스
  - `src/`: C++ 네이티브 소스 코드 및 CMake 설정
  - `lib/`: Dart API 및 FFI 바인딩 코드
- `example/`: 플러그인 기능을 시연하는 데모 애플리케이션

## 시작하기

1. Flutter SDK를 설치합니다.
2. `slr_input_kit` 폴더에서 의존성을 설치합니다: `flutter pub get`
3. 네이티브 바인딩을 생성합니다: `dart run ffigen --config ffigen.yaml`
4. 예제 앱을 실행합니다: `cd example && flutter run`

## 기술 스택
- **Framework:** Flutter (Dart)
- **Native Bridge:** Dart FFI
- **AI Engine:** MediaPipe Holistic, TensorFlow Lite (SignFormer-GCN)
- **Streaming:** WebRTC
