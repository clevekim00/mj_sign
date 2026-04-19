import os
import base64
import random
import asyncio
from fastapi import FastAPI, Request, HTTPException
from pydantic import BaseModel
from logger_config import logger # Assuming a logger setup

# The generated protobuf module
try:
    from schema import landmark_pb2
except ImportError:
    import landmark_pb2 # Fallback if path differs
from schema.landmark_pb2 import ClientStreamChunk
from logger_config import logger
from sign_gemma_model import engine

app = FastAPI(title="Sign-Gemma Inference Server")
# Configuration
MODEL_VERSION = os.getenv("MODEL_VERSION", "sign-gemma-v2-production-ready")
USE_REAL_MODEL = os.getenv("USE_REAL_MODEL", "false").lower() == "true"

class InferenceRequest(BaseModel):
    session_id: str
    protobuf_b64: str

# Mock dictionary for translating random session data
mock_sentences = [
    "나 밥 먹다",
    "너 학교 가다",
    "그 수어 무엇 입니까",
    "오늘 날씨 좋다",
    "만나다 반갑다"
]

@app.on_event("startup")
async def startup_event():
    if USE_REAL_MODEL:
        logger.info(f"Loading real Sign-Gemma model: {MODEL_VERSION}...")
        # TODO: model = load_sign_gemma_model()
    else:
        logger.info(f"Starting in MOCK mode. Model version: {MODEL_VERSION}")

@app.get("/health")
def health_check():
    return {"status": "ok", "model_version": MODEL_VERSION, "mode": "real" if USE_REAL_MODEL else "mock"}

@app.post("/api/v2/recognize")
async def recognize_sign(req: InferenceRequest):
    try:
        # Decode the base64 protobuf string
        decoded_bytes = base64.b64decode(req.protobuf_b64)
        chunk = landmark_pb2.ClientStreamChunk()
        chunk.ParseFromString(decoded_bytes)
        
        frame_count = len(chunk.frames)
        logger.info(f"Processing session {req.session_id} with {frame_count} frames")

        if frame_count == 0:
            raise HTTPException(status_code=400, detail="No frames provided")

        # Simulate processing delay
        delay = random.uniform(0.1, 0.4)
        await asyncio.sleep(delay)

        if USE_REAL_MODEL:
            # In a real scenario, you'd convert landmarks to a prompt for the model
            # For the notebook-based Gemma, we'll construct a prompt describing the action
            # or use it as a refinement step if the mock logic already provides keywords.
            mock_keyword = "너 학교 가다" # Example mock recognition
            prompt = f"Translate the following ASL sign keywords to natural Korean: {mock_keyword}"
            
            result_text = engine.generate(prompt)
            logger.info(f"SignGemma 추론 결과: {result_text}")
            
            return {
                "session_id": chunk.session_id,
                "text": result_text,
                "is_final": True,
                "confidence": 0.95
            }
        
        # Mock Response Mode
        return {
            "session_id": req.session_id,
            "text": random.choice(mock_sentences),
            "is_final": True,
            "confidence": round(random.uniform(0.85, 0.99), 2),
            "processing_time_ms": int(delay * 1000),
            "model_version": MODEL_VERSION,
        }

    except Exception as e:
        logger.error(f"Inference error: {str(e)}")
        return {"error": f"Internal Server Error: {str(e)}"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", 8000)))
