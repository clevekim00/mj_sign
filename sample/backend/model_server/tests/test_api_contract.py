import asyncio
import base64
import sys
import unittest
from pathlib import Path

from fastapi import HTTPException

MODEL_SERVER_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(MODEL_SERVER_ROOT))

import main  # noqa: E402
from schema import landmark_pb2  # noqa: E402


class ApiContractTest(unittest.TestCase):
    def test_real_model_flag_precedence_and_legacy_fallback(self):
        self.assertTrue(main.real_model_enabled({"SIGN_USE_REAL_MODEL": "true"}))
        self.assertTrue(main.real_model_enabled({"USE_REAL_MODEL": "true"}))
        self.assertFalse(main.real_model_enabled({
            "SIGN_USE_REAL_MODEL": "false", "USE_REAL_MODEL": "true",
        }))
        self.assertFalse(main.real_model_enabled({}))

    def test_rejects_invalid_base64(self):
        request = main.InferenceRequest(
            session_id="session-1",
            protobuf_b64="not-valid-base64!",
            frame_count=1,
        )

        with self.assertRaises(HTTPException) as context:
            asyncio.run(main.recognize_sign(request))

        self.assertEqual(400, context.exception.status_code)

    def test_rejects_envelope_and_protobuf_session_mismatch(self):
        chunk = landmark_pb2.ClientStreamChunk(session_id="protobuf-session")
        chunk.frames.add(timestamp_ms=1)
        request = main.InferenceRequest(
            session_id="envelope-session",
            protobuf_b64=base64.b64encode(chunk.SerializeToString()).decode("ascii"),
            frame_count=1,
        )

        with self.assertRaises(HTTPException) as context:
            asyncio.run(main.recognize_sign(request))

        self.assertEqual(400, context.exception.status_code)
        self.assertIn("session_id mismatch", context.exception.detail)

    def test_mock_engine_returns_protocol_metadata(self):
        chunk = landmark_pb2.ClientStreamChunk(session_id="session-ok")
        chunk.frames.add(timestamp_ms=1)
        request = main.InferenceRequest(
            session_id="session-ok",
            protobuf_b64=base64.b64encode(chunk.SerializeToString()).decode("ascii"),
            frame_count=1,
        )

        response = asyncio.run(main.recognize_sign(request))

        self.assertEqual("session-ok", response["session_id"])
        self.assertEqual("signbridge-model-v1", response["protocol_version"])
        self.assertGreaterEqual(response["confidence"], 0.0)
        self.assertLessEqual(response["confidence"], 1.0)


if __name__ == "__main__":
    unittest.main()
