#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
BRIDGE_BASE_URL="${BRIDGE_BASE_URL:-http://127.0.0.1:8080}"
MOCK_PORT="${MOCK_PORT:-8000}"
MOCK_BASE_URL="${MOCK_BASE_URL:-http://127.0.0.1:$MOCK_PORT}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-90}"
SKIP_START="${SKIP_START:-0}"
MOCK_VENV_DIR="${MOCK_VENV_DIR:-$ROOT_DIR/sample/backend/model_server/.venv}"
PYTHON_BIN="${PYTHON_BIN:-}"

MOCK_PID=""
BRIDGE_PID=""

cleanup() {
  status="$?"
  if [ "$SKIP_START" != "1" ]; then
    if [ -n "$BRIDGE_PID" ]; then
      kill "$BRIDGE_PID" >/dev/null 2>&1 || true
    fi
    if [ -n "$MOCK_PID" ]; then
      kill "$MOCK_PID" >/dev/null 2>&1 || true
    fi
  fi
  exit "$status"
}
trap cleanup EXIT INT TERM

wait_for() {
  url="$1"
  label="$2"
  deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
  while :; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return
    fi
    if [ "$(date +%s)" -ge "$deadline" ]; then
      echo "$label did not become available within ${TIMEOUT_SECONDS}s" >&2
      if [ -f /tmp/mj_sign_mock_smoke.log ]; then
        echo "Recent mock log:" >&2
        tail -n 80 /tmp/mj_sign_mock_smoke.log >&2 || true
      fi
      if [ -f /tmp/mj_sign_bridge_smoke.log ]; then
        echo "Recent bridge log:" >&2
        tail -n 120 /tmp/mj_sign_bridge_smoke.log >&2 || true
      fi
      exit 1
    fi
    sleep 2
  done
}

is_up() {
  curl -fsS "$1" >/dev/null 2>&1
}

mock_python() {
  if [ -n "$PYTHON_BIN" ]; then
    printf '%s\n' "$PYTHON_BIN"
    return
  fi
  if [ -x "$MOCK_VENV_DIR/bin/python" ]; then
    printf '%s\n' "$MOCK_VENV_DIR/bin/python"
    return
  fi
  printf '%s\n' "python3"
}

check_mock_python_runtime() {
  python_bin="$(mock_python)"
  if (
    cd "$ROOT_DIR/sample/backend/model_server"
    "$python_bin" -c "from schema import landmark_pb2" >/dev/null 2>&1
  ); then
    return
  fi

  echo "Python protobuf runtime is not compatible with the generated mock schema." >&2
  echo "Create the project-local mock venv first:" >&2
  echo "  ./scripts/setup_mock_venv.sh" >&2
  echo "Or set PYTHON_BIN to a Python that has sample/backend/model_server/requirements.txt installed." >&2
  echo "Or run the Docker stack verification instead:" >&2
  echo "  ./scripts/verify_docker_http_stack.sh" >&2
  exit 127
}

if [ "$SKIP_START" != "1" ]; then
  if is_up "$MOCK_BASE_URL/ready"; then
    echo "Using existing mock model server at $MOCK_BASE_URL"
  else
    check_mock_python_runtime
    echo "Starting mock model server..."
    (
      cd "$ROOT_DIR/sample/backend/model_server"
      PORT="$MOCK_PORT" "$(mock_python)" main.py
    ) >/tmp/mj_sign_mock_smoke.log 2>&1 &
    MOCK_PID="$!"

    wait_for "$MOCK_BASE_URL/ready" "mock model server"
  fi

  if is_up "$BRIDGE_BASE_URL/internal/healthz"; then
    echo "Using existing Spring bridge at $BRIDGE_BASE_URL"
  else
    echo "Starting Spring bridge..."
    (
      cd "$ROOT_DIR/sign/backend/bridge"
      ./gradlew bootRun --args="--sign.gpu.base-url=$MOCK_BASE_URL"
    ) >/tmp/mj_sign_bridge_smoke.log 2>&1 &
    BRIDGE_PID="$!"
  fi
fi

wait_for "$BRIDGE_BASE_URL/internal/healthz" "Spring bridge"

echo "Checking readiness..."
curl -fsS "$BRIDGE_BASE_URL/internal/readyz" >/dev/null

echo "Checking model profile discovery..."
curl -fsS "$BRIDGE_BASE_URL/api/v2/model-profiles" \
  | grep -q '"model_profile"[[:space:]]*:[[:space:]]*"sign-gemma"'

echo "Checking OpenAPI contract..."
OPENAPI="$(curl -fsS "$BRIDGE_BASE_URL/v3/api-docs")"
printf '%s\n' "$OPENAPI" | grep -q '/api/v2/model-profiles'
printf '%s\n' "$OPENAPI" | grep -q '/api/v2/sign/synthesize'
printf '%s\n' "$OPENAPI" | grep -q '/internal/readyz'
printf '%s\n' "$OPENAPI" | grep -q '/internal/metrics.prometheus'

echo "Checking Swagger UI..."
curl -fsS "$BRIDGE_BASE_URL/swagger-ui.html" >/dev/null

echo "Checking Prometheus metrics..."
curl -fsS "$BRIDGE_BASE_URL/internal/metrics.prometheus" \
  | grep -q 'signbridge_received_messages_total'

echo "Checking text-to-sign synthesis..."
curl -fsS \
  -H "Content-Type: application/json" \
  -d '{"session_id":"spring-smoke-t2s","text":"hello","locale":"en-US","sign_language":"asl","model_profile":"sign-gemma","protocol_version":"signbridge-synthesis-v1"}' \
  "$BRIDGE_BASE_URL/api/v2/sign/synthesize" \
  | grep -q '"event_type"[[:space:]]*:[[:space:]]*"synthesis_result"'

echo "Spring OpenAPI smoke verification passed."
