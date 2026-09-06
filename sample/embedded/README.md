# Embedded samples

`flutter_app/lib/samples/dual_mode_recognition_sample.dart`는 동일한
`SlrInputWidget`과 landmark source를 사용하면서 다음 두 실행 모드를 전환하는
예제다.

- `EmbeddedSignRecognitionEngine`: 네트워크 없이 앱에 주입한 모델 delegate 실행
- `BackendSignRecognitionEngine`: SignBridge WebSocket Protocol v2 사용

샘플의 내장 delegate는 구조 검증용 결정적 구현이다. 제품에서는 해당 delegate를
TFLite, Core ML 또는 MediaPipe Tasks 모델 호출로 교체한다.

- `flutter_app/`: `sign/embedded`를 path dependency로 사용하는 크로스플랫폼 예제

```sh
cd sample/embedded/flutter_app
flutter pub get
flutter test
flutter run -t lib/dual_mode_main.dart
```

내장 모드는 서버 없이 실행된다. 서버 연결 모드는 기본적으로
`ws://localhost:8080/ws/sign`에 연결하므로 SignBridge를 먼저 실행한다.
실기기에서는 `SignGemmaClient(url: ...)`의 주소를 접근 가능한 서버 주소로 설정한다.
