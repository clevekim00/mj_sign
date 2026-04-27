import os
import base64
import random
import asyncio
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from logger_config import logger # Assuming a logger setup

# The generated protobuf module
try:
    from schema import landmark_pb2
except ImportError:
    import landmark_pb2 # Fallback if path differs
from schema.landmark_pb2 import ClientStreamChunk
from logger_config import logger
from profile_registry import profile_registry
from sign_gemma_model import engine_registry

app = FastAPI(title="Sign-Gemma Inference Server")
# Configuration
USE_REAL_MODEL = os.getenv("USE_REAL_MODEL", "false").lower() == "true"
PRELOAD_PROFILES = [
    profile.strip()
    for profile in os.getenv("SIGN_GEMMA_PRELOAD_PROFILES", "").split(",")
    if profile.strip()
]

class InferenceRequest(BaseModel):
    session_id: str
    protobuf_b64: str
    frame_count: int | None = None
    transport: str | None = None
    client_schema_version: str | None = None
    protocol_version: str = "mj-sign-model-v1"
    locale: str = "ko-KR"
    sign_language: str = "ksl"
    model_profile: str = "sign-gemma-ko"

@app.on_event("startup")
async def startup_event():
    if USE_REAL_MODEL:
        profiles = PRELOAD_PROFILES or [profile_registry.default_profile]
        logger.info("Starting in REAL mode. Preloading profiles: %s", profiles)
        for profile_name in profiles:
            profile = profile_registry.get(profile_name)
            engine_registry.load_profile(profile)
    else:
        logger.info(
            "Starting in MOCK mode. Available profiles: %s",
            [profile.model_profile for profile in profile_registry.all()],
        )

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "mode": "real" if USE_REAL_MODEL else "mock",
        "default_profile": profile_registry.default_profile,
        "loaded_profiles": engine_registry.loaded_profiles(),
        "profiles": [
            profile.metadata(loaded=engine_registry.is_loaded(profile.model_profile))
            for profile in profile_registry.all()
        ],
    }

@app.get("/ready")
def readiness_check():
    default_profile = profile_registry.get(profile_registry.default_profile)
    loaded = engine_registry.is_loaded(default_profile.model_profile)
    ready = not USE_REAL_MODEL or loaded
    body = {
        "status": "ready" if ready else "not_ready",
        "mode": "real" if USE_REAL_MODEL else "mock",
        "default_profile": default_profile.model_profile,
        "loaded_profiles": engine_registry.loaded_profiles(),
        "profiles": [
            profile.metadata(loaded=engine_registry.is_loaded(profile.model_profile))
            for profile in profile_registry.all()
        ],
    }
    return JSONResponse(status_code=200 if ready else 503, content=body)

@app.post("/api/v2/recognize")
async def recognize_sign(req: InferenceRequest):
    try:
        profile = profile_registry.resolve(req.model_profile, req.locale, req.sign_language)
        # Decode the base64 protobuf string
        decoded_bytes = base64.b64decode(req.protobuf_b64)
        chunk = landmark_pb2.ClientStreamChunk()
        chunk.ParseFromString(decoded_bytes)
        
        frame_count = len(chunk.frames)
        logger.info(
            "Processing session %s with %s frames locale=%s sign_language=%s model_profile=%s",
            req.session_id,
            frame_count,
            req.locale,
            req.sign_language,
            profile.model_profile,
        )

        if frame_count == 0:
            raise HTTPException(status_code=400, detail="No frames provided")

        # Simulate processing delay
        delay = random.uniform(0.1, 0.4)
        await asyncio.sleep(delay)

        if USE_REAL_MODEL:
            # Landmark-to-keyword decoding is model-specific. Until a trained
            # visual SignGemma checkpoint is attached, the profile supplies a
            # keyword hint so the serving contract and profile routing can be
            # verified end to end.
            prompt = profile.prompt_for_keywords(profile.keyword_hint())
            
            result_text = engine_registry.generate(profile, prompt)
            logger.info("SignGemma profile %s inference result: %s", profile.model_profile, result_text)
            
            return {
                "session_id": chunk.session_id,
                "text": result_text,
                "is_final": True,
                "confidence": 0.95,
                "processing_time_ms": int(delay * 1000),
                "model_version": profile.model_version,
                "protocol_version": req.protocol_version or profile.protocol_version,
                "locale": req.locale or profile.locale,
                "sign_language": req.sign_language or profile.sign_language,
                "model_profile": profile.model_profile,
            }
        
        # Mock Response Mode
        sentences = profile.mock_sentences or ("No mock sentence configured.",)
        return {
            "session_id": req.session_id,
            "text": random.choice(sentences),
            "is_final": True,
            "confidence": round(random.uniform(0.85, 0.99), 2),
            "processing_time_ms": int(delay * 1000),
            "model_version": profile.model_version,
            "protocol_version": req.protocol_version or profile.protocol_version,
            "locale": req.locale or profile.locale,
            "sign_language": req.sign_language or profile.sign_language,
            "model_profile": profile.model_profile,
        }

    except Exception as e:
        logger.error(f"Inference error: {str(e)}")
        return {"error": f"Internal Server Error: {str(e)}"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", 8000)))
