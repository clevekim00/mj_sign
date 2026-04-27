# LinguaSign Web App

This Vite/React sample demonstrates the LinguaSign web experience on top of the
SignBridge backend. It captures camera input, runs a MediaPipe-style landmark
pipeline, streams protobuf frames to SignBridge, and renders the recognized text
inside a chat-style input surface.

## Run

```bash
npm install
npm run dev
```

The default local backend endpoint is `ws://127.0.0.1:8080/ws/sign`.

## Branding

- Product: LinguaSign
- Platform/backend: SignBridge
- SDK concept: SignInputKit
- Current protobuf namespace: `mj.sign` until the planned breaking namespace
  migration.
