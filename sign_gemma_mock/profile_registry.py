import os
from dataclasses import dataclass, field


SUPPORTED_LANDMARKS = [
    "left_hand:21x3",
    "right_hand:21x3",
    "pose:variable_upper_body_3d",
    "face_contour:variable_lip_jaw_3d",
]


@dataclass(frozen=True)
class SignGemmaProfile:
    model_profile: str
    model_family: str
    model_version: str
    model_id: str
    locale: str
    sign_language: str
    output_language: str
    output_mode: str = "sentence"
    input_schema: str = "mj.sign.ClientStreamChunk"
    input_schema_version: str = "v1"
    protocol_version: str = "mj-sign-model-v1"
    transport: str = "protobuf-b64"
    min_frames: int = 8
    max_frames: int = 24
    recommended_fps: str = "8-12"
    supports_partial: bool = False
    supports_final: bool = True
    lora_weights_path: str | None = None
    mock_sentences: tuple[str, ...] = field(default_factory=tuple)
    mock_keywords: tuple[str, ...] = field(default_factory=tuple)
    supported_landmarks: tuple[str, ...] = tuple(SUPPORTED_LANDMARKS)

    def metadata(self, loaded: bool = False) -> dict:
        return {
            "model_profile": self.model_profile,
            "model_family": self.model_family,
            "model_version": self.model_version,
            "model_id": self.model_id,
            "locale": self.locale,
            "sign_language": self.sign_language,
            "output_language": self.output_language,
            "output_mode": self.output_mode,
            "input_schema": self.input_schema,
            "input_schema_version": self.input_schema_version,
            "protocol_version": self.protocol_version,
            "transport": self.transport,
            "min_frames": self.min_frames,
            "max_frames": self.max_frames,
            "recommended_fps": self.recommended_fps,
            "supports_partial": self.supports_partial,
            "supports_final": self.supports_final,
            "lora_weights_configured": bool(self.lora_weights_path),
            "lora_weights_available": bool(self.lora_weights_path and os.path.exists(self.lora_weights_path)),
            "supported_landmarks": list(self.supported_landmarks),
            "loaded": loaded,
        }

    def keyword_hint(self) -> str:
        if self.mock_keywords:
            return self.mock_keywords[0]
        if self.sign_language == "asl":
            return "you school go"
        return "너 학교 가다"

    def prompt_for_keywords(self, keywords: str) -> str:
        return (
            f"Translate the following {self.sign_language.upper()} sign keywords "
            f"to natural {self.output_language}: {keywords}"
        )


class SignGemmaProfileRegistry:
    def __init__(self) -> None:
        self.default_profile = os.getenv("SIGN_GEMMA_DEFAULT_PROFILE", "sign-gemma-ko")
        self._profiles = {
            "sign-gemma": SignGemmaProfile(
                model_profile="sign-gemma",
                model_family="sign-gemma",
                model_version=os.getenv("SIGN_GEMMA_ASL_VERSION", "sign-gemma-asl-v1"),
                model_id=os.getenv("SIGN_GEMMA_ASL_MODEL_ID", "gemma2_instruct_2b_en"),
                lora_weights_path=os.getenv("SIGN_GEMMA_ASL_LORA_PATH") or None,
                locale="en-US",
                sign_language="asl",
                output_language="English",
                mock_sentences=(
                    "I eat rice.",
                    "You are going to school.",
                    "What does that sign mean?",
                    "The weather is nice today.",
                    "Nice to meet you.",
                ),
                mock_keywords=("you school go", "nice meet you"),
            ),
            "sign-gemma-ko": SignGemmaProfile(
                model_profile="sign-gemma-ko",
                model_family="sign-gemma",
                model_version=os.getenv("SIGN_GEMMA_KSL_VERSION", "sign-gemma-ksl-v1"),
                model_id=os.getenv("SIGN_GEMMA_KSL_MODEL_ID", "gemma2_instruct_2b_en"),
                lora_weights_path=os.getenv("SIGN_GEMMA_KSL_LORA_PATH") or None,
                locale="ko-KR",
                sign_language="ksl",
                output_language="Korean",
                mock_sentences=(
                    "나 밥 먹다",
                    "너 학교 가다",
                    "그 수어 무엇 입니까",
                    "오늘 날씨 좋다",
                    "만나다 반갑다",
                ),
                mock_keywords=("너 학교 가다", "만나다 반갑다"),
            ),
        }

    def get(self, model_profile: str | None) -> SignGemmaProfile:
        if model_profile and model_profile in self._profiles:
            return self._profiles[model_profile]
        return self._profiles.get(self.default_profile, next(iter(self._profiles.values())))

    def resolve(self, model_profile: str | None, locale: str | None, sign_language: str | None) -> SignGemmaProfile:
        if model_profile and model_profile in self._profiles:
            return self._profiles[model_profile]

        normalized_sign_language = (sign_language or "").lower()
        for profile in self._profiles.values():
            if profile.sign_language == normalized_sign_language:
                return profile

        normalized_locale = (locale or "").lower()
        for profile in self._profiles.values():
            if normalized_locale.startswith(profile.locale.split("-")[0].lower()):
                return profile

        return self.get(model_profile)

    def all(self) -> list[SignGemmaProfile]:
        return list(self._profiles.values())


profile_registry = SignGemmaProfileRegistry()
