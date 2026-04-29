# SignGemma 조사 노트

마지막 업데이트: 2026-04-27

English version: [SIGN_GEMMA_RESEARCH.md](./SIGN_GEMMA_RESEARCH.md)

## Project Architecture / 프로젝트 아키텍처

공통 런타임 구조와 SignGemma-compatible 예제 흐름은 [PROJECT_ARCHITECTURE_KO.md](PROJECT_ARCHITECTURE_KO.md)에 정리되어 있습니다. Spring Boot + cross-platform app 실행 가이드는 [SIGN_GEMMA_APP_DEMO_KO.md](SIGN_GEMMA_APP_DEMO_KO.md)를 참고하세요.

## 요약

현재 공개적으로 검증 가능한 SignGemma 정보는 아직 제한적입니다. 가장
강한 공개 신호는 Google I/O 2025 전후로 보도된 Google DeepMind 발표이며,
SignGemma는 수어를 텍스트로 변환하기 위한 Gemma 계열 공개 모델로 소개됐고
초기 초점은 ASL에서 영어로의 변환에 있는 것으로 보도됐습니다.

이번 검토 기준으로 Google의 공개 Gemma 모델 페이지에서 공식 SignGemma
모델 카드, 다운로드 가능한 weight 페이지, 정확한 landmark/input tensor
명세는 확인하지 못했습니다. 따라서 SignBridge는 공식 weight와 spec이 공개되기
전까지 SignGemma를 LinguaSign의 profile 호환 서빙 대상으로 다루고, BE와 모델
사이의 adapter 계약을 안정적으로 유지하는 전략을 채택합니다.

## 공개 보도로 확인되는 내용

- Google DeepMind의 Gemma 페이지는 Gemma를 공개 모델 family로 소개하고
  공식 Gemma variant와 integration을 나열하지만, 이번에 확인한 공개 페이지는
  SignGemma 모델 카드나 다운로드 가능한 SignGemma artifact를 제공하지
  않았습니다.
- Gadgets360은 Google DeepMind X 발표를 보도하며 SignGemma를 수어를
  음성/문자 텍스트로 변환하기 위한 Gemma 계열 공개 모델로 설명했고,
  ASL에서 영어로의 성능이 가장 좋다고 전했습니다.
- Slator도 SignGemma가 I/O 2025에서 발표됐고, 실시간 sign-language-to-text
  번역과 on-device 사용을 목표로 하며, 추후 공개될 예정이라고 보도했습니다.
- Google의 Keras Gemma 문서는 현재 mock engine 방향과 관련이 있습니다.
  KerasNLP 기반 Gemma 사용과 LoRA fine-tuning workflow를 설명하기 때문입니다.
- Google의 Gemma 3 개발자 가이드는 Gemma 계열의 multimodal 방향성을 이해하는
  배경 자료로 유용하지만, SignGemma 전용 model card는 아닙니다.

출처:

