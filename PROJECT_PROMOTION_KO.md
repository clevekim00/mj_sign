# MJ Sign 프로젝트 소개서

English version: [PROJECT_PROMOTION_EN.md](./PROJECT_PROMOTION_EN.md)

## 한 줄 소개

MJ Sign은 수어를 앱, 웹, 데스크톱 입력 경험으로 연결하고, 언어별 Sign Gemma profile로 확장할 수 있는 크로스 플랫폼 수어 인식 브릿지입니다.

## 문제 정의

많은 디지털 서비스는 음성 입력과 키보드 입력을 기본으로 제공하지만, 수어 사용자를 위한 자연스러운 입력 경로는 아직 부족합니다. 수어 인식은 카메라, landmark 추출, 실시간 추론, 문장 보정, 운영 안정성까지 모두 연결되어야 하므로 단일 앱 기능으로 끝나기 어렵습니다.

MJ Sign은 이 문제를 “입력 위젯 + 클라우드/edge 브릿지 + GPU serving + LLM 보정” 구조로 분리해, 여러 플랫폼에서 같은 수어 입력 경험을 재사용할 수 있게 합니다.

## 핵심 가치

- 앱 개발자는 `SlrInputWidget`을 붙이는 방식으로 수어 입력 UI를 빠르게 실험할 수 있습니다.
- 모델 개발자는 HTTP, gRPC, queue provider 중 하나로 GPU serving backend를 교체할 수 있습니다.
- 운영자는 health, readiness, metrics, queue retry/DLQ 정책을 기준으로 배포 안정성을 점검할 수 있습니다.
- 사용자는 원시 키워드가 아니라 LLM으로 다듬어진 자연스러운 문장을 받을 수 있습니다.
- 서비스 팀은 Korean/KSL, English/ASL처럼 언어와 수어 체계를 분리해 같은 BE SPI로 모델을 확장할 수 있습니다.

## 제품 시나리오

1. 사용자가 채팅창이나 검색창 옆 수어 입력 아이콘을 누릅니다.
2. Flutter 입력 위젯이 카메라에서 손/포즈/얼굴 landmark frame을 추출합니다.
3. Landmark batch가 WebSocket protobuf로 Sign Bridge에 전달됩니다.
4. Sign Bridge가 세션 단위로 frame을 모으고 idle timeout 시점에 추론을 flush합니다.
5. GPU serving backend가 수어 키워드 또는 문장을 반환합니다.
6. LLM refinement layer가 final 결과를 사용자의 언어 context에 맞는 자연스러운 문장으로 보정합니다.
7. 앱은 최종 텍스트를 입력창에 반영합니다.

## English/ASL Sign Gemma Profile

MJ Sign은 영어 기반 수어 입력을 위해 `locale=en-US`, `sign_language=asl`, `model_profile=sign-gemma` 조합을 표준 profile로 둡니다.

- Flutter client는 platform locale 또는 앱이 제공한 `SignLanguageContext`를 WebSocket query로 전달합니다.
- Sign Bridge는 이 값을 `InferenceContext`로 정규화해 HTTP, queue, future gRPC provider에 동일하게 넘깁니다.
- Mock GPU 서버는 `sign-gemma` profile registry를 통해 English/ASL mock response, model metadata, supported landmarks를 제공합니다.
- `/health`와 `/ready`는 profile 목록, 로드 상태, LoRA weight 설정 여부, 지원 landmark contract를 반환합니다.
- `scripts/verify_english_asl_profile.sh`로 WebSocket부터 BE, mock GPU, profile echo까지 end-to-end 검증할 수 있습니다.

## 지원 플랫폼 샘플

| 플랫폼 | 사용 장면 | 샘플 위치 |
| --- | --- | --- |
| Android | 모바일 채팅, 검색, 민원 앱 | `slr_input_kit/example/lib/samples/android_sample.dart` |
| iOS | iPhone 앱 내 접근성 입력 | `slr_input_kit/example/lib/samples/ios_sample.dart` |
| iPad | 교육, 키오스크, 상담 데스크 | `slr_input_kit/example/lib/samples/ipad_sample.dart` |
| Web | 브라우저 기반 상담/접수 서비스 | `slr_input_kit/example/lib/samples/web_sample.dart` |
| Windows | 데스크톱 키오스크, 공공기관 창구 | `slr_input_kit/example/lib/samples/windows_sample.dart` |
| macOS/OSX | 개발/시연/크리에이터 워크플로 | `slr_input_kit/example/lib/samples/macos_sample.dart` |
| Linux | edge GPU workstation, 연구 장비 | `slr_input_kit/example/lib/samples/linux_sample.dart` |

## 기술 차별점

