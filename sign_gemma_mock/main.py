from fastapi import FastAPI, Request
from pydantic import BaseModel
import base64
import random
import asyncio

# The generated protobuf module
from schema import landmark_pb2

app = FastAPI(title="Sign-Gemma Mock Server")
MODEL_VERSION = "sign-gemma-mock-v1"

class InferenceRequest(BaseModel):
    session_id: str
    protobuf_b64: str

# Mock dictionary for translating random session data
mock_sentences = [
    "안녕하세요, 만나서 반갑습니다.",
    "이것은 수어 인식 테스트입니다.",
    "오늘 날씨가 정말 좋네요.",
    "Gemma 3 모델 추론을 시뮬레이션합니다."
]

@app.get("/")
def read_root():
    return {
        "status": "ok",
        "provider": "mock-http",
        "model_version": MODEL_VERSION,
    }

@app.post("/api/v2/recognize")
async def recognize_sign(req: InferenceRequest):
    # Decode the base64 protobuf string
    try:
        decoded_bytes = base64.b64decode(req.protobuf_b64)
        chunk = landmark_pb2.ClientStreamChunk()
        chunk.ParseFromString(decoded_bytes)
    except Exception as e:
        return {"error": f"Failed to parse protobuf: {str(e)}"}

    print(f"Received {len(chunk.frames)} frames for session {req.session_id}")
    
    # Simulate processing delay (GPU Inference Time: 200ms - 800ms)
    delay = random.uniform(0.2, 0.8)
    await asyncio.sleep(delay)

    # Pick a random mock sentence
    text = random.choice(mock_sentences)

    return {
        "session_id": req.session_id,
        "text": text,
        "is_final": True,
        "confidence": round(random.uniform(0.85, 0.99), 2),
        "processing_time_ms": int(delay * 1000),
        "model_version": MODEL_VERSION,
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
