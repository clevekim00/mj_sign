# SignGemma Research Notes

Last updated: 2026-04-27

Korean version: [SIGN_GEMMA_RESEARCH_KO.md](./SIGN_GEMMA_RESEARCH_KO.md)

## Project Architecture

The shared runtime architecture and SignGemma-compatible demo flow are documented in [PROJECT_ARCHITECTURE.md](../backend/PROJECT_ARCHITECTURE.md). The Spring Boot + cross-platform app demo guide is [SIGN_GEMMA_APP_DEMO.md](../backend/SIGN_GEMMA_APP_DEMO.md). Korean versions are available in [PROJECT_ARCHITECTURE_KO.md](../backend/PROJECT_ARCHITECTURE_KO.md) and [SIGN_GEMMA_APP_DEMO_KO.md](../backend/SIGN_GEMMA_APP_DEMO_KO.md).

## Summary

Publicly verifiable SignGemma information is still limited. The strongest public
signal is the Google DeepMind announcement reported around Google I/O 2025:
SignGemma was described as an upcoming open Gemma-family model for translating
sign language into text, with the strongest initial focus on ASL to English.

As of this review, I could not verify an official Google model card, downloadable
weights page, or exact landmark/input tensor specification for SignGemma from
the public Google Gemma model page. Therefore, SignBridge treats SignGemma as a
profile-compatible serving target for LinguaSign and keeps the model adapter
contract stable until official weights/specs are available.

## What Is Publicly Reported

- Google DeepMind's Gemma page positions Gemma as an open model family and lists
  official Gemma variants and integrations, but the public page reviewed here
  does not expose a SignGemma model card or exact downloadable SignGemma
  artifact.
- Gadgets360 reported the Google DeepMind X announcement and described
  SignGemma as an upcoming Gemma-family open model for translating sign language
  into spoken/written text, with best performance reported for ASL to English.
- Slator also reported that SignGemma was announced at I/O 2025, intended for
  real-time sign-language-to-text translation, designed for on-device use, and
  expected to be publicly available later.
- Google's Keras Gemma documentation is relevant for the current mock engine
  because it documents Gemma support in KerasNLP and LoRA fine-tuning workflows.
- Google's Gemma 3 developer guide is relevant background because it documents
  the broader Gemma direction toward multimodal models, but it is not a
  SignGemma-specific model card.

Sources:

- [Google DeepMind Gemma page](https://deepmind.google/models/gemma/)
- [Gadgets360 SignGemma report](https://www.gadgets360.com/ai/news/google-signgemma-ai-model-translate-sign-language-to-spoken-text-unveiled-8537400)
- [Slator SignGemma report](https://slator.com/google-invites-feedback-for-signgemma-a-new-ai-sign-language-translation-model/)
- [Google Developers Blog: Gemma models in Keras](https://developers.googleblog.com/en/introducing-gemma-models-in-keras/)
- [Google Developers Blog: Gemma 3 developer guide](https://developers.googleblog.com/introducing-gemma3/)

## What Is Not Publicly Confirmed

The following items should not be treated as confirmed SignGemma facts until an
official model card or repository is available:

- Exact model size and architecture.
- Exact checkpoint names or download URLs.
- Required video frame resolution.
- Whether official inference takes raw video, image sequences, landmarks, or a
  hybrid representation.
- Exact supported landmark schema.
- Exact latency, accuracy, and benchmark numbers.
- Exact list of sign languages beyond the reported ASL/English focus.

## Landmark Support

### Official SignGemma

I could not verify an official SignGemma landmark schema. Public reporting
mentions sign-language translation and visual understanding, but does not define
whether SignGemma consumes MediaPipe landmarks, raw video, or another vision
representation.

### SignBridge Project Contract

SignBridge currently standardizes on a MediaPipe-style protobuf landmark contract:

```proto
message LandmarkFrame {
  int64 timestamp_ms = 1;
  repeated Point3D left_hand = 2;
  repeated Point3D right_hand = 3;
  repeated Point3D pose = 4;
  repeated Point3D face_contour = 5;
}
```

Supported project landmarks:

| Field | Expected shape | Notes |
| --- | --- | --- |
| `left_hand` | up to 21 points, 3D | MediaPipe-style left hand landmarks |
| `right_hand` | up to 21 points, 3D | MediaPipe-style right hand landmarks |
| `pose` | selected upper-body 3D points | Shoulders, elbows, torso context, etc. |
| `face_contour` | selected lip/jaw/face 3D points | Non-manual markers and mouth/face context |

MediaPipe reference:

- [Google AI Edge Holistic Landmarker](https://ai.google.dev/edge/mediapipe/solutions/vision/holistic_landmarker) describes a holistic landmarker that outputs pose, face, and 21 hand landmarks per hand.
- [MediaPipe Hands reference](https://chuoling.github.io/mediapipe/solutions/hands.html) describes hand landmark output as 21 landmarks per detected hand, with x/y/z coordinates.

## Implementation Decision for This Repository

Until official SignGemma weights/specs are available, SignBridge treats
`model_profile=sign-gemma` as an English/ASL-compatible serving profile:

- `locale`: `en-US`
- `sign_language`: `asl`
- `model_profile`: `sign-gemma`
- `protocol_version`: `signbridge-model-v1` (`mj-sign-model-v1` remains a legacy alias)
- input transport: `protobuf-b64`
- input schema: `mj.sign.ClientStreamChunk`
- landmark schema: `left_hand`, `right_hand`, `pose`, `face_contour`

This lets the bridge, queue, serializer/converter, health/readiness, and client
language routing be validated now, while leaving the actual visual SignGemma
checkpoint swappable later.

## Current Code Support

Implemented now:

- English/ASL profile registry in `sample/backend/model_server/profile_registry.py`.
- Profile-specific Keras/Gemma engine cache in `sample/backend/model_server/sign_gemma_model.py`.
- Profile-aware `/health` and `/ready` endpoints in `sample/backend/model_server/main.py`.
- English/ASL WebSocket verification script:
  `scripts/verify_english_asl_profile.sh`.
- WebSocket result events echo `locale`, `sign_language`, `model_profile`, and
  `protocol_version` for integration verification.

Not implemented yet:

- Real raw-video or landmark-to-sign SignGemma checkpoint inference.
- Official SignGemma model card validation.
- ASL dataset evaluation.
- Landmark normalization specific to an official SignGemma model.

## Recommended Next Step

When official SignGemma artifacts become available, add a new profile entry with
the official model id and LoRA/checkpoint path, then replace the temporary
keyword-hint path with real landmark/video feature extraction while preserving
the same BE-model envelope.
