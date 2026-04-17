# MJ Sign: 상세 기술 명세 및 통합 가이드 (Detailed Technical Guide)

이 문서는 `mj_sign` 시스템의 웹 샘플 데모를 기반으로 한 상세 기술 명세서입니다. 시스템의 아키텍처, 데이터 프로토콜, 그리고 실 서비스 통합을 위한 기술적 세부 사항을 다룹니다.

---

## 1. 시스템 아키텍처 (Cloud Bridge V2)

MJ Sign은 단순히 수어를 텍스트로 바꾸는 것을 넘어, 분산된 GPU 환경과 지연 시간에 민감한 클라이언트 간의 **고성능 브리지(Bridge)** 역할을 수행합니다.

### 1.1 데이터 흐름 (Data Pipeline)
1.  **Capture**: Flutter 클라이언트(또는 웹 샘플)가 MediaPipe를 통해 랜드마크를 추출합니다.
2.  **Streaming**: 추출된 `LandmarkFrame`들이 `ClientStreamChunk`로 배칭(Batching)되어 WebSocket을 통해 Spring Bridge로 전송됩니다.
3.  **Buffering**: Spring Bridge의 `SessionBufferService`가 세션별로 프레임을 버퍼링하며, 일정 임계치(Threshold)나 타임아웃(Idle Timeout)이 발생하면 추론을 디스패치합니다.
4.  **Inference**: `InferenceGateway`를 통해 GPU 서버(Sign-Gemma)로 데이터가 전달되며, 결과물인 `TranslationResult`가 다시 클라이언트로 스트리밍됩니다.

### 1.2 주요 컴포넌트
-   **Spring Bridge**: WebSocket 핸들링, 세션 관리, 메트릭 수집(Micrometer).
-   **Inference Gateway**: HTTP, gRPC, 또는 Kafka/RabbitMQ를 통한 비동기 추론 라우팅.
-   **Sign-Gemma**: Google Gemma 모델을 최적화하여 수어-텍스트 번역을 수행하는 추론 엔진.

---

## 2. 데이터 프로토콜 (Protobuf Specification)

시스템은 효율적인 바이너리 직렬화를 위해 **Protocol Buffers (proto3)**를 사용합니다.

### 2.1 LandmarkFrame
수어 인식의 기본 단위입니다.
-   `left_hand`, `right_hand`: 각각 21개의 (x, y, z) 좌표점.
-   `pose`: 상체 및 어깨 선을 포함한 포즈 포인트.
-   `face_contour`: 입술 및 턱선 데이터 (표정 분석용).

### 2.2 TranslationResult
사용자에게 보여지는 최종 결과물입니다.
-   `is_final`: `true`인 경우 문장이 완성되었음을 의미하며, VAD(Voice/Action Activity Detection) 결과에 해당합니다.
-   `confidence`: 모델이 예측한 결과의 신뢰도(0.0 ~ 1.0).

---

## 3. 프론트엔드 구현 메커니즘 (Internal Logic)

웹 샘플(`web_sample`)에 구현된 주요 로직은 다음과 같습니다.

### 3.1 랜드마크 시각화 엔진 (`LandmarkVisualizer`)
-   **좌표계 변환**: Canvas의 `Point3D` 데이터는 0.0 ~ 1.0 사이의 상대 좌표를 사용합니다. 이를 Canvas의 실제 픽셀(`width`, `height`)에 맞게 동적으로 스케일링합니다.
-   **지터링 시뮬레이션 (Jittering)**: 실제 카메라 캡처 시 발생하는 미세한 떨림을 `Math.sin(time)` 함수를 이용해 구현하여 현실감을 높였습니다.

### 3.2 메트릭 시스템 시뮬레이션 (`SystemMetrics`)
-   운영 환경의 `/internal/metrics` 데이터를 시각화합니다.
-   **Frames Processed**: 누적 처리량.
-   **Inference Latency**: 클라우드 왕복 시간을 포함한 실제 사용자가 체감하는 지연 시간.

---

## 4. 실 서비스 통합 가이드 (Integration Guide)

데모에서 실제 백엔드로 전환하기 위한 가이드입니다.

### 4.1 WebSocket 연결 코드 (Sample)
```javascript
const socket = new WebSocket('ws://your-bridge-server/ws/sign');

socket.onopen = () => {
    console.log('MJ Sign Bridge Connected');
    // 세션 시작 메시지 전송
};

socket.onmessage = (event) => {
    const result = JSON.parse(event.data); // 실제 운영 환경은 Protobuf Binary 사용 권장
    if(result.is_final) {
        updateFeed(result.text, true);
    } else {
        updateCurrentTyping(result.text);
    }
};
```

### 4.2 MediaPipe 통합 시 체크리스트
-   **프레임 레이트**: 초당 20~30 프레임 유지가 권장됩니다.
-   **동기화**: `timestamp_ms`를 정확히 기록하여 지연 시간 통계에 반영해야 합니다.
-   **데이터 정규화**: MediaPipe의 결과값을 반드시 0~1 사이로 정규화하여 전송하십시오.

---

## 5. 디자인 시스템 및 UX 원칙

-   **Glassmorphism**: 정보의 가독성을 해치지 않으면서 현대적인 느낌을 주기 위해 `backdrop-filter: blur`를 적극 활용하였습니다.
-   **Status Indicator**: 시스템의 작동 여부를 즉각적으로 알 수 있도록 상단에 Pulsing 상징색(Green)을 배치하였습니다.
-   **Responsive Layout**: 데스크톱 대시보드 환경과 모바일 모니터링 환경을 모두 지원하는 그리드 시스템을 채택하였습니다.
