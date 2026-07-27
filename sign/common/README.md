# Common contracts

내장형과 백엔드형 수어 인식이 함께 사용하는 안정적인 계약입니다.

- `schema/landmark.proto`: canonical landmark/stream/result protobuf
- `eval/fixtures/`: 모델 및 프로토콜 평가 fixture

스키마 변경 후 저장소 루트에서 다음 명령으로 런타임별 생성 코드를 갱신합니다.

```sh
./scripts/regenerate_protobuf.sh
```

