#!/usr/bin/env python3
import argparse
import base64
import json
import os
import sys
import time
import urllib.error
import urllib.request
from typing import Any

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "sample", "backend", "model_server"))

try:
    from schema import landmark_pb2  # type: ignore
except Exception as error:  # pragma: no cover - exact protobuf error type differs by runtime
    landmark_pb2 = None
    LANDMARK_IMPORT_ERROR = error
else:
    LANDMARK_IMPORT_ERROR = None


REQUIRED_FIELDS = (
    "id",
    "task",
    "session_id",
    "locale",
    "sign_language",
    "model_profile",
    "protocol_version",
    "frame_count",
)


def build_chunk(session_id: str, frame_count: int) -> bytes:
    if landmark_pb2 is None:
        raise RuntimeError(
            "Python protobuf schema could not be imported. "
            "Run `python3 -m pip install -r sample/backend/model_server/requirements.txt` "
            f"before using --model-url. Original error: {LANDMARK_IMPORT_ERROR}"
        )
    chunk = landmark_pb2.ClientStreamChunk()
    chunk.session_id = session_id
    now_ms = int(time.time() * 1000)
    for index in range(frame_count):
        frame = chunk.frames.add()
        frame.timestamp_ms = now_ms + index
        left = frame.left_hand.add()
        left.x = 0.25 + (index * 0.01)
        left.y = 0.35
        left.z = 0.1
        right = frame.right_hand.add()
        right.x = 0.75 - (index * 0.01)
        right.y = 0.35
        right.z = 0.1
    return chunk.SerializeToString()


def load_fixtures(path: str) -> dict[str, Any]:
    with open(path, "r", encoding="utf-8") as handle:
        data = json.load(handle)
    if data.get("schema_version") != "signbridge-eval-fixtures-v1":
        raise ValueError("Unsupported fixture schema_version.")
    fixtures = data.get("fixtures")
    if not isinstance(fixtures, list) or not fixtures:
        raise ValueError("Fixture file must contain a non-empty fixtures list.")
    return data


def validate_fixture(fixture: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    for field in REQUIRED_FIELDS:
        if field not in fixture:
            errors.append(f"missing field: {field}")
    if fixture.get("task") != "s2t":
        errors.append("only task=s2t is supported by this runner")
    frame_count = fixture.get("frame_count")
    if not isinstance(frame_count, int) or frame_count <= 0:
        errors.append("frame_count must be a positive integer")
    return errors


def request_for_fixture(fixture: dict[str, Any]) -> dict[str, Any]:
    chunk = build_chunk(fixture["session_id"], fixture["frame_count"])
    return {
        "session_id": fixture["session_id"],
        "protobuf_b64": base64.b64encode(chunk).decode("ascii"),
        "frame_count": fixture["frame_count"],
        "transport": "protobuf-b64",
        "client_schema_version": "v1",
        "protocol_version": fixture["protocol_version"],
        "locale": fixture["locale"],
        "sign_language": fixture["sign_language"],
        "model_profile": fixture["model_profile"],
    }


def post_json(url: str, body: dict[str, Any], timeout: float) -> dict[str, Any]:
    payload = json.dumps(body).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def evaluate_response(fixture: dict[str, Any], response: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    for field in ("session_id", "protocol_version", "locale", "sign_language", "model_profile"):
        expected = str(fixture[field])
        actual = str(response.get(field, ""))
        if actual and actual != expected:
            errors.append(f"{field} expected {expected!r}, got {actual!r}")
    if response.get("error"):
        errors.append(f"model returned error: {response['error']}")
    text = str(response.get("text", "")).strip()
    if not text:
        errors.append("response text is empty")
    expected_any = fixture.get("expected_any_text") or []
    if expected_any and text not in expected_any:
        errors.append(f"text {text!r} not in expected_any_text")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--fixtures",
        default=os.path.join(
            ROOT,
            "sign",
            "common",
            "eval",
            "fixtures",
            "signbridge_eval_fixtures.json",
        ),
    )
    parser.add_argument("--model-url", help="Optional model endpoint, e.g. http://127.0.0.1:8001/api/v2/recognize")
    parser.add_argument("--timeout-seconds", type=float, default=10)
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON report.")
    args = parser.parse_args()

    data = load_fixtures(args.fixtures)
    results: list[dict[str, Any]] = []
    exit_code = 0

    for fixture in data["fixtures"]:
        validation_errors = validate_fixture(fixture)
        result: dict[str, Any] = {
            "id": fixture.get("id"),
            "task": fixture.get("task"),
            "status": "valid" if not validation_errors else "invalid",
            "errors": validation_errors,
        }
        if validation_errors:
            exit_code = 1
            results.append(result)
            continue

        result["request"] = {
            "session_id": fixture["session_id"],
            "frame_count": fixture["frame_count"],
            "locale": fixture["locale"],
            "sign_language": fixture["sign_language"],
            "model_profile": fixture["model_profile"],
        }

        if args.model_url:
            try:
                request_body = request_for_fixture(fixture)
                response = post_json(args.model_url, request_body, args.timeout_seconds)
                response_errors = evaluate_response(fixture, response)
                result["response"] = response
                result["status"] = "passed" if not response_errors else "failed"
                result["errors"] = response_errors
                if response_errors:
                    exit_code = 1
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, RuntimeError) as error:
                result["status"] = "error"
                result["errors"] = [str(error)]
                exit_code = 1
        else:
            result["status"] = "validated"

        results.append(result)

    report = {
        "schema_version": data["schema_version"],
        "mode": "model" if args.model_url else "offline",
        "count": len(results),
        "passed": sum(1 for result in results if result["status"] in ("validated", "passed")),
        "failed": sum(1 for result in results if result["status"] in ("invalid", "failed", "error")),
        "results": results,
    }

    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print(f"mode={report['mode']} passed={report['passed']} failed={report['failed']} count={report['count']}")
        for result in results:
            print(f"- {result['id']}: {result['status']}")
            for error in result.get("errors", []):
                print(f"  error: {error}")
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
