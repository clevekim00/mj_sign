# MJ Sign — Web Sample Technical Guide

이 문서는 `mj_sign` 프로젝트의 웹 샘플 데모에 대한 기술 가이드입니다. 이 샘플은 수어 인식 시스템이 웹 환경에서 어떻게 구성되고 사용자에게 시각화되는지를 보여주는 프리미엄 대시보드 예제입니다.

---

## 📌 주요 특징

- **실시간 랜드마크 시각화**: 수어 인식의 핵심인 손과 포즈의 랜드마크 데이터 흐름을 Canvas 애니메이션으로 시각화합니다.
- **시스템 메트릭 대시보드**: 처리 프레임 수, 추론 지연 시간(Latency), GPU 부하량 등을 실시간으로 시뮬레이션하여 시스템의 상태를 모니터링합니다.
- **인터랙티브 번역 피드**: 인식된 수어 데이터가 텍스트로 변환되는 과정을 타이핑 효과(Typewriter effect)와 함께 실시간으로 보여줍니다.
- **브리지 V2 아키텍처 시각화**: Flutter 클라이언트부터 Spring 브리지, Sign-Gemma(GPU) 백엔드로 이어지는 데이터 경로를 하단 아키텍처 뷰에서 확인할 수 있습니다.

---

## 🏗 기술 스택

- **Core**: HTML5, Vanilla JavaScript (ES6+)
- **Styling**: Vanilla CSS (CSS Variables, Flexbox/Grid, Glassmorphism)
- **Typography**: Google Fonts (Outfit, Inter)
- **Visualization**: HTML5 Canvas API
- **Deployment**: 별도의 빌드 과정 없이 정적 파일로 서빙 가능

---

## 🚀 실행 방법

웹 샘플은 로컬 HTTP 서버를 통해 실행하는 것을 권장합니다.

### Python을 사용하는 경우 (권장)
```bash
cd web_sample
python3 -m http.server 8000
```
이후 브라우저에서 `http://localhost:8000`으로 접속하세요.

---

## 🛠 주요 구성 요소 설명

### 1. Landmark Visualizer (`app.js` - `LandmarkVisualizer`)
- `hand.proto` 스펙을 참고하여 21개의 손 마디 포인트를 정의합니다.
- `requestAnimationFrame`을 사용하여 포인트 간의 연결선(Connections)과 움직임을 자연스럽게 애니메이션화합니다.

### 2. Metrics Simulator (`app.js` - `SystemMetrics`)
- Spring Bridge의 `/internal/metrics` 엔드포인트를 시뮬레이션합니다.
- `setInterval`을 이용해 실제 운영 환경에서 발생할 수 있는 데이터 변동량(Fluctuations)을 무작위로 생성하여 대시보드에 반영합니다.

### 3. Translation Feed (`app.js` - `TranslationFeed`)
- 수어 번역 결과(`TranslationResult`)의 `is_final` 상태를 시뮬레이션합니다.
- 비동기 타이핑 애니메이션을 통해 AI가 문장을 완성해가는 과정을 보여줍니다.

---

## 🔮 향후 확장 계획

1. **MediaPipe 실시간 통합**: 브라우저의 웹캠을 통해 실제 손 모양을 추적하고 랜드마크 데이터를 생성할 수 있습니다.
2. **WebSocket 실시간 연결**: 구현된 데모를 실제 `Spring Boot Bridge`의 WebSocket 핸들러와 연결하여 실제 모델 추론 결과를 표시할 수 있습니다.
3. **Protobuf 직렬화**: 브라우저에서 Protobuf를 사용하여 백엔드와 이진 데이터를 직접 주고받는 기능을 추가할 수 있습니다.

---

## 📂 파일 구조
- `index.html`: 대시보드 레이아웃 및 SEO 설정
- `style.css`: 글래스모피즘(Glassmorphism) 및 다크 테마 스타일링
- `app.js`: 랜드마크 렌더링 및 상태 관리 로직
