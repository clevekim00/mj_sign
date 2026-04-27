# Language Model Onboarding Guide

이 문서는 MJ Sign에 새로운 언어/수어 모델을 추가하는 절차와 Sign Gemma 호환 model spec을 정리합니다. 목표는 새 언어를 추가해도 `InferenceGateway`, `QueueInferenceTransport`, WebSocket API를 바꾸지 않는 것입니다.

## 기본 원칙

- 언어별 차이는 `InferenceContext`와 model profile에서 처리합니다.
- BE 내부 SPI는 `ClientStreamChunk + InferenceContext`로 고정합니다.
- BE-model request/response envelope는 [MODEL_PROTOCOL.md](./MODEL_PROTOCOL.md)를 따릅니다.
- model backend는 `model_profile`을 기준으로 실제 모델, tokenizer, prompt, postprocessor를 선택합니다.
- Flutter에서 active keyboard language를 직접 알 수 있으면 `SignLanguageContext`로 명시합니다. 알 수 없으면 platform locale을 기본값으로 사용합니다.

## 현재 기본 매핑

| Locale language | Sign language | Model profile | 출력 언어 |
| --- | --- | --- | --- |
| `ko` | `ksl` | `sign-gemma-ko` | Korean |
| `en` | `asl` | `sign-gemma` | English |
| `ja` | `jsl` | `sign-gemma-ja` | Japanese |
| `zh` | `csl` | `sign-gemma-zh` | Chinese |
| `fr` | `lsf` | `sign-gemma-fr` | French |
| `de` | `dgs` | `sign-gemma-de` | German |
| `es` | `lse` | `sign-gemma-es` | Spanish |

## Sign Gemma-Compatible Model Spec

새 언어 모델은 아래 spec을 만족해야 합니다.

### Profile Metadata

```yaml
model_profile: sign-gemma
model_family: sign-gemma
model_version: sign-gemma-asl-v1
sign_language: asl
locale: en-US
output_language: en
output_mode: sentence
input_schema: mj.sign.ClientStreamChunk
input_schema_version: v1
protocol_version: mj-sign-model-v1
transport: protobuf-b64
min_frames: 8
max_frames: 24
recommended_fps: 8-12
supports_partial: false
supports_final: true
```

### Required Model Server Endpoints

Health:

```text
GET /health
```

Response:

```json
{
  "status": "ok",
  "model_version": "sign-gemma-asl-v1",
  "mode": "real"
}
```

Inference:

```text
POST /api/v2/recognize
```

Request:

```json
{
  "session_id": "sample-en",
  "protobuf_b64": "BASE64_PROTOBUF_CLIENT_STREAM_CHUNK",
  "frame_count": 12,
  "transport": "protobuf-b64",
  "client_schema_version": "v1",
  "protocol_version": "mj-sign-model-v1",
  "locale": "en-US",
  "sign_language": "asl",
  "model_profile": "sign-gemma"
}
```

Response:

```json
{
  "session_id": "sample-en",
  "text": "Nice to meet you.",
  "is_final": true,
  "confidence": 0.94,
  "processing_time_ms": 128,
  "model_version": "sign-gemma-asl-v1",
  "protocol_version": "mj-sign-model-v1",
  "locale": "en-US",
  "sign_language": "asl",
  "model_profile": "sign-gemma",
  "error": null
}
```

### Output Contract

| 필드 | 요구사항 |
| --- | --- |
| `text` | 최종 사용자에게 넣을 수 있는 자연어 문장 또는 후처리 가능한 키워드 문자열 |
| `is_final` | idle flush 또는 window inference 결과는 보통 `true` |
| `confidence` | `0.0`부터 `1.0` 사이 값 |
| `model_version` | 배포/롤백 가능한 정확한 모델 버전 |
| `error` | 오류 시 사람이 읽을 수 있는 짧은 메시지 |

권장:

- 모델이 키워드만 잘 반환한다면 `text`에는 키워드를 반환하고 BE LLM refinement가 문장화하게 둡니다.
- 모델이 문장까지 직접 반환한다면 `output_mode=sentence`로 문서화하고 LLM refinement threshold를 더 보수적으로 둡니다.
- confidence가 낮으면 빈 문장보다 짧은 후보 문장과 낮은 confidence를 반환하는 편이 운영 디버깅에 좋습니다.

## 새 언어 추가 절차

예시는 Brazilian Portuguese/Libras를 추가하는 흐름입니다.

### 1. 언어 코드 결정

정해야 할 값:

```text
locale: pt-BR
locale language key: pt
sign_language: libras
model_profile: sign-gemma-pt-br
output_language: Portuguese
```

규칙:

