import os
import base64
import random
import asyncio
import time
import binascii
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
from profile_registry import DEFAULT_PROTOCOL_VERSION, normalize_protocol_version, profile_registry
from recognition_engine import build_recognition_engine

app = FastAPI(title="Sign-Gemma Inference Server")
# Configuration
USE_REAL_MODEL = os.getenv("USE_REAL_MODEL", "false").lower() == "true"
recognition_engine = build_recognition_engine(USE_REAL_MODEL)
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
    protocol_version: str = DEFAULT_PROTOCOL_VERSION
    locale: str = "ko-KR"
    sign_language: str = "ksl"
    model_profile: str = "sign-gemma-ko"

@app.on_event("startup")
async def startup_event():
    logger.info(
        "Starting with recognition engine metadata=%s. Available profiles: %s",
        recognition_engine.metadata,
        [profile.model_profile for profile in profile_registry.all()],
    )

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "mode": "real" if USE_REAL_MODEL else "mock",
        "default_profile": profile_registry.default_profile,
        "loaded_profiles": (
            [profile.model_profile for profile in profile_registry.all()]
            if recognition_engine.ready
            else []
        ),
        "profiles": [
            profile.metadata(loaded=recognition_engine.ready)
            for profile in profile_registry.all()
        ],
        "recognition_engine": recognition_engine.metadata,
    }

@app.get("/ready")
def readiness_check():
    default_profile = profile_registry.get(profile_registry.default_profile)
    ready = recognition_engine.ready
    body = {
        "status": "ready" if ready else "not_ready",
        "mode": "real" if USE_REAL_MODEL else "mock",
        "default_profile": default_profile.model_profile,
        "loaded_profiles": (
            [profile.model_profile for profile in profile_registry.all()]
            if recognition_engine.ready
            else []
        ),
        "profiles": [
            profile.metadata(loaded=recognition_engine.ready)
            for profile in profile_registry.all()
        ],
        "recognition_engine": recognition_engine.metadata,
    }
    return JSONResponse(status_code=200 if ready else 503, content=body)

@app.post("/api/v2/recognize")
async def recognize_sign(req: InferenceRequest):
    started_at = time.monotonic()
    try:
        if not recognition_engine.ready:
            raise HTTPException(status_code=503, detail="Recognition engine is not ready")
        profile = profile_registry.resolve(req.model_profile, req.locale, req.sign_language)
        protocol_version = normalize_protocol_version(req.protocol_version)
        # Decode the base64 protobuf string
        try:
            decoded_bytes = base64.b64decode(req.protobuf_b64, validate=True)
        except (binascii.Error, ValueError) as error:
            raise HTTPException(status_code=400, detail="Invalid base64 protobuf payload") from error
        chunk = landmark_pb2.ClientStreamChunk()
        try:
            chunk.ParseFromString(decoded_bytes)
        except Exception as error:
            raise HTTPException(status_code=400, detail="Invalid protobuf payload") from error
        
        frame_count = len(chunk.frames)
        if chunk.session_id != req.session_id:
            raise HTTPException(status_code=400, detail="Envelope and protobuf session_id mismatch")
        if req.frame_count is not None and req.frame_count != frame_count:
            raise HTTPException(status_code=400, detail="frame_count does not match protobuf payload")
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

        if not USE_REAL_MODEL:
            await asyncio.sleep(random.uniform(0.1, 0.4))
        result_text, confidence = recognition_engine.recognize(chunk, profile)
        return {
            "session_id": req.session_id,
            "text": result_text,
            "is_final": True,
            "confidence": max(0.0, min(1.0, confidence)),
            "processing_time_ms": int((time.monotonic() - started_at) * 1000),
            "model_version": profile.model_version,
            "protocol_version": protocol_version,
            "locale": req.locale or profile.locale,
            "sign_language": req.sign_language or profile.sign_language,
            "model_profile": profile.model_profile,
        }

    except HTTPException:
        raise
    except Exception as error:
        logger.exception("Inference error")
        raise HTTPException(status_code=500, detail="Internal inference error") from error

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", 8000)))
