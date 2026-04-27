#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/docker-compose.stack.http.yml}"
KEEP_STACK="${KEEP_STACK:-0}"

cleanup() {
  if [ "$KEEP_STACK" = "1" ]; then
    echo "Leaving stack running because KEEP_STACK=1"
    return
  fi
  docker compose -f "$COMPOSE_FILE" down -v >/dev/null 2>&1 || true
}

trap cleanup EXIT

on_error() {
  echo
  echo "Verification failed. Recent bridge logs:"
  docker compose -f "$COMPOSE_FILE" logs --tail=80 bridge || true
  echo
  echo "Recent mock GPU logs:"
  docker compose -f "$COMPOSE_FILE" logs --tail=80 mock-gpu || true
}

trap on_error INT TERM HUP

docker compose -f "$COMPOSE_FILE" up -d

echo "Waiting for bridge health endpoint..."
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8080/internal/healthz >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "Checking bridge readiness..."
curl -fsS http://127.0.0.1:8080/internal/readyz
echo

echo "Checking mock GPU profile registry..."
curl -fsS http://127.0.0.1:8000/health | grep -q '"model_profile":"sign-gemma"'

echo "Sending English/ASL WebSocket protobuf probe through HTTP provider..."
python3 "$ROOT_DIR/scripts/send_websocket_probe.py" \
  --url 'ws://127.0.0.1:8080/ws/sign?locale=en-US&sign_language=asl&model_profile=sign-gemma&protocol_version=signbridge-model-v1' \
  --session-id "asl-profile-probe" \
  --expect-json-field locale=en-US \
  --expect-json-field sign_language=asl \
  --expect-json-field model_profile=sign-gemma \
  --expect-json-field protocol_version=signbridge-model-v1

echo "Metrics snapshot:"
METRICS="$(curl -fsS http://127.0.0.1:8080/internal/metrics)"
printf '%s\n' "$METRICS"

printf '%s\n' "$METRICS" | grep -q '"completed_inferences"[[:space:]]*:[[:space:]]*[1-9]'

echo
echo "English/ASL sign-gemma profile verification passed."