- `locale`은 가능하면 BCP-47 형식을 사용합니다.
- `sign_language`는 소문자 ASCII token을 사용합니다.
- `model_profile`은 `sign-gemma-{locale-or-sign-language}` 패턴을 권장합니다.

### 2. Backend Mapping 추가

파일:

```text
sign_bridge/src/main/resources/application.yml
```

추가:

```yaml
sign:
  language:
    sign-language-by-locale-language:
      pt: libras
    model-profile-by-sign-language:
      libras: sign-gemma-pt-br
```

주의:

- YAML map을 profile별 properties로 override할 수 있습니다.
- 운영에서는 locale mapping 변경이 모델 rollout과 같이 배포되어야 합니다.

### 3. Flutter Mapping 추가

파일:

```text
slr_input_kit/lib/src/sign_gemma_client.dart
```

추가:

```dart
static const Map<String, String> _signLanguageByLocaleLanguage = {
  'pt': 'libras',
};

static const Map<String, String> _modelProfileBySignLanguage = {
  'libras': 'sign-gemma-pt-br',
};
```

앱에서 active keyboard language를 알 수 있다면 hardcoded mapping에 의존하지 않고 아래처럼 명시할 수 있습니다.

```dart
const SignLanguageContext(
  locale: 'pt-BR',
  signLanguage: 'libras',
  modelProfile: 'sign-gemma-pt-br',
)
```

### 4. Model Server Profile 추가

model server는 `model_profile=sign-gemma-pt-br`을 받았을 때 다음을 선택해야 합니다.

- model artifact
- tokenizer/preprocessor
- landmark normalization
- decoder/postprocessor
- output language prompt
- confidence calibration

권장 profile registry 예:

```python
MODEL_PROFILES = {
    "sign-gemma-pt-br": {
        "sign_language": "libras",
        "locale": "pt-BR",
        "model_version": "sign-gemma-libras-v1",
        "output_language": "Portuguese",
    }
}
```

### 5. LLM Refinement Prompt 추가

파일:

```text
sign_bridge/src/main/kotlin/com/mj/sign/service/SignTranslationService.kt
```

추가할 내용:

- locale/sign language별 system prompt
- 키워드 순서를 보존하는 지시
- 출력 언어와 존댓말/문체 정책

현재 구현은 Korean/English prompt 분기를 갖고 있으므로, 새 언어를 제품 수준으로 지원하려면 이 분기를 profile registry 형태로 확장하는 것이 좋습니다.

### 6. Mock Server와 Tests 추가

권장 테스트:

- `SignLanguageResolverTest`: `pt-BR -> libras -> sign-gemma-pt-br`
- `HttpInferenceGatewayTest`: request envelope에 새 context가 들어가는지 검증
- `QueueInferenceGatewayTest`: queue task request가 같은 context를 유지하는지 검증
- WebSocket handler test: query parameter가 `InferenceContext`로 전달되는지 검증
- mock server: 새 profile일 때 샘플 문장을 반환하도록 추가

### 7. Documentation 업데이트

업데이트할 문서:

- `README.md`
- `README_ko.md`
- `README_en.md`
- `MODEL_PROTOCOL.md`
- `API_SPI_REFERENCE.md`
- `LANGUAGE_MODEL_GUIDE.md`
- platform sample README 또는 profile 문서

## Model Quality Spec

새 언어 모델은 최소 아래 기준을 통과해야 합니다.

| 항목 | 기준 |
| --- | --- |
| Schema compatibility | `ClientStreamChunk` protobuf v1 decode 가능 |
| Latency | mock/local 기준 p95 목표를 문서화 |
| Confidence | 0-1 범위 calibration 기준 문서화 |
| Empty frames | 400 또는 `error` response |
| Unknown gesture | 낮은 confidence와 안전한 fallback text |
| Locale echo | response에 locale/sign_language/model_profile echo 권장 |
| Versioning | `model_version`에 artifact 버전 포함 |
| Observability | processing time, error message 제공 |

## Release Checklist

- BE `application.yml` mapping 추가
- Flutter `SignLanguageContext` mapping 또는 host app override 추가
- model server profile 추가
- language-specific prompt 추가
- mock response 추가
- backend tests 통과
- Flutter analyze 통과
- queue integration script로 envelope 유지 확인
- README와 protocol docs 업데이트

## 운영 체크포인트

- 언어별 모델은 같은 endpoint에 붙이되 `model_profile`로 선택하는 것을 기본으로 합니다.
- 모델별 endpoint가 분리되어야 한다면 API Gateway 또는 model server 내부 router에서 처리하고 BE envelope는 유지합니다.
- 새 언어의 개인정보/생체정보 처리 정책을 별도로 확인합니다.
- 수어는 spoken language와 1:1 대응하지 않을 수 있으므로, `locale`과 `sign_language`를 항상 분리해서 관리합니다.