- [Google DeepMind Gemma page](https://deepmind.google/models/gemma/)
- [Gadgets360 SignGemma report](https://www.gadgets360.com/ai/news/google-signgemma-ai-model-translate-sign-language-to-spoken-text-unveiled-8537400)
- [Slator SignGemma report](https://slator.com/google-invites-feedback-for-signgemma-a-new-ai-sign-language-translation-model/)
- [Google Developers Blog: Gemma models in Keras](https://developers.googleblog.com/en/introducing-gemma-models-in-keras/)
- [Google Developers Blog: Gemma 3 developer guide](https://developers.googleblog.com/introducing-gemma3/)

## 아직 공식 확인이 필요한 항목

다음 항목은 공식 model card 또는 repository가 공개되기 전까지 SignGemma의
확정 사실로 취급하지 않는 편이 안전합니다.

- 정확한 model size와 architecture.
- checkpoint 이름과 다운로드 URL.
- 필요한 video frame 해상도.
- 공식 inference 입력이 raw video, image sequence, landmarks, hybrid
  representation 중 무엇인지.
- 공식 landmark schema.
- latency, accuracy, benchmark 수치.
- ASL/English 외에 공식 지원되는 sign language와 spoken/written language 목록.

## Landmark 지원 범위

### 공식 SignGemma 기준

공식 SignGemma landmark schema는 아직 확인하지 못했습니다. 공개 보도는
sign-language translation과 visual understanding을 언급하지만, SignGemma가
MediaPipe landmark, raw video, image sequence, 또는 다른 vision representation
중 무엇을 직접 소비하는지는 정의하지 않습니다.

### SignBridge 프로젝트 기준 계약

SignBridge는 현재 MediaPipe-style protobuf landmark contract를 표준 입력으로
사용합니다.

```proto
message LandmarkFrame {
  int64 timestamp_ms = 1;
  repeated Point3D left_hand = 2;
  repeated Point3D right_hand = 3;
  repeated Point3D pose = 4;
  repeated Point3D face_contour = 5;
}
```

프로젝트에서 지원하는 landmark 범위:

| 필드 | 기대 형태 | 목적 |
| --- | --- | --- |
| `left_hand` | 최대 21개 3D point | MediaPipe-style 왼손 landmark |
| `right_hand` | 최대 21개 3D point | MediaPipe-style 오른손 landmark |
| `pose` | 선택된 상체 3D point | 어깨, 팔꿈치, 몸 방향 등 문맥 |
| `face_contour` | 선택된 입/턱/얼굴 3D point | 비수지 신호, 표정, mouth gesture 문맥 |

MediaPipe 참고 자료:

- [Google AI Edge Holistic Landmarker](https://ai.google.dev/edge/mediapipe/solutions/vision/holistic_landmarker)는 pose, face, 양손 21개 landmark를 출력하는 holistic landmarker를 설명합니다.
- [MediaPipe Hands reference](https://chuoling.github.io/mediapipe/solutions/hands.html)는 감지된 손마다 21개 hand landmark와 x/y/z 좌표를 출력한다고 설명합니다.

## 이 저장소의 구현 판단

공식 SignGemma weight와 spec이 공개되기 전까지 SignBridge는
`model_profile=sign-gemma`를 English/ASL 호환 serving profile로 취급합니다.

| 항목 | 값 |
| --- | --- |
| `locale` | `en-US` |
| `sign_language` | `asl` |
| `model_profile` | `sign-gemma` |
| `protocol_version` | `signbridge-model-v1` (`mj-sign-model-v1`은 legacy alias) |
| 입력 transport | `protobuf-b64` |
| 입력 schema | `mj.sign.ClientStreamChunk` |
| landmark schema | `left_hand`, `right_hand`, `pose`, `face_contour` |

이 구조를 사용하면 실제 SignGemma checkpoint가 준비되기 전에도 bridge, queue,
serializer/converter, health/readiness, client language routing을 검증할 수
있고, 이후 실제 visual SignGemma 모델로 adapter만 교체할 수 있습니다.

## 현재 코드 지원 현황

현재 구현된 항목:

- `sign_gemma_mock/profile_registry.py`의 English/ASL profile registry.
- `sign_gemma_mock/sign_gemma_model.py`의 profile별 Keras/Gemma engine cache.
- `sign_gemma_mock/main.py`의 profile-aware `/health`, `/ready` endpoint.
- `scripts/verify_english_asl_profile.sh` 기반 English/ASL WebSocket 검증.
- WebSocket 결과 event의 `locale`, `sign_language`, `model_profile`,
  `protocol_version` echo.
- Spring bridge의 `InferenceContext` 기반 언어/수어/model profile 정규화.

아직 구현되지 않은 항목:

- 실제 raw-video 또는 landmark-to-sign SignGemma checkpoint inference.
- 공식 SignGemma model card 기반 validation.
- ASL dataset 기반 정량 평가.
- 공식 SignGemma 입력 spec에 맞춘 landmark normalization.
- 실제 사용자 카메라 입력에서 안정적으로 landmark를 추출하는 production-grade
  extractor.

## 권장 다음 단계

공식 SignGemma artifact가 공개되면 `sign-gemma` profile에 공식 model id,
checkpoint 또는 LoRA path를 추가하고, 현재 keyword hint 기반 mock 경로를 실제
landmark/video feature extraction 경로로 교체합니다. 이때 BE-model envelope와
`InferenceContext`는 유지해 앱, bridge, queue worker, 운영 endpoint가 모델
변경에 흔들리지 않게 하는 것이 좋습니다.