- WebSocket protobuf로 frame payload를 작고 명확하게 유지합니다.
- Session buffer와 idle timeout flush로 짧은 gesture 조각을 문맥 단위로 묶습니다.
- `InferenceContext`로 언어, 수어 체계, 모델 profile을 표준화해 BE SPI를 언어와 무관하게 유지합니다.
- 영어 입력은 기본적으로 `asl`과 `sign-gemma` profile로 라우팅됩니다.
- Mock GPU는 profile registry를 통해 `sign-gemma`와 `sign-gemma-ko`를 분리하고, 실제 weight가 준비되면 profile별 model id/LoRA path로 교체할 수 있습니다.
- API/SPI reference와 언어별 모델 추가 가이드를 분리해, 새 수어 모델을 붙일 때 provider/transport 코드를 흔들지 않습니다.
- HTTP provider는 빠른 mock/real serving 연결에 적합합니다.
- Queue provider는 Kafka/RabbitMQ 기반 비동기 worker 확장에 적합합니다.
- Worker consumer는 request consumption부터 result publication까지 실제 로컬 통합 검증이 가능하도록 구성되어 있습니다.
- Readiness는 provider health를 반영하므로 운영 환경에서 “앱은 떴지만 모델은 준비 안 됨” 상태를 구분할 수 있습니다.

## Landmark Contract

공식 SignGemma의 정확한 입력 landmark spec은 아직 공개 model card로 확인되지 않았습니다. 따라서 MJ Sign은 현재 제품/연구 구현 기준으로 MediaPipe-style protobuf landmark contract를 채택합니다.

| 입력 필드 | 현재 지원 범위 | 목적 |
| --- | --- | --- |
| `left_hand` | 21개 3D hand landmark 권장 | 왼손 수형과 움직임 |
| `right_hand` | 21개 3D hand landmark 권장 | 오른손 수형과 움직임 |
| `pose` | 상체 중심 pose landmark | 어깨, 팔, 몸 방향 문맥 |
| `face_contour` | 입/턱/얼굴 contour landmark | 비수지 신호와 표정 문맥 |

공식 SignGemma weight와 입력 스키마가 공개되면 이 contract는 adapter layer에서 맞추고, BE-model envelope는 유지하는 전략입니다.

## 데모 방법

Mock GPU와 bridge를 실행합니다.

```bash
cd sign_gemma_mock
python main.py
```

```bash
cd sign_bridge
./gradlew bootRun
```

Flutter 샘플 갤러리를 실행합니다.

```bash
cd slr_input_kit/example
flutter run
```

Kafka 또는 RabbitMQ queue worker 흐름까지 보여주려면 통합 스택 검증 스크립트를 실행합니다.

```bash
./scripts/verify_english_asl_profile.sh
./scripts/verify_kafka_stack.sh
./scripts/verify_rabbitmq_stack.sh
```

English/ASL profile만 빠르게 확인하려면 `docker-compose.stack.http.yml` 기반 HTTP 통합 스택을 사용합니다.

## 적용 가능 분야

- 공공기관 민원 키오스크
- 병원/약국 접수 및 안내
- 교육용 수어 학습 앱
- 메신저와 소셜 앱의 접근성 입력
- 콜센터/상담센터 보조 입력
- 로컬 GPU 기반 연구/실증 환경

## 현재 완성도와 남은 과제

현재는 bridge, provider routing, mock GPU, English/ASL `sign-gemma` profile registry, profile-aware health/readiness, queue worker contract, broker serializer/converter, platform sample gallery가 준비되어 있습니다. 제품 단계로 가려면 실제 landmark extractor, 실제 SignGemma 또는 SignGemma-compatible weight serving, 사용자 인증이 붙은 WSS endpoint, 운영 대시보드, 개인정보 보호 정책이 추가되어야 합니다.

개발자가 참고할 기준 문서는 `API_SPI_REFERENCE.md`, `MODEL_PROTOCOL.md`, `LANGUAGE_MODEL_GUIDE.md`, `SIGN_GEMMA_RESEARCH_KO.md`, `SIGN_GEMMA_RESEARCH.md`입니다. 이 문서들이 API/SPI 경계, BE-model envelope, 언어별 Sign Gemma 호환 model spec, 공개 SignGemma 조사와 landmark 지원 범위를 나눠 담당합니다.

## 다음 개발 우선순위

1. `DemoLandmarkFrameSource`를 실제 카메라 landmark extractor로 교체합니다.
2. 공식 SignGemma 또는 SignGemma-compatible ASL weight를 `sign-gemma` profile에 연결합니다.
3. 실제 GPU 모델 serving backend를 HTTP 또는 queue worker 뒤에 연결합니다.
4. Web/iOS/Android 카메라 권한과 lifecycle 처리를 제품 수준으로 보강합니다.
5. 운영 배포용 TLS, 인증, session 정책, metrics dashboard를 구성합니다.
6. 수어 데이터셋, 평가 지표, 사용자 피드백 루프를 문서화합니다.

## 메시지

MJ Sign의 목표는 “수어 인식 모델 하나 만들기”에서 끝나지 않습니다. 실제 사용자가 여러 기기에서 수어를 입력하고, 서비스가 안정적으로 받아들이고, 자연스러운 문장으로 이어주는 전체 경로를 만드는 것이 목표입니다.
