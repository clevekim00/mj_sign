# LinguaSign SNS 홍보 문안

이 문서는 [PROJECT_PROMOTION_KO.md](./PROJECT_PROMOTION_KO.md)를 기반으로 SNS에 바로 올릴 수 있는 홍보 문안을 정리합니다. 현재 데모는 공식 SignGemma weight를 포함하지 않으며, SignGemma-compatible profile과 mock serving backend를 통해 앱-브릿지-모델 서버 계약을 검증하는 단계입니다.

## 핵심 포지셔닝

LinguaSign은 수어 입력을 앱 하나의 기능이 아니라 Web, iOS, iPad, Android, Windows, macOS, Linux에서 재사용할 수 있는 입력 인프라로 만드는 프로젝트입니다. Flutter SignInputKit, Spring Boot SignBridge, model serving adapter를 분리해 실제 SignGemma 또는 SignGemma-compatible 모델이 준비되면 빠르게 교체할 수 있는 구조를 목표로 합니다.

## 짧은 버전

수어 입력을 여러 플랫폼에서 재사용 가능한 입력 인프라로 만들고 있습니다.

LinguaSign은 Flutter 기반 SignInputKit, Spring Boot SignBridge, SignGemma-compatible model serving adapter를 연결해 Web/iOS/iPad/Android/Windows/macOS/Linux 샘플에서 같은 수어 입력 흐름을 검증합니다.

현재는 실제 모델 성능보다 앱-브릿지-모델 서버 계약, WebSocket protobuf streaming, profile registry, readiness/metrics, T2S/STS playback UX를 production-ready 골격으로 만드는 데 집중했습니다.

#LinguaSign #SignBridge #SignGemma #Accessibility #Flutter #SpringBoot #AI

## LinkedIn / Facebook 긴 버전

LinguaSign 프로젝트를 정리하고 있습니다.

목표는 “수어 인식 모델 하나 붙이기”가 아니라, 수어 입력이 실제 서비스에서 반복적으로 재사용될 수 있는 제품/플랫폼 골격을 만드는 것입니다.

이번 단계에서 준비한 것들:

- Flutter 기반 SignInputKit 샘플
- Spring Boot SignBridge backend
- WebSocket protobuf landmark streaming
- SignGemma-compatible mock model server
- Korean/KSL, English/ASL model profile registry
- profile-aware health/readiness/metrics
- OpenAPI/Swagger 계약
- T2S(Text-to-Sign), STS(Speech-to-Sign) API와 playback UX
- Kafka/RabbitMQ worker 확장 골격
- Web/iOS/iPad/Android/Windows/macOS/Linux 샘플 흐름

아직 공식 SignGemma weight와 입력 schema가 공개적으로 확정된 상태는 아니기 때문에, 지금은 mock serving backend와 adapter boundary를 기준으로 앱-브릿지-모델 서버 계약을 고정하는 데 집중했습니다. 실제 모델이 준비되면 profile adapter 자리에 연결하는 방식으로 갈 수 있게 설계했습니다.

수어 접근성 입력은 단순한 데모가 아니라 카메라, landmark extractor, backend bridge, GPU serving, LLM 문장 보정, 운영 metrics까지 이어지는 전체 경로가 중요하다고 보고 있습니다.

다음 단계는 실제 camera landmark extractor와 실제 SignGemma 또는 SignGemma-compatible serving backend를 연결하는 것입니다.

#LinguaSign #SignBridge #SignInputKit #SignGemma #Accessibility #AssistiveTechnology #Flutter #SpringBoot #AIEngineering

## X / Threads 버전

수어 입력을 Web/iOS/iPad/Android/Windows/macOS/Linux에서 재사용 가능한 입력 인프라로 만드는 LinguaSign을 정리 중입니다.

Flutter SignInputKit + Spring Boot SignBridge + SignGemma-compatible model adapter 구조로 WebSocket protobuf streaming, profile registry, readiness/metrics, T2S/STS UX까지 먼저 고정했습니다.

공식 SignGemma artifact가 준비되면 adapter 자리에 실제 모델을 연결하는 방향입니다.

#LinguaSign #SignBridge #SignGemma #Flutter #SpringBoot #Accessibility

## 개발자 대상 버전

LinguaSign / SignBridge 쪽 작업을 production-ready 골격 중심으로 정리했습니다.

핵심은 모델을 앱에 직접 박는 방식이 아니라, 앱-브릿지-모델 서버 계약을 먼저 안정화하는 것입니다.

- Flutter SignInputKit sample
- Spring Boot SignBridge
- protobuf WebSocket streaming
- model profile registry
- health/readiness/Prometheus metrics
- OpenAPI smoke verification
- T2S/STS synthesis contract
- Kafka/RabbitMQ worker extension
- SignGemma-compatible mock serving

공식 SignGemma weight/spec이 공개되면 mock adapter를 실제 serving adapter로 교체할 수 있게 자리를 만들어두었습니다.

#AIEngineering #SpringBoot #Flutter #Protobuf #WebSocket #SignGemma #Accessibility

## 투자자 / 파트너 대상 버전

LinguaSign은 수어 사용자가 모바일, 웹, 데스크톱 서비스에서 더 자연스럽게 입력할 수 있도록 만드는 크로스 플랫폼 수어 입력 프로젝트입니다.

현재는 SignInputKit SDK, Spring Boot SignBridge, SignGemma-compatible model serving adapter를 기반으로 제품 골격을 검증하고 있습니다. 핵심은 특정 모델 하나에 종속되지 않고, 언어와 수어 체계별 model profile을 교체 가능한 방식으로 운영하는 것입니다.

적용 가능 분야는 공공기관 키오스크, 병원/약국 접수, 교육용 수어 학습, 상담센터 보조 입력, 메신저/소셜 앱 접근성 입력입니다.

다음 단계는 실제 camera landmark extractor와 실제 모델 serving을 연결해 파일럿 환경에서 사용자 흐름을 검증하는 것입니다.

#Accessibility #AssistiveTech #AIProduct #LinguaSign #SignBridge

## 주의해서 말할 표현

- “SignGemma를 탑재했다”보다는 “SignGemma-compatible adapter/profile을 준비했다”가 정확합니다.
- “실시간 완성형 수어 번역”보다는 “실시간 수어 입력 파이프라인과 제품 골격을 검증 중”이 안전합니다.
- “공식 모델 성능”을 주장하지 말고 “공식 artifact 공개 시 교체 가능한 구조”라고 설명합니다.
