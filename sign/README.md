# Sign libraries

재사용 가능한 수어 인식 라이브러리 소스입니다.

```text
sign/
├── embedded/  # 앱에 포함되는 Flutter/온디바이스 SDK
├── backend/   # 원격 AI 서버와 연결되는 Spring 브리지
└── common/    # 두 방식이 공유하는 wire schema와 평가 계약
```

의존 방향은 `embedded -> common <- backend`입니다. `embedded`와 `backend`는
서로의 내부 구현을 직접 참조하지 않고 `common`의 계약으로 통신합니다.

