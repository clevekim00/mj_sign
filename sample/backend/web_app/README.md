# LinguaSign Web App

This Vite/React sample demonstrates the LinguaSign web experience on top of the
SignBridge backend. It captures camera input, runs a MediaPipe-style landmark
pipeline, streams protobuf frames to SignBridge, and renders the recognized text
inside a chat-style input surface.

It also includes a phase 1 Text/Speech-to-Sign preview path:

- `SignSynthesisHttpClient` calls `POST /api/v2/sign/synthesize` and
  `POST /api/v2/speech/sign`.
- `SignSynthesisPreview` replays the returned `SignPlan + landmark motion` as an
  SVG landmark preview.

## Project Architecture

The shared runtime architecture and SignGemma-compatible demo flow are documented in [PROJECT_ARCHITECTURE.md](../../../docs/backend/PROJECT_ARCHITECTURE.md). The Spring Boot + cross-platform app demo guide is [SIGN_GEMMA_APP_DEMO.md](../../../docs/backend/SIGN_GEMMA_APP_DEMO.md). Korean versions are available in [PROJECT_ARCHITECTURE_KO.md](../../../docs/backend/PROJECT_ARCHITECTURE_KO.md) and [SIGN_GEMMA_APP_DEMO_KO.md](../../../docs/backend/SIGN_GEMMA_APP_DEMO_KO.md).

## Run

```bash
npm install
npm run dev
```

The default local backend endpoint is `ws://127.0.0.1:8080/ws/sign`.
The default synthesis HTTP endpoint is `http://127.0.0.1:8080`.

## Branding

- Product: LinguaSign
- Platform/backend: SignBridge
- SDK concept: SignInputKit
- Current protobuf namespace: `mj.sign` until the planned breaking namespace
  migration.
