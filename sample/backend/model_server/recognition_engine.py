from __future__ import annotations

import importlib
import os
import random
from pathlib import Path
from typing import Any, Protocol


class RecognitionEngine(Protocol):
    @property
    def ready(self) -> bool: ...

    @property
    def metadata(self) -> dict[str, Any]: ...

    def recognize(self, chunk: Any, profile: Any) -> tuple[str, float]: ...


class MockRecognitionEngine:
    @property
    def ready(self) -> bool:
        return True

    @property
    def metadata(self) -> dict[str, Any]:
        return {"engine": "mock", "checkpoint": None}

    def recognize(self, chunk: Any, profile: Any) -> tuple[str, float]:
        sentences = profile.mock_sentences or ("No mock sentence configured.",)
        return random.choice(sentences), round(random.uniform(0.85, 0.99), 2)


class PluggableCheckpointRecognitionEngine:
    """Loads a project-external landmark recognition engine factory.

    The factory is configured as ``module:function`` and receives the checkpoint
    path. Model weights stay outside the repository and are mounted at runtime.
    """

    def __init__(self, checkpoint_path: str | None, factory_path: str | None):
        self.checkpoint_path = Path(checkpoint_path).expanduser() if checkpoint_path else None
        self.factory_path = factory_path
        self._delegate: Any | None = None
        self._error: str | None = None
        self._load()

    def _load(self) -> None:
        if self.checkpoint_path is None or not self.checkpoint_path.exists():
            self._error = "SIGN_RECOGNITION_CHECKPOINT_PATH is missing or does not exist"
            return
        if not self.factory_path or ":" not in self.factory_path:
            self._error = "SIGN_RECOGNITION_ENGINE_FACTORY must be configured as module:function"
            return
        try:
            module_name, function_name = self.factory_path.split(":", 1)
            factory = getattr(importlib.import_module(module_name), function_name)
            self._delegate = factory(str(self.checkpoint_path))
        except Exception as error:  # pragma: no cover - provider-specific
            self._error = f"failed to load recognition engine: {error}"

    @property
    def ready(self) -> bool:
        return self._delegate is not None

    @property
    def metadata(self) -> dict[str, Any]:
        return {
            "engine": "checkpoint",
            "checkpoint": str(self.checkpoint_path) if self.checkpoint_path else None,
            "factory": self.factory_path,
            "error": self._error,
        }

    def recognize(self, chunk: Any, profile: Any) -> tuple[str, float]:
        if self._delegate is None:
            raise RuntimeError(self._error or "recognition engine is not ready")
        result = self._delegate.recognize(chunk=chunk, profile=profile)
        if not isinstance(result, tuple) or len(result) != 2:
            raise RuntimeError("recognition engine must return (text, confidence)")
        return str(result[0]), float(result[1])


def build_recognition_engine(real_mode: bool) -> RecognitionEngine:
    if not real_mode:
        return MockRecognitionEngine()
    return PluggableCheckpointRecognitionEngine(
        os.getenv("SIGN_RECOGNITION_CHECKPOINT_PATH"),
        os.getenv("SIGN_RECOGNITION_ENGINE_FACTORY"),
    )
