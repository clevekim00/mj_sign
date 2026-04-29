# LinguaSign 제품 소개서

English version: [PROJECT_PROMOTION_EN.md](./PROJECT_PROMOTION_EN.md)

## Project Architecture / 프로젝트 아키텍처

공통 런타임 구조와 SignGemma-compatible 예제 흐름은 [PROJECT_ARCHITECTURE_KO.md](PROJECT_ARCHITECTURE_KO.md)에 정리되어 있습니다. 영문판은 [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md)입니다. Spring Boot + cross-platform app 실행 가이드는 [SIGN_GEMMA_APP_DEMO_KO.md](SIGN_GEMMA_APP_DEMO_KO.md), 영문판은 [SIGN_GEMMA_APP_DEMO.md](SIGN_GEMMA_APP_DEMO.md)를 참고하세요.

## 예제 가이드

Spring Boot + SignGemma-compatible cross-platform 실행 예제는
[SIGN_GEMMA_APP_DEMO_KO.md](./SIGN_GEMMA_APP_DEMO_KO.md)에 정리했습니다.
영문 가이드는 [SIGN_GEMMA_APP_DEMO.md](./SIGN_GEMMA_APP_DEMO.md)입니다.
로컬 mock 모델 서버, Spring Boot profile, Flutter 플랫폼별 실행 명령,
API 확인, 문제 해결 절차를 포함합니다.

## 한 줄 소개

LinguaSign은 SignBridge 플랫폼 기반으로 수어를 앱, 웹, 데스크톱 입력 경험에 연결하고, 언어별 SignGemma-compatible profile로 확장할 수 있는 크로스 플랫폼 수어 입력 제품입니다.

## 문제 정의

많은 디지털 서비스는 음성 입력과 키보드 입력을 기본으로 제공하지만, 수어 사용자를 위한 자연스러운 입력 경로는 아직 부족합니다. 수어 인식은 카메라, landmark 추출, 실시간 추론, 문장 보정, 운영 안정성까지 모두 연결되어야 하므로 단일 앱 기능으로 끝나기 어렵습니다.

SignBridge는 이 문제를 “SignInputKit SDK + 클라우드/edge 브릿지 + GPU serving + LLM 보정” 구조로 분리해, LinguaSign이 여러 플랫폼에서 같은 수어 입력 경험을 재사용할 수 있게 합니다.

## 핵심 가치

- 앱 개발자는 SignInputKit SDK의 `SlrInputWidget`을 붙여 수어 입력 UI를 빠르게 실험할 수 있습니다.
- 모델 개발자는 언어별 수어 모델을 같은 SignBridge 연결 방식 뒤에 붙일 수 있습니다.
- 운영자는 health, readiness, metrics, retry/DLQ 정책으로 배포 안정성을 점검할 수 있습니다.
- 사용자는 원시 키워드가 아니라 문맥에 맞게 다듬어진 자연스러운 문장을 받을 수 있습니다.
- 서비스 팀은 Korean/KSL을 기본 흐름으로 두고, English/ASL 같은 새 언어/수어 조합을 같은 구조로 확장할 수 있습니다.

## 현재 데모와 목표 제품

현재 로컬 데모는 실제 카메라 extractor 대신 `DemoLandmarkFrameSource`를 사용해 SignBridge 연결, protobuf streaming, idle flush, mock GPU 응답, 언어 profile echo를 검증합니다.

목표 제품에서는 `DemoLandmarkFrameSource`를 실제 카메라/MediaPipe-style landmark extractor로 교체하고, SignGemma-compatible 또는 공식 SignGemma weight가 준비된 profile을 GPU serving backend에 연결합니다.

## 제품 시나리오

1. 사용자가 채팅창이나 검색창 옆 수어 입력 아이콘을 누릅니다.
2. 제품 환경에서는 Flutter 입력 위젯이 카메라에서 손/포즈/얼굴 landmark frame을 추출합니다.
3. Landmark batch가 WebSocket protobuf로 SignBridge에 전달됩니다.
4. SignBridge가 세션 단위로 frame을 모으고 idle timeout 시점에 추론을 flush합니다.
5. GPU serving backend가 수어 키워드 또는 문장을 반환합니다.
6. LLM refinement layer가 final 결과를 사용자의 언어 context에 맞는 자연스러운 문장으로 보정합니다.
7. 앱은 최종 텍스트를 입력창에 반영합니다.

## 언어별 모델 전략

SignBridge는 언어, 수어 체계, 모델 profile을 분리합니다. 한국어 서비스 흐름은 `locale=ko-KR`, `sign_language=ksl`, `model_profile=sign-gemma-ko`를 기본 profile로 두고, 영어 확장 흐름은 `locale=en-US`, `sign_language=asl`, `model_profile=sign-gemma`를 SignGemma-compatible profile로 둡니다.

- Flutter client는 platform locale 또는 앱이 제공한 `SignLanguageContext`를 WebSocket query로 전달합니다.
- SignBridge는 이 값을 `InferenceContext`로 정규화해 HTTP, queue, future gRPC provider에 동일하게 넘깁니다.
- Mock GPU 서버는 `sign-gemma-ko`와 `sign-gemma` profile registry를 통해 Korean/KSL 및 English/ASL mock response, model metadata, supported landmarks를 제공합니다.
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
- `InferenceContext`로 언어, 수어 체계, 모델 profile을 표준화해 새 언어 모델을 같은 연결 방식으로 추가할 수 있습니다.
- Mock GPU는 `sign-gemma-ko`와 `sign-gemma`를 분리해 실제 weight가 준비되면 profile별 model id/LoRA path로 교체할 수 있습니다.
- HTTP 연결은 빠른 mock/real serving 검증에 적합하고, Kafka/RabbitMQ queue 연결은 비동기 worker 확장에 적합합니다.
- Readiness는 SignBridge와 모델 backend 상태를 구분하므로 “앱은 떴지만 모델은 준비 안 됨” 상태를 운영에서 확인할 수 있습니다.
- T2S/STS 1차 계약과 `SpeechToTextAdapter`, `SignPlanner`, `SignMotionGenerator`, `SignSynthesisProvider` SPI를 분리해 텍스트/음성 입력을 `SignPlan + landmark motion`으로 돌려주는 playback 검증도 시작할 수 있습니다.

