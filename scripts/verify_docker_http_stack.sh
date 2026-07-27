#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-sample/backend/docker-compose.stack.http.yml}"
BRIDGE_BASE_URL="${BRIDGE_BASE_URL:-http://127.0.0.1:8080}"
WS_URL="${WS_URL:-ws://127.0.0.1:8080/ws/sign}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-90}"
KEEP_STACK="${KEEP_STACK:-0}"
PYTHON_BIN="${PYTHON_BIN:-$ROOT_DIR/sample/backend/model_server/.venv/bin/python}"

if [ ! -x "$PYTHON_BIN" ]; then
  PYTHON_BIN="python3"
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command is not available. Install Docker Desktop or Docker Engine, then rerun this script." >&2
  exit 127
fi

cd "$ROOT_DIR"

docker compose version >/dev/null

cleanup() {
  status="$?"
  if [ "$KEEP_STACK" != "1" ]; then
    docker compose -f "$COMPOSE_FILE" down --remove-orphans >/dev/null 2>&1 || true
  fi
  exit "$status"
}
trap cleanup EXIT INT TERM

echo "Starting $COMPOSE_FILE"
docker compose -f "$COMPOSE_FILE" up -d --build

deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
while :; do
  if curl -fsS "$BRIDGE_BASE_URL/internal/healthz" >/dev/null 2>&1 \
    && curl -fsS "$BRIDGE_BASE_URL/internal/readyz" >/dev/null 2>&1; then
    break
  fi

  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "Bridge did not become ready within ${TIMEOUT_SECONDS}s" >&2
    docker compose -f "$COMPOSE_FILE" ps >&2
    docker compose -f "$COMPOSE_FILE" logs --tail=120 >&2
    exit 1
  fi

  sleep 2
done

echo "Checking profile discovery"
curl -fsS "$BRIDGE_BASE_URL/api/v2/model-profiles" >/dev/null

echo "Checking text-to-sign synthesis"
curl -fsS \
  -H "Content-Type: application/json" \
  -d '{"session_id":"docker-http-t2s","text":"hello","locale":"en-US","sign_language":"asl","model_profile":"sign-gemma","protocol_version":"v1"}' \
  "$BRIDGE_BASE_URL/api/v2/sign/synthesize" >/dev/null

echo "Checking WebSocket protobuf streaming"
if "$PYTHON_BIN" "$ROOT_DIR/scripts/send_websocket_probe.py" \
  --url "$WS_URL" \
  --session-id "docker-http-ws" \
  --expect-json-field "event_type=result" >/dev/null; then
  echo "Docker HTTP stack verification passed"
else
  echo "WebSocket probe failed. If this is a host Python protobuf issue, run:" >&2
  echo "  python3 -m pip install -r sample/backend/model_server/requirements.txt" >&2
  docker compose -f "$COMPOSE_FILE" logs --tail=120 >&2
  exit 1
fi

echo "Checking WebSocket stream protocol v2 ACK/EOS"
"$PYTHON_BIN" "$ROOT_DIR/scripts/send_websocket_probe.py" \
  --url "$WS_URL" \
  --stream-v2 \
  --session-id "docker-http-ws-v2" \
  --expect-json-field "event_type=result" >/dev/null
