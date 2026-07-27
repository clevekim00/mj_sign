# Backend sign recognition

클라이언트의 landmark 스트림을 받아 AI 모델 서버로 전달하고 텍스트 결과를
반환하는 백엔드 기능입니다.

- `bridge/`: Spring Boot WebSocket/API 브리지
- 공통 protobuf 원본: `../common/schema/landmark.proto`
- 모델 서버 예제: `../../sample/backend/model_server/`

테스트:

```sh
cd sign/backend/bridge
./gradlew test
```