## 개발자 참고

- API/SPI reference와 언어별 모델 추가 가이드를 분리해, 새 수어 모델을 붙일 때 provider/transport 코드를 흔들지 않습니다.
- Queue worker는 request consumption부터 result publication까지 로컬 통합 검증이 가능하도록 구성되어 있습니다.
- BE-model envelope와 adapter layer를 유지하면 모델 weight나 serving 방식이 바뀌어도 앱과 SignBridge API를 안정적으로 유지할 수 있습니다.
- T2S/STS 설계는 `SIGN_SYNTHESIS_DESIGN_KO.md`와 `SIGN_SYNTHESIS_DESIGN.md`에 별도로 정리했습니다.

## Landmark Contract

공식 SignGemma의 정확한 입력 landmark spec은 아직 공개 model card로 확인되지 않았습니다. 따라서 SignBridge는 현재 mock/profile contract 검증 단계에서 MediaPipe-style protobuf landmark contract를 채택합니다.

| 입력 필드 | 현재 지원 범위 | 목적 |
| --- | --- | --- |
| `left_hand` | 21개 3D hand landmark 권장 | 왼손 수형과 움직임 |
| `right_hand` | 21개 3D hand landmark 권장 | 오른손 수형과 움직임 |
| `pose` | 상체 중심 pose landmark | 어깨, 팔, 몸 방향 문맥 |
| `face_contour` | 입/턱/얼굴 contour landmark | 비수지 신호와 표정 문맥 |

공식 SignGemma weight와 입력 스키마가 공개되면 이 contract는 adapter layer에서 맞추고, BE-model envelope는 유지하는 전략입니다. 그 전까지 `sign-gemma` 계열 profile은 SignGemma-compatible serving contract를 검증하기 위한 profile로 다룹니다.

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

HTTP 통합 스택을 빠르게 확인하려면 아래 명령을 실행합니다.

```bash
docker compose -f docker-compose.stack.http.yml up -d
./scripts/verify_english_asl_profile.sh
```

Kafka 또는 RabbitMQ queue worker 흐름까지 보여주려면 통합 스택 검증 스크립트를 실행합니다.

```bash
./scripts/verify_kafka_stack.sh
./scripts/verify_rabbitmq_stack.sh
```

## 적용 가능 분야

- 공공기관 민원 키오스크
- 병원/약국 접수 및 안내
- 교육용 수어 학습 앱
- 메신저와 소셜 앱의 접근성 입력
- 콜센터/상담센터 보조 입력
- 로컬 GPU 기반 연구/실증 환경

## 현재 완성도와 남은 과제

현재는 bridge, provider routing, mock GPU, Korean/KSL `sign-gemma-ko` 및 English/ASL `sign-gemma` profile registry, profile-aware health/readiness, queue worker contract, broker serializer/converter, platform sample gallery, T2S/STS mock synthesis contract, ASR/T2S HTTP provider 확장점과 playback stub이 준비되어 있습니다. 제품 단계로 가려면 실제 landmark extractor, 실제 SignGemma 또는 SignGemma-compatible weight serving, 실제 ASR/T2S model serving, 사용자 인증이 붙은 WSS endpoint, 운영 대시보드, 개인정보 보호 정책이 추가되어야 합니다.

개발자가 참고할 기준 문서는 `API_SPI_REFERENCE.md`, `MODEL_PROTOCOL.md`, `LANGUAGE_MODEL_GUIDE.md`, `SIGN_SYNTHESIS_DESIGN_KO.md`, `SIGN_SYNTHESIS_DESIGN.md`, `SIGN_GEMMA_RESEARCH_KO.md`, `SIGN_GEMMA_RESEARCH.md`입니다. 이 문서들이 API/SPI 경계, BE-model envelope, 언어별 Sign Gemma 호환 model spec, T2S/STS 설계, 공개 SignGemma 조사와 landmark 지원 범위를 나눠 담당합니다.

## 다음 개발 우선순위

1. `DemoLandmarkFrameSource`를 실제 카메라 landmark extractor로 교체합니다.
2. 공식 SignGemma 또는 SignGemma-compatible ASL weight를 `sign-gemma` profile에 연결합니다.
3. STS용 ASR adapter와 T2S용 실제 sign generation provider를 `signbridge-synthesis-v1` 뒤에 연결합니다.
4. 실제 GPU 모델 serving backend를 HTTP 또는 queue worker 뒤에 연결합니다.
5. Web/iOS/Android 카메라 권한과 lifecycle 처리를 제품 수준으로 보강합니다.
6. 운영 배포용 TLS, 인증, session 정책, metrics dashboard를 구성합니다.
7. 수어 데이터셋, 평가 지표, 사용자 피드백 루프를 문서화합니다.

## 메시지

LinguaSign의 목표는 “수어 인식 모델 하나 만들기”에서 끝나지 않습니다. 실제 사용자가 여러 기기에서 수어를 입력하고, SignBridge가 안정적으로 받아들이고, 자연스러운 문장으로 이어주는 전체 경로를 만드는 것이 목표입니다.
